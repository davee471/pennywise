package stud.brokers.pennywise.services

import java.awt.*

actual class NotificationService {
  private val trayIcon: TrayIcon by lazy {
    val image = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    TrayIcon(image, "PennyWise").apply { isImageAutoSize = true }
  }

  actual suspend fun sendAlert(message: String) {
    if (!SystemTray.isSupported()) return

    val tray = SystemTray.getSystemTray()

    if (tray.trayIcons.none { it == trayIcon }) {
      tray.add(trayIcon)
    }

    trayIcon.displayMessage("PennyWise Alert", message, MessageType.INFO)
  }
}
