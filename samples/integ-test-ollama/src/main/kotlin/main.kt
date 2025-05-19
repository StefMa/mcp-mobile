import guru.stefma.mcpmobile.MCPMobile
import guru.stefma.mcpmobile.provider.Message
import guru.stefma.mcpmobile.provider.ollama.Ollama
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.seconds

suspend fun main(args: Array<String>): Unit = coroutineScope {
    val mcpServerUrl = args.first()
    val prompt = args.last()

    val mcpMobile = MCPMobile(
        name = "mcpmobile-anthropic-sample",
        version = "0.0.1-SNAPSHOT",
        mcpServerUrl = mcpServerUrl,
        provider = Ollama(baseUrl = "http://localhost:11434", model = "qwen3:0.6b"),
    )

    val messages: Flow<List<Message>> = mcpMobile.connect()

    val messagesJob = launch(Dispatchers.IO) {
        messages.collect {
            println("(${it.lastOrNull()?.role}): ${it.lastOrNull()?.content}")
        }
    }

    mcpMobile.prompt(prompt)

    launch(Dispatchers.IO) {
        // Assuming the response happen within 20 seconds
        delay(20.seconds)
        messagesJob.cancel()
        mcpMobile.close()
    }
}
