import guru.stefma.mcpmobile.MCPMobile
import guru.stefma.mcpmobile.provider.anthropic.Anthropic

suspend fun main(args: Array<String>) {
    val mcpMobile = MCPMobile(
        name = "mcpmobile-anthropic-sample",
        version = "0.0.1-SNAPSHOT",
        mcpServerUrl = "http://localhost:3001",
        provider = Anthropic(apiKey = "API_KEY", model="claude-3-haiku-20240307"),
    )

    val mode = args.firstOrNull() ?: "simple"
    when (mode) {
        "simple" -> simple(mcpMobile)
        "chat" -> chat(mcpMobile)
        else -> println("Unknown mode: $mode")
    }

    mcpMobile.close()
}
