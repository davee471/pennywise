package stud.brokers.pennywise.services

import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.Result

/**
 * Platform-specific service for exporting transaction data to a CSV file.
 *
 * This is an `expect` declaration — each platform provides its own `actual` implementation:
 * - **Android**: writes to the app's external Downloads directory via
 * [android.os.Environment.DIRECTORY_DOWNLOADS].
 * - **JVM/Desktop**: writes to `~/Documents/` in the user's home directory.
 *
 * The CSV content is built by [stud.brokers.pennywise.util.buildCsv] in commonMain and is identical
 * across platforms. Only the file writing is platform-specific.
 *
 * File I/O runs on [kotlinx.coroutines.Dispatchers.IO].
 */
expect class ExportService {

  /**
   * Builds a CSV string from [transactions] and writes it to the platform export directory with a
   * timestamped filename.
   *
   * CSV format: `date,type,category,amount` — one row per transaction.
   *
   * @param transactions The list of transactions to export.
   * @return [Result.Success] with [Unit] on success, or [Result.Error] with
   * [Result.ErrorType.FILESYSTEM] if the file could not be written.
   */
  suspend fun exportToCsv(transactions: List<Transaction>): Result<Unit>
}
