package stud.brokers.pennywise.models
import stud.brokers.pennywise.util.DbResult
expect object DatabaseManager {
    suspend fun saveCycle(cycle: BudgetCycle):  DbResult<Unit>
    suspend fun saveTransaction(tx: Transaction):  DbResult<Unit>
    suspend fun saveCategory(cat: Category): DbResult<Unit>
    suspend fun fetchTransactions(): DbResult<List<Transaction>>
    suspend fun fetchCycle(): DbResult<BudgetCycle?>
    suspend fun fetchCategories(): DbResult<List<Category>>
    suspend fun deleteCycle(id: Int): DbResult<Unit>
    suspend fun deleteTransaction(id: Int): DbResult<Unit>
}



