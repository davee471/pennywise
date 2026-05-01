package stud.brokers.pennywise.ui.screens


        OutlinedTextField(
            value = totalBudget,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) totalBudget = it },
            label = { Text("Total Budget (EGP)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Simple Date Selection (In a real app, use a DatePicker dialog)
        Button(
            onClick = { /* Trigger DatePicker for End Date */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End Date: $endDate")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val amount = totalBudget.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    scope.launch {
                        budgetController.initCycle(amount, startDate, endDate)
                        onSetupComplete()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}
