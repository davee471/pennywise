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

class DatabaseManager(driverFactory: DriverFactory) {

    private val database = PennyWiseDatabase(driverFactory.createDriver())
    private val queries = database.pennyWiseQueries

    private fun <T> catchDbErrors(block: () -> T): Result<T> {
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
}