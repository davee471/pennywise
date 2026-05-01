package stud.brokers.pennywise.services

import android.os.Environment
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.buildCsv
import stud.brokers.pennywise.util.Result
import java.io.File
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ExportService(private val context: Context) {
    actual suspend fun exportToCsv(transactions: List<Transaction>): Result<Unit> {
        return withContext(Dispatchers.IO){
        try {
          val csv = buildCsv(transactions)
          val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
          if(dir == null){
            return@withContext Result.Error("External Storage is currently unavailable", Result.ErrorType.FILESYSTEM)
          } 
        
          val file = File(dir,"transactions_${System.currentTimeMillis()}.csv")
          file.writeText(csv)
          Result.Success(Unit)
  
        } catch (e: Exception) {
            Result.Error(e.message ?: "CSV Export Failed", Result.ErrorType.FILESYSTEM)
        }
    }
}
