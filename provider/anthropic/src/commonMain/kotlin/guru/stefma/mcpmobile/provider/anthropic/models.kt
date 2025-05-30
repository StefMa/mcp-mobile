package guru.stefma.mcpmobile.provider.anthropic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Tool models
 */
@Serializable
internal data class AnthropicTool(
    val name: String,
    val description: String?,
    @SerialName("input_schema")
    val inputSchema: InputSchema
) {
    @Serializable
    data class InputSchema(
        val type: String,
        val properties: JsonObject,
        val required: List<String>?
    )
}

/**
 * Request models
 */
@Serializable
internal data class AnthropicRequest(
    val model: String,
    val system: String?,
    val messages: List<Message>,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val tools: List<AnthropicTool>?
) {
    @Serializable
    data class Message(
        val role: String,
        val content: String
    )
}

/**
 * Response models
 */
@Serializable
internal data class AnthropicResponse(
    val id: String,
    val role: String,
    val content: List<Content>,
) {
    @Serializable
    data class Content(
        val type: String,
        val text: String? = null,

        // This is for tool use responses
        val id: String?,
        val name: String?,
        val input: Map<String, String>?,
    )
}
