package guru.stefma.mcpmobile.provider.openai

import guru.stefma.mcpmobile.provider.Message
import guru.stefma.mcpmobile.provider.PromptResult
import guru.stefma.mcpmobile.provider.Provider
import guru.stefma.mcpmobile.provider.Tool
import guru.stefma.mcpmobile.provider.ToolCallResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public class OpenAI(
    private val apiKey: String,
    private val model: String
) : Provider {

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private lateinit var tools: String

    override fun prepare(tools: List<Tool>) {
        this.tools = tools.joinToString(separator = ",", prefix = "[", postfix = "]") { tool ->
            """
            {
                "type": "function",
                "function": {
                    "name": "${tool.name}",
                    "description": "${tool.description}",
                    "parameters": {
                        "type": "${tool.inputSchema.type}",
                        "properties": ${tool.inputSchema.properties},
                        "required": ${tool.inputSchema.required?.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" } ?: "[]"}
                    }
                }
            }
            """.trimIndent()
        }
    }

    override fun prompt(messages: List<Message>): List<PromptResult> {
        return runBlocking {
            val urlString = "https://api.openai.com/v1/engines/$model/completions"
            val response = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json()
                }
                install(Logging) {
                    level = LogLevel.INFO
                }
            }.post(urlString) {
                contentType(ContentType.Application.Json)
                headers {
                    append("Authorization", "Bearer $apiKey")
                }
                val jsonString = """
                    {
                        "model": "$model",
                        "messages": ${messages.toJsonString(json)},
                        "stream": false,
                        "tools": $tools
                    }
                    """
                setBody(jsonString)
            }
            val responseBodyAsText = response.bodyAsText()
            val openAIResponse = json.decodeFromString<OpenAIResponse>(responseBodyAsText)
            if (!openAIResponse.choices.isNullOrEmpty()) {
                return@runBlocking openAIResponse.choices.map {
                    PromptResult.Content(
                        text = it.text,
                    )
                }
            }

            return@runBlocking listOf(
                PromptResult.Content(
                    text = openAIResponse.choices.first().text,
                )
            )
        }
    }

    override fun toolCallResultToMessage(toolCallResult: ToolCallResult): Message {
        return Message(
            role = Message.Role.TOOL,
            content = toolCallResult.contents.joinToString(separator = ".")
        )
    }
}

private fun List<Message>.toJsonString(json: Json): String {
    return joinToString(separator = ",", prefix = "[", postfix = "]") { message ->
        """{"role": "${message.role}","content": ${json.encodeToString(message.content)}}""".trimIndent()
    }
}

@Serializable
private data class OpenAIResponse(
    val choices: List<Choice>,
)

@Serializable
private data class Choice(
    val text: String,
)
