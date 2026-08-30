package app.olwle

import app.olwle.core.backend.MailBackendException
import app.olwle.core.db.DriverFactory
import app.olwle.core.imap.ImapBackend
import app.olwle.core.model.Envelope
import app.olwle.core.model.MailAccount
import app.olwle.core.model.MailFolder
import app.olwle.core.model.MessageContent
import app.olwle.core.store.MailRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Setup : Screen
    data object Mail : Screen
}

/** Plain state holder; a shared KMP view-model layer comes with the mobile targets. */
class AppState(private val scope: CoroutineScope) {

    private val db = DriverFactory.createDatabase()
    private val backend = ImapBackend()
    private var repository: MailRepository? = null

    val screen = MutableStateFlow<Screen>(Screen.Setup)
    val connecting = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    val folders = MutableStateFlow<List<MailFolder>>(emptyList())
    val selectedFolder = MutableStateFlow<MailFolder?>(null)
    val envelopes = MutableStateFlow<List<Envelope>>(emptyList())
    val listLoading = MutableStateFlow(false)
    val selectedMessage = MutableStateFlow<MessageContent?>(null)
    val messageLoading = MutableStateFlow(false)
    val accountEmail = MutableStateFlow("")

    private var folderJob: Job? = null
    private var envelopeJob: Job? = null

    fun connect(account: MailAccount) {
        if (connecting.value) return
        connecting.value = true
        error.value = null
        scope.launch {
            try {
                backend.connect(account)
                val repo = MailRepository(db, backend, account)
                repository = repo
                accountEmail.value = account.email

                folderJob?.cancel()
                folderJob = scope.launch {
                    repo.folders.collect { list ->
                        folders.value = list
                        if (selectedFolder.value == null) {
                            list.firstOrNull()?.let { selectFolder(it) }
                        }
                    }
                }

                repo.refreshFolders()
                screen.value = Screen.Mail
            } catch (e: MailBackendException) {
                error.value = e.message
            } catch (e: Exception) {
                error.value = e.message ?: "Connection failed"
            } finally {
                connecting.value = false
            }
        }
    }

    fun selectFolder(folder: MailFolder) {
        val repo = repository ?: return
        selectedFolder.value = folder
        selectedMessage.value = null

        envelopeJob?.cancel()
        envelopeJob = scope.launch {
            repo.envelopes(folder).collect { envelopes.value = it }
        }
        refreshCurrentFolder()
    }

    fun refreshCurrentFolder() {
        val repo = repository ?: return
        val folder = selectedFolder.value ?: return
        scope.launch {
            listLoading.value = true
            error.value = null
            try {
                repo.refreshEnvelopes(folder)
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                listLoading.value = false
            }
        }
    }

    fun openMessage(envelope: Envelope) {
        val repo = repository ?: return
        val folder = selectedFolder.value ?: return
        scope.launch {
            messageLoading.value = true
            error.value = null
            try {
                selectedMessage.value = repo.loadMessage(folder, envelope.uid)
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                messageLoading.value = false
            }
        }
    }
}
