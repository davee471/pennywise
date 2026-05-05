package stud.brokers.pennywise.services

import java.awt.*
import java.awt.TrayIcon.MessageType

/**
 * JVM/Desktop implementation of [NotificationService].
 *
 * Uses [java.awt.SystemTray] to display a system tray notification bubble. The tray icon is a 16x16
 * transparent [BufferedImage] — invisible in the tray but required by the [TrayIcon] API.
 *
 * Falls back silently if [SystemTray.isSupported] returns false (e.g. on headless Linux servers).
 * The icon is added to the tray lazily on first [sendAlert] call and reused for subsequent alerts.
 */
actual class NotificationService {
  /**
   * Lazy tray icon — created once on first [sendAlert] call. 
   * Draws a simple colored icon so it doesn't appear as a blank/invisible gap in the system tray.
   */
  private val trayIcon: TrayIcon by lazy {
    val image = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.color = java.awt.Color(0, 150, 136) // Teal background
    graphics.fillOval(2, 2, 12, 12)
    graphics.color = java.awt.Color.WHITE
    graphics.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 10)
    graphics.drawString("P", 4, 12)
    graphics.dispose()

    TrayIcon(image, "PennyWise").apply { isImageAutoSize = true }
  }

  /**
   * Displays a system tray notification bubble with [message] as the body.
   *
   * Silently returns if [SystemTray] is not supported on the current platform. Adds [trayIcon] to
   * the system tray on first call if not already present.
   *
   * @param message The alert message to display (e.g. "80% of budget reached").
   */
  actual suspend fun sendAlert(message: String) {
    if (!SystemTray.isSupported()) return

    try {
      val tray = SystemTray.getSystemTray()
  
      if (tray.trayIcons.none { it == trayIcon }) {
        tray.add(trayIcon)
      }
  
      trayIcon.displayMessage("PennyWise Alert", message, MessageType.INFO)
    } catch (e: Exception) {
      // Silently fail if the OS rejects the tray icon (common on Wayland Linux distros)
    }
  }
}
