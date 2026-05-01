package stud.brokers.pennywise.services

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.buildCsv

actual class ExportService {
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

