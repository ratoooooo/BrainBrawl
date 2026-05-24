package com.example.brainbrawl.viewmodels

import android.util.Log
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

        if (input.modoSolo) {
            mostrarResultadoSolo(input)
            return
        }

        pontuacoesListener = pontuacaoRepository.escutarResultadosGrupo(
            codigoSala = input.codigoSala,
            onResultados = { resumo ->
                val jogadores = if (input.modoJogo == GameConstants.MODO_ELIMINATORIAS) {
                    estatisticasService.ordenarPodioGrupoEliminatorias(resumo.jogadores)
                } else {
                    estatisticasService.ordenarPodio(resumo.jogadores)
                }
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

                if (resumo.completos && !historicoGuardado) {
                    guardarHistoricoSeNecessario(input, jogadores)
                }

                if (input.podeGravarPersistente() && resumo.completos && !estatisticasAtualizadas) {
                    if (!input.categoriaCompetitiva) {
                        estatisticasAtualizadas = true
                    } else {
                        estatisticasAtualizadas = true
                        pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
                            tipoSala = PontuacaoRepository.TipoSala.GRUPO,
                            codigoSala = input.codigoSala,
                            resultados = jogadores,
                            modo = EstatisticasService.Modo.GRUPO,
                            totalPerguntas = input.totalPerguntas,
                            jogadoresParaAtualizar = input.identificadoresJogadorAtual().toSet()
                        ).addOnFailureListener {
                            estatisticasAtualizadas = false
                        }
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
                pontos = jogador.pontos.toInt().toString(),
                avatar = jogador.avatar
            )
        }
    }

    private fun mostrarResultadoSolo(input: PontuacoesInput) {
        val resultado = ResultadoJogador(
            nome = input.nomeUtilizador.ifBlank { input.nomeJogador.ifBlank { "Jogador" } },
            pontos = input.totalPontos,
            respostasCertas = input.totalRespostasCertas,
            uid = input.uid,
            nomeUtilizador = input.nomeUtilizador,
            nomeJogador = input.nomeJogador,
            avatar = input.avatar
        )
        _uiState.value = PontuacoesUiState(
            mensagem = if (input.categoriaCompetitiva) "" else "Categoria não competitiva: não conta para ranking, recordes ou vitórias.",
            podio = criarPodio(listOf(resultado), completos = true)
        )

        if (!input.podeGravarHistorico() || historicoGuardado) {
            if (!historicoGuardado) {
                Log.d(
                    HISTORY_DEBUG_TAG,
                    "skip solo history uid=${input.uid} isGuest=${input.isGuest} tipo=${input.tipoJogador} " +
                        "mode=${input.modoJogo} category=${input.nomeCategoria}"
                )
            }
            return
        }
        historicoGuardado = true
        historicoRepository.guardarHistoricoUmaVez(
            uid = input.uid,
            historico = HistoricoJogo(
                historicoId = input.historicoId(),
                modo = input.modoJogo.ifBlank { GameConstants.MODO_CLASSICO },
                codigoSala = "",
                nomeCategoria = input.nomeCategoria,
                pontuacao = input.totalPontos,
                respostasCertas = input.totalRespostasCertas,
                totalPerguntas = input.totalPerguntas,
                venceu = true,
                empate = false,
                competitivo = input.categoriaCompetitiva,
                dataHora = System.currentTimeMillis(),
                jogadoresDaPartida = listOf(resultado.nome)
            )
        ).addOnFailureListener {
            historicoGuardado = false
        }

        if (input.podeGravarPersistente() && input.categoriaCompetitiva && !estatisticasAtualizadas) {
            estatisticasAtualizadas = true
            pontuacaoRepository.atualizarEstatisticasSolo(
                resultado = resultado,
                venceu = true,
                totalPerguntas = input.totalPerguntas
            ).addOnFailureListener {
                estatisticasAtualizadas = false
            }
        }
    }

    private fun guardarHistoricoSeNecessario(
        input: PontuacoesInput,
        jogadores: List<ResultadoJogador>
    ) {
        if (historicoGuardado) return
        if (!input.podeGravarHistorico()) {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip group history: invalid persistent identity uid=${input.uid} isGuest=${input.isGuest} " +
                    "tipo=${input.tipoJogador} mode=${input.modoJogo} room=${input.codigoSala}"
            )
            return
        }
        if (jogadores.isEmpty()) {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip group history: no playable results uid=${input.uid} mode=${input.modoJogo} room=${input.codigoSala}"
            )
            return
        }
        val resultadoAtual = jogadores.firstOrNull { jogador ->
            input.identificadoresJogadorAtual().any { jogador.corresponde(it) }
        } ?: run {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip group history: current player not in results uid=${input.uid} player=${input.nomeJogador} " +
                    "user=${input.nomeUtilizador} mode=${input.modoJogo} room=${input.codigoSala}"
            )
            return
        }

        historicoGuardado = true
        Log.d(
            HISTORY_DEBUG_TAG,
            "saving group history uid=${input.uid} mode=${input.modoJogo} category=${input.nomeCategoria} " +
                "competitivo=${input.categoriaCompetitiva} room=${input.codigoSala}"
        )
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
                competitivo = input.categoriaCompetitiva,
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
    val totalPontos: Double = 0.0,
    val totalRespostasCertas: Int = 0,
    val modoSolo: Boolean = false,
    val partidaId: String = "",
    val categoriaCompetitiva: Boolean = true,
    val tipoJogador: String = "",
    val isGuest: Boolean = false,
    val isHostOnly: Boolean = false,
    val avatar: String = ""
) {
    fun podeGravarPersistente(): Boolean {
        return uid.isNotBlank() &&
            !uid.startsWith("guest_") &&
            !isGuest &&
            tipoJogador != GameConstants.TIPO_JOGADOR_GUEST &&
            !isHostOnly
    }

    fun podeGravarHistorico(): Boolean {
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
        return if (modoSolo) {
            partidaId.ifBlank { "solo_${modoJogo}_${nomeCategoria}_${System.currentTimeMillis()}" }
        } else {
            "${modoJogo.ifBlank { FirebasePaths.SALAS }}_$codigoSala"
        }
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
    val pontos: String,
    val avatar: String = ""
)

private const val HISTORY_DEBUG_TAG = "HISTORY_DEBUG"
