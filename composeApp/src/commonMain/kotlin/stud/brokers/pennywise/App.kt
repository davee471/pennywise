package stud.brokers.pennywise

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import stud.brokers.pennywise.controllers.BudgetController
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.db.DatabaseManager
import stud.brokers.pennywise.db.DriverFactory
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.ui.screens.*

sealed class Screen {
    data object Dashboard : Screen()
    data object History : Screen()
    data object Transaction : Screen()
    data object Setup : Screen()
}

@Composable
fun App(driverFactory: DriverFactory) {
    // Initialize database and controllers
    val dbManager = remember { DatabaseManager(driverFactory) }
    val budgetController = remember { BudgetController(dbManager) }
    val txController = remember { TransactionController(dbManager) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    
    val activeCycle = budgetController.activeCycle

    // Add default categories if the database is empty
    LaunchedEffect(Unit) {
        val categories = txController.getCategories().getOrNull()
        if (categories.isNullOrEmpty()) {
            txController.addCategory("Food", "restaurant")
            txController.addCategory("Transport", "directions_bus")
            txController.addCategory("Shopping", "shopping_cart")
            txController.addCategory("Health", "medical_services")
            txController.addCategory("Entertainment", "movie")
        }
    }

    MaterialTheme {
        if (activeCycle == null) {
            // Show setup screen if no budget cycle exists
            SetupView(budgetController = budgetController) {
                // Cycle created successfully
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Dashboard,
                            onClick = { 
                                transactionToEdit = null
                                currentScreen = Screen.Dashboard 
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.History,
                            onClick = { 
                                transactionToEdit = null
                                currentScreen = Screen.History 
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History") },
                            label = { Text("History") }
                        )
                    }
                }
            ) { padding ->
                Surface(modifier = Modifier.padding(padding)) {
                    when (currentScreen) {
                        Screen.Dashboard -> {
                            var dailyLimit by remember { mutableStateOf(0.0) }
                            var categoryTotals by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

                            // Update dashboard data when cycle changes
                            LaunchedEffect(activeCycle) {
                                dailyLimit = budgetController.getDailyLimit()
                                categoryTotals = txController.getCategoryTotals(activeCycle.id).getOrNull()
                                    ?.mapKeys { entry -> entry.key.name } ?: emptyMap()
                            }

                            DashboardView(
                                dailyLimit = dailyLimit,
                                isFinalDay = false,
                                isLowBudget = false,
                                pieChartData = categoryTotals,
                                onLogExpenseClick = { 
                                    transactionToEdit = null
                                    currentScreen = Screen.Transaction 
                                },
                                onLogIncomeClick = {
                                    // Handle income logging here
                                }
                            )
                        }
                        Screen.History -> HistoryView(
                            txController = txController, 
                            cycleId = activeCycle.id,
                            onEditTransaction = { tx ->
                                transactionToEdit = tx
                                currentScreen = Screen.Transaction
                            }
                        )
                        Screen.Transaction -> TransactionView(
                            txController = txController,
                            cycleId = activeCycle.id,
                            transactionToEdit = transactionToEdit,
                            onTransactionSaved = { 
                                transactionToEdit = null
                                currentScreen = Screen.Dashboard 
                            },
                            onCancel = {
                                transactionToEdit = null
                                currentScreen = Screen.Dashboard
                            }
                        )
                        Screen.Setup -> {}
                    }
                }
            }
        }
    }
}
