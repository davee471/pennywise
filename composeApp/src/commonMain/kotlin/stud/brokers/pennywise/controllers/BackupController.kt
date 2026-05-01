package stud.brokers.pennywise.controllers

import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.services.BackupService
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.unwrap

class BackupController (
    private val db: DatabaseManager,
    private val backupService: BackupService
){
    suspend fun backup(): Result<Unit>{
        val payload = db.fetchAllThenBuildBackup().unwrap()
        return backupService.createSnapshot(payload)
    }

    suspend fun restore(): Result<Unit>{
            val payload = backupService.loadLastSnapshot().unwrap()
                ?: return Result.Error("No backup Found", Result.ErrorType.FILESYSTEM)
                return db.restoreFromBackup(payload)
    }
}