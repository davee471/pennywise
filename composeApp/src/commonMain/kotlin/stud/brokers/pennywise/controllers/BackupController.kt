package stud.brokers.pennywise.controllers

import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.services.BackupService
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.*

/**
 * Orchestrates the backup and restore workflow for PennyWise.
 *
 * Acts as the bridge between [DatabaseManager] (which knows how to read/write the database) and
 * [BackupService] (which knows how to read/write the snapshot file). Neither dependency knows about
 * the other — this controller coordinates them.
 *
 * it exposes two simple operations ([backup] and [restore]) hiding the multi-step coordination
 * underneath.
 *
 * @param db The database manager used to fetch and restore data.
 * @param backupService The platform-specific service used to persist the snapshot.
 */
class BackupController(private val db: DatabaseManager, private val backupService: BackupService) {

  /**
   * Creates a full snapshot of the current database state and writes it to disk.
   *
   * Steps:
   * 1. Fetches all data from the database via [DatabaseManager.fetchAllThenBuildBackup].
   * 2. Serializes and writes the resulting [stud.brokers.pennywise.models.BackupPayload]
   * ```
   *    to a JSON file via [BackupService.createSnapshot].
   *
   * @return [Result.Success]
   * ```
   * with [Unit] if the backup completed successfully, or [Result.Error] if either step failed.
   */
  suspend fun backup(): Result<Unit> {
    return when (val payloadResult = db.fetchAllThenBuildBackup()) {
      is Success -> backupService.createSnapshot(payloadResult.data)
      is Error -> payloadResult
    }
  }

  /**
   * Restores the database from the last saved snapshot.
   *
   * Steps:
   * 1. Loads the last snapshot from disk via [BackupService.loadLastSnapshot].
   * 2. Passes the deserialized payload to [DatabaseManager.restoreFromBackup],
   * ```
   *    which clears the database and restores all data inside a SQLite transaction.
   *
   * @return [Result.Success]
   * ```
   * with [Unit] if the restore completed successfully. Returns [Result.Error] if no snapshot
   * exists, if the file cannot be read, or if the database restore fails.
   */
  suspend fun restore(): Result<Unit> {
    val snapshotResult = backupService.loadLastSnapshot()
    if (snapshotResult is Error) return snapshotResult

    val payload =
            (snapshotResult as Success).data
                    ?: return Error("No backup Found", ErrorType.FILESYSTEM)

    return db.restoreFromBackup(payload)
  }
}
