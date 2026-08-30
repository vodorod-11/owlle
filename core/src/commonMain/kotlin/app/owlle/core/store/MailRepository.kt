package app.owlle.core.store

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.owlle.core.backend.MailBackend
import app.owlle.core.db.OwlleDb
import app.owlle.core.model.AttachmentMeta
import app.owlle.core.model.Envelope
import app.owlle.core.model.MailAccount
import app.owlle.core.model.MailFolder
import app.owlle.core.model.MessageContent
import app.owlle.core.model.SpecialUse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local-first mail store: the UI reads the SQLite cache reactively,
 * refresh calls hit the backend and write through. The offline
 * pending-operations queue (Mailspring/K-9 pattern) lands in a later
 * milestone alongside message actions.
 */
class MailRepository(
    private val db: OwlleDb,
    private val backend: MailBackend,
    private val account: MailAccount,
) {
    val folders: Flow<List<MailFolder>> =
        db.mailQueries.selectFolders(account.email)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    MailFolder(
                        path = row.path,
                        displayName = row.displayName,
                        specialUse = runCatching { SpecialUse.valueOf(row.specialUse) }
                            .getOrDefault(SpecialUse.CUSTOM),
                    )
                }.sortedWith(folderOrdering)
            }

    fun envelopes(folder: MailFolder, limit: Long = 100): Flow<List<Envelope>> =
        db.mailQueries.selectMessagesByFolder(account.email, folder.path, limit)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    Envelope(
                        uid = row.uid,
                        subject = row.subject,
                        fromName = row.fromName,
                        fromAddress = row.fromAddress,
                        sentAtEpochMs = row.sentAtEpochMs,
                        preview = row.preview,
                        seen = row.seen != 0L,
                    )
                }
            }

    suspend fun refreshFolders() {
        val remote = backend.folders()
        db.mailQueries.transaction {
            db.mailQueries.deleteFolders(account.email)
            remote.forEach { f ->
                db.mailQueries.upsertFolder(account.email, f.path, f.displayName, f.specialUse.name)
            }
        }
    }

    suspend fun refreshEnvelopes(folder: MailFolder, limit: Int = 50) {
        val remote = backend.envelopes(folder, limit)
        db.mailQueries.transaction {
            remote.forEach { e ->
                db.mailQueries.upsertMessage(
                    account.email, folder.path, e.uid, e.subject,
                    e.fromName, e.fromAddress, e.sentAtEpochMs, e.preview,
                    if (e.seen) 1 else 0,
                )
            }
        }
    }

    suspend fun saveAttachment(folder: MailFolder, uid: Long, attachment: AttachmentMeta): String =
        backend.saveAttachment(folder, uid, attachment)

    suspend fun loadMessage(folder: MailFolder, uid: Long): MessageContent {
        val content = backend.message(folder, uid)
        db.mailQueries.markSeen(account.email, folder.path, uid)
        return content
    }

    private val folderOrdering = compareBy<MailFolder>(
        { pinnedRank(it.specialUse) },
        { it.displayName.lowercase() },
    )

    private fun pinnedRank(use: SpecialUse): Int {
        val idx = SpecialUse.pinnedOrder.indexOf(use)
        return if (idx >= 0) idx else SpecialUse.pinnedOrder.size
    }
}
