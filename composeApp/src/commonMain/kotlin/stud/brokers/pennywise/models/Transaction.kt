package stud.brokers.pennywise.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Distinguishes whether a [Transaction] represents money leaving or entering the user's budget.
 *
 * - [EXPENSE] — money spent; subtracted from the active [BudgetCycle]'s remaining balance.
 * - [INCOME]  — money received; added to the active [BudgetCycle]'s remaining balance.
 */
enum class TransactionType { EXPENSE, INCOME }

/**
 * Represents a single financial event (a purchase or income entry) recorded by the user.
 *
 * Transactions are always associated with a [BudgetCycle] via [cycleId] and are classified by a
 * [Category]. The [amount] is always stored as a positive value; the [type] field determines
 * whether it is debited or credited.
 *
 * Instances are serialized to JSON as part of a [BackupPayload] and persisted in the local
 * database. Because [category] is embedded by value, historical transaction records remain intact
 * even if the originating category is later renamed or deleted.
 *
 * @property id Unique database identifier. Defaults to `0` for unsaved/transient instances.
 * @property cycleId Foreign key referencing the [BudgetCycle] this transaction belongs to.
 * @property amount Absolute monetary value of the transaction. Always positive; use [type] to
 * determine its effect on the budget balance.
 * @property type Whether this is an [TransactionType.EXPENSE] or [TransactionType.INCOME].
 * @property category The [Category] used to classify this transaction.
 * @property timestamp Unix epoch milliseconds at which the transaction was recorded. Defaults to
 * the current system time at the moment of object creation.
 */
@Serializable
data class Transaction(
    val id: Long = 0,
    val cycleId: Long,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
) {
    /**
     * The calendar date on which this transaction was recorded, derived from [timestamp].
     *
     * Computed lazily on each access using the device's current local timezone, so it correctly
     * reflects the user's local date rather than UTC.
     */
    val date: LocalDate
        get() = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
}
