package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.repositories.JogadorRepository

class PerfilAmigoViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository(),
    private val amigosRepository: AmigosRepository = AmigosRepository()
) : ViewModel() {

    private val _perfil = MutableLiveData<PerfilAmigoUiState>()
    val perfil: LiveData<PerfilAmigoUiState> = _perfil

    private val _evento = MutableLiveData<PerfilAmigoEvent?>()
    val evento: LiveData<PerfilAmigoEvent?> = _evento

    fun carregarPerfil(nomeAmigo: String) {
        jogadorRepository.obterPerfil(nomeAmigo).addOnSuccessListener { perfil ->
            _perfil.value = if (perfil != null) {
                PerfilAmigoUiState(
                    nome = nomeAmigo,
                    avatar = perfil.avatar,
                    pontuacao = perfil.estatisticas.pontuacao,
                    taxaAcertos = perfil.estatisticas.taxaAcertos,
                    totalJogos = perfil.estatisticas.totalJogos,
                    totalVitorias = perfil.estatisticas.totalVitorias,
                    totalRespostasCertas = perfil.estatisticas.totalRespostasCertas,
                    perfilExiste = true
                )
            } else {
                PerfilAmigoUiState.perfilDesconhecido(nomeAmigo)
            }
        }
    }

    fun removerAmigo(nomeUtilizador: String, nomeAmigo: String) {
        amigosRepository.removerAmigo(nomeUtilizador, nomeAmigo)
            .addOnSuccessListener {
                _evento.value = PerfilAmigoEvent.AmigoRemovido
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }
}

data class PerfilAmigoUiState(
    val nome: String,
    val avatar: String,
    val pontuacao: Double,
    val taxaAcertos: Double,
    val totalJogos: Int,
    val totalVitorias: Int,
    val totalRespostasCertas: Int,
    val perfilExiste: Boolean
) {
    companion object {
        private const val AVATAR_PADRAO = "avatar_1_playstore"

        fun perfilDesconhecido(nomeAmigo: String): PerfilAmigoUiState {
            return PerfilAmigoUiState(
                nome = nomeAmigo,
                avatar = AVATAR_PADRAO,
                pontuacao = 0.0,
                taxaAcertos = 0.0,
                totalJogos = 0,
                totalVitorias = 0,
                totalRespostasCertas = 0,
                perfilExiste = false
            )
        }
    }
}

sealed class PerfilAmigoEvent {
    data object AmigoRemovido : PerfilAmigoEvent()
}
