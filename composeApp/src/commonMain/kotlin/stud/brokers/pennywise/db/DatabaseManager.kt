package stud.brokers.pennywise.db
import stud.brokers.pennywise.Categories
import stud.brokers.pennywise.FetchExpenseTotalsByCategory
import stud.brokers.pennywise.FetchTransactionById
import stud.brokers.pennywise.FetchTransactionsWithCategory
import stud.brokers.pennywise.PennyWiseDatabase
import stud.brokers.pennywise.models.*
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.ErrorType
import kotlinx.datetime.LocalDate
import stud.brokers.pennywise.FetchAllTransactions
import stud.brokers.pennywise.Settings
import stud.brokers.pennywise.util.unwrap

class DatabaseManager(driverFactory: DriverFactory) {

    private val database = PennyWiseDatabase(driverFactory.createDriver())
    private val queries = database.pennyWiseQueries

    private suspend fun <T> catchDbErrors(block: suspend () -> T): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: Exception) {
            Result.Error(
                message = e.message ?: "Database error",
                type = ErrorType.DATABASE
            )
        }
    }

    private fun categoriesToModel(cat: Categories): Category {
        return Category(id = cat.id, name = cat.name, iconName = cat.iconName)
    }

    private fun txWithCatToModel(tx: FetchTransactionsWithCategory): Transaction {
        return Transaction(
            id = tx.id,
            cycleId = tx.cycleId,
            amount = tx.amount,
            type = TransactionType.valueOf(tx.type),
            category = Category(
                id = tx.categoryId,
                name = tx.categoryName,
                iconName = tx.categoryIconName
            ),
            timestamp = tx.timestamp
        )
    }


    // ============ CYCLE OPERATIONS =============

    suspend fun saveCycle(cycle: BudgetCycle): Result<Unit> = catchDbErrors {
        queries.insertCycle(
            totalAllowance = cycle.totalAllowance,
            startDate = cycle.startDate.toString(),
            endDate = cycle.endDate.toString()
        )
        Result.Success(Unit)
    }

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

    suspend fun fetchAllCycles(): Result<List<BudgetCycle>> = catchDbErrors {
        queries.fetchAllCycles()
            .executeAsList()
            .map { bc ->
                BudgetCycle(
                    id = bc.id,
                    totalAllowance = bc.totalAllowance,
                    startDate = LocalDate.parse(bc.startDate),
                    endDate = LocalDate.parse(bc.endDate)
                )
            }
    }

    suspend fun deleteCycle(id: Long): Result<Unit> = catchDbErrors {
        queries.deleteCycle(id)
        Result.Success(Unit)
    }

    suspend fun updateCycleBudget(id: Long, totalAllowance: Double): Result<Unit> = catchDbErrors {
        queries.updateCycleBudget(
            totalAllowance = totalAllowance,
            id = id
        )
        Result.Success(Unit)
    }

    // ============ CATEGORY OPERATIONS ============

    suspend fun saveCategory(cat: Category): Result<Unit> = catchDbErrors {
        queries.insertCategory(
            name = cat.name,
            iconName = cat.iconName
        )
        Result.Success(Unit)
    }

    suspend fun fetchCategories(): Result<List<Category>> = catchDbErrors {
        queries.fetchCategories()
            .executeAsList()
            .map { categoriesToModel(it) }
    }

    suspend fun fetchCategoryById(id: Long): Result<Category?> = catchDbErrors {
        queries.fetchCategoryById(id)
            .executeAsOneOrNull()
            ?.let { categoriesToModel(it) }
    }

    // ============ TRANSACTION OPERATIONS ============

    suspend fun saveTransaction(tx: Transaction): Result<Unit> = catchDbErrors {
        queries.insertTransaction(
            cycleId = tx.cycleId,
            categoryId = tx.category.id,
            amount = tx.amount,
            type = tx.type.name,
            timestamp = tx.timestamp
        )
        Result.Success(Unit)
    }

    suspend fun fetchTransactions(): Result<List<Transaction>> = catchDbErrors {
        val cycle = queries.fetchActiveCycle().executeAsOneOrNull()
        if (cycle == null) return@catchDbErrors emptyList()
        queries.fetchTransactionsWithCategory(cycle.id)
            .executeAsList()
            .map { txWithCatToModel(it) }
    }

    suspend fun fetchTransactionById(id: Long): Result<Transaction?> = catchDbErrors {
        queries.fetchTransactionById(id)
            .executeAsOneOrNull()
            ?.let { tx ->
                Transaction(
                    id = tx.id,
                    cycleId = tx.cycleId,
                    amount = tx.amount,
                    type = TransactionType.valueOf(tx.type),
                    category = Category(
                        id = tx.categoryId,
                        name = tx.categoryName,
                        iconName = tx.categoryIconName
                    ),
                    timestamp = tx.timestamp
                )
            }
    }

    suspend fun updateTransaction(tx: Transaction): Result<Unit> = catchDbErrors {
        queries.updateTransaction(
            amount = tx.amount,
            categoryId = tx.category.id,
            type = tx.type.name,
            id = tx.id
        )
        Result.Success(Unit)
    }

    suspend fun deleteTransaction(id: Long): Result<Unit> = catchDbErrors {
        queries.deleteTransaction(id)
        Result.Success(Unit)
    }

    suspend fun getCategoryTotals(cycleId: Long): Result<Map<Category, Double>> = catchDbErrors {
        val result = mutableMapOf<Category, Double>()
        queries.fetchExpenseTotalsByCategory(cycleId)
            .executeAsList()
            .forEach { row ->
                val category = Category(
                    id = row.categoryId,
                    name = row.categoryName,
                    iconName = row.categoryIconName
                )
                result[category] = row.total ?: 0.0
            }
        result
    }

    // ============ SETTINGS OPERATIONS ============

    suspend fun upsertSetting(key: String, value: String): Result<Unit> = catchDbErrors {
        queries.upsertSetting(key, value)
        Result.Success(Unit)
    }

    suspend fun fetchSetting(key: String): Result<String?> = catchDbErrors {
        queries.fetchSetting(key)
            .executeAsOneOrNull()
    }

    // ============ RESET OPERATIONS ============

    suspend fun clearAll(): Result<Unit> = catchDbErrors {
        queries.deleteAllTransactions()
        queries.deleteAllCycles()
        queries.deleteAllCategories()
        Result.Success(Unit)
    }

   // ===== Backup Operations ==========


    // === Retrive settings for backup===

    suspend fun fetchAllSettings(): Result<Map<String, String>> = catchDbErrors {
        queries.fetchAllSettings()
            .executeAsList()
            .associate { it.key to it.value_ }
    }

    suspend fun fetchAllTransactions(): Result<List<Transaction>> = catchDbErrors {
        queries.fetchAllTransactions()
            .executeAsList()
            .map { tx ->
                Transaction(
                    id = tx.id,
                    cycleId = tx.cycleId,
                    amount = tx.amount,
                    type = TransactionType.valueOf(tx.type),
                    category = Category(
                        id = tx.categroyId,
                        name = tx.categoryName,
                        iconName = tx.categoryIconName
                    ),
                    timestamp = tx.timestamp
                )
            }
    }

    //=== Build the backup object====

    suspend fun fetchAllThenBuildBackup(): Result<BackupPayload> = catchDbErrors {
        val categories: List<Category> = fetchCategories().unwrap()
        val cycles: List<BudgetCycle> = fetchAllCycles().unwrap()
        val transactions: List<Transaction> = fetchAllTransactions().unwrap()
        val settings: Map<String, String> = fetchAllSettings().unwrap()

        BackupPayload(
            categories = categories,
            cycles = cycles,
            transactions = transactions,
            settings = settings
        )

    }

    // === Load The backup object from the file ===

    suspend fun restoreFromBackup(payload: BackupPayload): Result<Unit> = catchDbErrors {
        queries.transaction{
            queries.deleteAllTransactions()
            queries.deleteAllCycles()
            queries.deleteAllCategories()

            payload.categories.forEach { cat->
                queries.restoreCategory(id=cat.id,name = cat.name, iconName = cat.iconName)
            }

            payload.cycles.forEach { c ->
                queries.restoreCycle(id = c.id,totalAllowance = c.totalAllowance, startDate = c.startDate.toString(), endDate = c.endDate.toString())
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

            payload.settings.forEach { (key,value) ->
                queries.upsertSetting(key,value)
            }
        }
    }

}