package guru.stefma.mcpmobile.provider

public interface Provider {
    public fun prepare(tools: List<Tool>)

    public fun prompt(messages: List<Message>): List<PromptResult>

    public fun toolCallResultToMessage(toolCallResult: ToolCallResult): Message
}