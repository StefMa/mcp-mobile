import guru.stefma.mcpmobile.MCPMobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun chat(mcpMobile: MCPMobile) = coroutineScope {
    val messageJob = launch(Dispatchers.IO) {
        mcpMobile.connect().collect { messages ->
            messages.lastOrNull()?.let {
                println("(${it.role}): ${it.content}")
            }
        }
    }

    while (true) {
        print("Enter your query (or 'exit' to quit): ")
        val input = readln()

        if (input == "exit") {
            messageJob.cancel()
            break
        }

        mcpMobile.prompt(input)
    }
}