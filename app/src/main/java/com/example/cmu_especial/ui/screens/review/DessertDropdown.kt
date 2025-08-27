package com.example.cmu_especial.ui.screens.review

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.cmu_especial.domain.model.DessertType

/**
 * Componente dropdown para escolher o tipo de doçaria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxSample(
    selected: DessertType,
    onSelected: (DessertType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val allTypes = DessertType.entries

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.titlecase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Doçaria") },
            modifier = modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            allTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.titlecase() }) },
                    onClick = { onSelected(type); expanded = false }
                )
            }
        }
    }
}
