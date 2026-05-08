package stud.brokers.pennywise.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.services.ExportService
import stud.brokers.pennywise.util.Result
import stud.brokers.pennywise.util.SettingsKeys
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Controller responsible for persisting and exposing user-configurable app settings.
 *
 * Bridges the UI and [DatabaseManager] for all settings-related operations, including:
 * - PIN lock management (enable/disable and hash-based verification).
 * - Push notification preferences.
 * - Display currency symbol selection.
 * - PDF export via [ExportService].
 * - Backup and restore via [BackupController].
 * - Full app data reset.
 *
 * Settings are stored as key-value pairs in the database using keys defined in [SettingsKeys].
 * All reactive properties are backed by Compose [mutableStateOf] so the UI recomposes
 * automatically when values change.
 *
 * Initialization is asynchronous: the [init] block launches a coroutine on [Dispatchers.Default]
 * that loads all settings and then sets [isLoaded] to `true`. Gate any settings-dependent UI
 * behind [isLoaded].
 *
 * @property dbManager The database manager used to read and write key-value settings.
 * @property budgetController The budget controller, used during a full reset to clear cycle state.
 * @property exportService The service responsible for rendering and saving PDF exports.
 * @property backupController The controller that handles backup file creation and restoration.
 */
class SettingsController(
    private val dbManager: DatabaseManager,
    private val budgetController: BudgetController,
    private val exportService: ExportService,
    private val backupController: BackupController
) {

    /**
     * Whether PIN lock is currently enabled.
     *
     * When `true`, the app should require the user to enter a PIN on launch and for sensitive
     * actions. Defaults to `false` until loaded from the database.
     */
    var isPinEnabled: Boolean = false
        private set

    /**
     * Whether push notifications are currently enabled.
     *
     * Defaults to `true` both as an in-memory fallback and when no persisted value exists.
     */
    var isNotificationsEnabled: Boolean = true
        private set

    /**
     * The currency symbol displayed throughout the UI (e.g., `"EGP"`, `"USD"`, `"€"`).
     *
     * Defaults to `"EGP"` until loaded from the database.
     */
    var currencySymbol: String = "EGP"
        private set

    /**
     * Whether the initial settings have finished loading from the database.
     *
     * Backed by Compose state. The UI should wait for this to be `true` before rendering
     * settings-dependent components to avoid displaying stale defaults.
     */
    var isLoaded by mutableStateOf(false)
        private set

    init {
        CoroutineScope(Dispatchers.Default).launch {
            loadSettings()
            isLoaded = true
        }
    }

    /**
     * Loads all user settings from the database and updates the in-memory state.
     *
     * Reads the following [SettingsKeys]:
     * - [SettingsKeys.PIN_ENABLED] → [isPinEnabled]
     * - [SettingsKeys.NOTIFICATION_ENABLED] → [isNotificationsEnabled]
     * - [SettingsKeys.CURRENCY_SYMBOL] → [currencySymbol]
     *
     * Falls back to safe defaults (`false`, `true`, `"EGP"` respectively) if any key is missing
     * or the fetch fails. Safe to call multiple times; re-reads from the database each time.
     */
    suspend fun loadSettings() {
        isPinEnabled = when (val res = dbManager.fetchSetting(SettingsKeys.PIN_ENABLED)) {
            is Result.Success -> res.data == "true"
            else -> false
        }

        isNotificationsEnabled = when (val res = dbManager.fetchSetting(SettingsKeys.NOTIFICATION_ENABLED)) {
            is Result.Success -> res.data?.toBoolean() ?: true
            else -> true
        }

        currencySymbol = when (val res = dbManager.fetchSetting(SettingsKeys.CURRENCY_SYMBOL)) {
            is Result.Success -> res.data ?: "EGP"
            else -> "EGP"
        }
    }

    /**
     * Persists a new currency symbol and updates [currencySymbol] if the save succeeds.
     *
     * @param symbol The currency symbol to display (e.g., `"USD"`, `"€"`). No format validation
     *   is performed; the caller is responsible for passing a valid value.
     * @return `true` if the setting was saved successfully; `false` otherwise.
     */
    suspend fun updateCurrency(symbol: String): Boolean {
        val res = dbManager.upsertSetting(SettingsKeys.CURRENCY_SYMBOL, symbol)
        if (res is Result.Success) {
            currencySymbol = symbol
            return true
        }
        return false
    }

    /**
     * Persists the notification preference and updates [isNotificationsEnabled] if the save succeeds.
     *
     * Note: this only persists the preference — actually scheduling or cancelling system
     * notifications is the responsibility of the caller (e.g., the settings screen ViewModel).
     *
     * @param enabled `true` to enable notifications; `false` to disable them.
     * @return `true` if the setting was saved successfully; `false` otherwise.
     */
    suspend fun toggleNotifications(enabled: Boolean): Boolean {
        val res = dbManager.upsertSetting(SettingsKeys.NOTIFICATION_ENABLED, enabled.toString())
        if (res is Result.Success) {
            isNotificationsEnabled = enabled
            return true
        }
        return false
    }

    /**
     * Enables or disables PIN lock and optionally saves a new hashed PIN.
     *
     * If [rawPin] is provided, it is hashed via [String.hashCode] before being stored under
     * [SettingsKeys.PIN_HASH]. This provides basic obfuscation; note that `hashCode` is **not**
     * a cryptographically secure hash function.
     *
     * The [SettingsKeys.PIN_ENABLED] flag is always written first; the PIN hash write only occurs
     * when [rawPin] is non-null. If the primary write fails, `false` is returned immediately and
     * the hash write is skipped.
     *
     * @param enabled `true` to enable PIN lock; `false` to disable it.
     * @param rawPin The plain-text PIN entered by the user. Pass `null` to leave the stored hash
     *   unchanged (e.g., when only toggling the lock without changing the PIN).
     * @return `true` if the [SettingsKeys.PIN_ENABLED] setting was saved successfully; `false`
     *   otherwise.
     */
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

    /**
     * Exports the app's report as a PDF using the provided HTML content.
     *
     * Delegates rendering and file-system operations entirely to [ExportService.exportToPdf].
     * The caller is responsible for generating valid HTML content before calling this function.
     *
     * @param htmlContent A complete HTML string representing the report to export.
     * @return [Result.Success] if the PDF was created successfully; [Result.Error] with a
     *   descriptive message otherwise.
     */
    suspend fun exportToPdf(htmlContent: String): Result<Unit> {
        return exportService.exportToPdf(htmlContent)
    }

    /**
     * Triggers a full database backup via [BackupController.backup].
     *
     * The backup format, storage location, and any platform-specific sharing sheet are managed
     * by [BackupController].
     *
     * @return [Result.Success] on success; [Result.Error] on failure.
     */
    suspend fun exportBackup(): Result<Unit> {
        return backupController.backup()
    }

    /**
     * Triggers a database restore via [BackupController.restore].
     *
     * The user is expected to have selected a backup file through a platform file-picker before
     * calling this. After a successful restore the app state should be refreshed by re-loading
     * all controllers.
     *
     * @return [Result.Success] on success; [Result.Error] on failure.
     */
    suspend fun importBackup(): Result<Unit> {
        return backupController.restore()
    }

    /**
     * Performs a full application data reset, clearing all database records and cycle state.
     *
     * Executes two operations in sequence:
     * 1. [DatabaseManager.clearAll] — drops all persisted data from every table.
     * 2. [BudgetController.resetCycle] — clears the in-memory active cycle reference.
     *
     * Both operations must succeed for this function to return `true`.
     *
     * **This action is irreversible.** The caller should present a confirmation dialog before
     * invoking this function.
     *
     * @return `true` if both operations completed successfully; `false` if either failed.
     */
    suspend fun performFullReset(): Boolean {
        val dbCleared = dbManager.clearAll() is Result.Success
        val cycleReset = budgetController.resetCycle()
        return dbCleared && cycleReset
    }

    /**
     * Verifies a user-entered PIN against the stored hash.
     *
     * Hashes [input] with [String.hashCode] and compares it to the value stored under
     * [SettingsKeys.PIN_HASH]. Returns `false` if no hash has been stored yet.
     *
     * @param input The plain-text PIN string entered by the user.
     * @return `true` if [input]'s hash matches the stored PIN hash; `false` otherwise (including
     *   when no hash is stored or the database fetch fails).
     */
    suspend fun verifyPin(input: String): Boolean {
        return when (val res = dbManager.fetchSetting(SettingsKeys.PIN_HASH)) {
            is Result.Success -> res.data == input.hashCode().toString()
            else -> false
        }
    }
}