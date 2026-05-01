package stud.brokers.pennywise.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.services.ExportService
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.SettingsKeys
class SettingsController(
    private val dbManager: DatabaseManager,
    private val budgetController: BudgetController,
    private val exportService: ExportService
) {
    var isPinEnabled: Boolean = false
        private set
    var isNotificationsEnabled: Boolean = false
        private set
    var currencySymbol: String = "EGP"
        private set

    init {
        CoroutineScope(Dispatchers.Default).launch { loadSettings() }
    }

    suspend fun loadSettings() {
        isPinEnabled = when (val res = dbManager.fetchSetting(SettingsKeys.PIN_ENABLED)) {
            is Result.Success -> res.data == "true"
            else -> false
        }

        isNotificationsEnabled = when (val res = dbManager.fetchSetting(SettingsKeys.NOTIFICATION_ENABLED)) {
            is Result.Success -> res.data == "true"
            else -> false
        }

        currencySymbol = when (val res = dbManager.fetchSetting(SettingsKeys.CURRENCY_SYMBOL)) {
            is Result.Success -> res.data ?: "EGP"
            else -> "EGP"
        }
    }

    suspend fun updateCurrency(symbol: String): Boolean {
        val res = dbManager.upsertSetting(SettingsKeys.CURRENCY_SYMBOL, symbol)
        if (res is Result.Success) {
            currencySymbol = symbol
            return true
        }
        return false
    }

    suspend fun toggleNotifications(enabled: Boolean): Boolean {
        val res = dbManager.upsertSetting(SettingsKeys.NOTIFICATION_ENABLED, enabled.toString())
        if (res is Result.Success) {
            isNotificationsEnabled = enabled
            return true
        }
        return false
    }

    suspend fun togglePinLock(enabled: Boolean, rawPin: String? = null): Boolean {
        val res = dbManager.upsertSetting(SettingsKeys.PIN_ENABLED, enabled.toString())
        if (res is Result.Success) {
            isPinEnabled = enabled
            if (rawPin != null) {
                // hash the PIN before saving to the db
                val hashedPin = rawPin.hashCode().toString()
                dbManager.upsertSetting(SettingsKeys.PIN_HASH, hashedPin)
            }
            return true
        }
        return false
    }

    suspend fun exportDataToCsv(): Result<Unit> {
        return when (val txResult = dbManager.fetchTransactions()) {
            is Result.Success -> {
                exportService.exportToCsv(txResult.data)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(txResult.message, txResult.type)
        }
    }

    suspend fun performFullReset(): Boolean {
        val dbCleared = dbManager.clearAll() is Result.Success
        val cycleReset = budgetController.resetCycle()
        return dbCleared && cycleReset
    }

    suspend fun verifyPin(input: String): Boolean {
        return when (val res = dbManager.fetchSetting(SettingsKeys.PIN_HASH)) {
            is Result.Success -> res.data == input.hashCode().toString()
            else -> false
        }
    }
}