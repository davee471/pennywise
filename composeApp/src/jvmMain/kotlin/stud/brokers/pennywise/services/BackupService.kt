package stud.brokers.pennywise.services

import stud.brokers.pennywise.models.BackupPayload
import stud.brokers.pennywise.util.Result
import java.io.File
import kotlinx.serialization.json.Json

actual class BackupService {
    private val BACKUP_FILE_NAME = "internal_backup.json"
    private val backupDir = File(System.getProperty("user.home"), ".pennywise/snapshot")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private fun <T> catchErrors(block: () -> T): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: Exception) {
            Result.Error(
                message = e.message ?: "Backup error",
                type = Result.ErrorType.FILESYSTEM
            )
        }
    }


    actual suspend fun createSnapshot(payload: BackupPayload): stud.brokers.pennywise.util.Result<Unit> = catchErrors {
        if(!backupDir.exists()) backupDir.mkdirs()
        val file = File(backupDir,BACKUP_FILE_NAME)
        val jsonString = json.encodeToString(payload)
        file.writeText(jsonString)
    }
    actual suspend fun loadLastSnapshot(): Result<BackupPayload?> = catchErrors{
        val file = File(backupDir,BACKUP_FILE_NAME)
        if(!file.exists()) return@catchErrors null
        val jsonString = file.readText()
        json.decodeFromString<BackupPayload>(jsonString)
    }
}