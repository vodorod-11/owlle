package app.owlle

import java.util.prefs.Preferences

/** Small non-secret preferences (theme, profile). Credentials never go here. */
object AppSettings {
    private val prefs = Preferences.userRoot().node("app/owlle")

    var darkMode: Boolean
        get() = prefs.getBoolean("darkMode", false)
        set(value) = prefs.putBoolean("darkMode", value)

    var profileName: String
        get() = prefs.get("profileName", "")
        set(value) = prefs.put("profileName", value)

    var profileEmoji: String
        get() = prefs.get("profileEmoji", "🦉")
        set(value) = prefs.put("profileEmoji", value)
}
