package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.models.Badge
import com.example.brainbrawl.models.BadgeProgress
import com.example.brainbrawl.models.UtilizadorSocial
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.services.BadgesService

class PerfilAmigoViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository(),
    private val amigosRepository: AmigosRepository = AmigosRepository(),
    private val badgesService: BadgesService = BadgesService()
) : ViewModel() {

    private val _perfil = MutableLiveData<PerfilAmigoUiState>()
    val perfil: LiveData<PerfilAmigoUiState> = _perfil

    private val _evento = MutableLiveData<PerfilAmigoEvent?>()
    val evento: LiveData<PerfilAmigoEvent?> = _evento

    fun carregarPerfil(identificadorAmigo: String, nomeAmigoFallback: String) {
        amigosRepository.resolverUtilizador(identificadorAmigo, nomeAmigoFallback)
            .addOnSuccessListener { utilizador ->
                if (utilizador == null) {
                    _perfil.value = PerfilAmigoUiState.perfilDesconhecido(nomeAmigoFallback)
                    return@addOnSuccessListener
                }

                jogadorRepository.obterPerfil(utilizador.chavePrimaria)
                    .addOnSuccessListener { perfil ->
                        _perfil.value = if (perfil != null) {
                            val progress = BadgeProgress(
                                totalRespostasCertas = perfil.estatisticas.totalRespostasCertas,
                                totalPartidasJogadas = perfil.estatisticas.totalJogos,
                                totalVitorias = perfil.estatisticas.totalVitorias
                            )
                            PerfilAmigoUiState(
                                utilizador = utilizador,
                                nome = perfil.nomeUtilizador.ifBlank { utilizador.nomeDisplay },
                                avatar = perfil.avatar,
                                pontuacao = perfil.estatisticas.pontuacao,
                                taxaAcertos = perfil.estatisticas.taxaAcertos,
                                totalJogos = perfil.estatisticas.totalJogos,
                                totalVitorias = perfil.estatisticas.totalVitorias,
                                totalRespostasCertas = perfil.estatisticas.totalRespostasCertas,
                                badges = badgesService.calcularBadges(
                                    progress = progress,
                                    badgesPersistidas = emptySet(),
                                    permitirDesbloqueioLocal = true
                                ),
                                perfilExiste = true
                            )
                        } else {
                            PerfilAmigoUiState.perfilDesconhecido(utilizador.nomeDisplay, utilizador)
                        }
                    }
                    .addOnFailureListener {
                        _perfil.value = PerfilAmigoUiState.perfilDesconhecido(utilizador.nomeDisplay, utilizador)
                    }
            }
            .addOnFailureListener {
                _perfil.value = PerfilAmigoUiState.perfilDesconhecido(nomeAmigoFallback)
            }
    }

    fun removerAmigo(uidUtilizador: String, nomeUtilizador: String, amigo: UtilizadorSocial) {
        val identificador = uidUtilizador.ifBlank { nomeUtilizador }
        amigosRepository.resolverUtilizador(identificador, nomeUtilizador)
            .addOnSuccessListener { utilizador ->
                if (utilizador == null) return@addOnSuccessListener
                amigosRepository.removerAmigo(utilizador, amigo)
                    .addOnSuccessListener {
                        _evento.value = PerfilAmigoEvent.AmigoRemovido
                    }
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }
}

data class PerfilAmigoUiState(
    val utilizador: UtilizadorSocial,
    val nome: String,
    val avatar: String,
    val pontuacao: Double,
    val taxaAcertos: Double,
    val totalJogos: Int,
    val totalVitorias: Int,
    val totalRespostasCertas: Int,
    val badges: List<Badge>,
    val perfilExiste: Boolean
) {
    companion object {
        private const val AVATAR_PADRAO = "avatar_1_playstore"

        fun perfilDesconhecido(nomeAmigo: String, utilizador: UtilizadorSocial? = null): PerfilAmigoUiState {
            val utilizadorFallback = utilizador ?: UtilizadorSocial(
                nomeUtilizador = nomeAmigo,
                chavePerfil = nomeAmigo,
                chaveOrigem = nomeAmigo
            )
            return PerfilAmigoUiState(
                utilizador = utilizadorFallback,
                nome = nomeAmigo,
                avatar = AVATAR_PADRAO,
                pontuacao = 0.0,
                taxaAcertos = 0.0,
                totalJogos = 0,
                totalVitorias = 0,
                totalRespostasCertas = 0,
                badges = emptyList(),
                perfilExiste = false
            )
        }
    }
}

sealed class PerfilAmigoEvent {
    data object AmigoRemovido : PerfilAmigoEvent()
}
