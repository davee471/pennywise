package stud.brokers.pennywise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import stud.brokers.pennywise.controllers.SettingsController
import stud.brokers.pennywise.controllers.BudgetController
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.ui.components.NavBar
import stud.brokers.pennywise.ui.components.PinMode
import stud.brokers.pennywise.ui.components.PinOverlay
import stud.brokers.pennywise.ui.screens.*
import stud.brokers.pennywise.util.Result

@Composable
fun App(
    settingsController: SettingsController,
    budgetController: BudgetController,
    transactionController: TransactionController
) {
    val coroutineScope = rememberCoroutineScope()

    var currentRoute by remember { mutableStateOf("dashboard") }
    var isUnlocked by remember { mutableStateOf(!settingsController.isPinEnabled) }

    // State for Transaction Logging
    var showTransactionScreen by remember { mutableStateOf(false) }
    var isLoggingIncome by remember { mutableStateOf(false) }

    // Load available categories
    val categories by produceState<List<stud.brokers.pennywise.models.Category>>(initialValue = emptyList(), showTransactionScreen, currentRoute) {
        val res = transactionController.getCategories()
        value = if (res is Result.Success) res.data else emptyList()
    }

    // NEW: State for tracking if we are setting up a new PIN
    var showPinSetup by remember { mutableStateOf(false) }

    var isPinEnabled by remember { mutableStateOf(settingsController.isPinEnabled) }
    var isNotificationsEnabled by remember { mutableStateOf(settingsController.isNotificationsEnabled) }
    var currency by remember { mutableStateOf(settingsController.currencySymbol) }

    // Dashboard and Stats data states
    var dailyLimit by remember { mutableStateOf(0.0) }
    var totalSpent by remember { mutableStateOf(0.0) }
    var isLowBudget by remember { mutableStateOf(false) }
    var pieChartData by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    LaunchedEffect(currentRoute, showTransactionScreen, budgetController.activeCycle) {
        val cycleId = budgetController.activeCycle?.id ?: 0L
        if (cycleId != 0L) {
            dailyLimit = budgetController.getDailyLimit()
            totalSpent = budgetController.getTotalSpent()
            isLowBudget = budgetController.checkLowBudget()
            val totalsRes = transactionController.getCategoryTotals(cycleId)
            if (totalsRes is Result.Success) {
                pieChartData = totalsRes.data.mapKeys { it.key.name }
            }
        }
    }

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
        return
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
        return // Stop drawing the app underneath
    }

    // 3. Transaction Logging Overlay
    if (showTransactionScreen) {
        TransactionView(
            isIncome = isLoggingIncome,
            categories = categories,
            onCancel = { showTransactionScreen = false },
            onSubmit = { amount, category ->
                coroutineScope.launch {
                    val currentCycle = budgetController.activeCycle
                    if (currentCycle != null) {
                        if (isLoggingIncome) {
                            budgetController.addIncome(amount)
                        } else if (category != null) {
                            transactionController.logExpense(amount, category, currentCycle.id)
                            // Force BudgetController to reload since it doesn't know about the expense
                            budgetController.loadActiveCycle()
                        }
                    }
                    showTransactionScreen = false
                }
            }
        )
        return
    }

    // 4. Setup Screen (If no active cycle)
    if (budgetController.activeCycle == null) {
        SetupView(
            budgetController = budgetController,
            onSetupComplete = {
                coroutineScope.launch {
                    budgetController.loadActiveCycle()
                }
            }
        )
        return
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
                    dailyLimit = dailyLimit,
                    isFinalDay = budgetController.isOnFinalDay,
                    isLowBudget = isLowBudget,
                    pieChartData = pieChartData,
                    onLogExpenseClick = { 
                        isLoggingIncome = false
                        showTransactionScreen = true 
                    },
                    onLogIncomeClick = { 
                        isLoggingIncome = true
                        showTransactionScreen = true 
                    }
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
                        coroutineScope.launch { 
                            settingsController.performFullReset() 
                            // Refresh active cycle
                            budgetController.loadActiveCycle()
                        }
                    }
                )
                "history" -> HistoryView(
                    txController = transactionController,
                    cycleId = budgetController.activeCycle?.id ?: 0L
                )
                "stats" -> StatsView(
                    totalSpent = totalSpent,
                    pieChartData = pieChartData
                )
            }
        }
    }
}
