# mcp-mobile

A Kotlin Multiplatform (KMP) Model Context Protocol (MCP) library for mobile devices.
Integrate easily MCP servers to your mobile applications.

## Supported platforms

Right now only JVM/Android is supported.
iOS support will follow, as soon as the upstream library,
[modelcontextprotocol/kotlin-sdk[(https://github.com/modelcontextprotocol/kotlin-sdk),
supports it (see [issue](https://github.com/modelcontextprotocol/kotlin-sdk/pull/81)).

## Download

```kotlin
// Required
implementation("guru.stefma.mcpmobile:mcpmobile:$currentVersion")

// Choose one of our provided providers:
implementation("guru.stefma.mcpmobile:provider-anthropic:$currentVersion")
// or
implementation("guru.stefma.mcpmobile:provider-ollama:$currentVersion")
// or
implementation("guru.stefma.mcpmobile:provider-openai:$currentVersion")

// or create your own provider
implementation("guru.stefma.mcpmobile:provider:$currentVersion")
```

## How

```kotlin
// Create a provider instance
// E.g. Anthropic
val anthropic = Anthropic(apiKey = "[API_KEY]", model = "[MODEL_NAME]")
// or Ollama
val ollama = Ollama(baseUrl = "[OLLAMA_SERVER_UL]", model = "[MODEL_NAME]")
// or OpenAI
val ollama = OpenAI(baseUrl = "[API_KEY]", model = "[MODEL_NAME]")
// or create your own provider
val customProvider = object : Provider {
    override fun prepare(tools: List<Tool>) {
        // Gets called when the provider is created
        // connection to the MCP server was successfully established
        // and the tools are ready to be used
    }
    override fun prompt(messages: List<Message>): List<PromptResult> {
        // Gets called when the user sends a message
        // returns a list of PromptResult
    }
    override fun toolCallResultToMessage(toolCall: PromptResult.ToolCall, toolCallResult: ToolCallResult): Message {
        // Converts the ToolCallResult to a Message
        // This is used when the provider receives a PromptResult.ToolCall
        // and needs to convert it to a Message
    }
}

// Then create a MCPMobile instance
val mcpMobile = MCPMobile(
    name = "[MCP_CLIENT_NAME]",
    version = "[MCP_CLIENT_VERSION]",
    mcpServerUrl = "[URL_TO_MCP_SERVER]",
    provider = provider,
)

// Connect to the server
val messages: Flow<List<Message>> = mcpMobile.connect()

coroutineScope.launch {
    messages.collect { messages ->
        // Messages is a List<Message>
        // it contains all messages
        // from system, user, tools and assistant
    }
}

// Send the prompt
mcpMobile.prompt("What is the current weather in France?")

// Close the connection when you're done
coroutineScope.launch {
    delay(20000) // Assuming the response happen within 20 seconds
    mcpMobile.close()
}
```

You can also check the [samples](samples) folder for more examples.