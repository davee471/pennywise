package stud.brokers.pennywise.util


import stud.brokers.pennywise.models.Transaction
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

internal fun buildCsv(transactions: List<Transaction>): String{
    val header = "date,type,category,amount"
    val rows = transactions.joinToString("\n"){ tx ->
        "${tx.timestamp},\"${tx.type.name}\",\"${tx.category.name}\",${tx.amount}"
    }
    return "$header\n$rows"
}