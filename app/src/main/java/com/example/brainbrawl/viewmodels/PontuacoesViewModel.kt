package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.repositories.HistoricoRepository
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.EstatisticasService.ResultadoJogador

class PontuacoesViewModel(
    private val pontuacaoRepository: PontuacaoRepository = PontuacaoRepository(),
    private val historicoRepository: HistoricoRepository = HistoricoRepository(),
    private val estatisticasService: EstatisticasService = EstatisticasService()
) : ViewModel() {

    private val _uiState = MutableLiveData(PontuacoesUiState())
    val uiState: LiveData<PontuacoesUiState> = _uiState

    private var pontuacoesListener: PontuacaoRepository.ListenerHandle? = null
    private var estatisticasAtualizadas = false
    private var historicoGuardado = false

    fun iniciar(input: PontuacoesInput) {
        removerListenerPontuacoes()
        estatisticasAtualizadas = false
        historicoGuardado = false

        pontuacoesListener = pontuacaoRepository.escutarResultadosGrupo(
            codigoSala = input.codigoSala,
            onResultados = { resumo ->
                val jogadores = estatisticasService.ordenarPodio(resumo.jogadores)
                val mensagem = when {
                    resumo.totalJogadores == 0 -> "Sem jogadores na sala."
                    !resumo.completos -> {
                        val total = resumo.totalJogadores.coerceAtLeast(1)
                        "A aguardar resultados... ${resumo.resultadosGuardados}/$total"
                    }
                    resumo.completos && jogadores.isEmpty() -> "Sem jogadores na sala."
                    else -> ""
                }

                _uiState.value = PontuacoesUiState(
                    mensagem = mensagem,
                    podio = criarPodio(jogadores, resumo.completos)
                )

                if (input.podeGravarPersistente() && resumo.completos && !estatisticasAtualizadas) {
                    guardarHistoricoSeNecessario(input, jogadores)
                    estatisticasAtualizadas = true
                    pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
                        tipoSala = PontuacaoRepository.TipoSala.GRUPO,
                        codigoSala = input.codigoSala,
                        resultados = jogadores,
                        modo = EstatisticasService.Modo.SOLO,
                        totalPerguntas = input.totalPerguntas,
                        jogadoresParaAtualizar = input.identificadoresJogadorAtual().toSet()
                    ).addOnFailureListener {
                        estatisticasAtualizadas = false
                    }
                }
            },
            onErro = {
                _uiState.value = PontuacoesUiState(mensagem = "Erro ao carregar resultados")
            }
        )
    }

    fun removerListenerPontuacoes() {
        pontuacaoRepository.removerListener(pontuacoesListener)
        pontuacoesListener = null
    }

    override fun onCleared() {
        removerListenerPontuacoes()
        super.onCleared()
    }

    private fun criarPodio(jogadores: List<ResultadoJogador>, completos: Boolean): List<PontuacoesPodioItemUi> {
        val maxCertas = if (completos) jogadores.maxOfOrNull { it.respostasCertas } ?: 0 else 0
        val mvps = jogadores.filter { it.respostasCertas == maxCertas && maxCertas > 0 }.map { it.nome }

        return jogadores.mapIndexed { index, jogador ->
            val medalha = when (index) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${index + 1}"
            }
            val corMedalha = when (index) {
                0 -> "#D8A42F"
                1 -> "#b0b0b0"
                2 -> "#ad7e54"
                else -> "#222"
            }
            PontuacoesPodioItemUi(
                medalha = medalha,
                corMedalha = corMedalha,
                nome = jogador.nome + if (mvps.contains(jogador.nome)) " 🏆 MVP" else "",
                pontos = jogador.pontos.toInt().toString()
            )
        }
    }

    private fun guardarHistoricoSeNecessario(
        input: PontuacoesInput,
        jogadores: List<ResultadoJogador>
    ) {
        if (historicoGuardado || !input.podeGravarPersistente() || jogadores.isEmpty()) return
        val resultadoAtual = jogadores.firstOrNull { jogador ->
            input.identificadoresJogadorAtual().any { jogador.corresponde(it) }
        } ?: return

        historicoGuardado = true
        val maxPontos = jogadores.maxOfOrNull { it.pontos } ?: resultadoAtual.pontos
        val empatadosNoTopo = jogadores.count { it.pontos == maxPontos }
        val empate = resultadoAtual.pontos == maxPontos && empatadosNoTopo > 1
        val venceu = resultadoAtual.pontos == maxPontos && !empate

        historicoRepository.guardarHistoricoUmaVez(
            uid = input.uid,
            historico = HistoricoJogo(
                historicoId = input.historicoId(),
                modo = input.modoJogo.ifBlank { "grupo" },
                codigoSala = input.codigoSala,
                nomeCategoria = input.nomeCategoria,
                pontuacao = resultadoAtual.pontos,
                respostasCertas = resultadoAtual.respostasCertas,
                totalPerguntas = input.totalPerguntas,
                venceu = venceu,
                empate = empate,
                dataHora = System.currentTimeMillis(),
                jogadoresDaPartida = jogadores.map { it.nome }
            )
        ).addOnFailureListener {
            historicoGuardado = false
        }
    }
}

data class PontuacoesInput(
    val codigoSala: String,
    val uid: String,
    val nomeUtilizador: String,
    val nomeJogador: String,
    val nomeCategoria: String,
    val totalPerguntas: Int,
    val modoJogo: String,
    val tipoJogador: String = "",
    val isGuest: Boolean = false
) {
    fun podeGravarPersistente(): Boolean {
        return uid.isNotBlank() &&
            !uid.startsWith("guest_") &&
            !isGuest &&
            tipoJogador != GameConstants.TIPO_JOGADOR_GUEST
    }

    fun identificadoresJogadorAtual(): List<String> {
        return listOf(
            uid,
            nomeUtilizador,
            nomeJogador,
            nomeUtilizador.ifBlank { nomeJogador }
        ).filter { it.isNotBlank() }.distinct()
    }

    fun historicoId(): String {
        return "${modoJogo.ifBlank { FirebasePaths.SALAS }}_$codigoSala"
    }
}

data class PontuacoesUiState(
    val mensagem: String = "",
    val podio: List<PontuacoesPodioItemUi> = emptyList()
)

data class PontuacoesPodioItemUi(
    val medalha: String,
    val corMedalha: String,
    val nome: String,
    val pontos: String
)
