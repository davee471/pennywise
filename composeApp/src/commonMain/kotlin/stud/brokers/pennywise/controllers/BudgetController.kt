package stud.brokers.pennywise.controllers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.models.BudgetCycle
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.TransactionType
import stud.brokers.pennywise.util.Result
import kotlin.math.truncate

/**
 * Controller responsible for managing the active budget cycle and calculating spending limits.
 *
 * Acts as the central coordinator between the UI layer, [DatabaseManager], and [TransactionController]
 * for all budget-lifecycle operations. Responsibilities include:
 * - Loading and persisting the active [BudgetCycle].
 * - Detecting and cleaning up expired cycles.
 * - Calculating the dynamic daily spending limit based on remaining allowance and days left.
 * - Exposing reactive state (via Compose [mutableStateOf]) for low-budget and exhaustion warnings.
 *
 * All heavy work runs on [Dispatchers.Default]; UI-observable state is updated via Compose state
 * holders so recomposition is triggered automatically.
 *
 * @property dbManager The database manager used to fetch, save, update, and delete cycle records.
 * @property txController The transaction controller used to log expenses/income and query history.
 */
class BudgetController(
    private val dbManager: DatabaseManager,
    private val txController: TransactionController
) {

    /**
     * Represents the current operational state of the budget cycle.
     *
     * Used by the UI to determine which screen or prompt to show the user.
     */
    enum class CycleStatus {
        /** No cycle has been created yet. */
        NO_CYCLE,

        /** A cycle exists and is currently running. */
        ACTIVE,

        /** Today is the last day of the active cycle. */
        FINAL_DAY,

        /** The cycle's end date has passed; it will be cleaned up automatically. */
        EXPIRED
    }

    /**
     * The currently active [BudgetCycle], or `null` if no cycle has been initialized.
     *
     * Backed by Compose state — UI components observing this property will recompose whenever it
     * changes (e.g., after [initCycle], [resetCycle], or [loadActiveCycle]).
     */
    var activeCycle by mutableStateOf<BudgetCycle?>(null)
        private set

    /**
     * The total amount spent in expense transactions recorded for today's date.
     *
     * Recalculated on every call to [refreshSpentToday]. Resets to `0.0` when there is no
     * active cycle.
     */
    var spentToday by mutableStateOf(0.0)
        private set

    /**
     * Whether the initial cycle data has finished loading from the database.
     *
     * Set to `true` once the `init` block's coroutine completes. The UI should gate any
     * cycle-dependent rendering behind this flag to avoid flash-of-empty-state issues.
     */
    var isLoaded by mutableStateOf(false)
        private set

    init {
        CoroutineScope(Dispatchers.Default).launch {
            loadActiveCycle()
            refreshSpentToday()
            isLoaded = true
        }
    }

    /**
     * The calculated spending limit for the remainder of today, in the cycle's currency.
     *
     * Derived by distributing the cycle's remaining allowance (after prior-day spending) evenly
     * across remaining days, then subtracting [spentToday]. The value is floored to two decimal
     * places and clamped to `0.0` — it never goes negative, even when the user is over limit.
     *
     * Check [isOverDailyLimit] to distinguish "exactly zero" from "over limit".
     */
    var dailyLimit by mutableStateOf(0.0)
        private set

    /**
     * `true` if the remaining total allowance has fallen to 20 % or below its starting value,
     * but is not yet fully exhausted.
     *
     * Intended to trigger a low-budget warning in the UI. Mutually exclusive with [isExhausted].
     */
    var isLowBudget by mutableStateOf(false)
        private set

    /**
     * `true` if the cycle's total allowance has been completely spent (remaining ≤ 0).
     *
     * When `true`, [isLowBudget] is always `false`.
     */
    var isExhausted by mutableStateOf(false)
        private set

    /**
     * `true` if today's spending has exceeded the base daily limit calculated for this day.
     *
     * This flag allows the UI to surface an over-limit warning even though [dailyLimit] is
     * clamped to `0.0` and cannot itself go negative.
     */
    var isOverDailyLimit by mutableStateOf(false)
        private set

    /**
     * Loads the active cycle from the database and handles any expiration cleanup.
     *
     * If the fetched cycle's end date is in the past, [handleExpiredCycle] is called to delete it
     * and set [activeCycle] to `null`. After loading, [refreshSpentToday] is invoked to ensure
     * all spending metrics are up to date.
     *
     * Safe to call multiple times; idempotent when the cycle has not changed.
     */
    suspend fun loadActiveCycle() {
        activeCycle = when (val result = dbManager.fetchCycle()) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
        if (isCycleExpired) {
            handleExpiredCycle()
        }
        refreshSpentToday()
    }

    /**
     * Deletes the expired cycle from the database and clears [activeCycle].
     *
     * Called internally by [loadActiveCycle] when [isCycleExpired] is `true`. No-ops gracefully
     * if [activeCycle] is already `null`.
     */
    private suspend fun handleExpiredCycle() {
        val cycle = activeCycle ?: return
        dbManager.deleteCycle(cycle.id)
        activeCycle = null
    }

    /**
     * Logs a new expense transaction against the active cycle and refreshes spending metrics.
     *
     * Delegates actual persistence to [TransactionController.logExpense]. If there is no active
     * cycle this is a no-op.
     *
     * @param amount The monetary value of the expense. Must be greater than `0` (validated by
     *   [TransactionController]).
     * @param category The [Category] to assign to this expense.
     */
    suspend fun logExpense(amount: Double, category: Category) {
        val currentCycle = activeCycle ?: return

        txController.logExpense(amount, category, currentCycle.id)
        refreshSpentToday()
    }

    /**
     * Recalculates all spending metrics based on current transactions and the current date.
     *
     * Updates the following properties atomically:
     * - [spentToday] — sum of today's expense amounts.
     * - [dailyLimit] — remaining budget for today, floored and clamped to `0.0`.
     * - [isLowBudget] — `true` when total remaining ≤ 20 % of starting allowance.
     * - [isExhausted] — `true` when total remaining ≤ 0.
     * - [isOverDailyLimit] — `true` when raw remaining-for-today is negative.
     *
     * If there is no active cycle, all metrics are reset to their zero/false defaults and the
     * function returns early.
     */
    suspend fun refreshSpentToday() {
        val currentCycle = activeCycle ?: run {
            spentToday = 0.0
            dailyLimit = 0.0
            isLowBudget = false
            isExhausted = false
            isOverDailyLimit = false
            return
        }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val transactions = when (val result = txController.getHistory(currentCycle.id)) {
            is Result.Success -> result.data
            else -> emptyList()
        }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }

        val spentBeforeTodayAmount = expenses.filter { it.date < today }.sumOf { it.amount }
        spentToday = expenses.filter { it.date == today }.sumOf { it.amount }

        val totalRemaining = currentCycle.totalAllowance - expenses.sumOf { it.amount }
        isExhausted = totalRemaining <= 0.0
        isLowBudget = totalRemaining <= (0.2 * currentCycle.totalAllowance) && !isExhausted

        val remainingBeforeToday = currentCycle.totalAllowance - spentBeforeTodayAmount
        val baseLimit = currentCycle.calculateLimit(remainingBeforeToday)
        val rawDayLimit = baseLimit - spentToday

        isOverDailyLimit = rawDayLimit < 0.0
        dailyLimit = (truncate(rawDayLimit * 100) / 100).coerceAtLeast(0.0)
    }

    /**
     * Forces a metrics refresh and returns the up-to-date [dailyLimit].
     *
     * Prefer reading [dailyLimit] directly from state when inside a Compose context; use this
     * function when you need a guaranteed-fresh value in a suspend context (e.g., a background
     * notification worker).
     *
     * @return The current daily spending limit after refreshing all metrics.
     */
    suspend fun getDailyLimit(): Double {
        refreshSpentToday()
        return dailyLimit
    }

    /**
     * Creates and persists a new budget cycle, then reloads state.
     *
     * Any previously active cycle should be reset via [resetCycle] before calling this, as the
     * database is expected to hold at most one active cycle at a time.
     *
     * @param totalAmount The starting monetary allowance for the cycle. Must be > 0.
     * @param start The inclusive start date of the cycle.
     * @param end The inclusive end date of the cycle. Must be ≥ [start].
     */
    suspend fun initCycle(totalAmount: Double, start: LocalDate, end: LocalDate) {
        val currentCycle = BudgetCycle(
            totalAllowance = totalAmount,
            startDate = start,
            endDate = end
        )
        dbManager.saveCycle(currentCycle)
        loadActiveCycle()
    }

    /**
     * Records an income transaction and increases the active cycle's total allowance accordingly.
     *
     * The cycle's [BudgetCycle.totalAllowance] is persisted via [DatabaseManager.updateCycleBudget]
     * so future daily-limit calculations reflect the additional funds. Reloads the full cycle state
     * afterwards.
     *
     * If there is no active cycle this is a no-op.
     *
     * @param amount The monetary value of the income. Must be > 0 (validated by
     *   [TransactionController]).
     */
    suspend fun addIncome(amount: Double) {
        val currentCycle = activeCycle ?: return
        txController.logIncome(amount, currentCycle.id)

        val updatedCycle = currentCycle.copy(
            totalAllowance = currentCycle.totalAllowance + amount
        )
        dbManager.updateCycleBudget(updatedCycle.id, updatedCycle.totalAllowance)
        loadActiveCycle()
    }

    /**
     * Adjusts the active cycle's total allowance by [amount] without logging a new transaction.
     *
     * Used when editing an existing income transaction — the caller has already updated the
     * transaction record; this method only reconciles the cycle's running total. Pass a negative
     * value to reduce the allowance (e.g., when the original income amount is being decreased).
     *
     * If there is no active cycle this is a no-op.
     *
     * @param amount The delta to apply to [BudgetCycle.totalAllowance]. May be negative.
     */
    suspend fun editIncome(amount: Double) {
        val currentCycle = activeCycle ?: return
        val updatedCycle = currentCycle.copy(
            totalAllowance = currentCycle.totalAllowance + amount
        )
        dbManager.updateCycleBudget(updatedCycle.id, updatedCycle.totalAllowance)
        loadActiveCycle()
    }

    /**
     * Permanently deletes the active cycle and all its associated data from the database.
     *
     * After a successful deletion, [activeCycle] is set to `null`. If there is no active cycle,
     * returns `true` immediately (idempotent).
     *
     * @return `true` if the cycle was successfully deleted or did not exist; `false` if the
     *   database operation failed.
     */
    suspend fun resetCycle(): Boolean {
        val currentCycle = activeCycle ?: return true
        return when (dbManager.deleteCycle(currentCycle.id)) {
            is Result.Success -> {
                activeCycle = null
                true
            }

            is Result.Error -> {
                false
            }
        }
    }

    /**
     * `true` if today is the last day of the active budget cycle.
     *
     * Determined by checking whether [BudgetCycle.remainingDays] equals `1`. Returns `false` when
     * there is no active cycle.
     */
    val isOnFinalDay: Boolean
        get() {
            val currentCycle = activeCycle ?: return false
            return currentCycle.remainingDays == 1
        }

    /**
     * `true` if the current calendar date is strictly after the active cycle's [BudgetCycle.endDate].
     *
     * Returns `false` when there is no active cycle. This property drives automatic expiration
     * cleanup in [loadActiveCycle].
     */
    val isCycleExpired: Boolean
        get() {
            val currentCycle = activeCycle ?: return false
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return today > currentCycle.endDate
        }

    /**
     * The current semantic lifecycle status of the budget cycle.
     *
     * Combines [activeCycle], [isCycleExpired], and [isOnFinalDay] into a single [CycleStatus]
     * value. Evaluated fresh on every read — no caching.
     *
     * | Condition                         | Status           |
     * |-----------------------------------|------------------|
     * | `activeCycle == null`             | [CycleStatus.NO_CYCLE]   |
     * | `isCycleExpired == true`          | [CycleStatus.EXPIRED]    |
     * | `isOnFinalDay == true`            | [CycleStatus.FINAL_DAY]  |
     * | Otherwise                         | [CycleStatus.ACTIVE]     |
     */
    val cycleStatus: CycleStatus
        get() = when {
            activeCycle == null -> CycleStatus.NO_CYCLE
            isCycleExpired -> CycleStatus.EXPIRED
            isOnFinalDay -> CycleStatus.FINAL_DAY
            else -> CycleStatus.ACTIVE
        }
}