package stud.brokers.pennywise.util

import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType

object InvoiceGenerator {

    fun buildHtml(
        transactions: List<Transaction>,
        totalAllowance: Double,
        currencySymbol: String
    ): String {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val remaining = totalAllowance - expenses

        val sb = StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; line-height: 1.6; }
                    .header { text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 2px solid #eee; }
                    .summary { display: flex; justify-content: space-between; margin-bottom: 20px; padding: 15px; background-color: #f8f9fa; border-radius: 8px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background-color: #f2f2f2; font-weight: bold; }
                    .expense { color: #d9534f; font-weight: bold; }
                    .income { color: #5cb85c; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>PennyWise Financial Summary</h1>
                </div>
                
                <div class="summary">
                    <div><strong>Allowance:</strong> $totalAllowance $currencySymbol</div>
                    <div><strong>Total Spent:</strong> $expenses $currencySymbol</div>
                    <div><strong>Remaining Balance:</strong> $remaining $currencySymbol</div>
                </div>
                
                <table>
                    <thead>
                        <tr>
                            <th>Category</th>
                            <th>Type</th>
                            <th>Amount</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        transactions.forEach { tx ->
            val typeClass = if (tx.type == TransactionType.INCOME) "income" else "expense"
            val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
            sb.append("<tr><td>${tx.category.name}</td><td>${tx.type.name}</td><td class=\"$typeClass\">$sign${tx.amount} $currencySymbol</td></tr>")
        }

        sb.append("</tbody></table></body></html>")

        return sb.toString()
    }
}