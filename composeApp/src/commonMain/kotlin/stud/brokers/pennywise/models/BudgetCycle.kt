package stud.brokers.pennywise.models

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * Represents a user-defined budgeting period with a fixed spending allowance.
 *
 * A budget cycle spans a contiguous date range ([startDate] to [endDate]) and carries a
 * [totalAllowance] — the maximum amount the user intends to spend during that period.
 * The controller layer creates one active cycle at a time; historical cycles are retained in the
 * database for reporting purposes.
 *
 * Instances are serialized to JSON as part of a [BackupPayload].
 *
 * @property id Unique database identifier. Defaults to `0` for unsaved/transient instances.
 * @property totalAllowance The total monetary budget allocated for this cycle. Mutable so the user
 * can adjust it after the cycle has started.
 * @property startDate The first day (inclusive) of this budget cycle.
 * @property endDate The last day (inclusive) of this budget cycle.
 */
@Serializable
data class BudgetCycle(
        val id: Long = 0,
        var totalAllowance: Double,
        val startDate: LocalDate,
        val endDate: LocalDate,
) {
    /**
     * The total length of this cycle in days, from [startDate] up to (but not including)
     * [endDate].
     *
     * Computed via [LocalDate.daysUntil], so a cycle from Jan 1 to Jan 31 yields 30 days.
     */
    val totalDays: Int
        get() = startDate.daysUntil(endDate)

    /**
     * The number of days remaining in this cycle from today until [endDate].
     *
     * Uses the device's current local timezone to determine today's date. The result is clamped
     * to a minimum of `1` so that [calculateLimit] never divides by zero on the final day of the
     * cycle.
     */
    val remainingDays: Int
        get() {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return today.daysUntil(endDate).coerceAtLeast(1)
        }

    /**
     * Calculates the maximum amount the user can spend per day for the remainder of this cycle
     * without exceeding [balance].
     *
     * This is the core "daily limit" figure displayed on the home screen. It distributes the
     * remaining [balance] evenly across [remainingDays].
     *
     * @param balance The current unspent balance available for this cycle (i.e. [totalAllowance]
     * minus all expenses recorded so far).
     * @return The per-day spending limit as a [Double]. Always ≥ `0` when [balance] is
     * non-negative, because [remainingDays] is clamped to at least `1`.
     */
    fun calculateLimit(balance: Double): Double {
        return balance / remainingDays
    }
}
