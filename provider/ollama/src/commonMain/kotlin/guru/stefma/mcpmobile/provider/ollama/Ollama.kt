package guru.stefma.mcpmobile.provider.ollama

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
import kotlinx.serialization.json.Json

public class Ollama(
    private val baseUrl: String,
    private val model: String
) : Provider {

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private var tools: List<OllamaTool>? = null

    override fun prepare(tools: List<Tool>) {
        this.tools = tools.map {
            OllamaTool(
                type = "function",
                function = OllamaTool.FunctionDetail(
                    name = it.name,
                    description = it.description,
                    parameters = OllamaTool.FunctionDetail.Parameters(
                        type = it.inputSchema.type,
                        properties = it.inputSchema.properties,
                        required = it.inputSchema.required
                    )
                )
            )
        }
    }

    override fun prompt(messages: List<Message>): List<PromptResult> = runBlocking {
        val urlString = "$baseUrl/api/chat"
        val response = HttpClient(CIO).post(urlString) {
            contentType(ContentType.Application.Json)
            val request = OllamaRequest(
                model = model,
                messages = messages.map {
                    OllamaRequest.Message(
                        role = it.role.toString().lowercase(),
                        content = it.content
                    )
                },
                stream = false,
                tools = tools
            )
            val json = json.encodeToString(request)
            setBody(json)
        }

        val responseBodyAsText = response.bodyAsText()
        val ollamaResponse = json.decodeFromString<OllamaResponse>(responseBodyAsText)
        if (!ollamaResponse.message.toolCalls.isNullOrEmpty()) {
            return@runBlocking ollamaResponse.message.toolCalls.map {
                PromptResult.ToolCall(
                    name = it.function.name,
                    arguments = it.function.arguments,
                    rawMessage = json.encodeToString(ollamaResponse.message)
                )
            }
        }

        return@runBlocking listOf(
            PromptResult.Content(
                text = ollamaResponse.message.content,
            )
        )
    }

    override fun toolCallResultToMessage(
        toolCall: PromptResult.ToolCall,
        toolCallResult: ToolCallResult
    ): Message {
        return Message(
            role = Message.Role.TOOL,
            content = toolCallResult.contents.joinToString(separator = "\n")
        )
    }
}
