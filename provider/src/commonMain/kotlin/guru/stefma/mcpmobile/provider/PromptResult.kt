package guru.stefma.mcpmobile.provider

public sealed interface PromptResult {
    public data class Content(public val text: String) : PromptResult
    public data class ToolCall(public val name: String, public val arguments: Map<String, Any?>) : PromptResult
}

