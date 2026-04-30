package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.models.Pergunta
import com.example.brainbrawl.repositories.JogoRepository
import com.example.brainbrawl.services.GameService
import com.example.brainbrawl.services.ScoreService

class JogoViewModel(
    private val jogoRepository: JogoRepository = JogoRepository(),
    private val gameService: GameService = GameService(),
    private val scoreService: ScoreService = ScoreService()
) : ViewModel() {

    private val _sala = MutableLiveData<JogoSalaUiState>()
    val sala: LiveData<JogoSalaUiState> = _sala

    private val _pergunta = MutableLiveData<JogoPerguntaUiState>()
    val pergunta: LiveData<JogoPerguntaUiState> = _pergunta

    private val _evento = MutableLiveData<JogoEvent?>()
    val evento: LiveData<JogoEvent?> = _evento

    private val perguntas = mutableListOf<Pergunta>()

    private var codigoSala: String = ""
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var nomeJogador: String = ""
    private var nomeCategoria: String = ""
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()
    private var modoJogo: String? = null
    private var admin = false
    private var perguntaAtualIndex = 0
    private var totalPontos = 0.0
    private var numeroPerguntasCertas = 0
    private var totalPerguntascertas = 0
    private var bonus = 50
    private var jaRespondeu = false
    private var acertouUltimaPergunta = false
    private var serverTimeOffset: Long = 0L
    private var eliminacaoEmCurso = false
    private var navegacaoPontuacoesIniciada = false

    private var perguntaIndexListener: JogoRepository.ListenerHandle? = null
    private var serverTimeOffsetListener: JogoRepository.ListenerHandle? = null
    private var estadoSalaListener: JogoRepository.ListenerHandle? = null

    fun iniciar(
        codigoSala: String,
        uid: String,
        nomeUtilizador: String,
        nomeJogador: String,
        nomeCategoria: String
    ) {
        this.codigoSala = codigoSala
        this.uid = uid
        this.nomeUtilizador = nomeUtilizador
        this.nomeJogador = nomeJogador
        this.nomeCategoria = nomeCategoria
        this.jogadorAtual = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador)

        observarOffsetServidor()
        jogoRepository.obterInfoSala(codigoSala, jogadorAtual)
            .addOnSuccessListener { infoSala ->
                admin = infoSala.admin
                modoJogo = infoSala.modoJogo
                _sala.value = JogoSalaUiState(admin, modoJogo)
                escutarFimEliminatorias()
                carregarPerguntas()
            }
            .addOnFailureListener { erro ->
                _evento.value = JogoEvent.ErroCarregarSala(erro.message.orEmpty())
            }
    }

    fun enviarResposta(
        numeroOpcao: Int,
        opcaoEscolhida: String,
        respostaCorreta: String,
        tempoRestante: Double
    ): JogoRespostaResultado? {
        if (admin || jaRespondeu) return null
        jaRespondeu = true
        acertouUltimaPergunta = false

        var bonusAplicado = 0
        if (numeroOpcao in 0..3 && opcaoEscolhida == respostaCorreta) {
            acertouUltimaPergunta = true
            numeroPerguntasCertas++
            totalPerguntascertas++
            val resultado = scoreService.calcularPontuacao(
                modoJogo = modoJogo,
                tempoRestante = tempoRestante,
                numeroPerguntasCertas = numeroPerguntasCertas,
                bonus = bonus
            )
            bonusAplicado = resultado.bonusAplicado
            totalPontos += resultado.pontos
        } else if (numeroOpcao in 0..3) {
            numeroPerguntasCertas = 0
        }

        jogoRepository.registarResposta(codigoSala, jogadorAtual, acertouUltimaPergunta)

        return JogoRespostaResultado(
            acertou = acertouUltimaPergunta,
            bonusAplicado = bonusAplicado,
            deveEliminar = modoJogo == GameConstants.MODO_ELIMINATORIAS && !admin && !acertouUltimaPergunta
        )
    }

    fun eliminarJogador() {
        if (admin || eliminacaoEmCurso) return
        eliminacaoEmCurso = true
        jogoRepository.marcarJogadorEliminado(
            codigoSala,
            jogadorAtual,
            totalPontos,
            totalPerguntascertas
        )
            .addOnSuccessListener {
                _evento.value = JogoEvent.AbrirEsperaEliminado(dadosNavegacao())
            }
            .addOnFailureListener {
                eliminacaoEmCurso = false
                _evento.value = JogoEvent.ErroEliminarJogador
            }
    }

    fun adminTempoTerminou() {
        if (modoJogo == GameConstants.MODO_ELIMINATORIAS) {
            verificarFimEliminatoriasOuAvancar()
        } else {
            perguntaAtualIndex++
            jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
            prepararPerguntaAdmin()
        }
    }

    fun finalizarJogo() {
        if (perguntaAtualIndex >= perguntas.size) {
            if (modoJogo == GameConstants.MODO_ELIMINATORIAS && admin) {
                terminarEliminatoriasEEnviar()
                return
            }

            jogoRepository.obterEstadoSala(codigoSala)
                .addOnSuccessListener {
                    guardarResultadoEEnviarPontuacoes()
                }
                .addOnFailureListener { erro ->
                    _evento.value = JogoEvent.ErroEstadoSala(erro.message.orEmpty())
                }
            return
        }

        if (modoJogo == GameConstants.MODO_ELIMINATORIAS) {
            verificarFimEliminatoriasOuAvancar()
        } else {
            perguntaAtualIndex++
            if (admin) {
                jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
                prepararPerguntaAdmin()
            }
        }
    }

    fun tempoTotal(): Double {
        return gameService.tempoTotal(modoJogo)
    }

    fun tempoServidorAtual(): Long {
        return System.currentTimeMillis() + serverTimeOffset
    }

    fun jaRespondeuPergunta(): Boolean {
        return jaRespondeu
    }

    fun removerListeners() {
        jogoRepository.removerListener(perguntaIndexListener)
        perguntaIndexListener = null
        jogoRepository.removerListener(serverTimeOffsetListener)
        serverTimeOffsetListener = null
        jogoRepository.removerListener(estadoSalaListener)
        estadoSalaListener = null
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun carregarPerguntas() {
        jogoRepository.carregarPerguntas(codigoSala)
            .addOnSuccessListener { perguntasCarregadas ->
                perguntas.clear()
                perguntas.addAll(perguntasCarregadas)
                if (perguntas.isEmpty()) {
                    _evento.value = JogoEvent.FinalizarJogo
                    return@addOnSuccessListener
                }

                if (admin) {
                    prepararPerguntaAdmin()
                } else {
                    escutarIndicePergunta()
                }
            }
            .addOnFailureListener { erro ->
                _evento.value = JogoEvent.ErroCarregarPerguntas(erro.message.orEmpty())
            }
    }

    private fun escutarIndicePergunta() {
        removerIndicePerguntaListener()
        perguntaIndexListener = jogoRepository.escutarIndicePergunta(
            codigoSala,
            onIndiceAlterado = { novoIndex ->
                if (novoIndex != perguntaAtualIndex) {
                    perguntaAtualIndex = novoIndex
                    if (perguntaAtualIndex < perguntas.size) {
                        prepararPerguntaJogador()
                    } else {
                        _evento.value = JogoEvent.FinalizarJogo
                    }
                }
            }
        )

        jogoRepository.obterIndicePergunta(codigoSala)
            .addOnSuccessListener { idx ->
                perguntaAtualIndex = idx
                if (perguntaAtualIndex < perguntas.size) {
                    prepararPerguntaJogador()
                } else {
                    _evento.value = JogoEvent.FinalizarJogo
                }
            }
    }

    private fun prepararPerguntaJogador() {
        if (perguntaAtualIndex >= perguntas.size) {
            _evento.value = JogoEvent.FinalizarJogo
            return
        }

        jaRespondeu = false
        acertouUltimaPergunta = false
        publicarPergunta(adminPergunta = false)
        jogoRepository.obterHoraInicioPergunta(codigoSala)
            .addOnSuccessListener { horaInicio ->
                _evento.value = JogoEvent.IniciarCronometro(horaInicio ?: tempoServidorAtual(), admin = false)
            }
            .addOnFailureListener {
                _evento.value = JogoEvent.IniciarCronometro(tempoServidorAtual(), admin = false)
            }
    }

    private fun prepararPerguntaAdmin() {
        if (perguntaAtualIndex >= perguntas.size) {
            _evento.value = JogoEvent.FinalizarJogo
            return
        }

        jaRespondeu = false
        acertouUltimaPergunta = false
        publicarPergunta(adminPergunta = true)
        jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
        jogoRepository.limparRespostasPergunta(codigoSala)
        jogoRepository.obterHoraInicioPergunta(codigoSala)
            .addOnSuccessListener { horaInicio ->
                _evento.value = JogoEvent.IniciarCronometro(horaInicio ?: tempoServidorAtual(), admin = true)
            }
            .addOnFailureListener {
                _evento.value = JogoEvent.IniciarCronometro(tempoServidorAtual(), admin = true)
            }
    }

    private fun publicarPergunta(adminPergunta: Boolean) {
        _pergunta.value = JogoPerguntaUiState(
            pergunta = perguntas[perguntaAtualIndex],
            indice = perguntaAtualIndex,
            totalPerguntas = perguntas.size,
            admin = adminPergunta
        )
    }

    private fun observarOffsetServidor() {
        jogoRepository.removerListener(serverTimeOffsetListener)
        serverTimeOffsetListener = jogoRepository.escutarOffsetServidor(
            onOffsetAlterado = { offset ->
                serverTimeOffset = offset
            }
        )
    }

    private fun verificarFimEliminatoriasOuAvancar() {
        if (!admin && !acertouUltimaPergunta) {
            eliminarJogador()
            return
        }

        jogoRepository.obterJogadoresEliminatorias(codigoSala)
            .addOnSuccessListener { jogadores ->
                val jogadoresRestantes = jogadores
                    .filter { jogador ->
                        jogador.nome != GameConstants.JOGADOR_ADMIN &&
                            jogador.chave != GameConstants.JOGADOR_ADMIN &&
                            !jogador.isHostOnly &&
                            jogador.estado != GameConstants.ESTADO_ELIMINADO
                    }
                    .map { it.nome }

                if (gameService.deveTerminarEliminatorias(jogadoresRestantes)) {
                    _evento.value = JogoEvent.MensagemFimEliminatorias
                    terminarEliminatoriasEEnviar()
                } else {
                    perguntaAtualIndex++
                    jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
                    if (admin) prepararPerguntaAdmin()
                }
            }
            .addOnFailureListener {
                _evento.value = JogoEvent.ErroVerificarJogadores
            }
    }

    private fun escutarFimEliminatorias() {
        if (modoJogo != GameConstants.MODO_ELIMINATORIAS || estadoSalaListener != null) return

        estadoSalaListener = jogoRepository.escutarEstadoSala(
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_TERMINADO) {
                    guardarResultadoEEnviarPontuacoes()
                }
            }
        )
    }

    private fun terminarEliminatoriasEEnviar() {
        if (navegacaoPontuacoesIniciada) return

        jogoRepository.atualizarEstadoSala(codigoSala, GameConstants.ESTADO_TERMINADO)
            .addOnCompleteListener {
                guardarResultadoEEnviarPontuacoes()
            }
    }

    private fun guardarResultadoEEnviarPontuacoes() {
        if (navegacaoPontuacoesIniciada) return
        navegacaoPontuacoesIniciada = true
        removerListeners()

        if (!admin) {
            jogoRepository.guardarResultadoJogador(
                codigoSala,
                jogadorAtual,
                totalPontos,
                totalPerguntascertas
            ).addOnCompleteListener {
                _evento.value = JogoEvent.AbrirPontuacoes(dadosNavegacao())
            }
        } else {
            _evento.value = JogoEvent.AbrirPontuacoes(dadosNavegacao())
        }
    }

    private fun dadosNavegacao(): JogoResultadoDados {
        return JogoResultadoDados(
            codigoSala = codigoSala,
            uid = uid,
            nomeJogador = nomeJogador,
            totalPontos = totalPontos,
            nomeCategoria = nomeCategoria,
            nomeUtilizador = nomeUtilizador,
            modoJogo = modoJogo,
            numeroPerguntasCertas = numeroPerguntasCertas,
            totalPerguntasCertas = totalPerguntascertas,
            totalPerguntas = perguntas.size
        )
    }

    private fun removerIndicePerguntaListener() {
        jogoRepository.removerListener(perguntaIndexListener)
        perguntaIndexListener = null
    }
}

data class JogoSalaUiState(
    val admin: Boolean,
    val modoJogo: String?
)

data class JogoPerguntaUiState(
    val pergunta: Pergunta,
    val indice: Int,
    val totalPerguntas: Int,
    val admin: Boolean
)

data class JogoRespostaResultado(
    val acertou: Boolean,
    val bonusAplicado: Int,
    val deveEliminar: Boolean
)

data class JogoResultadoDados(
    val codigoSala: String,
    val uid: String,
    val nomeJogador: String,
    val totalPontos: Double,
    val nomeCategoria: String,
    val nomeUtilizador: String,
    val modoJogo: String?,
    val numeroPerguntasCertas: Int,
    val totalPerguntasCertas: Int,
    val totalPerguntas: Int
)

sealed class JogoEvent {
    data class IniciarCronometro(val horaInicio: Long, val admin: Boolean) : JogoEvent()
    data class ErroCarregarSala(val mensagem: String) : JogoEvent()
    data class ErroCarregarPerguntas(val mensagem: String) : JogoEvent()
    data class ErroEstadoSala(val mensagem: String) : JogoEvent()
    data object ErroVerificarJogadores : JogoEvent()
    data object ErroEliminarJogador : JogoEvent()
    data object FinalizarJogo : JogoEvent()
    data object MensagemFimEliminatorias : JogoEvent()
    data class AbrirEsperaEliminado(val dados: JogoResultadoDados) : JogoEvent()
    data class AbrirPontuacoes(val dados: JogoResultadoDados) : JogoEvent()
}
