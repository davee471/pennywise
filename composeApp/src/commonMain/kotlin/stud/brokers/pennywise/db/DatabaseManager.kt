package stud.brokers.pennywise.db

import stud.brokers.pennywise.models.BudgetCycle
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.DbResult

expect object DatabaseManager {
    suspend fun saveCycle(cycle: BudgetCycle): DbResult<Unit>
    suspend fun saveTransaction(tx: Transaction): DbResult<Unit>
    suspend fun saveCategory(cat: Category): DbResult<Unit>
    suspend fun fetchTransactions(): DbResult<List<Transaction>>
    suspend fun fetchCycle(): DbResult<BudgetCycle?>
    suspend fun fetchCategories(): DbResult<List<Category>>
    suspend fun deleteCycle(id: Int): DbResult<Unit>
    suspend fun deleteTransaction(id: Int): DbResult<Unit>
}