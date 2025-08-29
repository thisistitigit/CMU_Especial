package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.ui.components.StarSelector
import com.example.reviewapp.viewmodels.ReviewFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewFormScreen(
    placeId: String,
    vm: ReviewFormViewModel = hiltViewModel(),
    onDone: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    // Se o teu ViewModel usar SavedStateHandle, já vai apanhar o placeId automaticamente.
    // Se precisares, podes expor vm.load(placeId) e chamar aqui via LaunchedEffect.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova avaliação") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Cancelar")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            OutlinedTextField(
                value = state.pastryName,
                onValueChange = vm::onPastryChanged,
                label = { Text("Doçaria") }
            )

            StarSelector(selected = state.stars, onChange = vm::onStarsChanged)

            OutlinedTextField(
                value = state.comment,
                onValueChange = vm::onCommentChanged,
                label = { Text("Comentário") }
            )

            Row {
                Button(onClick = vm::capturePhoto) { Text("Tirar Foto") }
                Spacer(Modifier.weight(1f))
                Button(onClick = vm::pickPhoto) { Text("Escolher da Galeria") }
            }

            Spacer(Modifier.height(16.dp))

            val canSubmit = state.canSubmit && state.rulesOk
            Button(
                enabled = canSubmit,
                onClick = {
                    vm.submit()
                    onDone()
                }
            ) { Text("Submeter") }

            if (!state.rulesOk) {
                Text(
                    "Precisas de estar a ≤ 50 m e a última avaliação tem de ter mais de 30 minutos.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
