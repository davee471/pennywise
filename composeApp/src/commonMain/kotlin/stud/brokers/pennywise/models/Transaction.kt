package stud.brokers.pennywise.models
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.time.Clock

enum class TransactionType {EXPENSE, INCOME}
data class Transaction(
    val id: Int = 0,
    val cycleId: Int,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val timestamp: Instant = Clock.System.now(),
    ) {
    val date: LocalDate
        get() = timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
}
