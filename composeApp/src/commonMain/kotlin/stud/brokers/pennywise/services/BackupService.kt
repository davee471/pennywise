package stud.brokers.pennywise.services

import stud.brokers.pennywise.models.BackupPayload
import stud.brokers.pennywise.util.Result

/**
 * Platform-specific service for persisting and loading backup snapshots.
 *
 * This is an `expect` declaration — each platform provides its own `actual` implementation:
 * - **Android**: writes to the device's public Downloads directory.
 * - **JVM/Desktop**: writes to `~/Documents/` in the user's home directory.
 *
 * Snapshots are serialized as pretty-printed JSON using `kotlinx.serialization`. Only one snapshot
 * is kept at a time — each [createSnapshot] call overwrites the previous file.
 *
 * File I/O runs on [kotlinx.coroutines.Dispatchers.IO] to avoid blocking the main thread.
 */
expect class BackupService {

  /**
   * Serializes [payload] to JSON and writes it to the platform snapshot file.
   *
   * @param payload The [BackupPayload] to persist.
   * @return [Result.Success] with [Unit] on success, or [Result.Error] with
   * [Result.ErrorType.FILESYSTEM] if the file could not be written.
   */
  suspend fun createSnapshot(payload: BackupPayload): Result<Unit>

  /**
   * Reads and deserializes the last snapshot from the platform snapshot file.
   *
   * @return [Result.Success] containing the deserialized [BackupPayload], or `null` if no snapshot
   * file exists yet. Returns [Result.Error] if the file exists but cannot be read or parsed.
   */
  suspend fun loadLastSnapshot(): Result<BackupPayload?>
}
