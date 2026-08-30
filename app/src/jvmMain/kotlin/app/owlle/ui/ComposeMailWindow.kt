package app.owlle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import app.owlle.AppState
import app.owlle.core.model.OutgoingMessage
import app.owlle.theme.OwlleColors
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeMailWindow(state: AppState, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var to by remember { mutableStateOf("") }
    var cc by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf(listOf<String>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun outgoing() = OutgoingMessage(
        to = to.trim(),
        cc = cc.trim(),
        subject = subject.trim(),
        body = body,
        attachmentPaths = attachments,
    )

    DialogWindow(
        onCloseRequest = onClose,
        title = if (subject.isBlank()) "New Message" else subject,
        state = rememberDialogState(width = 720.dp, height = 640.dp),
    ) {
        Surface(color = OwlleColors.paper, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = to, onValueChange = { to = it },
                    label = { Text("To") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cc, onValueChange = { cc = it },
                    label = { Text("Cc") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = subject, onValueChange = { subject = it },
                    label = { Text("Subject") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (attachments.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        attachments.forEach { path ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(OwlleColors.goldWash, MaterialTheme.shapes.small)
                                    .clickable { attachments = attachments - path }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.AttachFile, contentDescription = null,
                                    tint = OwlleColors.goldDeep, modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(File(path).name, fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Outlined.Close, contentDescription = "Remove",
                                    tint = OwlleColors.inkMuted, modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )

                if (error != null) {
                    Text(error!!, color = OwlleColors.danger, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val dialog = FileDialog(null as Frame?, "Attach files", FileDialog.LOAD)
                        dialog.isMultipleMode = true
                        dialog.isVisible = true
                        attachments = attachments + dialog.files.map { it.absolutePath }
                    }) {
                        Icon(
                            Icons.Outlined.AttachFile, contentDescription = null,
                            tint = OwlleColors.goldDeep, modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Attach", color = OwlleColors.goldDeep)
                    }
                    Spacer(Modifier.weight(1f))
                    if (busy) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp), strokeWidth = 2.dp, color = OwlleColors.goldDeep,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true; error = null
                                try {
                                    state.saveDraftMail(outgoing())
                                    onClose()
                                } catch (e: Exception) {
                                    error = e.message ?: "Could not save draft"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) { Text("Save draft", color = OwlleColors.inkMuted) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !busy && to.isNotBlank(),
                        onClick = {
                            scope.launch {
                                busy = true; error = null
                                try {
                                    state.sendMail(outgoing())
                                    onClose()
                                } catch (e: Exception) {
                                    error = e.message ?: "Could not send"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) { Text("Send") }
                }
            }
        }
    }
}
