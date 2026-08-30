package app.olwle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.olwle.core.model.MailAccount
import app.olwle.theme.OlwleColors

@Composable
fun AccountSetupScreen(
    connecting: Boolean,
    error: String?,
    onConnect: (MailAccount) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("993") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var useSsl by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.fillMaxSize().background(OlwleColors.paper),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = OlwleColors.sidebar,
            modifier = Modifier.width(420.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(OlwleColors.gold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🦉", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("olwle", fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                        Text(
                            "Add your mail account",
                            color = OlwleColors.inkMuted,
                            fontSize = 13.sp,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Your name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (username.isEmpty() || username == email.dropLast(1)) username = it
                    },
                    label = { Text("Email address") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = host, onValueChange = { host = it },
                    label = { Text("IMAP server") },
                    placeholder = { Text("imap.example.com") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = port, onValueChange = { port = it.filter(Char::isDigit) },
                        label = { Text("Port") },
                        singleLine = true, modifier = Modifier.width(110.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Switch(
                            checked = useSsl, onCheckedChange = { useSsl = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = OlwleColors.gold),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("SSL/TLS", fontSize = 13.sp, color = OlwleColors.inkMuted)
                    }
                }
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (error != null) {
                    Text(error, color = OlwleColors.danger, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onConnect(
                            MailAccount(
                                displayName = name.trim(),
                                email = email.trim(),
                                imapHost = host.trim(),
                                imapPort = port.toIntOrNull() ?: 993,
                                username = username.trim().ifEmpty { email.trim() },
                                password = password,
                                useSsl = useSsl,
                            )
                        )
                    },
                    enabled = !connecting && email.isNotBlank() && host.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) {
                    if (connecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = OlwleColors.paper,
                        )
                    } else {
                        Text("Connect")
                    }
                }

                Text(
                    "Gmail and many providers require an app password for IMAP. " +
                        "Your password stays on this device and is not saved to disk yet.",
                    color = OlwleColors.inkMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
