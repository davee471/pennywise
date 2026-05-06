package stud.brokers.pennywise.db

import kotlinx.datetime.LocalDate
import stud.brokers.pennywise.Categories
import stud.brokers.pennywise.FetchTransactionsWithCategory
import stud.brokers.pennywise.PennyWiseDatabase
import stud.brokers.pennywise.models.*
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.*
import stud.brokers.pennywise.util.getOrNull

/**
 * Single access point for all SQLite database operations in PennyWise.
 *
 * All public functions are suspend functions and return [Result] — either [Result.Success] wrapping
 * the requested data, or [Result.Error] with a message and an [Result.ErrorType] indicating what
 * went wrong.
 *
 * On first launch, if the categories table is empty, four default categories (Food, Transport,
 * Utilities, Entertainment) are seeded automatically.
 *
 * @param driverFactory Platform-specific factory that creates the SQLite driver. On Android this
 * wraps [AndroidSqliteDriver]; on JVM it wraps [JdbcSqliteDriver].
 */
class DatabaseManager(driverFactory: DriverFactory) {

  private val database = PennyWiseDatabase(driverFactory.createDriver())
  private val queries = database.pennyWiseQueries

  init {
    seedDefaultCategoriesIfNeeded()
  }

  private fun seedDefaultCategoriesIfNeeded() {
    // Auto-seed default categories if the database is completely empty
    if (queries.fetchCategories().executeAsList().isEmpty()) {
      queries.insertCategory(name = "Food", iconName = "restaurant")
      queries.insertCategory(name = "Transport", iconName = "directions_car")
      queries.insertCategory(name = "Utilities", iconName = "bolt")
      queries.insertCategory(name = "Entertainment", iconName = "movie")
    }
  }

  /**
   * Wraps a database operation in a try/catch and maps the result to [Result]. Any exception thrown
   * inside [block] is caught and returned as [Result.Error] with [ErrorType.DATABASE].
   */
  private suspend fun <T> catchDbErrors(block: suspend () -> T): Result<T> {
    return try {
      Success(block())
    } catch (e: Exception) {
      Error(message = e.message ?: "Database error", type = ErrorType.DATABASE)
    }
  }

  /** Maps a SQLDelight [Categories] row to the domain [Category] model. */
  private fun categoriesToModel(cat: Categories): Category {
    return Category(id = cat.id, name = cat.name, iconName = cat.iconName)
  }

  /**
   * Maps a SQLDelight [FetchTransactionsWithCategory] row (a JOIN result) to the domain
   * [Transaction] model with its nested [Category].
   */
  private fun txWithCatToModel(tx: FetchTransactionsWithCategory): Transaction {
    return Transaction(
            id = tx.id,
            cycleId = tx.cycleId,
            amount = tx.amount,
            type = TransactionType.valueOf(tx.type),
            category =
                    Category(
                            id = tx.categoryId,
                            name = tx.categoryName,
                            iconName = tx.categoryIconName
                    ),
            timestamp = tx.timestamp
    )
  }

  // ============ CYCLE OPERATIONS =============

  /**
   * Persists a new [BudgetCycle] to the database. Dates are stored as ISO-8601 strings (e.g.
   * "2026-05-01").
   *
   * @param cycle The cycle to insert. The [BudgetCycle.id] field is ignored; the database assigns a
   * new auto-incremented ID.
   * @return [Result.Success] with [Unit] on success.
   */
  suspend fun saveCycle(cycle: BudgetCycle): Result<Unit> = catchDbErrors {
    queries.insertCycle(
            totalAllowance = cycle.totalAllowance,
            startDate = cycle.startDate.toString(),
            endDate = cycle.endDate.toString()
    )
    Success(Unit)
  }

  /**
   * Fetches the most recently created budget cycle (treated as the active cycle).
   *
   * @return [Result.Success] containing the active [BudgetCycle], or `null` if no cycles exist yet.
   */
  suspend fun fetchCycle(): Result<BudgetCycle?> = catchDbErrors {
    val sqlCycle = queries.fetchActiveCycle().executeAsOneOrNull()
    sqlCycle?.let {
      BudgetCycle(
              id = it.id,
              totalAllowance = it.totalAllowance,
              startDate = LocalDate.parse(it.startDate),
              endDate = LocalDate.parse(it.endDate)
      )
    }
  }

  /**
   * Fetches all budget cycles ordered by most recent first. Used primarily by the backup system to
   * capture the full cycle history.
   *
   * @return [Result.Success] containing a [List] of all [BudgetCycle]s.
   */
  suspend fun fetchAllCycles(): Result<List<BudgetCycle>> = catchDbErrors {
    queries.fetchAllCycles().executeAsList().map { bc ->
      BudgetCycle(
              id = bc.id,
              totalAllowance = bc.totalAllowance,
              startDate = LocalDate.parse(bc.startDate),
              endDate = LocalDate.parse(bc.endDate)
      )
    }
  }

  /**
   * Deletes a budget cycle by its ID. Note: associated transactions are NOT cascade-deleted; call
   * [deleteTransaction] separately if needed.
   *
   * @param id The database ID of the cycle to delete.
   */
  suspend fun deleteCycle(id: Long): Result<Unit> = catchDbErrors {
    queries.deleteCycle(id)
    Success(Unit)
  }

  /**
   * Updates the spending limit of an existing cycle.
   *
   * @param id The database ID of the cycle to update.
   * @param totalAllowance The new total allowance in the user's currency.
   */
  suspend fun updateCycleBudget(id: Long, totalAllowance: Double): Result<Unit> = catchDbErrors {
    queries.updateCycleBudget(totalAllowance = totalAllowance, id = id)
    Success(Unit)
  }

  // ============ CATEGORY OPERATIONS ============
  /**
   * Inserts a new user-defined category. The [Category.id] field is ignored; the database assigns a
   * new auto-incremented ID.
   *
   * @param cat The category to insert.
   */
  suspend fun saveCategory(cat: Category): Result<Unit> = catchDbErrors {
    queries.insertCategory(name = cat.name, iconName = cat.iconName)
    Success(Unit)
  }

  /**
   * Fetches all categories (default and user-defined).
   *
   * @return [Result.Success] containing a [List] of all [Category]s.
   */
  suspend fun fetchCategories(): Result<List<Category>> = catchDbErrors {
    queries.fetchCategories().executeAsList().map { categoriesToModel(it) }
  }

  /**
   * Fetches a single category by its database ID.
   *
   * @param id The database ID of the category.
   * @return [Result.Success] containing the [Category], or `null` if not found.
   */
  suspend fun fetchCategoryById(id: Long): Result<Category?> = catchDbErrors {
    queries.fetchCategoryById(id).executeAsOneOrNull()?.let { categoriesToModel(it) }
  }

  // ============ TRANSACTION OPERATIONS ============

  /**
   * Persists a new [Transaction] to the database. The [Transaction.id] field is ignored; the
   * database assigns a new auto-incremented ID.
   *
   * @param tx The transaction to insert.
   */
  suspend fun saveTransaction(tx: Transaction): Result<Unit> = catchDbErrors {
    queries.insertTransaction(
            cycleId = tx.cycleId,
            categoryId = tx.category.id,
            amount = tx.amount,
            type = tx.type.name,
            timestamp = tx.timestamp
    )
    Success(Unit)
  }

  /**
   * Fetches all transactions belonging to the currently active cycle, joined with their category
   * data.
   *
   * @return [Result.Success] containing a [List] of [Transaction]s, or an empty list if no active
   * cycle exists.
   */
  suspend fun fetchTransactions(): Result<List<Transaction>> = catchDbErrors {
    val cycle = queries.fetchActiveCycle().executeAsOneOrNull()
    if (cycle == null) return@catchDbErrors emptyList()
    queries.fetchTransactionsWithCategory(cycle.id).executeAsList().map { txWithCatToModel(it) }
  }

  /**
   * Fetches a single transaction by its database ID, joined with its category.
   *
   * @param id The database ID of the transaction.
   * @return [Result.Success] containing the [Transaction], or `null` if not found.
   */
  suspend fun fetchTransactionById(id: Long): Result<Transaction?> = catchDbErrors {
    queries.fetchTransactionById(id).executeAsOneOrNull()?.let { tx ->
      Transaction(
              id = tx.id,
              cycleId = tx.cycleId,
              amount = tx.amount,
              type = TransactionType.valueOf(tx.type),
              category =
                      Category(
                              id = tx.categoryId,
                              name = tx.categoryName,
                              iconName = tx.categoryIconName
                      ),
              timestamp = tx.timestamp
      )
    }
  }

  /**
   * Updates the amount, category, and type of an existing transaction.
   *
   * @param tx The transaction with updated fields. [Transaction.id] must match an existing row.
   */
  suspend fun updateTransaction(tx: Transaction): Result<Unit> = catchDbErrors {
    queries.updateTransaction(
            amount = tx.amount,
            categoryId = tx.category.id,
            type = tx.type.name,
            id = tx.id
    )
    Success(Unit)
  }

  /**
   * Deletes a transaction by its database ID.
   *
   * @param id The database ID of the transaction to delete.
   */
  suspend fun deleteTransaction(id: Long): Result<Unit> = catchDbErrors {
    queries.deleteTransaction(id)
    Success(Unit)
  }

  /**
   * Computes the total amount spent per category for a given cycle, considering only
   * [TransactionType.EXPENSE] transactions.
   *
   * @param cycleId The database ID of the cycle to aggregate.
   * @return [Result.Success] containing a [Map] of [Category] to total amount spent.
   */
  suspend fun getCategoryTotals(cycleId: Long): Result<Map<Category, Double>> = catchDbErrors {
    val result = mutableMapOf<Category, Double>()
    queries.fetchExpenseTotalsByCategory(cycleId).executeAsList().forEach { row ->
      val category =
              Category(
                      id = row.categoryId,
                      name = row.categoryName,
                      iconName = row.categoryIconName
              )
      result[category] = row.total ?: 0.0
    }
    result
  }

  // ============ SETTINGS OPERATIONS ============

  /**
   * Inserts or updates a key-value setting using SQL `INSERT OR REPLACE`.
   *
   * @param key The setting key. Use constants from [stud.brokers.pennywise.util.SettingsKeys].
   * @param value The string value to store.
   */
  suspend fun upsertSetting(key: String, value: String): Result<Unit> = catchDbErrors {
    queries.upsertSetting(key, value)
    Success(Unit)
  }

  /**
   * Fetches a single setting value by key.
   *
   * @param key The setting key to look up.
   * @return [Result.Success] containing the value string, or `null` if the key does not exist.
   */
  suspend fun fetchSetting(key: String): Result<String?> = catchDbErrors {
    queries.fetchSetting(key).executeAsOneOrNull()
  }

  // ============ RESET OPERATIONS ============

  /**
   * Deletes all transactions, cycles, and categories from the database. Settings are intentionally
   * preserved (e.g. PIN, currency preference).
   */
  suspend fun clearAll(): Result<Unit> = catchDbErrors {
    queries.deleteAllTransactions()
    queries.deleteAllCycles()
    queries.deleteAllCategories()
    seedDefaultCategoriesIfNeeded()
    Success(Unit)
  }

  // ===== Backup Operations ==========

  /**
   * Fetches all settings as a flat key-value map. Used internally by [fetchAllThenBuildBackup].
   *
   * @return [Result.Success] containing a [Map] of all setting key-value pairs.
   */
  suspend fun fetchAllSettings(): Result<Map<String, String>> = catchDbErrors {
    queries.fetchAllSettings().executeAsList().associate { it.key to it.value_ }
  }

  /**
   * Fetches all transactions across all cycles joined with their category data. Used internally by
   * [fetchAllThenBuildBackup] to capture the full transaction history regardless of which cycle
   * they belong to.
   *
   * @return [Result.Success] containing a [List] of all [Transaction]s across all cycles, ordered
   * by most recent first.
   */
  suspend fun fetchAllTransactions(): Result<List<Transaction>> = catchDbErrors {
    queries.fetchAllTransactions().executeAsList().map { tx ->
      Transaction(
              id = tx.id,
              cycleId = tx.cycleId,
              amount = tx.amount,
              type = TransactionType.valueOf(tx.type),
              category =
                      Category(
                              id = tx.categoryId,
                              name = tx.categoryName,
                              iconName = tx.categoryIconName
                      ),
              timestamp = tx.timestamp
      )
    }
  }

  /**
   * Fetches all data from every table and assembles a [BackupPayload] object ready to be serialized
   * and written to disk by [stud.brokers.pennywise.services.BackupService].
   *
   * If any individual fetch fails, the entire operation fails and returns [Result.Error] — no
   * partial backups are produced.
   *
   * @return [Result.Success] containing a fully populated [BackupPayload].
   */
  suspend fun fetchAllThenBuildBackup(): Result<BackupPayload> = catchDbErrors {
    val categories = fetchCategories().getOrNull() ?: throw Exception("Failed to fetch Categories")
    val cycles = fetchAllCycles().getOrNull() ?: throw Exception("Failed to fetch cycles")
    val transactions =
            fetchAllTransactions().getOrNull() ?: throw Exception("Failed to fetch transactions")
    val settings = fetchAllSettings().getOrNull() ?: throw Exception("Failed to fetch settings")

    BackupPayload(
            categories = categories,
            cycles = cycles,
            transactions = transactions,
            settings = settings
    )
  }

  /**
   * Restores the database from a [BackupPayload] produced by [fetchAllThenBuildBackup].
   *
   * The entire operation runs inside a single SQLite transaction — if anything fails mid-restore,
   * the database is automatically rolled back to its previous state. This prevents partial restores
   * from corrupting user data.
   *
   * Restore order:
   * 1. Delete all existing transactions, cycles, and categories.
   * 2. Restore categories (transactions have a foreign key dependency on them).
   * 3. Restore cycles (transactions have a foreign key dependency on them).
   * 4. Restore transactions.
   * 5. Restore settings.
   *
   * @param payload The [BackupPayload] to restore from.
   */
  suspend fun restoreFromBackup(payload: BackupPayload): Result<Unit> = catchDbErrors {
    queries.transaction {
      queries.deleteAllTransactions()
      queries.deleteAllCycles()
      queries.deleteAllCategories()

      payload.categories.forEach { cat ->
        queries.restoreCategory(id = cat.id, name = cat.name, iconName = cat.iconName)
      }

      payload.cycles.forEach { c ->
        queries.restoreCycle(
                id = c.id,
                totalAllowance = c.totalAllowance,
                startDate = c.startDate.toString(),
                endDate = c.endDate.toString()
        )
      }

      payload.transactions.forEach { tx ->
        queries.restoreTransaction(
                id = tx.id,
                cycleId = tx.cycleId,
                categoryId = tx.category.id,
                amount = tx.amount,
                type = tx.type.name,
                timestamp = tx.timestamp,
        )
      }

      payload.settings.forEach { (key, value) -> queries.upsertSetting(key, value) }
    }
  }
}
