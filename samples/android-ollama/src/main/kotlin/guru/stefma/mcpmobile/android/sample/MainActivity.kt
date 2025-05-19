package guru.stefma.mcpmobile.android.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import guru.stefma.mcpmobile.MCPMobile
import guru.stefma.mcpmobile.provider.Message
import guru.stefma.mcpmobile.provider.ollama.Ollama

class MainActivity : AppCompatActivity() {

    private var mcpMobile = newMcpMobile()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(AppLayout())
    }

    private fun AppLayout(): ComposeView {
        val ui = ComposeView(this).apply {
            setContent {
                val messages = remember { mutableStateListOf<Message>() }
                var newChat by remember { mutableStateOf(false) }

                LaunchedEffect(newChat) {
                    if (newChat) {
                        mcpMobile.close()
                        mcpMobile = newMcpMobile()
                        messages.clear()
                        newChat = false
                    }
                }

                LaunchedEffect(!newChat) {
                    mcpMobile.connect().collect { aiMessages ->
                        messages.clear()
                        messages.addAll(aiMessages)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.White)
                        .navigationBarsPadding()
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = "MCPMobile Android Sample"
                    )
                    OutlinedButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            newChat = true
                        },
                        content = {
                            Text("New Chat")
                        }
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp)
                    ) {
                        items(messages) { message ->
                            val (color, emoji) = when (message.role) {
                                Message.Role.SYSTEM -> Color.Red to "🖥️"
                                Message.Role.USER -> Color.LightGray to "🙂"
                                Message.Role.ASSISTANT -> Color.Green to "🤖"
                                Message.Role.TOOL -> Color.Yellow to "🔨"
                            }
                            Text(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .background(
                                        color = color,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(8.dp),
                                text = "$emoji:\n${message.content}",
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        var textFieldText by remember { mutableStateOf("") }

                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = textFieldText,
                            onValueChange = { textFieldText = it },
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            modifier = Modifier.height(55.dp),
                            onClick = {
                                mcpMobile.prompt(textFieldText)
                                textFieldText = ""
                            },
                            content = {
                                Text("Send")
                            }
                        )
                    }
                }
            }
        }
        return ui
    }

    override fun onDestroy() {
        mcpMobile.close()
        super.onDestroy()
    }

    private fun newMcpMobile() = MCPMobile(
        name = "mcpmobile-ollama-sample",
        version = "0.0.1-SNAPSHOT",
        mcpServerUrl = "http://10.0.2.2:3001",
        provider = Ollama(baseUrl = "http://10.0.2.2:11434", model = "qwen3:4b"),
    )
}