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
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

public class OpenAI(
    private val apiKey: String,
    private val model: String
) : Provider {

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphic(OpenAIRequestMessage::class) {
                subclass(OpenAIDefaultMessage::class)
                subclass(OpenAIToolResultMessage::class)
                subclass(OpenAIResponse.Choice.Message::class)
            }
        }
    }

    private var tools: List<OpenAITool>? = null

    override fun prepare(tools: List<Tool>) {
        this.tools = tools.map {
            OpenAITool(
                type = "function",
                function = OpenAITool.Function(
                    name = it.name,
                    description = it.description,
                    parameters = OpenAITool.Function.Parameters(
                        type = it.inputSchema.type,
                        properties = it.inputSchema.properties,
                        required = it.inputSchema.required
                    )
                )
            )
        }
    }

    override fun prompt(messages: List<Message>): List<PromptResult> = runBlocking {
        val urlString = "https://api.openai.com/v1/chat/completions"
        val response = HttpClient(CIO).post(urlString) {
            contentType(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $apiKey")
                append("Content-Type", "application/json")
            }
            val request = OpenAIRequest(
                model = model,
                messages = messages.toOpenAIMessage(json),
                tools = tools
            )
            val json = json.encodeToString(request)
            setBody(json)
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

    override fun toolCallResultToMessage(toolCall: PromptResult.ToolCall, toolCallResult: ToolCallResult): Message {
        val id = json.decodeFromString<OpenAIResponse.Choice.Message>(toolCall.rawMessage).toolCalls!![0].id
        return Message(
            role = Message.Role.TOOL,
            content = json.encodeToString(
                OpenAIToolResultMessage(
                    role = "tool",
                    toolCallId = id,
                    content = toolCallResult.contents.joinToString(separator = "\n")
                )
            )
        )
    }
}

private fun List<Message>.toOpenAIMessage(json: Json): List<OpenAIRequestMessage> = map { message ->
    when (message.role) {
        Message.Role.USER -> OpenAIDefaultMessage(
            role = message.role.name.lowercase(),
            content = json.encodeToString(message.content)
        )

        Message.Role.SYSTEM -> OpenAIDefaultMessage(
            role = message.role.name.lowercase(),
            content = json.encodeToString(message.content)
        )

        Message.Role.ASSISTANT -> {
            try {
                // This is the message from the LLM saying we should call a tool
                // This is the `rawMessage` in the PromptResult.ToolCall
                val rawMessageResponse = json.decodeFromString<OpenAIResponse.Choice.Message>(message.content)
                return@map rawMessageResponse
            } catch (e: IllegalArgumentException) {
                // Ignore the exception
                // Fallback to default message
                OpenAIDefaultMessage(
                    role = message.role.name.lowercase(),
                    content = json.encodeToString(message.content)
                )
            }
        }

        Message.Role.TOOL -> {
            try {
                // This is the message from us to the LLM saying we called the tool
                // See `toolCallResultToMessage`.
                val toolResultMessage = json.decodeFromString<OpenAIToolResultMessage>(message.content)
                return@map toolResultMessage
            } catch (e: IllegalArgumentException) {
                // Ignore the exception
                error("Failed to decode OpenAIToolResultMessage from content: ${message.content}")
            }
        }
    }
}
