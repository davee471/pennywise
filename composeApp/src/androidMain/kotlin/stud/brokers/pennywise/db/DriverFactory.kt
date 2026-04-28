package stud.brokers.pennywise.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import stud.brokers.pennywise.PennyWiseDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = PennyWiseDatabase.Schema,
            context = context,
            name = "pennywise.db"
        )
    }
}