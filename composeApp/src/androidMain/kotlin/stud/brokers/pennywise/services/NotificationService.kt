package stud.brokers.pennywise.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import stud.brokers.pennywise.R

actual class NotificationService(private val context: Context) {

  private val channelId = "pennywise_alerts"
  private val notificationManager =
          context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    createNotificationChannel()
  }

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

  actual suspend fun sendAlert(message: String) {
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
