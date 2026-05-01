package stud.brokers.pennywise.services

import stud.brokers.pennywise.Transactions
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.util.Result

expect class ExportService {
    suspend fun exportToCsv(transactions: List<Transaction>): Result<Unit>
}

