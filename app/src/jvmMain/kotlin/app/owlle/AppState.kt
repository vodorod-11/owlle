package app.owlle

import app.owlle.core.backend.MailBackendException
import app.owlle.core.db.DriverFactory
import app.owlle.core.imap.ImapBackend
import app.owlle.core.model.AttachmentMeta
import app.owlle.core.model.Envelope
import app.owlle.core.model.MailAccount
import app.owlle.core.model.MailFolder
import app.owlle.core.model.MessageContent
import app.owlle.core.store.MailRepository
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
    val attachmentNote = MutableStateFlow<String?>(null)
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
                AccountStore.save(account)
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

    /** Reconnects with the stored account, if one exists with a retrievable password. */
    fun autoConnect() {
        if (repository != null || connecting.value) return
        AccountStore.load()?.let(::connect)
    }

    fun signOut() {
        scope.launch {
            runCatching { backend.close() }
            AccountStore.clear()
            folderJob?.cancel()
            envelopeJob?.cancel()
            folders.value = emptyList()
            selectedFolder.value = null
            envelopes.value = emptyList()
            selectedMessage.value = null
            attachmentNote.value = null
            accountEmail.value = ""
            error.value = null
            repository = null
            screen.value = Screen.Setup
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

    fun saveAttachment(attachment: AttachmentMeta) {
        val repo = repository ?: return
        val folder = selectedFolder.value ?: return
        val message = selectedMessage.value ?: return
        scope.launch {
            attachmentNote.value = "Saving ${attachment.name}…"
            try {
                val path = repo.saveAttachment(folder, message.uid, attachment)
                attachmentNote.value = "Saved to $path"
            } catch (e: Exception) {
                attachmentNote.value = e.message ?: "Could not save ${attachment.name}"
            }
        }
    }

    fun openMessage(envelope: Envelope) {
        val repo = repository ?: return
        val folder = selectedFolder.value ?: return
        scope.launch {
            messageLoading.value = true
            error.value = null
            attachmentNote.value = null
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
