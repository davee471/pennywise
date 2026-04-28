package stud.brokers.pennywise.controllers

import kotlinx.datetime.LocalDate
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.models.*

class BudgetController() {

    private val dbManager = DatabaseManager
    var activeCycle: BudgetCycle? = null
        private set

    init {
        activeCycle = dbManager.fetchCycle()
    }

    fun initCycle(totalAmount: Double, start: LocalDate, end: LocalDate){
        val cycle = BudgetCycle(
            totalAllowance = totalAmount,
            startDate = start,
            endDate = end
        )
        dbManager.saveCycle(cycle)
        activeCycle = cycle
    }

    fun getRemainingAllowance(): Double{
        val cycle = activeCycle?: return 0.0
        val spent = dbManager.fetchTransactions()
            .filter{it.type == TransactionType.EXPENSE}
            .sumOf{it.amount}
        return cycle.totalAllowance - spent
    }

    fun getDailyLimit(): Double{
        val cycle = activeCycle?: return 0.0
        val remaining = getRemainingAllowance()
        return cycle.calculateLimit(remaining)
    }

    fun addIncome(amount: Double){
        val currentCycle = activeCycle?: return

        // TEMPORARY HANDLING OF TRANSACTION UNTIL WE FIGURE OUT CLEANER DECOUPLED WAY
        val tx = Transaction(
            amount = amount,
            type = TransactionType.INCOME,
            category = Category(0, "Top-up", "income")
        )
        dbManager.saveTransaction(tx)

        val remaining = getRemainingAllowance()
        val updatedCycle = currentCycle.copy(totalAllowance = currentCycle.totalAllowance + amount)

        dbManager.saveCycle(updatedCycle)
        activeCycle = updatedCycle
    }

    fun resetCycle(): Boolean{
        val currentCycle = activeCycle?: return true
        val success = dbManager.deleteCycle(currentCycle.id)

        if(success){
            activeCycle = null
        }

        return success
    }

}