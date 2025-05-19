package guru.stefma.mcpmobile.provider.anthropic

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.ToolUnion
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import guru.stefma.mcpmobile.provider.Message
import guru.stefma.mcpmobile.provider.PromptResult
import guru.stefma.mcpmobile.provider.Provider
import guru.stefma.mcpmobile.provider.Tool
import guru.stefma.mcpmobile.provider.ToolCallResult
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.optionals.getOrNull

public class Anthropic(apiKey: String) : Provider {

    private val anthropic = AnthropicOkHttpClient.builder().apiKey(apiKey).build()

    private val messageParamsBuilder: MessageCreateParams.Builder = MessageCreateParams.builder()
        .model(Model.CLAUDE_3_5_SONNET_20241022)
        .maxTokens(1024)

    // List of tools offered by the server
    private lateinit var tools: List<ToolUnion>

    private fun JsonObject.toJsonValue(): JsonValue {
        val mapper = ObjectMapper()
        val node = mapper.readTree(this.toString())
        return JsonValue.fromJsonNode(node)
    }

    override fun prepare(tools: List<Tool>) {
        this.tools = tools.map { tool ->
            ToolUnion.ofTool(
                com.anthropic.models.messages.Tool.builder()
                    .name(tool.name)
                    .description(tool.description ?: "")
                    .inputSchema(
                        com.anthropic.models.messages.Tool.InputSchema.builder()
                            .type(JsonValue.from(tool.inputSchema.type))
                            .properties(tool.inputSchema.properties.toJsonValue())
                            .putAdditionalProperty("required", JsonValue.from(tool.inputSchema.required))
                            .build()
                    )
                    .build()
            )
        }
    }

    override fun prompt(messages: List<Message>): List<PromptResult> {
        val messages = messages.map { message ->
            MessageParam.builder()
                .role(
                    when (message.role) {
                        Message.Role.SYSTEM -> MessageParam.Role.ASSISTANT
                        Message.Role.USER -> MessageParam.Role.USER
                        Message.Role.ASSISTANT -> MessageParam.Role.ASSISTANT
                        Message.Role.TOOL -> MessageParam.Role.USER
                    }
                )
                .content(message.content)
                .build()
        }

        // Send the query to the Anthropic model and get the response
        val response = anthropic.messages().create(
            messageParamsBuilder
                .messages(messages)
                .tools(tools)
                .build()
        )

        return response.content().map { response ->
            when {
                response.isText() -> {
                    PromptResult.Content(
                        text = response.text().getOrNull()?.text() ?: "",
                    )
                }

                response.isToolUse() -> {
                    val toolName = response.toolUse().get().name()
                    val toolArgs =
                        response.toolUse().get()._input().convert(object : TypeReference<Map<String, JsonValue>>() {})

                    PromptResult.ToolCall(
                        name = toolName,
                        arguments = toolArgs ?: emptyMap(),
                    )
                }

                else -> {
                    error("Unsupported response type")
                }
            }
        }
    }

    override fun toolCallResultToMessage(toolCallResult: ToolCallResult): Message {
        return Message(
            role = Message.Role.TOOL,
            content = """
                {
                    "type": "tool_result",
                    "tool_name": "${toolCallResult.name}",
                    "result": "${toolCallResult.contents.joinToString(separator = "\n")}"
                }
            """.trimIndent(),
        )
    }
}