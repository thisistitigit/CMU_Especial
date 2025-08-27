package com.example.cmu_especial.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cmu_especial.domain.usecase.GetEstablishmentDetails
import com.example.cmu_especial.domain.usecase.GetRecentReviews
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// ui/screens/details/DetailsViewModel.kt
@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getDetails: GetEstablishmentDetails,
    private val getRecent: GetRecentReviews
): ViewModel() {
    val ui = MutableStateFlow(DetailsUiState())
    fun load(id: String) = viewModelScope.launch {
        val est = getDetails(id)
        val last10 = getRecent(id, 10) // últimas 10 avaliações no ecrã de detalhes.
        ui.value = DetailsUiState(establishment = est, recent = last10)
    }
}
