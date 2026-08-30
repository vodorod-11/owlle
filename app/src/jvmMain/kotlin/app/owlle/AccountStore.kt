package app.owlle

import app.owlle.core.model.MailAccount
import com.github.javakeyring.Keyring
import java.util.prefs.Preferences

/**
 * Persists the signed-in account. Non-secret fields go to Preferences;
 * the password goes to the OS credential store (macOS Keychain,
 * Windows Credential Manager, GNOME Keyring). If no credential store
 * is available, the password is simply not persisted and the user
 * signs in again next launch — it is never written to disk in plain text.
 */
object AccountStore {
    private const val SERVICE = "owlle"
    private val prefs = Preferences.userRoot().node("app/owlle/account")

    fun save(account: MailAccount) {
        prefs.put("email", account.email)
        prefs.put("displayName", account.displayName)
        prefs.put("imapHost", account.imapHost)
        prefs.putInt("imapPort", account.imapPort)
        prefs.put("username", account.username)
        prefs.putBoolean("useSsl", account.useSsl)
        runCatching {
            Keyring.create().use { it.setPassword(SERVICE, account.email, account.password) }
        }
    }

    /** Full account including password, or null if either part is missing. */
    fun load(): MailAccount? {
        val config = loadConfigOnly() ?: return null
        val password = runCatching {
            Keyring.create().use { it.getPassword(SERVICE, config.email) }
        }.getOrNull() ?: return null
        return config.copy(password = password)
    }

    /** Non-secret fields only — used to prefill the setup form. */
    fun loadConfigOnly(): MailAccount? {
        val email = prefs.get("email", "")
        val host = prefs.get("imapHost", "")
        if (email.isBlank() || host.isBlank()) return null
        return MailAccount(
            displayName = prefs.get("displayName", ""),
            email = email,
            imapHost = host,
            imapPort = prefs.getInt("imapPort", 993),
            username = prefs.get("username", email),
            password = "",
            useSsl = prefs.getBoolean("useSsl", true),
        )
    }

    fun clear() {
        val email = prefs.get("email", "")
        if (email.isNotBlank()) {
            runCatching { Keyring.create().use { it.deletePassword(SERVICE, email) } }
        }
        runCatching { prefs.clear() }
    }
}
