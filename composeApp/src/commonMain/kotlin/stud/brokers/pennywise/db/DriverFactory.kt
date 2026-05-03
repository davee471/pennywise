package stud.brokers.pennywise.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific factory responsible for creating the SQLite driver.
 *
 * This is an `expect` declaration — each platform provides its own `actual` implementation:
 * - **Android**: [AndroidSqliteDriver] backed by Android's built-in SQLite.
 * - **JVM/Desktop**: [JdbcSqliteDriver] backed by a local `.db` file.
 *
 * [DatabaseManager] depends on this factory and calls [createDriver] once during initialization.
 */
expect class DriverFactory {

  /**
   * Creates and returns a platform-appropriate [SqlDriver] connected to the PennyWise SQLite
   * database.
   */
  fun createDriver(): SqlDriver
}

