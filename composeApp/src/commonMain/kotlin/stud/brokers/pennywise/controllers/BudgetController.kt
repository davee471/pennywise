package stud.brokers.pennywise.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import stud.brokers.pennywise.models.*
import  stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.util.Result

class BudgetController(private val dbManager: DatabaseManager) {

    enum class CycleStatus { NO_CYCLE, ACTIVE, FINAL_DAY, EXPIRED }
    var activeCycle: BudgetCycle? = null
        private set

    init {
        CoroutineScope(Dispatchers.Default).launch{loadActiveCycle()}
    }

    suspend fun loadActiveCycle() {
        activeCycle = when (val result = dbManager.fetchCycle()) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
        if(isCycleExpired){
            handleExpiredCycle()
        }
    }

    suspend fun initCycle(totalAmount: Double, start: LocalDate, end: LocalDate) {
        val cycle = BudgetCycle(
            totalAllowance = totalAmount,
            startDate = start,
            endDate = end
        )
        dbManager.saveCycle(cycle)
        activeCycle = cycle
    }

    suspend fun getRemainingAllowance(): Double {
        val cycle = activeCycle ?: return 0.0
        val transactions = when (val result = dbManager.fetchTransactions()) {
            is Result.Success -> result.data
            is Result.Error -> return 0.0
        }
        val spent = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        return cycle.totalAllowance - spent
    }

    suspend fun getDailyLimit(): Double {
        val cycle = activeCycle ?: return 0.0
        val remaining = getRemainingAllowance()
        return cycle.calculateLimit(remaining)
    }

    suspend fun addIncome(amount: Double) {
        val currentCycle = activeCycle ?: return
        // TEMPORARY HANDLING OF TRANSACTION UNTIL WE FIGURE OUT CLEANER DECOUPLED WAY
        val tx = Transaction(
            amount = amount,
            cycleId = currentCycle.id,
            type = TransactionType.INCOME,
            category = Category(0, "Top-up", "income")
        )
        dbManager.saveTransaction(tx)
        getRemainingAllowance()
        val updatedCycle = currentCycle.copy(
            totalAllowance = currentCycle.totalAllowance + amount
        )
        dbManager.updateCycleBudget(updatedCycle.id, updatedCycle.totalAllowance)
        activeCycle = updatedCycle
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
            val currentCycle = activeCycle?: return false
            return currentCycle.remainingDays == 1
        }

    val isCycleExpired: Boolean
        get() {
            val currentCycle = activeCycle?: return false
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return today > currentCycle.endDate
        }

    val cycleStatus: CycleStatus
        get() = when {
            activeCycle == null  -> CycleStatus.NO_CYCLE
            isCycleExpired       -> CycleStatus.EXPIRED
            isOnFinalDay         -> CycleStatus.FINAL_DAY
            else                 -> CycleStatus.ACTIVE
        }

    suspend fun checkLowBudget(): Boolean {
        val currentCycle = activeCycle ?: return false
        return getRemainingAllowance() <= (0.2 * currentCycle.totalAllowance)
    }

    private suspend fun handleExpiredCycle() {
        val cycle = activeCycle ?: return
        dbManager.deleteCycle(cycle.id)
        activeCycle = null
    }
}
