package com.example.brainbrawl.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.RoomFlowType
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.repositories.JogoCompetitivoRepository
import com.example.brainbrawl.repositories.JogoCompetitivoRepository.ModoCompetitivo

class Sala1x1ViewModel(
    private val jogoCompetitivoRepository: JogoCompetitivoRepository = JogoCompetitivoRepository()
) : ViewModel() {

    private val _estado = MutableLiveData<SalaCompetitivaUiState>()
    val estado: LiveData<SalaCompetitivaUiState> = _estado

    private val _evento = MutableLiveData<Sala1x1Event?>()
    val evento: LiveData<Sala1x1Event?> = _evento

    private var jogadoresListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var prontosListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var estadoListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var salaListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var jogadoresNaSala: List<JogoCompetitivoRepository.JogadorCompetitivo> = emptyList()
    private var prontos: Set<String> = emptySet()
    private var admin = false
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()
    private var chaveJogador = ""
    private var saidaManual = false
    private var salaConfirmada = false
    private var salaMatchmaking = false
    private var origemSala = ""
    private var codigoSalaVisivel = false
    private var textoCodigoSalaPrivado = "A carregar sala..."
    private var nomeCategoriaSala = ""
    private var categoriaTodasLabel = "Todas as Categorias"
    private var picoJogadoresPresentes = 0
    private var aVerificarProntos = false
    private var inicioJogoEmCurso = false
    private var jogoIniciado = false
    private val fluxoSala: RoomFlowType
        get() = RoomFlowType.fromOrigin(origemSala)

    fun iniciar(
        codigoSala: String,
        uid: String,
        nomeUtilizador: String,
        nomeJogador: String,
        playerKey: String = "",
        tipoJogador: String = "",
        avatar: String = "",
        categoriaTodas: String = categoriaTodasLabel
    ) {
        jogadorAtual = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador, playerKey, tipoJogador, avatar)
        chaveJogador = jogadorAtual.chaveSala
        categoriaTodasLabel = categoriaTodas.ifBlank { categoriaTodasLabel }
        Log.d(
            START_TAG,
            "mode=1x1 room=$codigoSala uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()} " +
                "keyInitial=${chaveJogador.maskedLogId()} type=$tipoJogador avatar=${avatar.ifBlank { "<empty>" }}"
        )
        saidaManual = false
        picoJogadoresPresentes = 0
        salaConfirmada = false
        inicioJogoEmCurso = false
        jogoIniciado = false
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.UM_CONTRA_UM, codigoSala, jogadorAtual)
            .addOnSuccessListener { jogadorNaSala ->
                chaveJogador = jogadorNaSala.chave
                Log.d(
                    START_TAG,
                    "mode=1x1 room=$codigoSala joined key=${chaveJogador.maskedLogId()} " +
                        "uid=${jogadorNaSala.uid.maskedLogId()} username=${jogadorNaSala.nomeDisplay} " +
                        "avatar=${jogadorNaSala.avatar.ifBlank { "<empty>" }}"
                )
                carregarInfoSala(codigoSala) {
                    if (salaMatchmaking) {
                        jogoCompetitivoRepository.marcarPronto1x1(codigoSala, chaveJogador, pronto = false)
                    } else {
                        jogoCompetitivoRepository.marcarPronto1x1(codigoSala, chaveJogador, pronto = true)
                    }
                    atualizarAdmin(codigoSala)
                    publicarEstado()
                }
                observarProntos(codigoSala)
            }
            .addOnFailureListener {
                _evento.value = Sala1x1Event.EntradaBloqueada
            }
    }

    fun carregarExposicaoCodigo(codigoSala: String) {
        carregarInfoSala(codigoSala)
    }

    private fun carregarInfoSala(codigoSala: String, onComplete: () -> Unit = {}) {
        jogoCompetitivoRepository.obterCodigoSalaInfo(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .addOnSuccessListener { info ->
                origemSala = info.origem
                salaMatchmaking = fluxoSala.isMatchmaking
                codigoSalaVisivel = info.codigoVisivel
                textoCodigoSalaPrivado = info.textoPrivado
                Log.d(
                    FLOW_TAG,
                    "mode=1x1 room=$codigoSala roomInfo origem=${origemSala.ifBlank { "<empty>" }} " +
                        "flow=${fluxoSala.firebaseValue} matchmaking=$salaMatchmaking entradaFechada=${info.entradaFechada} " +
                        "uid=${jogadorAtual.uid.maskedLogId()} key=${chaveJogador.maskedLogId()}"
                )
                carregarCategoriaSala(codigoSala, onComplete)
            }
            .addOnFailureListener { error ->
                Log.w(
                    FLOW_TAG,
                    "mode=1x1 room=$codigoSala roomInfoFailed=${error.message} " +
                        "fallbackFlow=${RoomFlowType.PRIVATE.firebaseValue} fallbackMatchmaking=false " +
                        "uid=${jogadorAtual.uid.maskedLogId()} key=${chaveJogador.maskedLogId()}"
                )
                origemSala = ""
                salaMatchmaking = false
                codigoSalaVisivel = true
                textoCodigoSalaPrivado = ""
                carregarCategoriaSala(codigoSala, onComplete)
            }
    }

    private fun carregarCategoriaSala(codigoSala: String, onComplete: () -> Unit = {}) {
        jogoCompetitivoRepository.carregarNomeCategoria(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            nomeCategoriaSala
        ).addOnSuccessListener { categoria ->
            nomeCategoriaSala = categoria
            Log.d(
                START_TAG,
                "mode=1x1 room=$codigoSala categoryFromRoom=${nomeCategoriaSala.ifBlank { "<empty>" }}"
            )
            publicarEstado()
            onComplete()
        }.addOnFailureListener {
            publicarEstado()
            onComplete()
        }
    }

    fun observarJogadores(codigoSala: String) {
        removerJogadoresListener()
        jogadoresListener = jogoCompetitivoRepository.escutarJogadores(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            onJogadoresAlterados = { jogadores ->
                val presentes = jogadores.jogadoresPresentes()
                Log.d(
                    TAG,
                    "Sala1x1 jogadores: codigo=$codigoSala matchmaking=$salaMatchmaking " +
                        "todos=${jogadores.resumoEstados()} presentes=${presentes.map { it.chave }} " +
                        "pico=$picoJogadoresPresentes"
                )
                jogadoresNaSala = presentes
                if (presentes.size > picoJogadoresPresentes) {
                    picoJogadoresPresentes = presentes.size
                }
                if (picoJogadoresPresentes >= 2 && presentes.size < picoJogadoresPresentes) {
                    val ignorarQueda = inicioJogoEmCurso || jogoIniciado
                    Log.d(
                        FLOW_TAG,
                        "mode=1x1 room=$codigoSala presenceDrop origem=${origemSala.ifBlank { "<empty>" }} " +
                            "flow=${fluxoSala.firebaseValue} matchmaking=$salaMatchmaking " +
                            "presentes=${presentes.size} pico=$picoJogadoresPresentes " +
                            "startInProgress=$inicioJogoEmCurso gameStarted=$jogoIniciado " +
                            "eventEmitted=${!ignorarQueda} cleanupIntentional=false"
                    )
                    if (!ignorarQueda) {
                        _evento.value = Sala1x1Event.OponenteSaiu
                    }
                    picoJogadoresPresentes = presentes.size
                }
                atualizarAdmin(codigoSala)
                publicarEstado()
            }
        )
    }

    private fun observarProntos(codigoSala: String) {
        removerProntosListener()
        prontosListener = jogoCompetitivoRepository.escutarProntos(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            onProntosAlterados = { prontosAtualizados ->
                Log.d(START_TAG, "mode=1x1 room=$codigoSala ready=$prontosAtualizados key=${chaveJogador.maskedLogId()}")
                prontos = prontosAtualizados
                publicarEstado()
                tentarIniciarMatchmakingSePronto(codigoSala)
            }
        )
    }

    fun observarEstadoSala(codigoSala: String) {
        removerEstadoListener()
        estadoListener = jogoCompetitivoRepository.escutarEstadoSala(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            onEstadoAlterado = { estado ->
                Log.d(
                    START_TAG,
                    "mode=1x1 room=$codigoSala stateChanged=$estado uid=${jogadorAtual.uid.maskedLogId()} " +
                        "key=${chaveJogador.maskedLogId()} category=${nomeCategoriaSala.ifBlank { "<empty>" }}"
                )
                if (estado == GameConstants.ESTADO_EM_JOGO) {
                    jogoIniciado = true
                    inicioJogoEmCurso = true
                    _evento.value = Sala1x1Event.JogoIniciado
                }
            }
        )
    }

    fun observarSalaApagada(codigoSala: String) {
        removerSalaListener()
        salaListener = jogoCompetitivoRepository.escutarSalaApagada(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            onSalaExisteAlterada = { existe ->
                if (existe) {
                    salaConfirmada = true
                } else if (salaConfirmada && !saidaManual && !inicioJogoEmCurso && !jogoIniciado) {
                    Log.w(
                        START_TAG,
                        "mode=1x1 room=$codigoSala navigatingMainReason=room_deleted uid=${jogadorAtual.uid.maskedLogId()} " +
                            "key=${chaveJogador.maskedLogId()} manualExit=$saidaManual"
                    )
                    _evento.value = Sala1x1Event.SalaEncerrada
                } else if (salaConfirmada) {
                    Log.d(
                        FLOW_TAG,
                        "mode=1x1 room=$codigoSala roomDeletedIgnored flow=${fluxoSala.firebaseValue} manualExit=$saidaManual " +
                            "startInProgress=$inicioJogoEmCurso gameStarted=$jogoIniciado"
                    )
                }
            }
        )
    }

    fun acaoPrincipal(codigoSala: String) {
        when (fluxoSala) {
            RoomFlowType.MATCHMAKING -> marcarProntoMatchmaking(codigoSala)
            RoomFlowType.INVITE,
            RoomFlowType.PRIVATE -> verificarProntosEAvancar(codigoSala)
        }
    }

    private fun marcarProntoMatchmaking(codigoSala: String) {
        if (chaveJogador.isBlank() || chaveJogador in prontos) return
        jogoCompetitivoRepository.marcarPronto1x1(codigoSala, chaveJogador, pronto = true)
            .addOnSuccessListener {
                prontos = prontos + chaveJogador
                publicarEstado()
                tentarIniciarMatchmakingSePronto(codigoSala)
            }
            .addOnFailureListener {
                _evento.value = Sala1x1Event.JogadoresNaoProntos
            }
    }

    private fun verificarProntosEAvancar(codigoSala: String) {
        if (aVerificarProntos) return
        if (!(admin && jogadoresNaSala.size == 2)) {
            Log.d(
                START_TAG,
                "mode=1x1 room=$codigoSala startBlocked admin=$admin players=${jogadoresNaSala.size} " +
                    "key=${chaveJogador.maskedLogId()}"
            )
            _evento.value = Sala1x1Event.AguardarAdversario
            return
        }

        aVerificarProntos = true
        inicioJogoEmCurso = true
        publicarEstado()
        jogoCompetitivoRepository.obterProntos1x1(codigoSala)
            .addOnCompleteListener { aVerificarProntos = false }
            .addOnSuccessListener { prontos ->
                val chavesPresentes = jogadoresNaSala.map { it.chave }.toSet()
                val prontosValidos = prontos.filter { it in chavesPresentes }
                if (prontosValidos.size == 2 && jogadoresNaSala.size == 2) {
                    Log.d(
                        HOST_REMOVAL_TAG,
                        "mode=1x1 room=$codigoSala startGame admin=$admin players=${jogadoresNaSala.map { it.chave.maskedLogId() }} " +
                            "flow=${fluxoSala.firebaseValue} roomType=${origemSala.ifBlank { "<empty>" }} matchmaking=$salaMatchmaking " +
                            "category=${nomeCategoriaSala.ifBlank { "<empty>" }} statusBefore=${GameConstants.ESTADO_EM_ESPERA} " +
                            "cleanupBlocked=true method=iniciarJogo1x1"
                    )
                    jogoCompetitivoRepository.iniciarJogo1x1(
                        codigoSala,
                        nomeCategoriaSala,
                        categoriaTodasLabel
                    ).addOnSuccessListener {
                        Log.d(HOST_REMOVAL_TAG, "mode=1x1 room=$codigoSala statusAfter=${GameConstants.ESTADO_EM_JOGO}")
                    }.addOnFailureListener { error ->
                        inicioJogoEmCurso = false
                        Log.w(HOST_REMOVAL_TAG, "mode=1x1 room=$codigoSala startFailed=${error.message}")
                        _evento.value = Sala1x1Event.JogadoresNaoProntos
                    }
                } else {
                    inicioJogoEmCurso = false
                    publicarEstado()
                    Log.d(
                        START_TAG,
                        "mode=1x1 room=$codigoSala startBlocked ready=${prontosValidos.size} players=${jogadoresNaSala.size}"
                    )
                    _evento.value = Sala1x1Event.JogadoresNaoProntos
                }
            }
            .addOnFailureListener { error ->
                inicioJogoEmCurso = false
                publicarEstado()
                Log.w(HOST_REMOVAL_TAG, "mode=1x1 room=$codigoSala readyReadFailed=${error.message}")
                _evento.value = Sala1x1Event.JogadoresNaoProntos
            }
    }

    private fun tentarIniciarMatchmakingSePronto(codigoSala: String) {
        if (!salaMatchmaking || !admin || aVerificarProntos || jogadoresNaSala.size != 2) return
        val chavesPresentes = jogadoresNaSala.map { it.chave }.toSet()
        val prontosValidos = prontos.filter { it in chavesPresentes }
        if (prontosValidos.size != 2) return

        aVerificarProntos = true
        inicioJogoEmCurso = true
        jogoCompetitivoRepository.iniciarJogo1x1(
            codigoSala,
            nomeCategoriaSala,
            categoriaTodasLabel
        ).addOnCompleteListener {
            aVerificarProntos = false
        }.addOnFailureListener { error ->
            inicioJogoEmCurso = false
            Log.w(HOST_REMOVAL_TAG, "mode=1x1 room=$codigoSala matchmakingStartFailed=${error.message}")
            _evento.value = Sala1x1Event.JogadoresNaoProntos
        }
    }

    fun sairDaSala(codigoSala: String, reason: String = "explicit_leave") {
        if (inicioJogoEmCurso) {
            Log.d(
                HOST_REMOVAL_TAG,
                "mode=1x1 room=$codigoSala sairDaSala skipped cleanupTriggered=false reason=start_in_progress " +
                    "admin=$admin key=${chaveJogador.maskedLogId()}"
            )
            return
        }
        saidaManual = true
        Log.d(
            FLOW_TAG,
            "mode=1x1 room=$codigoSala cleanupRequested flow=${fluxoSala.firebaseValue} " +
                "matchmaking=$salaMatchmaking roomType=${origemSala.ifBlank { "<empty>" }} admin=$admin " +
                "chave=${chaveJogador.maskedLogId()} cleanupIntentional=true reason=$reason"
        )
        when (fluxoSala) {
            RoomFlowType.MATCHMAKING -> sairDaSalaMatchmaking(codigoSala)
            RoomFlowType.INVITE,
            RoomFlowType.PRIVATE -> sairDaSalaPrivada(codigoSala)
        }
    }

    private fun sairDaSalaMatchmaking(codigoSala: String) {
        Log.d(
            FLOW_TAG,
            "mode=1x1 room=$codigoSala cleanupPath=matchmaking action=delete_room " +
                "key=${chaveJogador.maskedLogId()}"
        )
        jogoCompetitivoRepository.apagarSala(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
    }

    private fun sairDaSalaPrivada(codigoSala: String) {
        val action = if (admin) "delete_room" else "remove_player"
        Log.d(
            FLOW_TAG,
            "mode=1x1 room=$codigoSala cleanupPath=invite_private action=$action " +
                "key=${chaveJogador.maskedLogId()}"
        )
        if (admin) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
        } else {
            jogoCompetitivoRepository.removerJogador1x1(codigoSala, jogadorAtual, chaveJogador)
        }
    }

    fun removerListeners() {
        removerJogadoresListener()
        removerProntosListener()
        removerEstadoListener()
        removerSalaListener()
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun publicarEstado() {
        val jogadorPronto = chaveJogador.isNotBlank() && chaveJogador in prontos
        val jogadorPresente = jogadoresNaSala.any { it.chave == chaveJogador }
        val jogadoresDetalhe = jogadoresNaSala.map { jogador ->
            jogador.toSalaJogadorUiState(pronto = jogador.chave in prontos)
        }
        _estado.value = SalaCompetitivaUiState(
            jogadores = jogadoresNaSala.map { jogador ->
                if (salaMatchmaking) {
                    "${jogador.nomeDisplay} · ${if (jogador.chave in prontos) "Pronto" else "A aguardar"}"
                } else {
                    jogador.nomeDisplay
                }
            },
            jogadoresDetalhe = jogadoresDetalhe,
            admin = admin,
            podeIniciar = if (salaMatchmaking) {
                jogadorPresente && !jogadorPronto
            } else {
                admin && jogadoresNaSala.size == 2 && !inicioJogoEmCurso && !jogoIniciado
            },
            codigoSalaVisivel = codigoSalaVisivel,
            textoCodigoSalaPrivado = textoCodigoSalaPrivado,
            matchmaking = salaMatchmaking,
            origemSala = origemSala,
            jogadorPronto = jogadorPronto,
            chaveJogadorAtual = chaveJogador,
            avatarJogadorAtual = jogadoresNaSala.firstOrNull { it.chave == chaveJogador }?.avatar.orEmpty(),
            nomeCategoria = nomeCategoriaSala
        )
    }

    private fun JogoCompetitivoRepository.JogadorCompetitivo.toSalaJogadorUiState(
        pronto: Boolean
    ): SalaJogadorUiState {
        return SalaJogadorUiState(
            chave = chave,
            nomeDisplay = nomeDisplay,
            uid = uid,
            playerKey = playerKey,
            tipoJogador = tipoJogador,
            avatar = avatar,
            estado = estado,
            pronto = pronto
        )
    }

    private fun List<JogoCompetitivoRepository.JogadorCompetitivo>.jogadoresPresentes(): List<JogoCompetitivoRepository.JogadorCompetitivo> {
        return filterNot { jogador ->
            jogador.chave == GameConstants.JOGADOR_ADMIN || jogador.estado == GameConstants.ESTADO_OFF
        }
    }

    private fun List<JogoCompetitivoRepository.JogadorCompetitivo>.resumoEstados(): List<String> {
        return map { "${it.chave}:${it.estado.ifBlank { GameConstants.ESTADO_ON }}" }
    }

    private fun atualizarAdmin(codigoSala: String) {
        jogoCompetitivoRepository.obterChavesAdmin(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .addOnSuccessListener { chavesAdmin ->
                admin = if (chavesAdmin.isEmpty()) {
                    jogadoresNaSala.firstOrNull()?.chave == chaveJogador
                } else {
                    chavesAdmin.any { chave ->
                        chave == chaveJogador || chave in jogadorAtual.chavesCompatibilidade
                    }
                }
                publicarEstado()
            }
    }

    private fun removerJogadoresListener() {
        jogoCompetitivoRepository.removerListener(jogadoresListener)
        jogadoresListener = null
    }

    private fun removerProntosListener() {
        jogoCompetitivoRepository.removerListener(prontosListener)
        prontosListener = null
    }

    private fun removerEstadoListener() {
        jogoCompetitivoRepository.removerListener(estadoListener)
        estadoListener = null
    }

    private fun removerSalaListener() {
        jogoCompetitivoRepository.removerListener(salaListener)
        salaListener = null
    }

    private companion object {
        const val TAG = "Matchmaking"
        const val START_TAG = "GameStart"
        const val HOST_REMOVAL_TAG = "RoomLifecycle"
        const val FLOW_TAG = "RoomFlow"
    }
}

data class SalaCompetitivaUiState(
    val jogadores: List<String>,
    val jogadoresDetalhe: List<SalaJogadorUiState> = emptyList(),
    val admin: Boolean,
    val podeIniciar: Boolean,
    val codigoSalaVisivel: Boolean = true,
    val textoCodigoSalaPrivado: String = "",
    val matchmaking: Boolean = false,
    val origemSala: String = "",
    val jogadorPronto: Boolean = false,
    val chaveJogadorAtual: String = "",
    val avatarJogadorAtual: String = "",
    val nomeCategoria: String = ""
)

data class SalaJogadorUiState(
    val chave: String,
    val nomeDisplay: String,
    val uid: String,
    val playerKey: String,
    val tipoJogador: String,
    val avatar: String,
    val estado: String,
    val pronto: Boolean
)

sealed class Sala1x1Event {
    data object JogoIniciado : Sala1x1Event()
    data object SalaEncerrada : Sala1x1Event()
    data object AguardarAdversario : Sala1x1Event()
    data object JogadoresNaoProntos : Sala1x1Event()
    data object EntradaBloqueada : Sala1x1Event()
    /** Contagem de jogadores activos desceu (ex.: adversário saiu ou ficou off). */
    data object OponenteSaiu : Sala1x1Event()
}

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
