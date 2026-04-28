package stud.brokers.pennywise.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import stud.brokers.pennywise.PennyWiseDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:pennywise.db")
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        PennyWiseDatabase.Schema.create(driver)
        return driver
    }
}