package app.owlle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.owlle.theme.OwlleColors

@Composable
fun ProfileDialog(
    currentName: String,
    currentEmoji: String,
    onSave: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var emoji by remember { mutableStateOf(currentEmoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OwlleColors.sidebar,
        title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(OwlleColors.gold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji.ifBlank { "🦉" }, fontSize = 20.sp) }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(4) },
                        label = { Text("Avatar emoji") },
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Shown in the sidebar and, later, as the sender name on outgoing mail.",
                    fontSize = 12.sp,
                    color = OwlleColors.inkMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), emoji.trim().ifBlank { "🦉" }) }) {
                Text("Save", color = OwlleColors.goldDeep, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OwlleColors.inkMuted) }
        },
    )
}
