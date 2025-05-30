package guru.stefma.mcpmobile.provider.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Tool models
 */
@Serializable
internal data class OpenAITool(
    val type: String,
    val function: Function
) {
    @Serializable
    internal data class Function(
        val name: String,
        val description: String?,
        val parameters: Parameters
    ) {
        @Serializable
        internal data class Parameters(
            val type: String,
            val properties: JsonObject,
            val required: List<String>?
        )
    }
}

/**
 * Request models
 */
@Serializable
internal data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIRequestMessage>,
    val stream: Boolean = false,
    val tools: List<OpenAITool>?
)

internal sealed interface OpenAIRequestMessage {
    val role: String
    val content: String?
}

@Serializable
internal data class OpenAIDefaultMessage(
    override val role: String,
    override val content: String?,
) : OpenAIRequestMessage

@Serializable
internal data class OpenAIToolResultMessage(
    override val role: String,
    override val content: String,
    @SerialName("tool_call_id")
    val toolCallId: String,
) : OpenAIRequestMessage

/**
 * Response models
 */
@Serializable
internal data class OpenAIResponse(
    val choices: List<Choice>,
) {
    @Serializable
    internal data class Choice(
        val message: Message,
    ) {
        @Serializable
        internal data class Message(
            override val role: String,
            override val content: String?,
            @SerialName("tool_calls") val toolCalls: List<ToolCall>?,
        ) : OpenAIRequestMessage {
            @Serializable
            internal data class ToolCall(
                val id: String,
                val type: String,
                val function: Function,
            ) {
                @Serializable
                internal data class Function(
                    val name: String,
                    val arguments: String,
                )
            }
        }
    }
}
