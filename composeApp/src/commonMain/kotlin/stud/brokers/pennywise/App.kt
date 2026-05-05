package stud.brokers.pennywise

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import stud.brokers.pennywise.controllers.BudgetController
import stud.brokers.pennywise.controllers.SettingsController
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType
import stud.brokers.pennywise.ui.components.NavBar
import stud.brokers.pennywise.ui.components.PinMode
import stud.brokers.pennywise.ui.components.PinOverlay
import stud.brokers.pennywise.ui.screens.*
import stud.brokers.pennywise.ui.theme.AppTheme // <-- Custom Theme Import!

@Composable
fun App(
    settingsController: SettingsController,
    budgetController: BudgetController,
    txController: TransactionController
) {
    // Default to the system theme on launch
    val systemTheme = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemTheme) }

    // 1. WRAP THE ENTIRE APP IN YOUR CUSTOM THEME
    AppTheme(darkTheme = isDarkTheme) {
        val coroutineScope = rememberCoroutineScope()

        var currentRoute by remember { mutableStateOf("dashboard") }
        var isUnlocked by remember { mutableStateOf(!settingsController.isPinEnabled) }
        var showPinSetup by remember { mutableStateOf(false) }

        var isPinEnabled by remember { mutableStateOf(settingsController.isPinEnabled) }
        var isNotificationsEnabled by remember { mutableStateOf(settingsController.isNotificationsEnabled) }
        var currency by remember { mutableStateOf(settingsController.currencySymbol) }

        // State for holding a transaction if the user clicks "Edit" in HistoryView
        var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

        // 2. Loading State Blocker (Prevents the NO_CYCLE SetupView flash glitch)
        if (!settingsController.isLoaded || !budgetController.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@AppTheme
        }

        // 3. App Startup Lock (VERIFY)
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

        // 4. Security Setup Lock (SET)
        if (showPinSetup) {
            PinOverlay(
                mode = PinMode.SET,
                onPinEnter = { newPin ->
                    coroutineScope.launch {
                        if (settingsController.togglePinLock(true, newPin)) {
                            isPinEnabled = true
                            showPinSetup = false
                        }
                    }
                },
                onCancel = { showPinSetup = false }
            )
            return@AppTheme
        }

        // 5. Initial Setup Gatekeeper
        // If there is no active cycle, force the user to set one up.
        if (budgetController.cycleStatus == BudgetController.CycleStatus.NO_CYCLE) {
            SetupView(
                budgetController = budgetController,
                currencySymbol = currency,
                onSetupComplete = {
                    // The activeCycle variable inside BudgetController will update automatically,
                    // triggering a recomposition that removes this SetupView!
                }
            )
            return@AppTheme
        }

        // Safe to unwrap since we checked NO_CYCLE above
        val activeCycleId = budgetController.activeCycle?.id ?: return@AppTheme

        // 6. Main App Routing
        Scaffold(
            bottomBar = {
                // Hide NavBar if we are currently inside the Transaction form
                if (currentRoute != "transaction") {
                    NavBar(
                        selectedScreen = currentRoute,
                        onNavigate = { route -> currentRoute = route }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentRoute) {
                    "dashboard" -> {
                        // Local state to hold the real data
                        var dailyLimit by remember { mutableStateOf(0.0) }
                        var isLowBudget by remember { mutableStateOf(false) }
                        var pieChartData by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

                        var showIncomeDialog by remember { mutableStateOf(false) }
                        var incomeInput by remember { mutableStateOf("") }

                        // Fetch fresh data every time we visit the Dashboard
                        LaunchedEffect(currentRoute, budgetController.activeCycle) {
                            dailyLimit = budgetController.getDailyLimit()
                            isLowBudget = budgetController.isLowBudget

                            // Calculate real Pie Chart data from History
                            val res = txController.getHistory(activeCycleId)
                            val transactions =
                                if (res is stud.brokers.pennywise.util.Result.Success<*>) {
                                    @Suppress("UNCHECKED_CAST")
                                    res.data as List<Transaction>
                                } else emptyList()

                            // Group expenses by category and sum them up
                            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
                            pieChartData = expenses.groupBy { it.category.name }
                                .mapValues { entry -> entry.value.sumOf { it.amount } }
                        }

                        // Pass the REAL variables to the View
                        DashboardView(
                            dailyLimit = dailyLimit,
                            currencySymbol = currency,
                            isFinalDay = budgetController.isOnFinalDay,
                            isLowBudget = budgetController.isLowBudget,
                            pieChartData = pieChartData,
                            onLogExpenseClick = { currentRoute = "transaction" },
                            onLogIncomeClick = { showIncomeDialog = true }
                        )

                        // The Income Pop-up Dialog
                        if (showIncomeDialog) {
                            AlertDialog(
                                onDismissRequest = { showIncomeDialog = false },
                                title = { Text("Log Income") },
                                text = {
                                    OutlinedTextField(
                                        value = incomeInput,
                                        onValueChange = {
                                            if (it.all { char -> char.isDigit() || char == '.' }) incomeInput = it
                                        },
                                        label = { Text("Amount ($currency)") }
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        val amount = incomeInput.toDoubleOrNull() ?: 0.0
                                        if (amount > 0) {
                                            coroutineScope.launch {
                                                budgetController.addIncome(amount) // Save to DB

                                                // Refresh the dashboard numbers instantly
                                                dailyLimit = budgetController.getDailyLimit()
                                                isLowBudget = budgetController.isLowBudget

                                                showIncomeDialog = false
                                                incomeInput = "" // Clear for next time
                                            }
                                        }
                                    }) { Text("Add Income") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showIncomeDialog = false
                                    }) { Text("Cancel") }
                                }
                            )
                        }
                    }

                    "transaction" -> TransactionView(
                        txController = txController,
                        budgetController = budgetController,
                        cycleId = activeCycleId,
                        currencySymbol = currency,
                        transactionToEdit = transactionToEdit,
                        onTransactionSaved = {
                            transactionToEdit = null
                            currentRoute = "dashboard" // Go back home on save
                        },
                        onCancel = {
                            transactionToEdit = null
                            currentRoute = "dashboard" // Go back home on cancel
                        }
                    )

                    "history" -> HistoryView(
                        txController = txController,
                        cycleId = activeCycleId,
                        onEditTransaction = { tx ->
                            transactionToEdit = tx
                            currentRoute = "transaction" // Open the form with this specific TX
                        }
                    )

                    "settings" -> SettingsView(
                        isPinEnabled = isPinEnabled,
                        isNotificationsEnabled = isNotificationsEnabled,
                        isDarkTheme = isDarkTheme,
                        currencySymbol = currency,
                        onExportCsvClick = { coroutineScope.launch { settingsController.exportDataToCsv() } },
                        onTogglePinClick = { enabled ->
                            if (enabled) {
                                showPinSetup = true
                            } else {
                                coroutineScope.launch {
                                    if (settingsController.togglePinLock(false)) isPinEnabled = false
                                }
                            }
                        },
                        onToggleNotificationsClick = { enabled ->
                            coroutineScope.launch {
                                if (settingsController.toggleNotifications(enabled)) isNotificationsEnabled = enabled
                            }
                        },
                        onToggleThemeClick = { enabled ->
                            isDarkTheme = enabled
                        },
                        onChangeCurrencyClick = { newCurrency ->
                            coroutineScope.launch {
                                if (settingsController.updateCurrency(newCurrency)) currency = newCurrency
                            }
                        },
                        onResetCycleClick = {
                            coroutineScope.launch { settingsController.performFullReset() }
                        }
                    )

                    "stats" -> StatsView(
                        txController = txController,
                        cycleId = activeCycleId,
                        currencySymbol = currency
                    )
                }
            }
        }
    }
}