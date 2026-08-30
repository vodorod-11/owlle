package app.olwle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.olwle.theme.OlwleTheme
import app.olwle.ui.AccountSetupScreen
import app.olwle.ui.MailShell

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "olwle",
        state = rememberWindowState(size = DpSize(1200.dp, 780.dp)),
    ) {
        OlwleTheme {
            App()
        }
    }
}

@Composable
private fun App() {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(scope) }

    val screen by state.screen.collectAsState()
    val connecting by state.connecting.collectAsState()
    val error by state.error.collectAsState()

    when (screen) {
        Screen.Setup -> AccountSetupScreen(
            connecting = connecting,
            error = error,
            onConnect = state::connect,
        )
        Screen.Mail -> {
            val folders by state.folders.collectAsState()
            val selectedFolder by state.selectedFolder.collectAsState()
            val envelopes by state.envelopes.collectAsState()
            val listLoading by state.listLoading.collectAsState()
            val selectedMessage by state.selectedMessage.collectAsState()
            val messageLoading by state.messageLoading.collectAsState()
            val accountEmail by state.accountEmail.collectAsState()

            MailShell(
                accountEmail = accountEmail,
                folders = folders,
                selectedFolder = selectedFolder,
                envelopes = envelopes,
                listLoading = listLoading,
                selectedMessage = selectedMessage,
                messageLoading = messageLoading,
                error = error,
                onSelectFolder = state::selectFolder,
                onRefresh = state::refreshCurrentFolder,
                onOpenMessage = state::openMessage,
            )
        }
    }
}
