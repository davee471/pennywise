package stud.brokers.pennywise.services

import java.awt.*
import java.awt.TrayIcon.MessageType

actual class NotificationService {

  actual suspend fun sendAlert(message: String) {
    if (!SystemTray.isSupported()) {
      println("SystemTray is not supported on this platform")
      return
    }

    val tray = SystemTray.getSystemTray()

    val image = Toolkit.getDefaultToolkit().createImage("")
    val trayIcon = TrayIcon(image, "PennyWise")

    try {
      tray.add(trayIcon)
      // The displayMessage method triggers the OS notification
      trayIcon.displayMessage("PennyWise Alert", message, MessageType.INFO)

      tray.remove(trayIcon)
    } catch (e: AWTException) {
      println("Could not add TrayIcon: ${e.message}")
    }
  }
}
