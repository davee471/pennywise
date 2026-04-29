package stud.brokers.pennywise.ui.components
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings

@Composable
fun NavBar(
    selectedScreen: String = "dashboard",
    onNavigate: (String) -> Unit = {}
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedScreen == "dashboard",
            onClick = { onNavigate("dashboard") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = selectedScreen == "history",
            onClick = { onNavigate("history") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("History") }
        )

        NavigationBarItem(
            selected = selectedScreen == "stats",
            onClick = { onNavigate("stats") },
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            label = { Text("Stats") }
        )

        NavigationBarItem(
            selected = selectedScreen == "settings",
            onClick = { onNavigate("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") }
        )
    }
}