package stud.brokers.pennywise.controllers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.models.BudgetCycle
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType
import stud.brokers.pennywise.util.Result

class BudgetController(private val dbManager: DatabaseManager) {

    // Reactive state for the current budget cycle
    var activeCycle by mutableStateOf<BudgetCycle?>(null)
        private set

    init {
        // Load the cycle from database on startup
        CoroutineScope(Dispatchers.Default).launch { loadActiveCycle() }
    }

    suspend fun loadActiveCycle() {
        activeCycle = when (val result = dbManager.fetchCycle()) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
    }

    suspend fun initCycle(totalAmount: Double, start: LocalDate, end: LocalDate) {
        val cycle = BudgetCycle(
            totalAllowance = totalAmount,
            startDate = start,
            endDate = end
        )
        dbManager.saveCycle(cycle)
        // Refresh the active cycle to get the generated ID
        loadActiveCycle()
    }

    suspend fun getRemainingAllowance(): Double {
        val cycle = activeCycle ?: return 0.0
        val transactions = when (val result = dbManager.fetchTransactions()) {
            is Result.Success -> result.data
            is Result.Error -> return 0.0
        }
        // Calculate total spent from expense transactions
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
        val tx = Transaction(
            amount = amount,
            cycleId = currentCycle.id,
            type = TransactionType.INCOME,
            category = Category(0, "Top-up", "income")
        )
        dbManager.saveTransaction(tx)
        // Update the cycle total allowance in the database
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
            is Result.Error -> false
        }
    }
}
