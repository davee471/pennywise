package stud.brokers.pennywise.util

import stud.brokers.pennywise.models.Transaction

/**
 * Builds a CSV-formatted string from a list of [Transaction]s.
 *
 * Output format:
 * ```
 * date,type,category,amount
 * 1746134400000,"EXPENSE","Food",150.0
 * 1746048000000,"INCOME","Income",5000.0
 * ```
 *
 * - `date` is the raw Unix epoch milliseconds timestamp.
 * - `type` and `category` are quoted to handle names with commas.
 * - `amount` is the raw [Double] value.
 *
 * This function is `internal` — it is only accessible within the module and is called exclusively
 * by the platform-specific [stud.brokers.pennywise.services.ExportService] implementations.
 *
 * @param transactions The list of transactions to serialize.
 * @return A CSV string with a header row followed by one row per transaction.
 */
internal fun buildCsv(transactions: List<Transaction>): String {
  val header = "date,type,category,amount"
  val rows =
          transactions.joinToString("\n") { tx ->
            "${tx.timestamp},\"${tx.type.name}\",\"${tx.category.name}\",${tx.amount}"
          }
  return "$header\n$rows"
}
