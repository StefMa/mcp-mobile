package guru.stefma.mcpmobile

import guru.stefma.mcpmobile.provider.Message
import guru.stefma.mcpmobile.provider.PromptResult
import guru.stefma.mcpmobile.provider.Provider
import guru.stefma.mcpmobile.provider.Tool
import guru.stefma.mcpmobile.provider.ToolCallResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.EmbeddedResource
import io.modelcontextprotocol.kotlin.sdk.ImageContent
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.UnknownContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

public class MCPMobile(
    name: String,
    version: String,
    private val mcpServerUrl: String,
    private val provider: Provider,
) : AutoCloseable {

    private val mcpClient: Client = Client(clientInfo = Implementation(name = name, version = version))
    private val sseHttpClient = HttpClient(CIO) { install(SSE) }

    private var toolsResult: ListToolsResult? = null

    private val messages: MutableStateFlow<List<Message>> = MutableStateFlow(emptyList())

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    public suspend fun connect(): Flow<List<Message>> {
        try {
            val transport = SseClientTransport(
                client = sseHttpClient,
                urlString = mcpServerUrl,
            )

            // Connect the MCP client to the server using the transport
            mcpClient.connect(transport)

            // Request the list of available tools from the server
            toolsResult = mcpClient.listTools()

            provider.prepare(toolsResult?.tools?.asMcpMobileTools ?: emptyList())
        } catch (e: Exception) {
            throw e
        }

        return messages
    }

    public fun prompt(prompt: String) {
        // Add the new message with the user's query
        messages.add(
            Message(
                role = Message.Role.USER,
                content = prompt
            )
        )

        promptInternal()
    }

    private fun promptInternal() {
        coroutineScope.launch {
            val results = provider.prompt(messages.value)

            results.forEach { result ->
                when (result) {
                    is PromptResult.Content -> {
                        messages.add(
                            Message(
                                role = Message.Role.ASSISTANT,
                                content = result.text
                            )
                        )
                    }

                    is PromptResult.ToolCall -> {
                        val toolResultBase = mcpClient.callTool(
                            name = result.name,
                            arguments = result.arguments
                        )

                        val toolResults = toolResultBase!!.content.map {
                            when (it) {
                                is EmbeddedResource -> it.resource.uri
                                is ImageContent -> it.data
                                is TextContent -> it.text ?: ""
                                is UnknownContent -> error("Received unknownContent from tool call: $it")
                            }
                        }

                        val message = provider.toolCallResultToMessage(
                            ToolCallResult(
                                name = result.name,
                                contents = toolResults
                            )
                        )

                        messages.add(message)

                        promptInternal()
                    }
                }
            }
        }
    }

    override fun close() {
        runBlocking {
            mcpClient.close()
            coroutineScope.cancel()
        }
    }
}

private val List<io.modelcontextprotocol.kotlin.sdk.Tool>.asMcpMobileTools: List<Tool>
    get() = map { mcpTool ->
        Tool(
            name = mcpTool.name,
            description = mcpTool.description,
            inputSchema = Tool.Input(
                properties = mcpTool.inputSchema.properties,
                required = mcpTool.inputSchema.required
            )
        )
    }

private fun MutableStateFlow<List<Message>>.add(message: Message) {
    update { it + message }
}