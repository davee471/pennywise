package stud.brokers.pennywise.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class BudgetCycle(
    var totalAllowance: Double,
    val startDate: LocalDate,
    val endDate: LocalDate,
){
    val totalDays: Int
        get() = startDate.daysUntil(endDate)

    val remainingDays: Int
        get() {
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            return today.daysUntil(endDate).coerceAtLeast(1)
        }

    fun calculateLimit(balance: Double): Double{
        return balance / remainingDays
    }
}