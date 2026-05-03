package stud.brokers.pennywise.services

/**
 * Platform-specific service for sending budget alert notifications.
 *
 * This is an `expect` declaration — each platform provides its own `actual` implementation:
 * - **Android**: uses [android.app.NotificationManager] with a dedicated notification channel
 * ("pennywise_alerts").
 * - **JVM/Desktop**: uses [java.awt.SystemTray] to display a system tray message.
 *
 * [sendAlert] is called by [stud.brokers.pennywise.controllers.TransactionController] after a
 * transaction is logged, when spending crosses 80% or 100% of the total allowance (US #6).
 */
expect class NotificationService {

  /**
   * Sends a budget alert notification to the user.
   *
   * @param message The alert message to display (e.g. "80% of budget reached" or "Budget
   * exhausted").
   */
  suspend fun sendAlert(message: String)
}
