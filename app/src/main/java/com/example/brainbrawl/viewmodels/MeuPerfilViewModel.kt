package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.JogadorRepository

class MeuPerfilViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository()
) : ViewModel() {

    private val _perfil = MutableLiveData<MeuPerfilUiState>()
    val perfil: LiveData<MeuPerfilUiState> = _perfil

    fun carregarPerfil(nomeUtilizador: String) {
        jogadorRepository.obterPerfil(nomeUtilizador).addOnSuccessListener { perfil ->
            if (perfil != null) {
                _perfil.value = MeuPerfilUiState(
                    nome = nomeUtilizador,
                    avatar = perfil.avatar,
                    pontuacao = perfil.estatisticas.pontuacao,
                    taxaAcertos = perfil.estatisticas.taxaAcertos,
                    totalJogos = perfil.estatisticas.totalJogos,
                    totalVitorias = perfil.estatisticas.totalVitorias,
                    totalRespostasCertas = perfil.estatisticas.totalRespostasCertas
                )
            }
        }
    }
}

data class MeuPerfilUiState(
    val nome: String,
    val avatar: String,
    val pontuacao: Double,
    val taxaAcertos: Double,
    val totalJogos: Int,
    val totalVitorias: Int,
    val totalRespostasCertas: Int
)
