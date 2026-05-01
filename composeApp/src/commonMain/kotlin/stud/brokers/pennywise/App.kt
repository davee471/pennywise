package stud.brokers.pennywise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import stud.brokers.pennywise.controllers.SettingsController
import stud.brokers.pennywise.ui.components.NavBar
import stud.brokers.pennywise.ui.components.PinMode
import stud.brokers.pennywise.ui.components.PinOverlay
import stud.brokers.pennywise.ui.screens.DashboardView
import stud.brokers.pennywise.ui.screens.SettingsView
import stud.brokers.pennywise.ui.theme.AppTheme

@Composable
fun App(settingsController: SettingsController) {
    AppTheme{
        val coroutineScope = rememberCoroutineScope()

        var currentRoute by remember { mutableStateOf("dashboard") }
        var isUnlocked by remember { mutableStateOf(!settingsController.isPinEnabled) }

        // NEW: State for tracking if we are setting up a new PIN
        var showPinSetup by remember { mutableStateOf(false) }

        var isPinEnabled by remember { mutableStateOf(settingsController.isPinEnabled) }
        var isNotificationsEnabled by remember { mutableStateOf(settingsController.isNotificationsEnabled) }
        var currency by remember { mutableStateOf(settingsController.currencySymbol) }

        // 1. App Startup Lock (VERIFY)
        if (!isUnlocked && settingsController.isPinEnabled) {
            PinOverlay(
                mode = PinMode.VERIFY,
                onPinEnter = { enteredPin ->
                    coroutineScope.launch {
                        if (settingsController.verifyPin(enteredPin)) {
                            isUnlocked = true
                        }
                    }
                },
                onCancel = { /* No-op */ }
            )
            return@AppTheme
        }

        // 2. Security Setup Lock (SET) - Added here!
        if (showPinSetup) {
            PinOverlay(
                mode = PinMode.SET,
                onPinEnter = { newPin ->
                    coroutineScope.launch {
                        // Save the REAL pin they just typed
                        if (settingsController.togglePinLock(true, newPin)) {
                            isPinEnabled = true
                            showPinSetup = false // Hide the overlay
                        }
                    }
                },
                onCancel = {
                    showPinSetup = false // Hide if they click cancel
                }
            )
            return@AppTheme // Stop drawing the app underneath
        }

        Scaffold(
            bottomBar = {
                NavBar(
                    selectedScreen = currentRoute,
                    onNavigate = { route -> currentRoute = route }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentRoute) {
                    "dashboard" -> DashboardView(
                        dailyLimit = 150.0,
                        isFinalDay = false,
                        isLowBudget = false,
                        pieChartData = mapOf("Food" to 300.0, "Transport" to 100.0),
                        onLogExpenseClick = { },
                        onLogIncomeClick = { }
                    )
                    "settings" -> SettingsView(
                        isPinEnabled = isPinEnabled,
                        isNotificationsEnabled = isNotificationsEnabled,
                        currencySymbol = currency,
                        onExportCsvClick = {
                            coroutineScope.launch { settingsController.exportDataToCsv() }
                        },

                        // Added new toggle logic here!
                        onTogglePinClick = { enabled ->
                            if (enabled) {
                                // User wants to turn it ON -> Show the setup screen!
                                showPinSetup = true
                            } else {
                                // User wants to turn it OFF -> Just turn it off in the database
                                coroutineScope.launch {
                                    if (settingsController.togglePinLock(false)) {
                                        isPinEnabled = false
                                    }
                                }
                            }
                        },

                        onToggleNotificationsClick = { enabled ->
                            coroutineScope.launch {
                                if (settingsController.toggleNotifications(enabled)) {
                                    isNotificationsEnabled = enabled
                                }
                            }
                        },
                        onChangeCurrencyClick = { newCurrency ->
                            coroutineScope.launch {
                                if (settingsController.updateCurrency(newCurrency)) {
                                    currency = newCurrency
                                }
                            }
                        },
                        onResetCycleClick = {
                            coroutineScope.launch { settingsController.performFullReset() }
                        }
                    )
                    "history" -> { /* Add HistoryView later */ }
                    "stats" -> { /* Add StatsView later */ }
    }

            }
        }
    }
}