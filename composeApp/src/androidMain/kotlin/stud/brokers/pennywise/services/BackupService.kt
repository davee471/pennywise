package stud.brokers.pennywise.services

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import stud.brokers.pennywise.models.BackupPayload
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.ErrorType

actual class BackupService(private val context: Context) {
  private val BACKUP_FILE_NAME = "internal_backup.json"

  private val snapshotDir by lazy { File(context.filesDir, "snapshots") }

  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
  }

  private suspend fun <T> catchErrors(block: suspend () -> T): Result<T> {
    return withContext(Dispatchers.IO) {
      try {
        Result.Success(block())
      } catch (e: Throwable) {
        Result.Error(message = e.message ?: "Backup error", type = ErrorType.FILESYSTEM)
      }
    }
  }

  actual suspend fun createSnapshot(payload: BackupPayload): Result<Unit> = catchErrors {
    if (!snapshotDir.exists()) snapshotDir.mkdirs()

    val file = File(snapshotDir, BACKUP_FILE_NAME)
    val jsonString = json.encodeToString(payload)
    file.writeText(jsonString)
  }

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
