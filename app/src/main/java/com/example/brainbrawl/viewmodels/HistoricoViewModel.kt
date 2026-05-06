package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.repositories.HistoricoRepository

class HistoricoViewModel(
    private val repository: HistoricoRepository = HistoricoRepository()
) : ViewModel() {
    private val _estado = MutableLiveData(HistoricoUiState(carregando = true))
    val estado: LiveData<HistoricoUiState> = _estado

    fun carregarHistorico(uid: String) {
        if (uid.isBlank()) {
            _estado.value = HistoricoUiState(mensagem = "Sem histórico para convidados.")
            return
        }
        _estado.value = HistoricoUiState(carregando = true)
        repository.carregarUltimosJogos(uid)
            .addOnSuccessListener { jogos ->
                _estado.value = HistoricoUiState(
                    jogos = jogos,
                    mensagem = if (jogos.isEmpty()) "Ainda não há jogos no histórico." else ""
                )
            }
            .addOnFailureListener {
                _estado.value = HistoricoUiState(mensagem = "Erro ao carregar histórico.")
            }
    }
}

data class HistoricoUiState(
    val carregando: Boolean = false,
    val jogos: List<HistoricoJogo> = emptyList(),
    val mensagem: String = ""
)
