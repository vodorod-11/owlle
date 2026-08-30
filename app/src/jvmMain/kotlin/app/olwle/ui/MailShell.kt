package app.olwle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.olwle.core.model.Envelope
import app.olwle.core.model.MailFolder
import app.olwle.core.model.MessageContent
import app.olwle.core.model.SpecialUse
import app.olwle.theme.OlwleColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun SpecialUse.icon(): ImageVector = when (this) {
    SpecialUse.INBOX -> Icons.Outlined.Inbox
    SpecialUse.SENT -> Icons.Outlined.Send
    SpecialUse.DRAFTS -> Icons.Outlined.Drafts
    SpecialUse.TRASH -> Icons.Outlined.Delete
    SpecialUse.JUNK -> Icons.Outlined.Report
    SpecialUse.ARCHIVE, SpecialUse.ALL -> Icons.Outlined.Archive
    SpecialUse.FLAGGED -> Icons.Outlined.Star
    SpecialUse.CUSTOM -> Icons.Outlined.Folder
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("d MMM")
private val fullFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'at' HH:mm")

private fun formatListDate(epochMs: Long): String {
    if (epochMs == 0L) return ""
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMs).atZone(zone)
    val today = Instant.now().atZone(zone).toLocalDate()
    return if (date.toLocalDate() == today) timeFormatter.format(date) else dateFormatter.format(date)
}

@Composable
fun MailShell(
    accountEmail: String,
    folders: List<MailFolder>,
    selectedFolder: MailFolder?,
    envelopes: List<Envelope>,
    listLoading: Boolean,
    selectedMessage: MessageContent?,
    messageLoading: Boolean,
    error: String?,
    onSelectFolder: (MailFolder) -> Unit,
    onRefresh: () -> Unit,
    onOpenMessage: (Envelope) -> Unit,
) {
    Row(Modifier.fillMaxSize().background(OlwleColors.paper)) {
        FolderSidebar(accountEmail, folders, selectedFolder, onSelectFolder)
        VerticalHairline()
        MessageListPane(selectedFolder, envelopes, listLoading, error, onRefresh, onOpenMessage)
        VerticalHairline()
        MessageViewPane(selectedMessage, messageLoading)
    }
}

@Composable
private fun VerticalHairline() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(OlwleColors.hairline))
}

// ---- sidebar ---------------------------------------------------------------

@Composable
private fun FolderSidebar(
    accountEmail: String,
    folders: List<MailFolder>,
    selected: MailFolder?,
    onSelect: (MailFolder) -> Unit,
) {
    val pinned = folders.filter { it.specialUse != SpecialUse.CUSTOM }
    val custom = folders.filter { it.specialUse == SpecialUse.CUSTOM }

    Column(
        Modifier.width(230.dp).fillMaxHeight().background(OlwleColors.sidebar).padding(vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Box(
                Modifier.size(26.dp).background(OlwleColors.gold, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("🦉", fontSize = 13.sp) }
            Spacer(Modifier.width(8.dp))
            Text(
                accountEmail,
                fontSize = 12.sp,
                color = OlwleColors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(16.dp))
        SidebarLabel("Mailboxes")
        LazyColumn(Modifier.fillMaxSize()) {
            items(pinned, key = { it.path }) { folder ->
                SidebarRow(folder, folder == selected, onSelect)
            }
            if (custom.isNotEmpty()) {
                item { Spacer(Modifier.height(10.dp)); SidebarLabel("Folders") }
                items(custom, key = { it.path }) { folder ->
                    SidebarRow(folder, folder == selected, onSelect)
                }
            }
        }
    }
}

@Composable
private fun SidebarLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.SemiBold,
        color = OlwleColors.inkMuted,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
    )
}

@Composable
private fun SidebarRow(folder: MailFolder, selected: Boolean, onSelect: (MailFolder) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .fillMaxWidth()
            .background(
                if (selected) OlwleColors.selection else OlwleColors.sidebar,
                MaterialTheme.shapes.small,
            )
            .clickable { onSelect(folder) }
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Icon(
            folder.specialUse.icon(),
            contentDescription = null,
            tint = OlwleColors.goldDeep,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            folder.displayName,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---- message list ----------------------------------------------------------

@Composable
private fun MessageListPane(
    folder: MailFolder?,
    envelopes: List<Envelope>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpen: (Envelope) -> Unit,
) {
    Column(Modifier.width(340.dp).fillMaxHeight()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
        ) {
            Text(
                folder?.displayName ?: "",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (loading) {
                CircularProgressIndicator(
                    Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = OlwleColors.goldDeep,
                )
                Spacer(Modifier.width(6.dp))
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Refresh",
                    tint = OlwleColors.goldDeep,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(color = OlwleColors.hairline)

        if (error != null) {
            Text(
                error,
                color = OlwleColors.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp),
            )
        }

        if (envelopes.isEmpty() && !loading && error == null) {
            EmptyHint("No messages here yet")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(envelopes, key = { it.uid }) { envelope ->
                    EnvelopeRow(envelope, onOpen)
                    HorizontalDivider(
                        color = OlwleColors.hairline,
                        modifier = Modifier.padding(start = 26.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnvelopeRow(envelope: Envelope, onOpen: (Envelope) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(envelope) }
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Box(Modifier.width(16.dp).padding(top = 6.dp), contentAlignment = Alignment.Center) {
            if (!envelope.seen) {
                Box(Modifier.size(8.dp).background(OlwleColors.unread, CircleShape))
            }
        }
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    envelope.fromName.ifBlank { envelope.fromAddress },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatListDate(envelope.sentAtEpochMs),
                    fontSize = 11.sp,
                    color = OlwleColors.inkMuted,
                )
            }
            Text(
                envelope.subject,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (envelope.seen) OlwleColors.inkMuted else OlwleColors.ink,
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = OlwleColors.inkMuted, fontSize = 13.sp)
    }
}

// ---- message view ----------------------------------------------------------

@Composable
private fun MessageViewPane(message: MessageContent?, loading: Boolean) {
    Box(Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OlwleColors.goldDeep)
            }
            message == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Mail,
                        contentDescription = null,
                        tint = OlwleColors.hairline,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Select a message", color = OlwleColors.inkMuted, fontSize = 13.sp)
                }
            }
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    message.subject,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                )
                Text("From: ${message.fromDisplay}", fontSize = 12.sp, color = OlwleColors.inkMuted)
                if (message.toDisplay.isNotBlank()) {
                    Text("To: ${message.toDisplay}", fontSize = 12.sp, color = OlwleColors.inkMuted)
                }
                if (message.sentAtEpochMs > 0) {
                    Text(
                        fullFormatter.format(
                            Instant.ofEpochMilli(message.sentAtEpochMs).atZone(ZoneId.systemDefault())
                        ),
                        fontSize = 12.sp,
                        color = OlwleColors.inkMuted,
                    )
                }
                HorizontalDivider(color = OlwleColors.hairline, modifier = Modifier.padding(vertical = 10.dp))
                SelectionContainer {
                    Text(message.bodyText, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
        }
    }
}
