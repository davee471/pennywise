package stud.brokers.pennywise.controllers

import kotlinx.datetime.LocalDate
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.ErrorType

/**
 * Handles all business logic related to transactions and categories.
 *
 * Sits between the UI layer and [DatabaseManager] — the UI never calls the database directly for
 * transaction operations. All amount validations happen here before any database call is made.
 *
 * @param dbManager The database manager used for all persistence operations.
 */
class TransactionController(private val dbManager: DatabaseManager) {

  /**
   * Logs a new expense transaction for the given cycle.
   *
   * @param amount The expense amount. Must be greater than 0.
   * @param category The category this expense belongs to.
   * @param cycleId The ID of the active budget cycle this transaction belongs to.
   * @return [Result.Error] with [ErrorType.VALIDATION] if amount <= 0, otherwise the result of the
   * database insert.
   */
  suspend fun logExpense(amount: Double, category: Category, cycleId: Long): Result<Unit> {
    if (amount <= 0) {
      return Result.Error(message = "Amount must be greater than 0", type = ErrorType.VALIDATION)
    }
    val transaction =
            Transaction(
                    cycleId = cycleId,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = category,
                    timestamp = System.currentTimeMillis(),
            )
    return dbManager.saveTransaction(transaction)
  }

  /**
   * Logs a new income transaction for the given cycle.
   *
   * Fetches or creates a persistent "Income" category in the database to ensure the transaction's
   * categoryId references a real row. This is required for backup/restore integrity — categoryId =
   * 0 would violate the foreign key constraint on [DatabaseManager.restoreFromBackup].
   *
   * @param amount The income amount. Must be greater than 0.
   * @param cycleId The ID of the active budget cycle this income belongs to.
   * @return [Result.Error] with [ErrorType.VALIDATION] if amount <= 0, [Result.Error] with
   * [ErrorType.DATABASE] if the income category cannot be fetched or created, otherwise the result
   * of the database insert.
   */
  suspend fun logIncome(amount: Double, cycleId: Long): Result<Unit> {
    if (amount <= 0) {
      return Result.Error("Amount must be greater than 0", ErrorType.VALIDATION)
    }

    // Fetch or create a real "Income" category in the DB
    val categoriesResult = dbManager.fetchCategories()
    if (categoriesResult is Result.Error) {
      return Result.Error(categoriesResult.message, categoriesResult.type)
    }
    val categories = (categoriesResult as Result.Success).data
    val incomeCategory =
            categories.firstOrNull { it.name == "Income" }
                    ?: run {
                      // Create it if it doesn't exist yet
                      val createResult =
                              dbManager.saveCategory(
                                      Category(id = 0, name = "Income", iconName = "income")
                              )
                      if (createResult is Result.Error) {
                        return Result.Error(createResult.message, createResult.type)
                      }
                      // Fetch it back to get the real DB-assigned ID
                      val updated = dbManager.fetchCategories()
                      if (updated is Result.Error)
                              return Result.Error(updated.message, updated.type)
                      (updated as Result.Success).data.first { it.name == "Income" }
                    }

    val transaction =
            Transaction(
                    cycleId = cycleId,
                    amount = amount,
                    type = TransactionType.INCOME,
                    category = incomeCategory
            )
    return dbManager.saveTransaction(transaction)
  }
  /**
   * Edits the amount and category of an existing transaction.
   *
   * Fetches the existing transaction first, applies the changes via [copy], then persists the
   * updated record.
   *
   * @param id The database ID of the transaction to edit.
   * @param newAmount The new amount. Must be greater than 0.
   * @param newCategory The new category to assign.
   * @return [Result.Error] with [ErrorType.VALIDATION] if amount <= 0, [ErrorType.NOT_FOUND] if no
   * transaction with [id] exists, otherwise the result of the database update.
   */
  suspend fun editTransaction(id: Long, newAmount: Double, newCategory: Category): Result<Unit> {
    if (newAmount <= 0) {
      return Result.Error(message = "Amount must be greater than 0", type = ErrorType.VALIDATION)
    }
    return when (val existingResult = dbManager.fetchTransactionById(id)) {
      is Result.Success -> {
        val existingTx = existingResult.data
        if (existingTx == null) {
          Result.Error(message = "Transaction not found", type = ErrorType.NOT_FOUND)
        } else {
          val updatedTx = existingTx.copy(amount = newAmount, category = newCategory)
          dbManager.updateTransaction(updatedTx)
        }
      }
      is Result.Error -> Result.Error(message = existingResult.message, type = existingResult.type)
    }
  }

  /**
   * Deletes a transaction by its database ID.
   *
   * @param id The database ID of the transaction to delete.
   */
  suspend fun deleteTransaction(id: Long): Result<Unit> = dbManager.deleteTransaction(id)

  /**
   * Returns the transaction history for a given cycle with optional filtering.
   *
   * Fetches all transactions for the active cycle from the database, then applies in-memory
   * filtering by [category] and/or [dateRange] if provided.
   *
   * @param cycleId The ID of the cycle whose transactions to fetch.
   * @param category Optional category filter. Pass `null` to include all categories.
   * @param dateRange Optional date range filter as a [Pair] of start and end [LocalDate]. Pass
   * `null` to include all dates.
   * @return [Result.Success] containing the filtered [List] of [Transaction]s.
   */
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
          transactions = transactions.filter { tx -> tx.date in startDate..endDate }
        }
        Result.Success(transactions)
      }
      is Result.Error -> Result.Error(result.message, result.type)
    }
  }

  /**
   * Returns total amounts spent per category for a given cycle. Only [TransactionType.EXPENSE]
   * transactions are counted.
   *
   * @param cycleId The ID of the cycle to aggregate.
   * @return [Result.Success] containing a [Map] of [Category] to total spent amount.
   */
  suspend fun getCategoryTotals(cycleId: Long): Result<Map<Category, Double>> =
          dbManager.getCategoryTotals(cycleId)

  /**
   * Creates and persists a new user-defined category.
   *
   * @param name The display name of the category. Must not be blank.
   * @param iconName The Material icon name string for this category.
   * @return [Result.Error] with [ErrorType.VALIDATION] if name is blank, otherwise the result of
   * the database insert.
   */
  suspend fun addCategory(name: String, iconName: String): Result<Unit> {
    if (name.isBlank()) {
      return Result.Error(message = "Category name cannot be empty", type = ErrorType.VALIDATION)
    }
    val category = Category(id = 0, name = name, iconName = iconName)
    return dbManager.saveCategory(category)
  }

  /**
   * Fetches all available categories (default and user-defined).
   *
   * @return [Result.Success] containing a [List] of all [Category]s.
   */
  suspend fun getCategories(): Result<List<Category>> = dbManager.fetchCategories()
}
