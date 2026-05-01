package stud.brokers.pennywise.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class BackupPayload(
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val categories: List<Category>,
    val cycles: List<BudgetCycle>,
    val transaction: List<Transaction>,
    val settings: Map<String,String>
) {
    val date: LocalDate
        get() = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
}
