package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.models.Badge
import com.example.brainbrawl.models.BadgeProgress
import com.example.brainbrawl.repositories.BadgesRepository
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.services.BadgesService

class MeuPerfilViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository(),
    private val badgesRepository: BadgesRepository = BadgesRepository(),
    private val badgesService: BadgesService = BadgesService(),
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _perfil = MutableLiveData<MeuPerfilUiState>()
    val perfil: LiveData<MeuPerfilUiState> = _perfil

    fun carregarPerfil(uid: String, nomeUtilizador: String) {
        val authUid = authService.utilizadorAtual()?.uid.orEmpty()
        val identificador = uid.ifBlank { authUid.ifBlank { nomeUtilizador } }
        jogadorRepository.obterPerfil(identificador).addOnSuccessListener { perfil ->
            if (perfil != null) {
                val podePersistirConquistas = authUid.isNotBlank() &&
                    perfil.uid == authUid &&
                    !perfil.uid.startsWith(PREFIXO_GUEST)
                val progress = BadgeProgress(
                    totalRespostasCertas = perfil.estatisticas.totalRespostasCertas,
                    totalPartidasJogadas = perfil.estatisticas.totalJogos,
                    totalVitorias = perfil.estatisticas.totalVitorias,
                    xpTotal = perfil.estatisticas.xpTotal,
                    creditos = perfil.estatisticas.pontuacao.toInt()
                )

                val estadoInicial = criarUiState(
                    perfil = perfil,
                    nomeFallback = nomeUtilizador,
                    progress = progress,
                    badgesPersistidas = emptySet(),
                    podePersistirConquistas = podePersistirConquistas
                )
                _perfil.value = estadoInicial

                badgesRepository.obterConquistas(authUid, isGuest = !podePersistirConquistas)
                    .addOnSuccessListener { badgesPersistidas ->
                        val estadoAtualizado = criarUiState(
                            perfil = perfil,
                            nomeFallback = nomeUtilizador,
                            progress = progress,
                            badgesPersistidas = badgesPersistidas,
                            podePersistirConquistas = podePersistirConquistas
                        )
                        _perfil.value = estadoAtualizado

                        val badgesNovas = badgesService.badgesParaGravar(
                            badges = estadoAtualizado.badges,
                            badgesPersistidas = badgesPersistidas
                        )
                        badgesRepository.gravarConquistasDesbloqueadas(
                            uid = authUid,
                            isGuest = !podePersistirConquistas,
                            badges = badgesNovas
                        )
                    }
            }
        }
    }

    private fun criarUiState(
        perfil: JogadorRepository.PerfilJogador,
        nomeFallback: String,
        progress: BadgeProgress,
        badgesPersistidas: Set<String>,
        podePersistirConquistas: Boolean
    ): MeuPerfilUiState {
        val totalJogos = perfil.estatisticas.totalJogos
        val totalVitorias = perfil.estatisticas.totalVitorias
        val taxaVitoria = if (totalJogos > 0) {
            (totalVitorias.toDouble() / totalJogos.toDouble()) * 100.0
        } else {
            0.0
        }

        return MeuPerfilUiState(
            nome = perfil.nomeUtilizador.ifBlank { nomeFallback },
            avatar = perfil.avatar,
            pontuacao = perfil.estatisticas.pontuacao,
            recordePontuacao = perfil.estatisticas.recordePontuacao,
            taxaAcertos = perfil.estatisticas.taxaAcertos,
            taxaVitoria = taxaVitoria,
            totalJogos = totalJogos,
            totalVitorias = totalVitorias,
            totalDerrotas = (totalJogos - totalVitorias).coerceAtLeast(0),
            totalRespostasCertas = perfil.estatisticas.totalRespostasCertas,
            nivel = perfil.estatisticas.nivel,
            xpTotal = perfil.estatisticas.xpTotal,
            xpNoNivelAtual = perfil.estatisticas.xpNoNivelAtual,
            xpNecessarioProximoNivel = perfil.estatisticas.xpNecessarioProximoNivel,
            badges = badgesService.calcularBadges(
                progress = progress,
                badgesPersistidas = badgesPersistidas,
                permitirDesbloqueioLocal = podePersistirConquistas
            ),
            conquistasPersistentesAtivas = podePersistirConquistas
        )
    }

    private companion object {
        const val PREFIXO_GUEST = "guest_"
    }
}

data class MeuPerfilUiState(
    val nome: String,
    val avatar: String,
    val pontuacao: Double,
    val recordePontuacao: Double,
    val taxaAcertos: Double,
    val taxaVitoria: Double,
    val totalJogos: Int,
    val totalVitorias: Int,
    val totalDerrotas: Int,
    val totalRespostasCertas: Int,
    val nivel: Int,
    val xpTotal: Int,
    val xpNoNivelAtual: Int,
    val xpNecessarioProximoNivel: Int,
    val badges: List<Badge>,
    val conquistasPersistentesAtivas: Boolean
)
