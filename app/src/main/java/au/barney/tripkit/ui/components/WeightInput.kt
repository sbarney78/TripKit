package au.barney.tripkit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun WeightInput(
    weightInput: String,
    onWeightInputChange: (String) -> Unit,
    weightUnit: String,
    onWeightUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Weight (Each)"
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        OutlinedTextField(
            value = weightInput,
            onValueChange = onWeightInputChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        var unitExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { unitExpanded = true }) {
                Text(weightUnit)
            }
            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("g") },
                    onClick = {
                        onWeightUnitChange("g")
                        unitExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("kg") },
                    onClick = {
                        onWeightUnitChange("kg")
                        unitExpanded = false
                    }
                )
            }
        }
    }
}

fun convertToGrams(input: String, unit: String): Int {
    return try {
        val value = input.toDouble()
        if (unit == "kg") (value * 1000).toInt() else value.toInt()
    } catch (e: Exception) {
        0
    }
}

fun formatWeightForInput(grams: Int): Pair<String, String> {
    return if (grams >= 1000) {
        val kg = grams / 1000.0
        if (kg == kg.toInt().toDouble()) {
            Pair(kg.toInt().toString(), "kg")
        } else {
            Pair(kg.toString(), "kg")
        }
    } else {
        Pair(grams.toString(), "g")
    }
}
