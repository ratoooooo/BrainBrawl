package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.models.RankingJogador
import com.example.brainbrawl.models.RankingTipo
import com.example.brainbrawl.repositories.RankingRepository

class RankingViewModel(
    private val rankingRepository: RankingRepository = RankingRepository()
) : ViewModel() {

    private var rankingTipoAtual: RankingTipo = RankingTipo.GLOBAL
    private val _estado = MutableLiveData<RankingUiState>()
    val estado: LiveData<RankingUiState> = _estado

    fun carregarRanking(tipo: RankingTipo = rankingTipoAtual) {
        rankingTipoAtual = tipo
        _estado.value = RankingUiState.Loading(tipo)

        rankingRepository.carregarRankingPorTipo(tipo)
            .addOnSuccessListener { jogadores ->
                _estado.value = if (jogadores.isEmpty()) {
                    RankingUiState.Empty(tipo)
                } else {
                    RankingUiState.Content(tipo, jogadores)
                }
            }
            .addOnFailureListener {
                _estado.value = RankingUiState.Error(tipo)
            }
    }
}

sealed class RankingUiState {
    data class Loading(val tipo: RankingTipo) : RankingUiState()
    data class Empty(val tipo: RankingTipo) : RankingUiState()
    data class Error(val tipo: RankingTipo) : RankingUiState()
    data class Content(val tipo: RankingTipo, val jogadores: List<RankingJogador>) : RankingUiState()
}
