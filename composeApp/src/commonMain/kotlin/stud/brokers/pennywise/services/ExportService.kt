package stud.brokers.pennywise.services

import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.Result

/**
 * Platform-specific service for exporting transaction data to a PDF file.
 *
 * This is an `expect` declaration — each platform provides its own `actual` implementation:
 * - **Android**: writes to the app's external Downloads directory via
 * [android.os.Environment.DIRECTORY_DOWNLOADS].
 * - **JVM/Desktop**: writes to `~/Documents/` in the user's home directory.
 *
 * The HTML content is built by [stud.brokers.pennywise.util.InvoiceGenerator] and is completely cross-platform.
 *
 * File I/O runs on [kotlinx.coroutines.Dispatchers.IO].
 */
expect class ExportService {

  /**
   * Takes an HTML invoice string and converts/saves it as a PDF document.
   *
   * @param htmlContent The formatted HTML invoice string.
   * @return [Result.Success] with [Unit] on success, or [Result.Error] with
   * [Result.ErrorType.FILESYSTEM] if the file could not be written.
   */
  suspend fun exportToPdf(htmlContent: String): Result<Unit>
}
