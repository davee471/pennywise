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
 * A snapshot is self-contained: restoring from a [BackupPayload] fully replaces the current
 * database state with the categories, cycles, transactions, and settings it carries. No external
 * references are needed.
 *
 * @property timestamp Unix epoch milliseconds at the time the backup was created. Defaults to the
 * current system time at the moment of object construction.
 * @property categories All [Category] entries at the time of backup, including both built-in
 * defaults and any user-defined categories.
 * @property cycles All [BudgetCycle] records ever created by the user, including historical ones.
 * @property transactions All [Transaction] records across every cycle.
 * @property settings All persisted application settings as key-value string pairs (e.g. PIN
 * enabled state, selected currency code, notification preferences).
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
     * The calendar date on which this backup was created, derived from [timestamp].
     *
     * Computed lazily on each access using the device's current local timezone, so it reflects the
     * user's local date rather than UTC. Primarily used for display purposes (e.g. "Backup from
     * May 8, 2026") in the restore screen.
     */
    val date: LocalDate
        get() =
                Instant.fromEpochMilliseconds(timestamp)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
}
