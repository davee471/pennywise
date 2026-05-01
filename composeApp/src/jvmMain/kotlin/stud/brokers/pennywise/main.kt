package stud.brokers.pennywise

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import stud.brokers.pennywise.controllers.BudgetController
import stud.brokers.pennywise.controllers.SettingsController
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.db.DriverFactory
import stud.brokers.pennywise.services.ExportService

fun main() = application {
    // Build the controllers
    val driverFactory = DriverFactory()
    val exportService = ExportService()
    val dbManager = DatabaseManager(driverFactory)
    val txController = TransactionController(dbManager)
    val budgetController = BudgetController(dbManager, txController = txController)
    val settingsController = SettingsController(dbManager, budgetController, exportService)

    Window(
        onCloseRequest = ::exitApplication,
        title = "PennyWise",
    ) {
        // Pass it in!
        App(settingsController = settingsController)
    }
}