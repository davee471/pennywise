package stud.brokers.pennywise.controllers

import kotlinx.datetime.LocalDate
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.ErrorType
import stud.brokers.pennywise.db.DatabaseManager

class TransactionController(private val dbManager: DatabaseManager) {

    suspend fun logExpense(amount: Double, category: Category, cycleId: Long): Result<Unit> {
        if (amount <= 0) {
            return Result.Error(
                message = "Amount must be greater than 0",
                type = ErrorType.VALIDATION
            )
        }
        val transaction = Transaction(
            cycleId = cycleId,
            amount = amount,
            type = TransactionType.EXPENSE,
            category = category,
            timestamp = System.currentTimeMillis(),
        )
        return dbManager.saveTransaction(transaction)
    }

    suspend fun logIncome(amount: Double, cycleId: Long): Result<Unit> {
        if (amount <= 0) {
            return Result.Error(
                message = "Amount must be greater than 0",
                type = ErrorType.VALIDATION
            )
        }
        val transaction = Transaction(
            cycleId = cycleId,
            amount = amount,
            type = TransactionType.INCOME,
            category = Category(id = 0, name = "Income", iconName = "income")
        )
        return dbManager.saveTransaction(transaction)
    }

    suspend fun editTransaction(id: Long, newAmount: Double, newCategory: Category): Result<Unit> {
        if (newAmount <= 0) {
            return Result.Error(
                message = "Amount must be greater than 0",
                type = ErrorType.VALIDATION
            )
        }
        return when (val existingResult = dbManager.fetchTransactionById(id)) {
            is Result.Success -> {
                val existingTx = existingResult.data
                if (existingTx == null) {
                    Result.Error(
                        message = "Transaction not found",
                        type = ErrorType.NOT_FOUND
                    )
                } else {
                    val updatedTx = existingTx.copy(
                        amount = newAmount,
                        category = newCategory
                    )
                    dbManager.updateTransaction(updatedTx)
                }
            }
            is Result.Error -> Result.Error(
                message = existingResult.message,
                type = existingResult.type
            )
        }
    }

    suspend fun deleteTransaction(id: Long): Result<Unit> =
        dbManager.deleteTransaction(id)

    suspend fun getHistory(
        cycleId: Long,
        category: Category? = null,
        dateRange: Pair<LocalDate, LocalDate>? = null
    ): Result<List<Transaction>> {
        return when (val result = dbManager.fetchTransactions()) {
            is Result.Success -> {
                var transactions = result.data.filter { it.cycleId == cycleId }
                if (category != null) {
                    transactions = transactions.filter { it.category.id == category.id }
                }
                if (dateRange != null) {
                    val (startDate, endDate) = dateRange
                    transactions = transactions.filter { tx ->
                        tx.date in startDate..endDate
                    }
                }
                Result.Success(transactions)
            }
            is Result.Error -> Result.Error(result.message, result.type)
        }
    }


    suspend fun getCategoryTotals(cycleId: Long): Result<Map<Category, Double>> =
        dbManager.getCategoryTotals(cycleId)

    suspend fun addCategory(name: String, iconName: String): Result<Unit> {
        if (name.isBlank()) {
            return Result.Error(
                message = "Category name cannot be empty",
                type = ErrorType.VALIDATION
            )
        }
        val category = Category(id = 0, name = name, iconName = iconName)
        return dbManager.saveCategory(category)
    }

    suspend fun getCategories(): Result<List<Category>> =
        dbManager.fetchCategories()
}