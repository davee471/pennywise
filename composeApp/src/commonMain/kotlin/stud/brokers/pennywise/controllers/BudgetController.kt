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

class BudgetController(
    private val dbManager: DatabaseManager,
    private val txController: TransactionController
) {

    enum class CycleStatus { NO_CYCLE, ACTIVE, FINAL_DAY, EXPIRED }

    var activeCycle by mutableStateOf<BudgetCycle?>(null)
        private set

    var spentToday by mutableStateOf(0.0)
        private set

    var isLoaded by mutableStateOf(false)
        private set

    init {
        CoroutineScope(Dispatchers.Default).launch {
            loadActiveCycle()
            refreshSpentToday()
            isLoaded = true
        }
    }

    var dailyLimit by mutableStateOf(0.0)
        private set

    var isLowBudget by mutableStateOf(false)
        private set

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

    private suspend fun getRemainingAllowance(): Double {
        val currentCycle = activeCycle ?: return 0.0
        val transactions = when (val result = txController.getHistory(currentCycle.id)) {
            is Result.Success -> result.data
            is Result.Error -> return 0.0
        }
        val spent = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        return currentCycle.totalAllowance - spent
    }

    private suspend fun handleExpiredCycle() {
        val cycle = activeCycle ?: return
        dbManager.deleteCycle(cycle.id)
        activeCycle = null
    }

    suspend fun logExpense(amount: Double, category: Category) {
        val currentCycle = activeCycle ?: return

        txController.logExpense(amount, category, currentCycle.id)
        refreshSpentToday()
    }

    suspend fun refreshSpentToday() {
        val currentCycle = activeCycle ?: run {
            spentToday = 0.0
            dailyLimit = 0.0
            isLowBudget = false
            return
        }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // 1. Get all expenses
        val transactions = when (val result = txController.getHistory(currentCycle.id)) {
            is Result.Success -> result.data
            else -> emptyList()
        }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }

        // 2. Separate past expenses from today's expenses
        val spentBeforeTodayAmount = expenses.filter { it.date < today }.sumOf { it.amount }
        spentToday = expenses.filter { it.date == today }.sumOf { it.amount } // Actually updates your variable!

        // 3. Update low budget warning (using total remaining)
        val totalRemaining = currentCycle.totalAllowance - expenses.sumOf { it.amount }
        isLowBudget = totalRemaining <= (0.2 * currentCycle.totalAllowance)

        // 4. Calculate the true daily limit!
        val remainingBeforeToday = currentCycle.totalAllowance - spentBeforeTodayAmount
        val baseLimit = currentCycle.calculateLimit(remainingBeforeToday) // e.g. 3000 / 30 = 100
        val rawDayLimit = baseLimit - spentToday // e.g. 100 - 50 = 50

        dailyLimit = (truncate(rawDayLimit * 100) / 100).coerceAtLeast(0.0)
    }

    suspend fun getDailyLimit(): Double {
        refreshSpentToday() // Force a fresh calculation
        return dailyLimit
    }

    suspend fun initCycle(totalAmount: Double, start: LocalDate, end: LocalDate) {
        val currentCycle = BudgetCycle(
            totalAllowance = totalAmount,
            startDate = start,
            endDate = end
        )
        dbManager.saveCycle(currentCycle)
        loadActiveCycle()
    }

    suspend fun addIncome(amount: Double) {
        val currentCycle = activeCycle ?: return
        txController.logIncome(amount, currentCycle.id)

        val updatedCycle = currentCycle.copy(
            totalAllowance = currentCycle.totalAllowance + amount
        )
        dbManager.updateCycleBudget(updatedCycle.id, updatedCycle.totalAllowance)
        loadActiveCycle()
    }

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

    val isOnFinalDay: Boolean
        get() {
            val currentCycle = activeCycle ?: return false
            return currentCycle.remainingDays == 1
        }

    val isCycleExpired: Boolean
        get() {
            val currentCycle = activeCycle ?: return false
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return today > currentCycle.endDate
        }

    val cycleStatus: CycleStatus
        get() = when {
            activeCycle == null -> CycleStatus.NO_CYCLE
            isCycleExpired -> CycleStatus.EXPIRED
            isOnFinalDay -> CycleStatus.FINAL_DAY
            else -> CycleStatus.ACTIVE
        }
}
