package stud.brokers.pennywise.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import stud.brokers.pennywise.R

/**
 * Android implementation of [NotificationService].
 *
 * Uses [NotificationManager] to display system push notifications. A dedicated notification channel
 * ("pennywise_alerts") is created in [init] — this is required on Android 8.0 (API 26) and above.
 * On older versions the channel creation is skipped via the [Build.VERSION_CODES.O] check.
 *
 * Each call to [sendAlert] posts a new notification with a unique ID derived from the current
 * timestamp, so multiple alerts never overwrite each other.
 *
 * Requires `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` in
 * `AndroidManifest.xml` (enforced on Android 13+).
 *
 * @param context Android [Context] used to access [NotificationManager] and build notifications.
 */
actual class NotificationService(private val context: Context) {

  private val channelId = "pennywise_alerts"
  private val notificationManager =
          context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    createNotificationChannel()
  }
  /**
   * Creates the PennyWise notification channel with [NotificationManager.IMPORTANCE_DEFAULT]. Safe
   * to call multiple times — Android ignores channel creation if it already exists. No-op on API <
   * 26.
   */
  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "PennyWise Alerts"
      val descriptionText = "Notifications for budget alerts and transactions"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel =
              NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
              }
      notificationManager.createNotificationChannel(channel)
    }
  }

  /**
   * Posts a push notification with [message] as the body text.
   *
   * Uses [System.currentTimeMillis] as the notification ID so rapid successive alerts don't
   * overwrite each other. The notification auto-cancels when tapped.
   *
   * @param message The alert message to display (e.g. "80% of budget reached").
   */
  actual suspend fun sendAlert(message: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        return // Silently abort if permission denied to prevent security crashes
      }
    }

    val builder =
            NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("PennyWise Alert")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)

    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
  }
}
