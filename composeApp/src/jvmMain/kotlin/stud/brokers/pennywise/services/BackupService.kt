package stud.brokers.pennywise.services

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import stud.brokers.pennywise.models.BackupPayload
import stud.brokers.pennywise.util.Result

actual class BackupService {
  private val BACKUP_FILE_NAME = "internal_backup.json"

  private val backupDir by lazy { File(System.getProperty("user.home"), ".pennywise/snapshot") }

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
        Result.Error(message = e.message ?: "Backup error", type = Result.ErrorType.FILESYSTEM)
      }
    }
  }

  actual suspend fun createSnapshot(payload: BackupPayload): Result<Unit> = catchErrors {
    if (!backupDir.exists()) {
      val created = backupDir.mkdirs()
      if (!created && !backupDir.exists()) throw Exception("Could not create backup directory")
    }

    val file = File(backupDir, BACKUP_FILE_NAME)
    val jsonString = json.encodeToString(payload)
    file.writeText(jsonString)
  }

  actual suspend fun loadLastSnapshot(): Result<BackupPayload?> = catchErrors {
    val file = File(backupDir, BACKUP_FILE_NAME)
    if (!file.exists()) return@catchErrors null

    val jsonString = file.readText()
    json.decodeFromString<BackupPayload>(jsonString)
  }
}
