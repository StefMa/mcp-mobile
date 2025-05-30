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
import kotlinx.serialization.json.Json

public class Anthropic(
    private val apiKey: String,
    private val model: String,
) : Provider {

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private var tools: List<AnthropicTool>? = null

    override fun prepare(tools: List<Tool>) {
        this.tools = tools.map {
            AnthropicTool(
                name = it.name,
                description = it.description,
                inputSchema = AnthropicTool.InputSchema(
                    type = it.inputSchema.type,
                    properties = it.inputSchema.properties,
                    required = it.inputSchema.required
                )
            )
        }
    }

    override fun prompt(messages: List<Message>): List<PromptResult> = runBlocking {
        val urlString = "https://api.anthropic.com/v1/messages"
        val response = HttpClient(CIO).post(urlString) {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            val request = AnthropicRequest(
                model = model,
                maxTokens = 1024,
                messages = messages.filter { it.role != Message.Role.SYSTEM }.map {
                    AnthropicRequest.Message(
                        role = it.role.toAnthropicRole(),
                        content = it.content
                    )
                },
                system = messages.find { it.role == Message.Role.SYSTEM }?.content,
                tools = tools,
            )
            val json = json.encodeToString(request)
            setBody(json)
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
            role = Message.Role.TOOL,
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

private fun Message.Role.toAnthropicRole(): String {
    return when (this) {
        Message.Role.USER -> "user"
        Message.Role.ASSISTANT -> "assistant"
        Message.Role.TOOL -> "user"
        Message.Role.SYSTEM -> error("No system role in Anthropic")
    }
}
