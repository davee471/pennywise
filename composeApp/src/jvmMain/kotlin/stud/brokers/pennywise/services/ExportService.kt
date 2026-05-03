package stud.brokers.pennywise.services

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.buildCsv

/**
 * JVM/Desktop implementation of [ExportService].
 *
 * Writes CSV files to `~/Documents/` in the user's home directory. Creates the Documents directory
 * if it does not exist. Each export produces a new timestamped file — existing exports are never
 * overwritten. All file I/O runs on [Dispatchers.IO].
 */
actual class ExportService {

  /**
   * Builds a CSV string from [transactions] using [buildCsv] and writes it to
   * `~/Documents/pennywise_export_<epochMillis>.csv`.
   *
   * @param transactions The list of transactions to export.
   * @return [Result.Success] with [Unit] on success, or [Result.Error] with
   * [Result.ErrorType.FILESYSTEM] if the file could not be written.
   */
  actual suspend fun exportToCsv(transactions: List<Transaction>): Result<Unit> {
    return withContext(Dispatchers.IO) {
      try {
        val csvData = buildCsv(transactions)

        val homeDir = System.getProperty("user.home")

        val targetDir = File(homeDir, "Documents")

        if (!targetDir.exists()) {
          targetDir.mkdirs()
        }

        val file = File(targetDir, "pennywise_export_${System.currentTimeMillis()}.csv")

        file.writeText(csvData)
        Result.Success(Unit)
      } catch (e: Exception) {
        Result.Error(e.message ?: "CSV Export Failed", Result.ErrorType.FILESYSTEM)
      }
    }
  }
}
