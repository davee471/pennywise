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
import stud.brokers.pennywise.services.NotificationService
import android.os.Build
import android.Manifest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        //test shit

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // Build the controllers
        val driverFactory = DriverFactory(this)
        //testing notificaitonservice
        val notificationService = NotificationService(this)
        val exportService = ExportService(
            this,
            noti = notificationService
        )
        val dbManager = DatabaseManager(driverFactory)
        val txController = TransactionController(dbManager)
        val budgetController = BudgetController(dbManager,txController)
        val settingsController = SettingsController(dbManager, budgetController, exportService)



        setContent {
            // Pass it in!
            App(
                settingsController = settingsController,
                budgetController = budgetController,
                txController = txController
            )
        }
    }
}