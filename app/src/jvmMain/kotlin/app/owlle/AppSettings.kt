package app.owlle

import java.util.prefs.Preferences

/** Small non-secret preferences (theme, profile). Credentials never go here. */
object AppSettings {
    private val prefs = Preferences.userRoot().node("app/owlle")

    // macOS flushes java.util.prefs lazily (up to 30s); an explicit flush
    // makes writes survive the app being closed right after a change.
    private fun put(key: String, value: String) {
        prefs.put(key, value)
        runCatching { prefs.flush() }
    }

    var darkMode: Boolean
        get() = prefs.getBoolean("darkMode", false)
        set(value) {
            prefs.putBoolean("darkMode", value)
            runCatching { prefs.flush() }
        }

    var profileName: String
        get() = prefs.get("profileName", "")
        set(value) = put("profileName", value)

    var profileEmoji: String
        get() = prefs.get("profileEmoji", "🦉")
        set(value) = put("profileEmoji", value)

    /** Avatar circle color as RRGGBB hex; the brand gold by default. */
    var avatarColor: String
        get() = prefs.get("avatarColor", "EFAF1C")
        set(value) = put("avatarColor", value)

    /** App accent color as RRGGBB hex; drives both light and dark palettes. */
    var accentColor: String
        get() = prefs.get("accentColor", "EFAF1C")
        set(value) = put("accentColor", value)
}
