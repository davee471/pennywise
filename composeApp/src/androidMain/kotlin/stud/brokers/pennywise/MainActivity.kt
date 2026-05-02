package stud.brokers.pennywise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import stud.brokers.pennywise.controllers.BudgetController
import stud.brokers.pennywise.controllers.SettingsController
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.db.DriverFactory
import stud.brokers.pennywise.services.ExportService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Build the controllers
        val driverFactory = DriverFactory(this)
        val exportService = ExportService()
        val dbManager = DatabaseManager(driverFactory)
        val txController = TransactionController(dbManager)
        val budgetController = BudgetController(dbManager, txController)
        val settingsController = SettingsController(dbManager, budgetController, exportService)

        setContent {
            // Pass it in!
            App(settingsController = settingsController,
                budgetController = budgetController,
                txController = txController)
        }
    }
}