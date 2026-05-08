package stud.brokers.pennywise.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import stud.brokers.pennywise.PennyWiseDatabase

/**
 * JVM/Desktop implementation of [DriverFactory].
 *
 * Uses [JdbcSqliteDriver] to connect to a local `pennywise.db` file in the working directory.
 * Unlike [AndroidSqliteDriver], the JDBC driver does NOT call [PennyWiseDatabase.Schema.create]
 * automatically — this implementation checks whether the file exists before connecting, and only
 * creates the schema on a brand-new database to avoid wiping existing data.
 */
actual class DriverFactory {
  /**
   * Creates and initializes the [JdbcSqliteDriver].
   *
   * Checks if the `pennywise.db` file exists. If it does not, a new database is created
   * along with its schema. Otherwise, it simply connects to the existing database.
   *
   * @return A configured [SqlDriver] ready for database operations.
   */
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
