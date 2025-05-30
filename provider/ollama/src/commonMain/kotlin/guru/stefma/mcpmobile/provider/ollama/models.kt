package guru.stefma.mcpmobile.provider.ollama

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Tool models
 */
@Serializable
internal data class OllamaTool(
    val type: String,
    val function: FunctionDetail
) {
    @Serializable
    data class FunctionDetail(
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
internal data class OllamaRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean,
    val tools: List<OllamaTool>?
) {
    @Serializable
    internal data class Message(
        val role: String,
        val content: String
    )
}

/**
 * Response models
 */
@Serializable
internal data class OllamaResponse(
    val message: Message,
) {
    @Serializable
    internal data class Message(
        val role: String,
        val content: String,
        @SerialName("tool_calls")
        val toolCalls: List<ToolCall>? = null,
    ) {
        @Serializable
        internal data class ToolCall(
            val function: Function,
        ) {
            @Serializable
            internal data class Function(
                val name: String,
                val arguments: Map<String, String>,
            )
        }
    }
}


