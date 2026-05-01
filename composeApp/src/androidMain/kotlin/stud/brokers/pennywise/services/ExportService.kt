package stud.brokers.pennywise.services

import android.os.Environment
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.buildCsv
import stud.brokers.pennywise.util.Result
import java.io.File

actual class ExportService {
     actual suspend fun exportToCsv(transactions: List<Transaction>): Result<Unit>{
        return try{
            val csv = buildCsv(transactions)
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "pennywise_export.csv"
            )
        } catch (e: Exception){
            Result.Error(e.message ?: "Export Failed", Result.ErrorType.FILESYSTEM)
        } as Result<Unit>
     }
}