package com.example.brainbrawl.viewmodels

import android.util.Log
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
    private var novaSalaDesforraListener: PontuacaoRepository.ListenerHandle? = null
    private var desforraListener: PontuacaoRepository.ListenerHandle? = null
    private var inputAtual: Pontuacao2x2Input? = null
    private var chaveJogadorAtual: String = ""
    private var aCriarDesforra = false
    private var navegacaoEmitida = false

    fun iniciar(input: Pontuacao2x2Input) {
        removerListenerPontuacao()
        removerListenersDesforra()
        estatisticasAtualizadas = false
        historicoGuardado = false
        recordeConsultado = false
        inputAtual = input
        chaveJogadorAtual = input.playerKey
        aCriarDesforra = false
        navegacaoEmitida = false
        observarNovaSalaDesforra(input.codigoSala)

        pontuacaoListener = pontuacaoRepository.escutarPontuacoes2x2(
            codigoSala = input.codigoSala,
            onPontuacoes = { resultado ->
                val podio2x2 = estatisticasService.ordenarPodio2x2(resultado.equipaA, resultado.equipaB)
                val podio = podio2x2.podio
                val resultados = resultado.equipaA + resultado.equipaB
                val completo = podio.size >= 4
                val resultadoAtual = resultados.firstOrNull { jogador ->
                    input.identificadoresJogadorAtual().any { jogador.corresponde(it) }
                }
                val chaveResolvida = resultadoAtual?.chave.orEmpty()
                    .ifBlank { input.playerKey }
                    .ifBlank { input.uid }
                if (chaveResolvida.isNotBlank() && chaveResolvida != chaveJogadorAtual) {
                    chaveJogadorAtual = chaveResolvida
                    observarDesforras(input.codigoSala)
                } else if (chaveJogadorAtual.isNotBlank() && desforraListener == null) {
                    observarDesforras(input.codigoSala)
                }
                val equipas = ordenarEquipasUi(
                    equipaA = resultado.equipaA.toTeamUi(GameConstants.EQUIPA_A, podio2x2.totalA),
                    equipaB = resultado.equipaB.toTeamUi(GameConstants.EQUIPA_B, podio2x2.totalB),
                    totalA = podio2x2.totalA,
                    totalB = podio2x2.totalB,
                    completo = completo
                )

                _uiState.value = Pontuacao2x2UiState(
                    podio = (0 until 4).map { index ->
                        PontuacaoJogadorUi(
                            nome = podio.getOrNull(index)?.nome.orEmpty(),
                            pontos = podio.getOrNull(index)?.pontos?.toInt()?.toString().orEmpty(),
                            avatar = podio.getOrNull(index)?.avatar.orEmpty()
                        )
                    },
                    equipas = equipas,
                    resultado = if (completo) {
                        "Batalha concluída"
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

                if (completo && !historicoGuardado) {
                    guardarHistoricoSeNecessario(input, resultados, podio2x2.totalA, podio2x2.totalB)
                }

                if (input.podeGravarPersistente() && completo && !estatisticasAtualizadas) {
                    if (!input.categoriaCompetitiva) {
                        estatisticasAtualizadas = true
                    } else {
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
                }
            },
            onErro = {
                _evento.value = Pontuacao2x2Event.MostrarMensagem("Erro ao carregar pontuação")
            }
        )
    }

    fun pedirDesforra() {
        val input = inputAtual ?: run {
            _evento.value = Pontuacao2x2Event.MostrarMensagem("Ainda a carregar dados da partida.")
            return
        }
        val chave = chaveJogadorAtual.ifBlank { input.playerKey }.ifBlank { input.uid }
        if (chave.isBlank()) {
            _evento.value = Pontuacao2x2Event.MostrarMensagem("Não foi possível identificar o jogador para a desforra.")
            return
        }
        Log.d(
            REMATCH_DEBUG_TAG,
            "2x2 rematch requested room=${input.codigoSala} key=$chave category=${input.nomeCategoria} " +
                "flow=private_rematch target=SalaDeEspera2x2Activity"
        )
        _evento.value = Pontuacao2x2Event.MostrarMensagem("A aguardar restantes jogadores...")
        pontuacaoRepository.marcarDesforra2x2(input.codigoSala, chave)
            .addOnFailureListener {
                _evento.value = Pontuacao2x2Event.MostrarMensagem("Erro ao pedir desforra 2x2.")
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }

    fun removerListenerPontuacao() {
        pontuacaoRepository.removerListener(pontuacaoListener)
        pontuacaoListener = null
    }

    private fun removerListenersDesforra() {
        pontuacaoRepository.removerListener(novaSalaDesforraListener)
        novaSalaDesforraListener = null
        pontuacaoRepository.removerListener(desforraListener)
        desforraListener = null
    }

    override fun onCleared() {
        removerListenerPontuacao()
        removerListenersDesforra()
        super.onCleared()
    }

    private fun observarNovaSalaDesforra(codigoSala: String) {
        pontuacaoRepository.removerListener(novaSalaDesforraListener)
        novaSalaDesforraListener = pontuacaoRepository.escutarNovaSalaDesforra2x2(
            codigoSala = codigoSala,
            onNovaSala = { novaSala ->
                emitirAbrirSala(novaSala)
            },
            onErro = {
                _evento.value = Pontuacao2x2Event.MostrarMensagem("Erro ao observar desforra 2x2.")
            }
        )
    }

    private fun observarDesforras(codigoSala: String) {
        val chave = chaveJogadorAtual.takeIf { it.isNotBlank() } ?: return
        pontuacaoRepository.removerListener(desforraListener)
        desforraListener = pontuacaoRepository.escutarDesforra2x2(
            codigoSala = codigoSala,
            chaveJogadorAtual = chave,
            onTodosAceitaram = {
                criarDesforra2x2()
            },
            onAguardar = { aceites, total ->
                _evento.value = Pontuacao2x2Event.MostrarMensagem("Desforra: $aceites/$total jogadores prontos.")
            },
            onErro = {
                _evento.value = Pontuacao2x2Event.MostrarMensagem("Erro ao observar pedidos de desforra 2x2.")
            }
        )
    }

    private fun criarDesforra2x2() {
        val input = inputAtual ?: return
        if (aCriarDesforra || navegacaoEmitida) return
        aCriarDesforra = true
        val origemCategoria = if (input.categoriaCompetitiva) {
            GameConstants.ORIGEM_CATEGORIA_OFICIAL
        } else {
            GameConstants.ORIGEM_CATEGORIA_PUBLICA
        }
        Log.d(
            REMATCH_DEBUG_TAG,
            "2x2 rematch create oldRoom=${input.codigoSala} category=${input.nomeCategoria} " +
                "categoryOrigin=$origemCategoria target=SalaDeEspera2x2Activity"
        )
        pontuacaoRepository.criarOuObterSalaDesforra2x2(input.codigoSala, input.nomeCategoria, origemCategoria)
            .addOnSuccessListener { novaSala ->
                aCriarDesforra = false
                emitirAbrirSala(novaSala)
            }
            .addOnFailureListener {
                aCriarDesforra = false
                _evento.value = Pontuacao2x2Event.MostrarMensagem("Erro ao criar desforra 2x2.")
            }
    }

    private fun emitirAbrirSala(codigoNovaSala: String) {
        if (navegacaoEmitida) return
        navegacaoEmitida = true
        _evento.value = Pontuacao2x2Event.AbrirNovaSalaDesforra(codigoNovaSala)
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
        if (historicoGuardado) return
        if (!input.podeGravarHistorico()) {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip 2x2 history: invalid persistent identity uid=${input.uid} playerKey=${input.playerKey} " +
                    "isGuest=${input.isGuest} tipo=${input.tipoJogador} room=${input.codigoSala}"
            )
            return
        }
        if (resultados.size < 4) {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip 2x2 history: waiting for complete results uid=${input.uid} room=${input.codigoSala} players=${resultados.size}"
            )
            return
        }
        val resultadoAtual = resultados.firstOrNull { resultado ->
            input.identificadoresJogadorAtual().any { resultado.corresponde(it) }
        } ?: run {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip 2x2 history: current player not in results uid=${input.uid} playerKey=${input.playerKey} room=${input.codigoSala}"
            )
            return
        }

        val equipaAtual = resultadoAtual.equipa ?: input.equipa.orEmpty()
        val empate = totalA == totalB
        val venceu = when (equipaAtual) {
            GameConstants.EQUIPA_A -> totalA > totalB
            GameConstants.EQUIPA_B -> totalB > totalA
            else -> false
        }

        historicoGuardado = true
        Log.d(
            HISTORY_DEBUG_TAG,
            "saving 2x2 history uid=${input.uid} playerKey=${input.playerKey} category=${input.nomeCategoria} " +
                "competitivo=${input.categoriaCompetitiva} room=${input.codigoSala}"
        )
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
                competitivo = input.categoriaCompetitiva,
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

    private fun ordenarEquipasUi(
        equipaA: Pontuacao2x2EquipaUi,
        equipaB: Pontuacao2x2EquipaUi,
        totalA: Double,
        totalB: Double,
        completo: Boolean
    ): List<Pontuacao2x2EquipaUi> {
        if (!completo) {
            return listOf(
                equipaA.copy(resultado = Pontuacao2x2ResultadoUi.AGUARDANDO),
                equipaB.copy(resultado = Pontuacao2x2ResultadoUi.AGUARDANDO)
            )
        }

        return when {
            totalA > totalB -> listOf(
                equipaA.copy(resultado = Pontuacao2x2ResultadoUi.VITORIA),
                equipaB.copy(resultado = Pontuacao2x2ResultadoUi.DERROTA)
            )
            totalB > totalA -> listOf(
                equipaB.copy(resultado = Pontuacao2x2ResultadoUi.VITORIA),
                equipaA.copy(resultado = Pontuacao2x2ResultadoUi.DERROTA)
            )
            else -> listOf(
                equipaA.copy(resultado = Pontuacao2x2ResultadoUi.EMPATE),
                equipaB.copy(resultado = Pontuacao2x2ResultadoUi.EMPATE)
            )
        }
    }

    private fun List<ResultadoJogador>.toTeamUi(
        equipa: String,
        total: Double
    ): Pontuacao2x2EquipaUi {
        return Pontuacao2x2EquipaUi(
            equipa = equipa,
            jogadores = sortedByDescending { it.pontos }
                .take(2)
                .map { jogador ->
                    PontuacaoJogadorUi(
                        nome = jogador.nome,
                        pontos = jogador.pontos.toInt().toString(),
                        avatar = jogador.avatar
                    )
                },
            pontos = total.toInt().toString()
        )
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
    val isGuest: Boolean,
    val categoriaCompetitiva: Boolean = true
) {
    fun podeGravarPersistente(): Boolean {
        return podeGravarHistorico()
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
            playerKey,
            nomeUtilizador,
            nomeJogador,
            nomeUtilizador.ifBlank { nomeJogador }
        ).filter { it.isNotBlank() }.distinct()
    }
}

data class Pontuacao2x2UiState(
    val podio: List<PontuacaoJogadorUi> = List(4) { PontuacaoJogadorUi() },
    val equipas: List<Pontuacao2x2EquipaUi> = listOf(
        Pontuacao2x2EquipaUi(equipa = GameConstants.EQUIPA_A),
        Pontuacao2x2EquipaUi(equipa = GameConstants.EQUIPA_B)
    ),
    val resultado: String = "",
    val estado: String = "A aguardar todos os jogadores terminarem... 0/4"
)

data class Pontuacao2x2EquipaUi(
    val equipa: String = "",
    val jogadores: List<PontuacaoJogadorUi> = emptyList(),
    val pontos: String = "",
    val resultado: Pontuacao2x2ResultadoUi = Pontuacao2x2ResultadoUi.AGUARDANDO
)

enum class Pontuacao2x2ResultadoUi {
    VITORIA,
    DERROTA,
    EMPATE,
    AGUARDANDO
}

sealed class Pontuacao2x2Event {
    data class MostrarMensagem(val mensagem: String) : Pontuacao2x2Event()
    data class AbrirNovaSalaDesforra(val codigoSala: String) : Pontuacao2x2Event()
}

private const val HISTORY_DEBUG_TAG = "HISTORY_DEBUG"
private const val REMATCH_DEBUG_TAG = "REMATCH_FLOW_DEBUG"
