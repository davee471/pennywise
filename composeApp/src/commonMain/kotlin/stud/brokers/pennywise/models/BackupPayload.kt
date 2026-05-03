package stud.brokers.pennywise.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * Represents a complete point-in-time snapshot of the PennyWise database.
 *
 * This is the data transfer object (DTO) that flows between [DatabaseManager] and
 * [stud.brokers.pennywise.services.BackupService]. It is serialized to JSON for storage and
 * deserialized back when restoring.
 *
 * All fields are marked `@Serializable` via the class-level annotation, which requires all nested
 * types ([Category], [BudgetCycle], [Transaction]) to also be `@Serializable`.
 *
 * @property timestamp Unix epoch milliseconds at the time the backup was created. Defaults to the
 * current system time.
 * @property categories All categories at the time of backup (default + user-defined).
 * @property cycles All budget cycles ever created.
 * @property transactions All transactions across all cycles.
 * @property settings All key-value settings (PIN state, currency, notification preferences).
 */
@Serializable
data class BackupPayload(
        val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
        val categories: List<Category>,
        val cycles: List<BudgetCycle>,
        val transactions: List<Transaction>,
        val settings: Map<String, String>
) {

  /**
   * The calendar date this backup was created, derived from [timestamp]. Computed lazily using the
   * device's local timezone.
   */
  val date: LocalDate
    get() =
            Instant.fromEpochMilliseconds(timestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
}
