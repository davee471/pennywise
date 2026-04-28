package stud.brokers.pennywise.controllers

import kotlinx.datetime.LocalDate
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.ErrorType

class TransactionController {

    private val dbManager = DatabaseManager

    suspend fun logExpense(amount: Double, category: Category, cycleId: Int): Result<Unit> {
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
            category = category
        )

        return dbManager.saveTransaction(transaction)
    }

    suspend fun editTransaction(id: Int, newAmount: Double, newCategory: Category): Result<Unit> {
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

    suspend fun getHistory(
        cycleId: Int,
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
}