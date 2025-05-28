package guru.stefma.mcpmobile.provider.openai

import guru.stefma.mcpmobile.provider.Message
import guru.stefma.mcpmobile.provider.PromptResult
import guru.stefma.mcpmobile.provider.Provider
import guru.stefma.mcpmobile.provider.Tool
import guru.stefma.mcpmobile.provider.ToolCallResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public class OpenAI(
    private val apiKey: String,
    private val model: String
) : Provider {

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private var tools: String? = null

    override fun prepare(tools: List<Tool>) {
        this.tools = tools.joinToString(separator = ",", prefix = "[", postfix = "]") { tool ->
            """
            {
                "type": "function",
                "function": {
                    "name": "${tool.name}",
                    "description": "${tool.description}",
                    "parameters": {
                        "type": "${tool.inputSchema.type}",
                        "properties": ${tool.inputSchema.properties},
                        "required": ${tool.inputSchema.required.toJsonString()}
                    }
                }
            }
            """.trimIndent()
        }
    }

    override fun prompt(messages: List<Message>): List<PromptResult> {
        return runBlocking {
            val urlString = "https://api.openai.com/v1/chat/completions"
            val response = HttpClient(CIO).post(urlString) {
                contentType(ContentType.Application.Json)
                headers {
                    append("Authorization", "Bearer $apiKey")
                    append("Content-Type", "application/json")
                }
                val jsonString = """
                    {
                        "model": "$model",
                        "messages": ${messages.toJsonString(json)}
                        ${if (tools != null) ",\"tools\": $tools" else ""}
                    }
                    """.trimIndent().trim()
                setBody(jsonString)
            }
            val responseBodyAsText = response.bodyAsText()
            val openAIResponse = json.decodeFromString<OpenAIResponse>(responseBodyAsText)
            val tools = openAIResponse.choices.filter { it.message.toolCalls?.isNotEmpty() ?: false }
                .map { choice ->
                    choice.message.toolCalls!!.map { toolCall ->
                        val arguments = json.decodeFromString<Map<String, String>>(toolCall.function.arguments)
                        PromptResult.ToolCall(
                            name = toolCall.function.name,
                            arguments = arguments,
                            rawMessage = json.encodeToString(choice.message)
                        )
                    }
                }
                .flatten()
            val openAIMessage = openAIResponse.choices.filter { it.message.toolCalls?.isEmpty() ?: true }
                .map {
                    PromptResult.Content(
                        text = it.message.content!!,
                    )
                }

            return@runBlocking tools + openAIMessage
        }
    }

    override fun toolCallResultToMessage(toolCall: PromptResult.ToolCall, toolCallResult: ToolCallResult): Message {
        val id = json.decodeFromString<OpenAIMessage>(toolCall.rawMessage).toolCalls!![0].id
        return Message(
            role = Message.Role.TOOL,
            content = """
                {
                    "role": "tool",
                    "tool_call_id": "$id",
                    "content": "${toolCallResult.contents.joinToString(separator = "\n")}"
                }
            """.trimIndent()
        )
    }
}

private fun List<String>?.toJsonString(): String {
    return this?.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" } ?: "[]"
}

private fun List<Message>.toJsonString(json: Json): String {
    return joinToString(separator = ",", prefix = "[", postfix = "]") { message ->
        when {
            // This is the message from the LLM saying we should call a tool
            // This is the `rawMessage` in the PromptResult.ToolCall
            message.content.contains("tool_calls") -> {
                message.content
            }

            // This is the message from us to the LLM saying we called the tool
            // See `toolCallResultToMessage`.
            message.content.contains("tool_call_id") -> {
                message.content
            }

            else -> """{"role": "${message.role.name.lowercase()}","content": ${json.encodeToString(message.content)}}""".trimIndent()
        }
    }
}

@Serializable
private data class OpenAIResponse(
    val choices: List<OpenAIChoice>,
)

@Serializable
private data class OpenAIChoice(
    val message: OpenAIMessage,
)

@Serializable
private data class OpenAIMessage(
    val role: String,
    val content: String?,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>?,
)

@Serializable
private data class ToolCall(
    val id: String,
    val type: String,
    val function: Function,
)

@Serializable
private data class Function(
    val name: String,
    val arguments: String,
)
