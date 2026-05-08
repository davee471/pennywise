package stud.brokers.pennywise.util
 
/**
 * Compile-time constants for all setting keys stored in the `settings` database table.
 *
 * Keys are used with [stud.brokers.pennywise.db.DatabaseManager.upsertSetting] and
 * [stud.brokers.pennywise.db.DatabaseManager.fetchSetting] to read and write application
 * preferences. All values are persisted as plain strings; callers are responsible for
 * converting to and from the appropriate type (e.g. `"true"`/`"false"` for booleans).
 *
 * These same keys appear inside [stud.brokers.pennywise.models.BackupPayload.settings], so
 * changing a key's string value is a breaking change for existing backups.
 */
object SettingsKeys {
 
    /**
     * Whether the PIN lock feature is active. Stored as `"true"` or `"false"`.
     *
     * When `"true"`, the app prompts for a PIN on launch and before sensitive operations.
     * Must be read in conjunction with [PIN_HASH] to validate the user's input.
     */
    const val PIN_ENABLED = "pin_enabled"
 
    /**
     * The bcrypt/hashed representation of the user's PIN.
     *
     * Never store the raw PIN. This value is only meaningful when [PIN_ENABLED] is `"true"`.
     * When the user disables the PIN, this key should be cleared or left stale — it must not
     * be used for authentication while [PIN_ENABLED] is `"false"`.
     */
    const val PIN_HASH = "pin_hash"
 
    /**
     * Tracks whether the budget-threshold alert has already been dispatched for the active cycle.
     *
     * Stored as `"true"` or `"false"`. Reset to `"false"` when a new [stud.brokers.pennywise.models.BudgetCycle]
     * begins. Prevents duplicate notifications from firing if the user records multiple
     * transactions while already past the 80% or 100% threshold.
     */
    const val ALERT_SENT = "alert_sent"
 
    /**
     * Whether push/system-tray budget alert notifications are enabled. Stored as `"true"` or
     * `"false"`.
     *
     * When `"false"`, [stud.brokers.pennywise.services.NotificationService.sendAlert] should be
     * skipped entirely by the controller layer.
     */
    const val NOTIFICATION_ENABLED = "notifications_enabled"
 
    /**
     * The currency symbol displayed throughout the UI and in exported invoices (e.g. `"$"`,
     * `"€"`, `"EGP"`).
     *
     * Stored as a raw string. Used by [stud.brokers.pennywise.util.InvoiceGenerator.buildHtml]
     * and all amount-formatting logic across the app.
     */
    const val CURRENCY_SYMBOL = "currency_symbol"
}
