package stud.brokers.pennywise.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import stud.brokers.pennywise.PennyWiseDatabase
import stud.brokers.pennywise.models.*
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.Result.ErrorType
import kotlinx.datetime.LocalDate

actual object DatabaseManager {

    private val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:pennywise.db")
    private val database: PennyWiseDatabase

    init {
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        PennyWiseDatabase.Schema.create(driver)
        database = PennyWiseDatabase(driver)
    }

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

    private fun PennyWiseDatabase.Category.toModel(): Category {
        return Category(
            id = id,
            name = name,
            iconName = iconName
        )
    }

    private fun PennyWiseDatabase.TransactionWithCategory.toModel(): Transaction {
        return Transaction(
            id = id,
            cycleId = cycleId,
            amount = amount,
            type = TransactionType.valueOf(type),
            category = Category(
                id = categoryId,
                name = categoryName,
                iconName = categoryIconName
            ),
            timestamp = timestamp
        )
    }

    // ============ CYCLE OPERATIONS =============

    actual suspend fun saveCycle(cycle: BudgetCycle): Result<Unit> = catchDbErrors {
        database.cycleQueries.insertCycle(
            totalAllowance = cycle.totalAllowance,
            startDate = cycle.startDate.toString(),
            endDate = cycle.endDate.toString()
        )
        Result.Success(Unit)
    }

    actual suspend fun fetchCycle(): Result<BudgetCycle?> = catchDbErrors {
        val sqlCycle = database.cycleQueries.fetchActiveCycle().executeAsOneOrNull()
        sqlCycle?.let {
            BudgetCycle(
                id = it.id,
                totalAllowance = it.totalAllowance,
                startDate = LocalDate.parse(it.startDate),
                endDate = LocalDate.parse(it.endDate)
            )
        }
    }

    actual suspend fun deleteCycle(id: Long): Result<Unit> = catchDbErrors {
        database.cycleQueries.deleteCycle(id)
        Result.Success(Unit)
    }

    // ============ CATEGORY OPERATIONS ============

    actual suspend fun saveCategory(cat: Category): Result<Unit> = catchDbErrors {
        database.categoryQueries.insertCategory(
            name = cat.name,
            iconName = cat.iconName
        )
        Result.Success(Unit)
    }

    actual suspend fun fetchCategories(): Result<List<Category>> = catchDbErrors {
        database.categoryQueries.fetchCategories()
            .executeAsList()
            .map { it.toModel() }
    }

    actual suspend fun fetchCategoryById(id: Long): Result<Category?> = catchDbErrors {
        database.categoryQueries.fetchCategoryById(id)
            .executeAsOneOrNull()
            ?.toModel()
    }

    // ============ TRANSACTION OPERATIONS ============

    actual suspend fun saveTransaction(tx: Transaction): Result<Unit> = catchDbErrors {
        database.transactionQueries.insertTransaction(
            cycleId = tx.cycleId,
            categoryId = tx.category.id,
            amount = tx.amount,
            type = tx.type.name,
            timestamp = tx.timestamp
        )
        Result.Success(Unit)
    }

    actual suspend fun fetchTransactions(): Result<List<Transaction>> = catchDbErrors {
        val cycle = database.cycleQueries.fetchActiveCycle().executeAsOneOrNull()
        if (cycle == null) return@catchDbErrors emptyList()
        database.transactionQueries.fetchTransactionsWithCategory(cycle.id)
            .executeAsList()
            .map { it.toModel() }
    }

    actual suspend fun fetchTransactionById(id: Long): Result<Transaction?> = catchDbErrors {
        database.transactionQueries.fetchTransactionById(id)
            .executeAsOneOrNull()
            ?.toModel()
    }

    actual suspend fun updateTransaction(tx: Transaction): Result<Unit> = catchDbErrors {
        database.transactionQueries.updateTransaction(
            amount = tx.amount,
            categoryId = tx.category.id,
            type = tx.type.name,
            id = tx.id
        )
        Result.Success(Unit)
    }

    actual suspend fun deleteTransaction(id: Long): Result<Unit> = catchDbErrors {
        database.transactionQueries.deleteTransaction(id)
        Result.Success(Unit)
    }

    actual suspend fun getCategoryTotals(cycleId: Long): Result<Map<Category, Double>> = catchDbErrors {
        val result = mutableMapOf<Category, Double>()
        database.transactionQueries.fetchExpenseTotalsByCategory(cycleId)
            .executeAsList()
            .forEach { row ->
                val category = Category(
                    id = row.categoryId,
                    name = row.categoryName,
                    iconName = row.categoryIconName
                )
                result[category] = row.total
            }
        result
    }

    // ============ SETTINGS OPERATIONS ============

    actual suspend fun upsertSetting(key: String, value: String): Result<Unit> = catchDbErrors {
        database.settingsQueries.upsertSetting(key, value)
        Result.Success(Unit)
    }

    actual suspend fun fetchSetting(key: String): Result<String?> = catchDbErrors {
        database.settingsQueries.fetchSetting(key)
            .executeAsOneOrNull()
    }

    // ============ RESET OPERATIONS ============

    actual suspend fun clearAll(): Result<Unit> = catchDbErrors {
        database.transactionQueries.deleteAllTransactions()
        database.cycleQueries.deleteAllCycles()
        database.categoryQueries.deleteAllCategories()
        Result.Success(Unit)
    }
}