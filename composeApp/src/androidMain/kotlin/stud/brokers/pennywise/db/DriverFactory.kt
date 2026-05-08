package stud.brokers.pennywise.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import stud.brokers.pennywise.PennyWiseDatabase

/**
 * Android implementation of [DriverFactory].
 *
 * Uses [AndroidSqliteDriver] which delegates to Android's built-in SQLite engine. The database file
 * is managed by the Android system at the app's private data path. Schema creation and migrations
 * are handled automatically by the driver.
 *
 * @param context Android [Context] required by [AndroidSqliteDriver].
 */
actual class DriverFactory(private val context: Context) {
  /**
   * Creates and initializes the [AndroidSqliteDriver].
   *
   * @return A configured [SqlDriver] ready for database operations.
   */
  actual fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(
            schema = PennyWiseDatabase.Schema,
            context = context,
            name = "pennywise.db"
    )
  }
}

