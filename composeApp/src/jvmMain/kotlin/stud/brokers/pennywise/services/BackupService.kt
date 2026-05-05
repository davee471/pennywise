package stud.brokers.pennywise.services

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import stud.brokers.pennywise.models.BackupPayload
import stud.brokers.pennywise.util.Result

/**
 * JVM/Desktop implementation of [BackupService].
 *
 * Snapshots are written to `~/Documents/pennywise_backup.json` in the user's home
 * directory.
 *
 * Only one snapshot is kept at a time. Each [createSnapshot] call overwrites the previous file. All
 * file I/O runs on [Dispatchers.IO].
 */
actual class BackupService {
  /** The snapshot filename. Fixed — only one backup is kept at a time. */
  private val BACKUP_FILE_NAME = "pennywise_backup.json"

  private val backupDir by lazy { 
      val homeDir = System.getProperty("user.home")
      var targetDir = File(homeDir, "Documents")
      if (!targetDir.exists() && !targetDir.mkdirs()) {
          targetDir = File(homeDir)
      }
      targetDir
  }

  /**
   * JSON serializer configured with:
   * - [prettyPrint] for human-readable output.
   * - [ignoreUnknownKeys] for forward compatibility.
   * - [coerceInputValues] to handle null values in older backups gracefully.
   */
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
  }

  /**
   * Wraps a file I/O operation in a try/catch on [Dispatchers.IO]. Any [Throwable] is caught and
   * returned as [Result.Error] with [Result.ErrorType.FILESYSTEM].
   */
  private suspend fun <T> catchErrors(block: suspend () -> T): Result<T> {
    return withContext(Dispatchers.IO) {
      try {
        Result.Success(block())
      } catch (e: Throwable) {
        Result.Error(message = e.message ?: "Backup error", type = Result.ErrorType.FILESYSTEM)
      }
    }
  }

  /**
   * Serializes [payload] to JSON and writes it to `~/.pennywise/snapshot/internal_backup.json`.
   * Creates the backup directory if it does not exist. Throws if directory creation fails and the
   * directory still does not exist after the attempt.
   *
   * @param payload The [BackupPayload] to persist.
   * @return [Result.Success] with [Unit] on success, or [Result.Error] with
   * [Result.ErrorType.FILESYSTEM] if the directory could not be created or the file could not be
   * written.
   */
  actual suspend fun createSnapshot(payload: BackupPayload): Result<Unit> = catchErrors {
    if (!backupDir.exists()) {
      val created = backupDir.mkdirs()
      if (!created && !backupDir.exists()) throw Exception("Could not create backup directory")
    }

    val file = File(backupDir, BACKUP_FILE_NAME)
    val jsonString = json.encodeToString(payload)
    file.writeText(jsonString)
  }

  /**
   * Reads and deserializes the last snapshot from `~/.pennywise/snapshot/internal_backup.json`.
   *
   * @return [Result.Success] containing the [BackupPayload], or `null` if no snapshot file exists
   * yet. Returns [Result.Error] if the file exists but cannot be read or parsed.
   */
  actual suspend fun loadLastSnapshot(): Result<BackupPayload?> = catchErrors {
    val file = File(backupDir, BACKUP_FILE_NAME)
    if (!file.exists()) return@catchErrors null

    val jsonString = file.readText()
    json.decodeFromString<BackupPayload>(jsonString)
  }
}
