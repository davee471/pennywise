package stud.brokers.pennywise.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class TransactionType { EXPENSE, INCOME }
@Serializable
data class Transaction(
    val id: Long = 0,
    val cycleId: Long,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
) {
    val date: LocalDate
        get() = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
}