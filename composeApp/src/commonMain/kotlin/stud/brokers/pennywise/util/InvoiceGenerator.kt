package stud.brokers.pennywise.util
 
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType
 
/**
 * Utility object that generates a self-contained HTML invoice summarising a budget cycle's
 * transactions.
 *
 * The produced HTML string is entirely cross-platform and contains inline CSS; it requires no
 * external stylesheets or assets. It is intended to be passed directly to
 * [stud.brokers.pennywise.services.ExportService.exportToPdf], which converts it to a PDF on the
 * host platform.
 *
 * This object is stateless — [buildHtml] is a pure function with no side effects.
 */
object InvoiceGenerator {
 
    /**
     * Builds a styled HTML document summarising the provided [transactions] for a single budget
     * cycle.
     *
     * The document contains two sections:
     * 1. **Summary bar** — shows the total allowance, total amount spent (expenses only), and the
     *    remaining balance.
     * 2. **Transaction table** — one row per transaction listing the category name, transaction
     *    type, and signed amount. Income rows are styled green (`+`), expense rows red (`-`).
     *
     * Only [TransactionType.EXPENSE] transactions contribute to the "Total Spent" figure.
     * [TransactionType.INCOME] transactions are included in the table but do not reduce the
     * remaining balance displayed in the summary.
     *
     * @param transactions The list of [Transaction]s to include in the invoice. May be empty, in
     * which case the summary will show zero spent and the table body will be blank.
     * @param totalAllowance The budget ceiling for the cycle, used to compute the remaining
     * balance shown in the summary bar.
     * @param currencySymbol The currency symbol appended to every monetary value (e.g. `"$"`,
     * `"€"`, `"EGP"`). Typically read from
     * [stud.brokers.pennywise.util.SettingsKeys.CURRENCY_SYMBOL].
     * @return A fully-formed, self-contained HTML string ready for PDF conversion. The string is
     * never empty; even an empty transaction list produces a valid HTML document.
     */
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
