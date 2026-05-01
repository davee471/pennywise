package stud.brokers.pennywise.controllers

import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.services.BackupService
import stud.brokers.pennywise.util.Result

class BackupController(private val db: DatabaseManager, private val backupService: BackupService) {
  suspend fun backup(): Result<Unit> {
    return when (val payloadResult = db.fetchAllThenBuildBackup()) {
      is Result.Success -> backupService.createSnapshot(payloadResult.data)
      is Result.Error -> payloadResult
    }
  }

  suspend fun restore(): Result<Unit> {
    val snapshotResult = backupService.loadLastSnapshot()
    if (snapshotResult is Result.Error) return snapshotResult

    val payload =
            (snapshotResult as Result.Success).data
                    ?: return Result.Error("No backup Found", Result.ErrorType.FILESYSTEM)

    return db.restoreFromBackup(payload)
  }
}

