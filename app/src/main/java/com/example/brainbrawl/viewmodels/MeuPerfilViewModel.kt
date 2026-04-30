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

    fun carregarPerfil(uid: String, nomeUtilizador: String) {
        val identificador = uid.ifBlank { nomeUtilizador }
        jogadorRepository.obterPerfil(identificador).addOnSuccessListener { perfil ->
            if (perfil != null) {
                _perfil.value = MeuPerfilUiState(
                    nome = perfil.nomeUtilizador.ifBlank { nomeUtilizador },
                    avatar = perfil.avatar,
                    pontuacao = perfil.estatisticas.pontuacao,
                    taxaAcertos = perfil.estatisticas.taxaAcertos,
                    totalJogos = perfil.estatisticas.totalJogos,
                    totalVitorias = perfil.estatisticas.totalVitorias,
                    totalRespostasCertas = perfil.estatisticas.totalRespostasCertas,
                    nivel = perfil.estatisticas.nivel,
                    xpTotal = perfil.estatisticas.xpTotal,
                    xpNoNivelAtual = perfil.estatisticas.xpNoNivelAtual,
                    xpNecessarioProximoNivel = perfil.estatisticas.xpNecessarioProximoNivel
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
    val totalRespostasCertas: Int,
    val nivel: Int,
    val xpTotal: Int,
    val xpNoNivelAtual: Int,
    val xpNecessarioProximoNivel: Int
)
