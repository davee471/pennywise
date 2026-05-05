package stud.brokers.pennywise.services

import android.content.Context
import android.os.Environment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import stud.brokers.pennywise.models.BackupPayload
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.ErrorType

/**
 * Android implementation of [BackupService].
 *
 * Snapshots are written to the public Downloads directory at
 * `Downloads/pennywise_backup.json`. This makes it easy for the user to import and export data manually.
 *
 * Only one snapshot is kept at a time. Each [createSnapshot] call overwrites the previous file. All
 * file I/O runs on [Dispatchers.IO].
 *
 */
actual class BackupService(private val context: Context) {

  /** The snapshot filename. */
  private val BACKUP_FILE_NAME = "pennywise_backup.json"

  private val snapshotDir by lazy { 
      val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      if (!dir.exists()) dir.mkdirs()
      dir
  }

  /**
   * JSON serializer configured with:
   * - [prettyPrint] for human-readable output if the user ever inspects the file.
   * - [ignoreUnknownKeys] for forward compatibility if new fields are added.
   * - [coerceInputValues] to handle null values in older backups gracefully.
   */
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
  }

  /**
   * Wraps a file I/O operation in a try/catch on [Dispatchers.IO]. Any [Throwable] is caught and
   * returned as [Result.Error] with [ErrorType.FILESYSTEM]. Uses [Throwable] instead of [Exception]
   * to also catch [OutOfMemoryError] on large backups.
   */
  private suspend fun <T> catchErrors(block: suspend () -> T): Result<T> {
    return withContext(Dispatchers.IO) {
      try {
        Result.Success(block())
      } catch (e: Throwable) {
        Result.Error(message = e.message ?: "Backup error", type = ErrorType.FILESYSTEM)
      }
    }
  }

  /**
   * Serializes [payload] to JSON and writes it to `filesDir/snapshots/internal_backup.json`.
   * Creates the snapshots directory if it does not exist yet.
   *
   * @param payload The [BackupPayload] to persist.
   * @return [Result.Success] with [Unit] on success, or [Result.Error] with [ErrorType.FILESYSTEM]
   * if the directory could not be created or the file could not be written.
   */
  actual suspend fun createSnapshot(payload: BackupPayload): Result<Unit> = catchErrors {
    if (!snapshotDir.exists()) snapshotDir.mkdirs()

    val file = File(snapshotDir, BACKUP_FILE_NAME)
    val jsonString = json.encodeToString(payload)
    file.writeText(jsonString)
  }

  /**
   * Reads and deserializes the last snapshot from `filesDir/snapshots/internal_backup.json`.
   *
   * @return [Result.Success] containing the [BackupPayload], or `null` if no snapshot file exists
   * yet. Returns [Result.Error] if the file exists but cannot be read or parsed.
   */
  actual suspend fun loadLastSnapshot(): Result<BackupPayload?> = catchErrors {
    val file = File(snapshotDir, BACKUP_FILE_NAME)

    if (!file.exists()) {
      null
    } else {
      val jsonString = file.readText()
      json.decodeFromString<BackupPayload>(jsonString)
    }
  }
}
