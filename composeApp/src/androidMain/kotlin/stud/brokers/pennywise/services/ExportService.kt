package stud.brokers.pennywise.services

import android.os.Environment
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.buildCsv
import stud.brokers.pennywise.util.Result
import java.io.File
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import stud.brokers.pennywise.services.NotificationService

/**
 * Android implementation of [ExportService].
 *
 * Writes CSV files to the app's external Downloads directory via
 * [Context.getExternalFilesDir] with [Environment.DIRECTORY_DOWNLOADS].
 * This path is accessible to the user via their file manager without
 * requiring the `WRITE_EXTERNAL_STORAGE` permission on Android 10+.
 *
 * Each export produces a new timestamped file — existing exports are never overwritten.
 * All file I/O runs on [Dispatchers.IO].
 *
 * @param context Android [Context] used to resolve the external Downloads directory.
 */

actual class ExportService(private val context: Context, private val noti: NotificationService) {
    /**
     * Builds a CSV string from [transactions] using [buildCsv] and writes it to
     * the external Downloads directory with a timestamped filename
     * (`transactions_<epochMillis>.csv`).
     *
     * Returns [Result.Error] immediately if external storage is unavailable
     * (e.g. SD card removed, storage not mounted) without attempting to write.
     *
     * @param transactions The list of transactions to export.
     * @return [Result.Success] with [Unit] on success, or [Result.Error] with
     *   [Result.ErrorType.FILESYSTEM] if storage is unavailable or the write fails.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    actual suspend fun exportToCsv(transactions: List<Transaction>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val csv = buildCsv(transactions)
                val fileName = "transactions_${System.currentTimeMillis()}.csv"

                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.Error(
                        "Failed to create file",
                        Result.ErrorType.FILESYSTEM
                    )

                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csv.toByteArray())
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                //this is just a test
                 noti.sendAlert("test")
                noti.sendAlert("Saved csv file to $fileName")

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "CSV Export Failed", Result.ErrorType.FILESYSTEM)
            }
        }
    }
}

