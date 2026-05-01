package stud.brokers.pennywise.services

import android.os.Environment
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.buildCsv
import stud.brokers.pennywise.util.Result
import java.io.File
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stud.brokers.pennywise.util.buildPdf
actual class ExportService(private val context: Context) {
    actual suspend fun exportToCsv(transactions: List<Transaction>): Result<Unit> {
        return try {
            val csv = buildCsv(transactions)
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, "pennywise_export.csv")

            file.writeText(csv)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "CSV Export Failed", Result.ErrorType.FILESYSTEM)
        }
    }
}