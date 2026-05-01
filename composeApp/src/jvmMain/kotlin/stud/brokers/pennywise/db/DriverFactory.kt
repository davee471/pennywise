package stud.brokers.pennywise.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import stud.brokers.pennywise.PennyWiseDatabase
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = File("pennywise.db")
        // Check if the file exists BEFORE the driver connects and creates an empty one
        val isNewDatabase = !dbFile.exists()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        // Only create the tables if this is a brand new database
        if (isNewDatabase) {
            PennyWiseDatabase.Schema.create(driver)
        }

        return driver
    }
}