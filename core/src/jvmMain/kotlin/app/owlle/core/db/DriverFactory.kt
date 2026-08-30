package app.owlle.core.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

object DriverFactory {

    /** Opens (and creates on first run) the local message cache. */
    fun createDatabase(): OwlleDb {
        val dir = File(System.getProperty("user.home"), "Library/Application Support/owlle")
            .takeIf { System.getProperty("os.name").contains("Mac", ignoreCase = true) }
            ?: File(System.getProperty("user.home"), ".local/share/owlle")
        dir.mkdirs()

        val dbFile = File(dir, "owlle.db")
        val fresh = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", Properties())
        if (fresh) {
            OwlleDb.Schema.create(driver)
        }
        return OwlleDb(driver)
    }
}
