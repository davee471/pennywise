package stud.brokers.pennywise.ui.components

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PinMode{
    SET,
    VERIFY,
}

@Composable
fun PinOverlay(
    mode: PinMode,
    onPinEnter: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    val title = when (mode) {
        PinMode.SET -> "Please Enter The Desired 4-digit PIN"
        PinMode.VERIFY -> "Please Enter Your 4-digit PIN"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- HEADER SECTION ---
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            if (mode == PinMode.SET) {
                Text(
                    text = "Used to unlock the app later",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- PIN INPUT FIELD ---
            OutlinedTextField(
                value = pin,
                onValueChange = { input ->
                    // Logic: Only allow digits and limit to 4 characters
                    if (input.length <= 4 && input.all { it.isDigit() }) {
                        pin = input
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    letterSpacing = 10.sp, // Adds space between digits/stars
                    color = Color.Black
                ),
                // This creates the masking effect for verification
                visualTransformation = if (mode == PinMode.VERIFY) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black,
                    cursorColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- THE UNLOCK BUTTON ---
            Button(
                onClick = { if (pin.length == 4) onPinEnter(pin) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.Gray
                ),
                enabled = pin.length == 4
            ) {
                Text(
                    text = "UNLOCK",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}