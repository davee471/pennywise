package stud.brokers.pennywise.models

expect object DatabaseManager {
    fun saveCycle(cycle: BudgetCycle): Boolean
    fun saveTransaction(tx: Transaction): Boolean
    fun saveCategory(cat: Category): Boolean
    fun fetchTransactions(): List<Transaction>
    fun fetchCycle(): BudgetCycle?
    fun fetchCategories(): List<Category>

}