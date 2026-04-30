package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.models.RankingJogador
import com.example.brainbrawl.repositories.RankingRepository

class RankingViewModel(
    private val rankingRepository: RankingRepository = RankingRepository()
) : ViewModel() {

    private val _estado = MutableLiveData<RankingUiState>()
    val estado: LiveData<RankingUiState> = _estado

    fun carregarRanking() {
        _estado.value = RankingUiState.Loading

        rankingRepository.carregarRankingGlobal()
            .addOnSuccessListener { jogadores ->
                _estado.value = if (jogadores.isEmpty()) {
                    RankingUiState.Empty
                } else {
                    RankingUiState.Content(jogadores)
                }
            }
            .addOnFailureListener {
                _estado.value = RankingUiState.Error
            }
    }
}

sealed class RankingUiState {
    data object Loading : RankingUiState()
    data object Empty : RankingUiState()
    data object Error : RankingUiState()
    data class Content(val jogadores: List<RankingJogador>) : RankingUiState()
}
