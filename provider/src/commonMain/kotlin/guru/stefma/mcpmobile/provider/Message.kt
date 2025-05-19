package guru.stefma.mcpmobile.provider

public data class Message(
    val role: Role,
    val content: String,
) {
    public enum class Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL,
    }
}