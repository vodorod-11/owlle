package app.olwle.core.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

object DriverFactory {

    /** Opens (and creates on first run) the local message cache. */
    fun createDatabase(): OlwleDb {
        val dir = File(System.getProperty("user.home"), "Library/Application Support/olwle")
            .takeIf { System.getProperty("os.name").contains("Mac", ignoreCase = true) }
            ?: File(System.getProperty("user.home"), ".local/share/olwle")
        dir.mkdirs()

        val dbFile = File(dir, "olwle.db")
        val fresh = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", Properties())
        if (fresh) {
            OlwleDb.Schema.create(driver)
        }
        return OlwleDb(driver)
    }
}
