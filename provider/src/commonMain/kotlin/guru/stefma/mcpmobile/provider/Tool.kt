package guru.stefma.mcpmobile.provider

import kotlinx.serialization.json.JsonObject

public data class Tool(
    /**
     * The name of the tool.
     */
    val name: String,
    /**
     * A human-readable description of the tool.
     */
    val description: String?,
    /**
     * A JSON object defining the expected parameters for the tool.
     */
    val inputSchema: Input,
) {
    public data class Input(
        val properties: JsonObject = JsonObject(emptyMap()),
        val required: List<String>? = null,
    ) {
        val type: String = "object"
    }
}