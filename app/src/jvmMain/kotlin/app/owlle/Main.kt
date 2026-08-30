package app.owlle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.owlle.theme.OwlleTheme
import app.owlle.ui.AccountSetupScreen
import app.owlle.ui.MailShell
import app.owlle.ui.ProfileDialog
import app.owlle.ui.avatarColor
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() {
    // Let the macOS window chrome follow the system light/dark appearance.
    System.setProperty("apple.awt.application.appearance", "system")
    installDockIcon()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "owlle",
            icon = painterResource("icon.png"),
            state = rememberWindowState(size = DpSize(1200.dp, 780.dp)),
        ) {
            var dark by remember { mutableStateOf(AppSettings.darkMode) }
            OwlleTheme(dark = dark) {
                App(
                    dark = dark,
                    onToggleTheme = {
                        dark = !dark
                        AppSettings.darkMode = dark
                    },
                )
            }
        }
    }
}

/** macOS shows the JVM's default dock icon during dev runs unless we replace it. */
private fun installDockIcon() {
    runCatching {
        if (!Taskbar.isTaskbarSupported()) return
        val taskbar = Taskbar.getTaskbar()
        if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return
        object {}.javaClass.getResourceAsStream("/icon.png")?.use { stream ->
            taskbar.iconImage = ImageIO.read(stream)
        }
    }
}

@Composable
private fun App(dark: Boolean, onToggleTheme: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(scope) }

    val screen by state.screen.collectAsState()
    val connecting by state.connecting.collectAsState()
    val error by state.error.collectAsState()

    var profileName by remember { mutableStateOf(AppSettings.profileName) }
    var profileEmoji by remember { mutableStateOf(AppSettings.profileEmoji) }
    var profileColorHex by remember { mutableStateOf(AppSettings.avatarColor) }
    var profileOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { state.autoConnect() }

    when (screen) {
        Screen.Setup -> AccountSetupScreen(
            connecting = connecting,
            error = error,
            initial = remember { AccountStore.loadConfigOnly() },
            onConnect = { account ->
                if (profileName.isBlank() && account.displayName.isNotBlank()) {
                    profileName = account.displayName
                    AppSettings.profileName = account.displayName
                }
                state.connect(account)
            },
        )
        Screen.Mail -> {
            val folders by state.folders.collectAsState()
            val selectedFolder by state.selectedFolder.collectAsState()
            val envelopes by state.envelopes.collectAsState()
            val listLoading by state.listLoading.collectAsState()
            val selectedMessage by state.selectedMessage.collectAsState()
            val messageLoading by state.messageLoading.collectAsState()
            val attachmentNote by state.attachmentNote.collectAsState()
            val accountEmail by state.accountEmail.collectAsState()

            MailShell(
                accountEmail = accountEmail,
                profileName = profileName,
                profileEmoji = profileEmoji,
                profileColor = avatarColor(profileColorHex),
                folders = folders,
                selectedFolder = selectedFolder,
                envelopes = envelopes,
                listLoading = listLoading,
                selectedMessage = selectedMessage,
                messageLoading = messageLoading,
                attachmentNote = attachmentNote,
                error = error,
                dark = dark,
                onToggleTheme = onToggleTheme,
                onOpenProfile = { profileOpen = true },
                onSelectFolder = state::selectFolder,
                onRefresh = state::refreshCurrentFolder,
                onOpenMessage = state::openMessage,
                onSaveAttachment = state::saveAttachment,
            )

            if (profileOpen) {
                ProfileDialog(
                    currentName = profileName,
                    currentEmoji = profileEmoji,
                    currentColorHex = profileColorHex,
                    onSave = { name, emoji, colorHex ->
                        profileName = name
                        profileEmoji = emoji
                        profileColorHex = colorHex
                        AppSettings.profileName = name
                        AppSettings.profileEmoji = emoji
                        AppSettings.avatarColor = colorHex
                        profileOpen = false
                    },
                    onSignOut = {
                        profileOpen = false
                        state.signOut()
                    },
                    onDismiss = { profileOpen = false },
                )
            }
        }
    }
}
