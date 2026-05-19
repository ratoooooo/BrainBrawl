package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.repositories.HistoricoRepository
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.EstatisticasService.ResultadoJogador

class Pontuacao2x2ViewModel(
    private val pontuacaoRepository: PontuacaoRepository = PontuacaoRepository(),
    private val historicoRepository: HistoricoRepository = HistoricoRepository(),
    private val estatisticasService: EstatisticasService = EstatisticasService()
) : ViewModel() {

    private val _uiState = MutableLiveData(Pontuacao2x2UiState())
    val uiState: LiveData<Pontuacao2x2UiState> = _uiState

    private val _evento = MutableLiveData<Pontuacao2x2Event?>()
    val evento: LiveData<Pontuacao2x2Event?> = _evento

    private var pontuacaoListener: PontuacaoRepository.ListenerHandle? = null
    private var estatisticasAtualizadas = false
    private var historicoGuardado = false
    private var recordeConsultado = false

    fun iniciar(input: Pontuacao2x2Input) {
        removerListenerPontuacao()
        estatisticasAtualizadas = false
        historicoGuardado = false
        recordeConsultado = false

        pontuacaoListener = pontuacaoRepository.escutarPontuacoes2x2(
            codigoSala = input.codigoSala,
            onPontuacoes = { resultado ->
                val podio2x2 = estatisticasService.ordenarPodio2x2(resultado.equipaA, resultado.equipaB)
                val podio = podio2x2.podio
                val resultados = resultado.equipaA + resultado.equipaB
                val completo = podio.size >= 4

                _uiState.value = Pontuacao2x2UiState(
                    podio = (0 until 4).map { index ->
                        PontuacaoJogadorUi(
                            nome = podio.getOrNull(index)?.nome.orEmpty(),
                            pontos = podio.getOrNull(index)?.pontos?.toInt()?.toString().orEmpty()
                        )
                    },
                    resultado = if (completo) {
                        estatisticasService.textoVencedor2x2(podio2x2.totalA, podio2x2.totalB)
                    } else {
                        ""
                    },
                    estado = if (completo) {
                        "Resultados finais completos"
                    } else {
                        "A aguardar todos os jogadores terminarem... ${podio.size}/4"
                    }
                )

                if (input.podeGravarPersistente() && completo && !recordeConsultado) {
                    recordeConsultado = true
                    mostrarNovoRecordSeAplicavel(input, resultados)
                }

                if (input.podeGravarPersistente() && completo && !estatisticasAtualizadas) {
                    guardarHistoricoSeNecessario(input, resultados, podio2x2.totalA, podio2x2.totalB)
                    estatisticasAtualizadas = true
                    pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
                        tipoSala = PontuacaoRepository.TipoSala.DOIS_CONTRA_DOIS,
                        codigoSala = input.codigoSala,
                        resultados = resultados,
                        modo = EstatisticasService.Modo.DOIS_CONTRA_DOIS,
                        totalPerguntas = input.totalPerguntas,
                        jogadoresParaAtualizar = input.identificadoresJogadorAtual().toSet()
                    ).addOnFailureListener {
                        estatisticasAtualizadas = false
                    }
                }
            },
            onErro = {
                _evento.value = Pontuacao2x2Event.MostrarMensagem("Erro ao carregar pontuação")
            }
        )
    }

    fun consumirEvento() {
        _evento.value = null
    }

    fun removerListenerPontuacao() {
        pontuacaoRepository.removerListener(pontuacaoListener)
        pontuacaoListener = null
    }

    override fun onCleared() {
        removerListenerPontuacao()
        super.onCleared()
    }

    private fun mostrarNovoRecordSeAplicavel(
        input: Pontuacao2x2Input,
        resultados: List<ResultadoJogador>
    ) {
        val resultadoAtual = resultados.firstOrNull { resultado ->
            input.identificadoresJogadorAtual().any { resultado.corresponde(it) }
        } ?: return

        pontuacaoRepository.salaCompetitiva(PontuacaoRepository.TipoSala.DOIS_CONTRA_DOIS, input.codigoSala)
            .addOnSuccessListener { competitiva ->
                if (!competitiva) return@addOnSuccessListener
                pontuacaoRepository.obterRecordePontuacaoJogador(input.uid)
                    .addOnSuccessListener { recordeGuardado ->
                        if (resultadoAtual.pontos > recordeGuardado) {
                            _evento.value = Pontuacao2x2Event.MostrarMensagem("NOVO RECORD!")
                        }
                    }
            }
    }

    private fun guardarHistoricoSeNecessario(
        input: Pontuacao2x2Input,
        resultados: List<ResultadoJogador>,
        totalA: Double,
        totalB: Double
    ) {
        if (historicoGuardado || !input.podeGravarPersistente() || resultados.size < 4) return
        val resultadoAtual = resultados.firstOrNull { resultado ->
            input.identificadoresJogadorAtual().any { resultado.corresponde(it) }
        } ?: return

        val equipaAtual = resultadoAtual.equipa ?: input.equipa.orEmpty()
        val empate = totalA == totalB
        val venceu = when (equipaAtual) {
            GameConstants.EQUIPA_A -> totalA > totalB
            GameConstants.EQUIPA_B -> totalB > totalA
            else -> false
        }

        historicoGuardado = true
        historicoRepository.guardarHistoricoUmaVez(
            uid = input.uid,
            historico = HistoricoJogo(
                historicoId = "${GameConstants.MODO_2X2}_${input.codigoSala}",
                modo = GameConstants.MODO_2X2,
                codigoSala = input.codigoSala,
                nomeCategoria = input.nomeCategoria,
                pontuacao = resultadoAtual.pontos,
                respostasCertas = resultadoAtual.respostasCertas.ifZero(input.totalRespostasCertas),
                totalPerguntas = input.totalPerguntas,
                venceu = venceu,
                empate = empate,
                equipa = equipaAtual,
                dataHora = System.currentTimeMillis(),
                jogadoresDaPartida = resultados.map { jogador ->
                    jogador.equipa?.let { "${jogador.nome} ($it)" } ?: jogador.nome
                }
            )
        ).addOnFailureListener {
            historicoGuardado = false
        }
    }

    private fun Int.ifZero(fallback: Int): Int {
        return if (this == 0) fallback else this
    }
}

data class Pontuacao2x2Input(
    val codigoSala: String,
    val uid: String,
    val nomeUtilizador: String,
    val nomeJogador: String,
    val nomeCategoria: String,
    val equipa: String?,
    val totalRespostasCertas: Int,
    val totalPerguntas: Int,
    val playerKey: String,
    val tipoJogador: String,
    val isGuest: Boolean
) {
    fun podeGravarPersistente(): Boolean {
        return uid.isNotBlank() && !isGuest && tipoJogador != GameConstants.TIPO_JOGADOR_GUEST
    }

    fun identificadoresJogadorAtual(): List<String> {
        return listOf(
            uid,
            playerKey,
            nomeUtilizador,
            nomeJogador,
            nomeUtilizador.ifBlank { nomeJogador }
        ).filter { it.isNotBlank() }.distinct()
    }
}

data class Pontuacao2x2UiState(
    val podio: List<PontuacaoJogadorUi> = List(4) { PontuacaoJogadorUi() },
    val resultado: String = "",
    val estado: String = "A aguardar todos os jogadores terminarem... 0/4"
)

sealed class Pontuacao2x2Event {
    data class MostrarMensagem(val mensagem: String) : Pontuacao2x2Event()
}
