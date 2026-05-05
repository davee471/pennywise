package stud.brokers.pennywise.services

import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import stud.brokers.pennywise.util.Result
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder

/**
 * JVM/Desktop implementation of [ExportService].
 *
 * Writes CSV files to `~/Documents/` in the user's home directory. Creates the Documents directory
 * if it does not exist. Each export produces a new timestamped file — existing exports are never
 * overwritten. All file I/O runs on [Dispatchers.IO].
 */
actual class ExportService {

  /**
   * Writes the generated invoice to `~/Documents/`.
   *
   * @param htmlContent The HTML content to export.
   * @return [Result.Success] with [Unit] on success, or [Result.Error] with
   * [Result.ErrorType.FILESYSTEM] if the file could not be written.
   */
  actual suspend fun exportToPdf(htmlContent: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
      try {

        val homeDir = System.getProperty("user.home")

        var targetDir = File(homeDir, "Documents")

        // If Documents doesn't exist and we can't create it (common on Linux), fallback to the Home directory.
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            targetDir = File(homeDir)
        }

        val file = File(targetDir, "pennywise_invoice_${System.currentTimeMillis()}.pdf")

        FileOutputStream(file).use { os ->
            val builder = PdfRendererBuilder()
            builder.useFastMode() // Uses the HTML5 parser
            builder.withHtmlContent(htmlContent, null)
            builder.toStream(os)
            builder.run()
        }
        Result.Success(Unit)
      } catch (e: Exception) {
        Result.Error(e.message ?: "Invoice Export Failed", Result.ErrorType.FILESYSTEM)
      }
    }
  }
}
