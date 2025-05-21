package guru.stefma.mcpmobile.provider.gemini

import guru.stefma.mcpmobile.provider.Message
import guru.stefma.mcpmobile.provider.PromptResult
import guru.stefma.mcpmobile.provider.Provider
import guru.stefma.mcpmobile.provider.Tool
import guru.stefma.mcpmobile.provider.ToolCallResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public class Gemini(
    private val baseUrl: String,
    private val model: String
) : Provider {

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private lateinit var tools: String

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
                        "required": ${tool.inputSchema.required?.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" } ?: "[]"}
                    }
                }
            }
            """.trimIndent()
        }
    }

    override fun prompt(messages: List<Message>): List<PromptResult> {
        return runBlocking {
            val urlString = "$baseUrl/api/chat"
            val response = HttpClient(CIO).post(urlString) {
                contentType(ContentType.Application.Json)
                val jsonString = """
                    {
                        "model": "$model",
                        "messages": ${messages.toJsonString(json)},
                        "stream": false,
                        "tools": $tools
                    }
                    """
                setBody(jsonString)
            }
            val responseBodyAsText = response.bodyAsText()
            val geminiResponse = json.decodeFromString<GeminiResponse>(responseBodyAsText)
            if (!geminiResponse.message.toolCalls.isNullOrEmpty()) {
                return@runBlocking geminiResponse.message.toolCalls.map {
                    PromptResult.ToolCall(
                        name = it.function.name,
                        arguments = it.function.arguments
                    )
                }
            }

            return@runBlocking listOf(
                PromptResult.Content(
                    text = geminiResponse.message.content,
                )
            )
        }
    }

    override fun toolCallResultToMessage(toolCallResult: ToolCallResult): Message {
        return Message(
            role = Message.Role.TOOL,
            content = toolCallResult.contents.joinToString(separator = ".")
        )
    }
}

private fun List<Message>.toJsonString(json: Json): String {
    return joinToString(separator = ",", prefix = "[", postfix = "]") { message ->
        """{"role": "${message.role}","content": ${json.encodeToString(message.content)}}""".trimIndent()
    }
}

@Serializable
private data class GeminiResponse(
    val message: GeminiMessage,
)

@Serializable
private data class GeminiMessage(
    val role: String,
    val content: String,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
)

@Serializable
private data class ToolCall(
    val function: Function,
)

@Serializable
private data class Function(
    val name: String,
    val arguments: Map<String, String>,
)
