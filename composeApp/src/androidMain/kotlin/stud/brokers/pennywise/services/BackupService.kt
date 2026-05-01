package stud.brokers.pennywise.services

import stud.brokers.pennywise.models.BackupPayload
import kotlinx.serialization.*
import android.content.Context
import kotlinx.serialization.json.Json
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.ErrorType
import java.io.File

actual class BackupService(private val context: Context){
    private val BACKUP_FILE_NAME = "internal_backup.json"
    private val json = Json {
        prettyPrint = true;
        ignoreUnknownKeys = true
    }
    private fun <T> catchErrors(block: () -> T): stud.brokers.pennywise.util.Result<T> {
        return try {
            stud.brokers.pennywise.util.Result.Success(block())
        } catch (e: Exception) {
            Result.Error(
                message = e.message ?: "Backup error",
                type = ErrorType.FILESYSTEM
            )
        }
    }

    actual suspend fun createSnapshot(payload: BackupPayload): Result<Unit> = catchErrors {
        val dir = File(context.filesDir, "snapshots")
        if(!dir.exists()) dir.mkdirs()

        val file = File(dir,BACKUP_FILE_NAME)
        val jsonString = json.encodeToString(payload)
        file.writeText(jsonString)
    }
    actual suspend fun loadLastSnapshot(): Result<BackupPayload?> = catchErrors {
        val dir = File(context.filesDir,"snapshots")
        val file = File(dir,BACKUP_FILE_NAME)

        if(!file.exists()) {
            return@catchErrors null
        } else {
            val jsonString = file.readText()
            json.decodeFromString<BackupPayload>(jsonString)
        }
    }
}