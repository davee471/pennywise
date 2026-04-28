package stud.brokers.pennywise.db

import stud.brokers.pennywise.models.BudgetCycle
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.Result

expect object DatabaseManager {
    suspend fun saveCycle(cycle: BudgetCycle): Result<Unit>
    suspend fun saveTransaction(tx: Transaction): Result<Unit>
    suspend fun updateTransaction(tx: Transaction): Result<Unit>
    suspend fun saveCategory(cat: Category): Result<Unit>

    suspend fun fetchTransactions(): Result<List<Transaction>>
    suspend fun fetchTransactionById(id: Int): Result<Transaction?>
    suspend fun fetchCategoryById(id: Int): Result<Category?>
    suspend fun fetchCategories(it: Any): Result<List<Category>>
    suspend fun getCategoryTotals(cycleId: Int): Result<Map<Category, Double>>

    suspend fun fetchCycle(): Result<BudgetCycle?>
    suspend fun deleteCycle(id: Int): Result<Unit>
    suspend fun deleteTransaction(id: Int): Result<Unit>
    suspend fun clearAll(): Result<Unit>

    suspend fun upsertSetting(key: String, value: String): Result<Unit>
    suspend fun fetchSetting(key: String): Result<String?>
}