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
 * Acts as the single entry point for the UI layer into all transaction and category operations —
 * the UI **never** calls [DatabaseManager] directly for these concerns. Responsibilities include:
 * - Validating amounts before any database write.
 * - Ensuring income transactions are always associated with a real, persisted [Category] row
 *   (required for backup/restore foreign-key integrity).
 * - Providing filtered and aggregated views over the raw transaction log.
 *
 * All functions are `suspend` and must be called from a coroutine context.
 *
 * @param dbManager The database manager used for all persistence operations.
 */
class TransactionController(private val dbManager: DatabaseManager) {

    /**
     * Logs a new expense transaction against the given budget cycle.
     *
     * Validates that [amount] is positive before writing to the database. The transaction is stored
     * with [TransactionType.EXPENSE], the current system timestamp, and the supplied [category].
     *
     * @param amount The expense amount. Must be strictly greater than `0`.
     * @param category The [Category] this expense belongs to.
     * @param cycleId The ID of the active [BudgetCycle] this transaction is linked to.
     * @return [Result.Error] with [ErrorType.VALIDATION] if [amount] ≤ 0; otherwise the result of
     *   the database insert operation.
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
     * Logs a new income transaction against the given budget cycle.
     *
     * Before saving, this function fetches — or lazily creates — a persistent "Income" [Category]
     * row in the database. A real category ID is required because a `categoryId` of `0` would
     * violate the foreign-key constraint enforced during [DatabaseManager.restoreFromBackup].
     *
     * The resolution order for the "Income" category is:
     * 1. Return the existing category if one named `"Income"` already exists.
     * 2. Create a new category via [DatabaseManager.saveCategory] if none is found.
     * 3. Re-fetch all categories to obtain the database-assigned ID of the newly created row.
     *
     * @param amount The income amount. Must be strictly greater than `0`.
     * @param cycleId The ID of the active [BudgetCycle] this income is linked to.
     * @return
     *   - [Result.Error] with [ErrorType.VALIDATION] if [amount] ≤ 0.
     *   - [Result.Error] with [ErrorType.DATABASE] if the "Income" category cannot be fetched or
     *     created.
     *   - Otherwise, the result of the database insert operation.
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
     * Edits the amount and category of an existing transaction in place.
     *
     * Fetches the existing [Transaction] by [id], applies [newAmount] and [newCategory] via
     * [Transaction.copy], then persists the updated record. The transaction type, cycle ID, and
     * original timestamp are preserved.
     *
     * @param id The database primary-key ID of the transaction to edit.
     * @param newAmount The replacement amount. Must be strictly greater than `0`.
     * @param newCategory The replacement [Category] to assign to the transaction.
     * @return
     *   - [Result.Error] with [ErrorType.VALIDATION] if [newAmount] ≤ 0.
     *   - [Result.Error] with [ErrorType.NOT_FOUND] if no transaction with [id] exists.
     *   - Otherwise, the result of the database update operation.
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
     * Permanently deletes a transaction by its database ID.
     *
     * Delegates directly to [DatabaseManager.deleteTransaction]. The caller is responsible for
     * reconciling any side effects (e.g., adjusting a cycle's allowance for deleted income entries).
     *
     * @param id The database primary-key ID of the transaction to delete.
     * @return The result of the database delete operation.
     */
    suspend fun deleteTransaction(id: Long): Result<Unit> = dbManager.deleteTransaction(id)

    /**
     * Returns the transaction history for a cycle, with optional in-memory filtering.
     *
     * Fetches all transactions from the database, then filters to those belonging to [cycleId].
     * Additional filters are applied in order if provided:
     * 1. **Category filter** — keeps only transactions whose [Transaction.category] ID matches
     *    [category].
     * 2. **Date range filter** — keeps only transactions whose [Transaction.date] falls within the
     *    inclusive `[startDate, endDate]` range.
     *
     * @param cycleId The ID of the cycle whose transactions to retrieve.
     * @param category Optional [Category] to filter by. Pass `null` to include all categories.
     * @param dateRange Optional inclusive date range as a [Pair] of `(startDate, endDate)`. Pass
     *   `null` to include all dates.
     * @return [Result.Success] containing the (possibly filtered) list of [Transaction]s, or
     *   [Result.Error] if the database fetch fails.
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
     * Returns the total amount spent per [Category] for all expenses in the given cycle.
     *
     * Only [TransactionType.EXPENSE] transactions contribute to the totals. Income transactions
     * are excluded. Aggregation is performed by [DatabaseManager.getCategoryTotals].
     *
     * @param cycleId The ID of the cycle to aggregate spending for.
     * @return [Result.Success] containing a [Map] from [Category] to its total expense amount, or
     *   [Result.Error] if the database query fails.
     */
    suspend fun getCategoryTotals(cycleId: Long): Result<Map<Category, Double>> =
        dbManager.getCategoryTotals(cycleId)

    /**
     * Creates and persists a new user-defined category.
     *
     * The category is saved with `id = 0`; the database assigns the real primary-key ID on insert.
     *
     * @param name The display name of the category. Must not be blank.
     * @param iconName The Material icon name string used to render this category's icon in the UI.
     * @return [Result.Error] with [ErrorType.VALIDATION] if [name] is blank; otherwise the result
     *   of the database insert operation.
     */
    suspend fun addCategory(name: String, iconName: String): Result<Unit> {
        if (name.isBlank()) {
            return Result.Error(message = "Category name cannot be empty", type = ErrorType.VALIDATION)
        }
        val category = Category(id = 0, name = name, iconName = iconName)
        return dbManager.saveCategory(category)
    }

    /**
     * Fetches all available categories, including both built-in defaults and user-defined ones.
     *
     * @return [Result.Success] containing a [List] of all persisted [Category] objects, or
     *   [Result.Error] if the database fetch fails.
     */
    suspend fun getCategories(): Result<List<Category>> = dbManager.fetchCategories()
}