package stud.brokers.pennywise

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import stud.brokers.pennywise.controllers.BudgetController
import stud.brokers.pennywise.controllers.SettingsController
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.controllers.BackupController
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.db.DriverFactory
import stud.brokers.pennywise.services.ExportService
import stud.brokers.pennywise.services.BackupService
import stud.brokers.pennywise.services.NotificationService

fun main() = application {
    // Build the controllers
    val driverFactory = DriverFactory()
    val exportService = ExportService()
    val backupService = BackupService()
    val notificationService = NotificationService()
    val dbManager = DatabaseManager(driverFactory)
    val txController = TransactionController(dbManager)
    val budgetController = BudgetController(dbManager, txController = txController)
    val backupController = BackupController(dbManager, backupService)
    val settingsController = SettingsController(dbManager, budgetController, exportService, backupController)

    Window(
        onCloseRequest = ::exitApplication,
        title = "PennyWise",
    ) {
        // Pass it in!
        App(settingsController = settingsController,
            budgetController = budgetController,
            txController = txController,
            notificationService = notificationService)
    }
}