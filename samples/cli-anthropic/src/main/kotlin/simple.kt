import guru.stefma.mcpmobile.MCPMobile
import guru.stefma.mcpmobile.provider.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

suspend fun simple(mcpMobile: MCPMobile) = coroutineScope {
    val messages: Flow<List<Message>> = mcpMobile.connect()

    val messagesJob = launch(Dispatchers.IO) {
        messages
            .filter { it.lastOrNull()?.role != Message.Role.TOOL }
            .collect {
                println("(${it.lastOrNull()?.role}): ${it.lastOrNull()?.content}")
            }
    }

    mcpMobile.prompt("Call my tool pls")

    launch(Dispatchers.IO) {
        // Assuming the response happen within 10 seconds
        delay(10.seconds)
        messagesJob.cancel()
    }
}