package stud.brokers.pennywise.models

expect object DatabaseManager {
    suspend fun saveCycle(cycle: BudgetCycle):  DbResult<T>
    suspend fun saveTransaction(tx: Transaction):  DbResult<unit>
    suspend fun saveCategory(cat: Category): DbResult<T>
    suspend fun fetchTransactions(): DbResult<List<Transaction>>
    suspend fun fetchCycle(): DbResult<BudgetCycle?>
    suspend fun fetchCategories(): DbResult<List<Category>>
    suspend fun deleteCycle(id: Int): DbResult<T>
    suspend fun deleteTransaction(id: Int): DbResult<T>
}



