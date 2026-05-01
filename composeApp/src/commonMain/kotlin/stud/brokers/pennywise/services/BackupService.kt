package stud.brokers.pennywise.services

import stud.brokers.pennywise.models.BackupPayload
import stud.brokers.pennywise.util.Result

expect class BackupService {
    suspend fun createSnapshot(payload: BackupPayload): Result<Unit>
    suspend fun loadLastSnapshot(): Result<BackupPayload?>
}
