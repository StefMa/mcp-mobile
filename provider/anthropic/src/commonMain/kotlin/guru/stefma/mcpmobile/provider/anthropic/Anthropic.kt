package guru.stefma.mcpmobile.provider.anthropic

import guru.stefma.mcpmobile.provider.Message
import guru.stefma.mcpmobile.provider.PromptResult
import guru.stefma.mcpmobile.provider.Provider
import guru.stefma.mcpmobile.provider.Tool
import guru.stefma.mcpmobile.provider.ToolCallResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public class Anthropic(
    private val apiKey: String,
    private val model: String,
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
                "name": "${tool.name}",
                "description": "${tool.description}",
                "input_schema": {
                    "type": "${tool.inputSchema.type}",
                    "properties": ${tool.inputSchema.properties},
                    "required": ${
                tool.inputSchema.required?.joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]"
                ) { "\"$it\"" } ?: "[]"
            }
                }
            }
            """.trimIndent()
        }
    }

    override fun prompt(messages: List<Message>): List<PromptResult> = runBlocking {
        val urlString = "https://api.anthropic.com/v1/messages"
        val response = HttpClient(CIO).post(urlString) {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            val jsonString = """
                    {
                        "model": "$model",
                        "messages": ${messages.toJsonString(json)},
                        "max_tokens": 1024,
                        "tools": $tools
                    }
                    """
            setBody(jsonString)
        }
        val responseBodyAsText = response.bodyAsText()
        val anthropicResponse = json.decodeFromString<AnthropicResponse>(responseBodyAsText)
        anthropicResponse.content.map {
            when (it.type) {
                "text" -> PromptResult.Content(
                    text = it.text ?: "",
                )

                "tool_use" -> PromptResult.ToolCall(
                    name = it.name!!,
                    arguments = it.input ?: emptyMap(),
                    rawMessage = json.encodeToString(anthropicResponse)
                )

                else -> error("Unknown content type: ${it.type}")
            }
        }
    }

    override fun toolCallResultToMessage(toolCall: PromptResult.ToolCall, toolCallResult: ToolCallResult): Message {
        val id = json.decodeFromString<AnthropicResponse>(toolCall.rawMessage).id
        return Message(
            role = Message.Role.USER,
            content = """
                {
                    "type": "tool_result",
                    "tool_use_id": $id,
                    "result": "${toolCallResult.contents.joinToString(separator = "\n")}"
                }
            """.trimIndent(),
        )
    }
}

private fun List<Message>.toJsonString(json: Json): String {
    return joinToString(separator = ",", prefix = "[", postfix = "]") { message ->
        """{"role": "${message.role.name.lowercase()}","content": ${json.encodeToString(message.content)}}""".trimIndent()
    }
}

@Serializable
private data class AnthropicResponse(
    val id: String,
    val role: String,
    val content: List<AnthropicContent>,
)

@Serializable
private data class AnthropicContent(
    val type: String,
    val text: String?,

    // This is for tool use responses
    val id: String?,
    val name: String?,
    val input: Map<String, String>?,
)