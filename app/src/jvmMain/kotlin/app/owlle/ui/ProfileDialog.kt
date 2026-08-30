package app.owlle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.owlle.theme.OwlleColors

val avatarColorChoices = listOf(
    "EFAF1C", "E8833A", "E25C4A", "D96BA1", "9B6BD9", "4A90D9", "3AAFA9", "5CA65C", "6E6E6E",
)

fun avatarColor(hex: String): Color =
    runCatching { Color(("FF$hex").toLong(16)) }.getOrDefault(Color(0xFFEFAF1C))

@Composable
fun ProfileDialog(
    currentName: String,
    currentEmoji: String,
    currentColorHex: String,
    currentAccentHex: String,
    onSave: (name: String, emoji: String, colorHex: String, accentHex: String) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var emoji by remember { mutableStateOf(currentEmoji) }
    var colorHex by remember { mutableStateOf(currentColorHex) }
    var accentHex by remember { mutableStateOf(currentAccentHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OwlleColors.sidebar,
        title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(avatarColor(colorHex), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji.ifBlank { "🦉" }, fontSize = 20.sp) }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(16) },
                        label = { Text("Avatar emoji") },
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                }
                ColorPicker(selected = colorHex, onPick = { colorHex = it })
                EmojiPicker(selected = emoji, onPick = { emoji = it })
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "App color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OwlleColors.inkMuted,
                )
                ColorPicker(selected = accentHex, onPick = { accentHex = it })
                Text(
                    "Shown in the sidebar and, later, as the sender name on outgoing mail.",
                    fontSize = 12.sp,
                    color = OwlleColors.inkMuted,
                )
                TextButton(onClick = onSignOut) {
                    Text("Sign out of this account", color = OwlleColors.danger, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), emoji.trim().ifBlank { "🦉" }, colorHex, accentHex) }) {
                Text("Save", color = OwlleColors.goldDeep, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OwlleColors.inkMuted) }
        },
    )
}

private val emojiChoices = listOf(
    "🦉", "🦊", "🐻", "🐼", "🐸", "🐙", "🦄", "🐯", "🦁", "🐝",
    "😀", "😎", "🤓", "😇", "🥳", "🤠", "👻", "🤖", "🐲", "🧙",
    "🌞", "🌙", "⭐", "🔥", "⚡", "🌈", "🍀", "🌻", "☕", "🚀",
)

@Composable
private fun ColorPicker(selected: String, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        avatarColorChoices.forEach { hex ->
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(avatarColor(hex), CircleShape)
                    .border(
                        width = if (hex == selected) 2.dp else 1.dp,
                        color = if (hex == selected) OwlleColors.ink else OwlleColors.hairline,
                        shape = CircleShape,
                    )
                    .clickable { onPick(hex) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPicker(selected: String, onPick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        emojiChoices.forEach { candidate ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (candidate == selected) OwlleColors.goldWash else OwlleColors.sidebar,
                        MaterialTheme.shapes.small,
                    )
                    .border(
                        1.dp,
                        if (candidate == selected) OwlleColors.gold else OwlleColors.hairline,
                        MaterialTheme.shapes.small,
                    )
                    .clickable { onPick(candidate) },
            ) { Text(candidate, fontSize = 16.sp) }
        }
    }
}
