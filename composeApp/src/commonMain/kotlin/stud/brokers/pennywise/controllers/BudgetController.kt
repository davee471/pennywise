package stud.brokers.pennywise.controllers

import kotlinx.datetime.LocalDate
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.models.*
import stud.brokers.pennywise.util.Result

/**
 * BudgetController handles all budget cycle operations.
 * It manages the active budget cycle and provides calculations for daily limits.
 * 
 * Responsibilities:
 * - Initializing a new budget cycle (US1)
 * - Calculating daily spending limits (US3, US5)
 * - Adding income/top-ups (US13)
 * - Resetting the cycle (US11)
 */
class BudgetController {

    private val dbManager = DatabaseManager
    private val transactionController = TransactionController()
    
    var activeCycle: BudgetCycle? = null
        private set

    init {
        // Load active cycle from database on initialization
        // This ensures we have the correct ID after any save operation
        loadActiveCycle()
    }

    /**
     * Load the active budget cycle from database.
     * Called on init and after save operations to get the correct cycle ID.
     */
    private fun loadActiveCycle() {
        activeCycle = when (val result = dbManager.fetchCycle()) {
            is Result.Succuess -> result.data
            is DbResult.Error -> null
        }
    }

    /**
     * Initialize a new budget cycle with the given parameters.
     * US1: Set Initial Budget Cycle
     * 
     * @param totalAmount The total budget allowance
     * @param start Start date of the cycle
     * @param end End date of the cycle
     * @return DbResult indicating success or failure
     */
    fun initCycle(totalAmount: Double, start: LocalDate, end: LocalDate): Result<Unit> {
        // Validate inputs per US1: amount > 0, endDate > startDate
        if (totalAmount <= 0) {
            return Result.Error("Total allowance must be greater than 0")
        }
        if (end <= start) {
            return Result.Error("End date must be after start date")
        }

        val cycle = BudgetCycle(
            totalAllowance = totalAmount,
            startDate = start,
            endDate = end
        )

        // Save the cycle
        val saveResult = dbManager.saveCycle(cycle)
        
        // After saving, reload to get the database-generated ID
        loadActiveCycle()
        
        return saveResult
    }

    /**
     * Calculate the remaining allowance (total - spent).
     * Used for daily limit calculation.
     * 
     * @return Remaining allowance amount
     */
    fun getRemainingAllowance(): Double {
        val cycle = activeCycle ?: return 0.0
        
        // Fetch all transactions and sum expenses
        val fetchResult = dbManager.fetchTransactions()
        
        // If fetch fails or returns null, treat as 0 spent
        val spent = when (fetchResult) {
            is Result.Succuess -> {
                fetchResult.data
                    .filter { it.type == TransactionType.EXPENSE && it.cycleId == cycle.id }
                    .sumOf { it.amount }
            }
            is DbResult.Error -> 0.0
        }
        
        return cycle.totalAllowance - spent
    }

    /**
     * Calculate the daily spending limit based on remaining budget and days left.
     * US3: Dynamic Daily Limit View
     * US5: Daily Rollover Management (automatic via this calculation)
     * 
     * Formula: (totalAllowance - totalSpent) / remainingDays
     * 
     * @return Daily limit amount
     */
    fun getDailyLimit(): Double {
        val cycle = activeCycle ?: return 0.0
        val remaining = getRemainingAllowance()
        return cycle.calculateLimit(remaining)
    }

    /**
     * Add income/top-up to the current budget cycle.
     * US13: Mid-Cycle Income Top-up
     * 
     * This creates an INCOME transaction and updates the cycle's total allowance.
     * The daily limit will automatically reflect this change on next recalculation.
     * 
     * @param amount The income amount to add
     * @return DbResult indicating success or failure
     */
    fun addIncome(amount: Double): Result<Unit> {
        val currentCycle = activeCycle ?: return Result.Error("No active budget cycle")

        if (amount <= 0) {
            return Result.Error("Amount must be greater than 0")
        }

        // Create income category if it doesn't exist
        val incomeCategory = Category(
            id = 0,
            name = "Top-up",
            iconName = "income"
        )

        // Log the income transaction using TransactionController
        val txResult = transactionController.logIncome(
            amount = amount,
            category = incomeCategory,
            cycleId = currentCycle.id
        )

        when (txResult) {
            is DbResult.Error -> return txResult
            is Result.Succuess -> {
                // Update the cycle's total allowance
                val updatedCycle = currentCycle.copy(
                    totalAllowance = currentCycle.totalAllowance + amount
                )

                // Save updated cycle
                val saveResult = dbManager.saveCycle(updatedCycle)
                
                // Reload to get updated data
                loadActiveCycle()
                
                return saveResult
            }
        }
    }

    /**
     * Reset the budget cycle by clearing all data.
     * US11: Cycle Reset and Data Clearance
     * 
     * Deletes all transactions and cycles from the database.
     * 
     * @return DbResult indicating success or failure
     */
    fun resetCycle(): Result<Unit> {
        // Clear all data from database
        val clearResult = dbManager.clearAll()
        
        when (clearResult) {
            is DbResult.Error -> return clearResult
            is Result.Succuess -> {
                // Clear local active cycle
                activeCycle = null
                return Result.Succuess(Unit)
            }
        }
    }

    /**
     * Get the number of remaining days in the cycle.
     * Used for displaying "Final Day" badge in US3.
     * 
     * @return Number of remaining days, or 0 if no active cycle
     */
    fun getRemainingDays(): Int {
        return activeCycle?.remainingDays ?: 0
    }

    /**
     * Check if this is the final day of the cycle.
     * US3: Show "Final Day" badge when remainingDays == 1
     * 
     * @return true if only one day remaining
     */
    fun isFinalDay(): Boolean {
        return activeCycle?.remainingDays == 1
    }

    /**
     * Check if the daily limit is below 20% of total allowance.
     * US3: Color the limit orange when remaining < 20% of totalAllowance
     * 
     * @return true if limit is below 20% threshold
     */
    fun isLimitLow(): Boolean {
        val cycle = activeCycle ?: return false
        val threshold = cycle.totalAllowance * 0.20
        return getRemainingAllowance() < threshold
    }
}