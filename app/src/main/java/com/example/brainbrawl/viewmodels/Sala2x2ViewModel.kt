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

class Sala2x2ViewModel(
    private val jogoCompetitivoRepository: JogoCompetitivoRepository = JogoCompetitivoRepository()
) : ViewModel() {

    private val _estado = MutableLiveData<Sala2x2UiState>()
    val estado: LiveData<Sala2x2UiState> = _estado

    private val _evento = MutableLiveData<Sala2x2Event?>()
    val evento: LiveData<Sala2x2Event?> = _evento

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
    private var aIniciarJogo = false
    private var salaConfirmada = false
    private var salaMatchmaking = false
    private var origemSala = ""
    private var codigoSalaVisivel = false
    private var textoCodigoSalaPrivado = "A carregar sala..."
    private var nomeCategoriaSala = ""
    private var categoriaTodasLabel = "Todas as Categorias"
    private var picoJogadoresPresentes = 0
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
            "mode=2x2 room=$codigoSala uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()} " +
                "keyInitial=${chaveJogador.maskedLogId()} type=$tipoJogador avatar=${avatar.ifBlank { "<empty>" }}"
        )
        saidaManual = false
        aIniciarJogo = false
        salaConfirmada = false
        picoJogadoresPresentes = 0
        jogoIniciado = false
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala, jogadorAtual)
            .addOnSuccessListener { jogadorNaSala ->
                chaveJogador = jogadorNaSala.chave
                Log.d(
                    START_TAG,
                    "mode=2x2 room=$codigoSala joined key=${chaveJogador.maskedLogId()} " +
                        "uid=${jogadorNaSala.uid.maskedLogId()} username=${jogadorNaSala.nomeDisplay} " +
                        "avatar=${jogadorNaSala.avatar.ifBlank { "<empty>" }}"
                )
                carregarInfoSala(codigoSala) {
                    jogoCompetitivoRepository.marcarPronto2x2(codigoSala, chaveJogador, pronto = !salaMatchmaking)
                    atualizarAdminEPublicar(codigoSala)
                }
                observarProntos(codigoSala)
            }
            .addOnFailureListener {
                _evento.value = Sala2x2Event.EntradaBloqueada
            }
    }

    fun carregarExposicaoCodigo(codigoSala: String) {
        carregarInfoSala(codigoSala)
    }

    private fun carregarInfoSala(codigoSala: String, onComplete: () -> Unit = {}) {
        jogoCompetitivoRepository.obterCodigoSalaInfo(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .addOnSuccessListener { info ->
                origemSala = info.origem
                salaMatchmaking = fluxoSala.isMatchmaking
                codigoSalaVisivel = info.codigoVisivel
                textoCodigoSalaPrivado = info.textoPrivado
                Log.d(
                    FLOW_TAG,
                    "mode=2x2 room=$codigoSala roomInfo origem=${origemSala.ifBlank { "<empty>" }} " +
                        "flow=${fluxoSala.firebaseValue} matchmaking=$salaMatchmaking entradaFechada=${info.entradaFechada} " +
                        "uid=${jogadorAtual.uid.maskedLogId()} key=${chaveJogador.maskedLogId()}"
                )
                carregarCategoriaSala(codigoSala, onComplete)
            }
            .addOnFailureListener { error ->
                Log.w(
                    FLOW_TAG,
                    "mode=2x2 room=$codigoSala roomInfoFailed=${error.message} " +
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
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            nomeCategoriaSala
        ).addOnSuccessListener { categoria ->
            nomeCategoriaSala = categoria
            Log.d(
                START_TAG,
                "mode=2x2 room=$codigoSala categoryFromRoom=${nomeCategoriaSala.ifBlank { "<empty>" }}"
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
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onJogadoresAlterados = { jogadores ->
                val presentes = jogadoresUnicosDeLista(jogadores)
                Log.d(
                    TAG,
                    "Sala2x2 jogadores: codigo=$codigoSala matchmaking=$salaMatchmaking " +
                        "todos=${jogadores.resumoEstados()} presentes=${presentes.map { it.chave }} " +
                        "pico=$picoJogadoresPresentes"
                )
                if (presentes.size > picoJogadoresPresentes) {
                    picoJogadoresPresentes = presentes.size
                }
                if (picoJogadoresPresentes >= 4 && presentes.size < picoJogadoresPresentes) {
                    val ignorarQueda = aIniciarJogo || jogoIniciado
                    Log.d(
                        FLOW_TAG,
                        "mode=2x2 room=$codigoSala presenceDrop origem=${origemSala.ifBlank { "<empty>" }} " +
                            "flow=${fluxoSala.firebaseValue} matchmaking=$salaMatchmaking " +
                            "presentes=${presentes.size} pico=$picoJogadoresPresentes " +
                            "startInProgress=$aIniciarJogo gameStarted=$jogoIniciado " +
                            "eventEmitted=${!ignorarQueda} cleanupIntentional=false"
                    )
                    if (!ignorarQueda) {
                        _evento.value = Sala2x2Event.OponenteSaiu
                    }
                    picoJogadoresPresentes = presentes.size
                }
                jogadoresNaSala = jogadores
                atualizarAdminEPublicar(codigoSala)
            }
        )
    }

    private fun observarProntos(codigoSala: String) {
        removerProntosListener()
        prontosListener = jogoCompetitivoRepository.escutarProntos(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onProntosAlterados = { prontosAtualizados ->
                Log.d(START_TAG, "mode=2x2 room=$codigoSala ready=$prontosAtualizados key=${chaveJogador.maskedLogId()}")
                prontos = prontosAtualizados
                publicarEstado()
                tentarIniciarMatchmakingSePronto(codigoSala)
            }
        )
    }

    fun observarEstadoSala(codigoSala: String) {
        removerEstadoListener()
        estadoListener = jogoCompetitivoRepository.escutarEstadoSala(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onEstadoAlterado = { estado ->
                Log.d(
                    START_TAG,
                    "mode=2x2 room=$codigoSala stateChanged=$estado uid=${jogadorAtual.uid.maskedLogId()} " +
                        "key=${chaveJogador.maskedLogId()} category=${nomeCategoriaSala.ifBlank { "<empty>" }}"
                )
                if (estado == GameConstants.ESTADO_EM_JOGO) {
                    jogoIniciado = true
                    aIniciarJogo = true
                    _evento.value = Sala2x2Event.JogoIniciado
                }
            }
        )
    }

    fun observarSalaApagada(codigoSala: String) {
        removerSalaListener()
        salaListener = jogoCompetitivoRepository.escutarSalaApagada(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onSalaExisteAlterada = { existe ->
                if (existe) {
                    salaConfirmada = true
                } else if (salaConfirmada && !saidaManual && !aIniciarJogo && !jogoIniciado) {
                    Log.w(
                        START_TAG,
                        "mode=2x2 room=$codigoSala navigatingMainReason=room_deleted uid=${jogadorAtual.uid.maskedLogId()} " +
                            "key=${chaveJogador.maskedLogId()} manualExit=$saidaManual"
                    )
                    _evento.value = Sala2x2Event.SalaEncerrada
                } else if (salaConfirmada) {
                    Log.d(
                        FLOW_TAG,
                        "mode=2x2 room=$codigoSala roomDeletedIgnored flow=${fluxoSala.firebaseValue} manualExit=$saidaManual " +
                            "startInProgress=$aIniciarJogo gameStarted=$jogoIniciado"
                    )
                }
            }
        )
    }

    fun iniciarJogo(codigoSala: String) {
        when (fluxoSala) {
            RoomFlowType.MATCHMAKING -> marcarProntoMatchmaking(codigoSala)
            RoomFlowType.INVITE,
            RoomFlowType.PRIVATE -> iniciarJogoSeCompleto(codigoSala)
        }
    }

    private fun marcarProntoMatchmaking(codigoSala: String) {
        if (chaveJogador.isBlank() || chaveJogador in prontos) return
        jogoCompetitivoRepository.marcarPronto2x2(codigoSala, chaveJogador, pronto = true)
            .addOnSuccessListener {
                prontos = prontos + chaveJogador
                publicarEstado()
                tentarIniciarMatchmakingSePronto(codigoSala)
            }
            .addOnFailureListener {
                _evento.value = Sala2x2Event.JogadoresNaoProntos
            }
    }

    private fun iniciarJogoSeCompleto(codigoSala: String) {
        val jogadores = jogadoresUnicos()
        if (!admin || jogadores.size != 4 || aIniciarJogo) {
            Log.d(
                START_TAG,
                "mode=2x2 room=$codigoSala startBlocked admin=$admin players=${jogadores.size} " +
                    "alreadyStarting=$aIniciarJogo key=${chaveJogador.maskedLogId()}"
            )
            return
        }
        aIniciarJogo = true
        publicarEstado()

        jogoCompetitivoRepository.obterProntos2x2(codigoSala)
            .addOnSuccessListener { prontos ->
                val chavesPresentes = jogadores.map { it.chave }.toSet()
                val prontosValidos = prontos.filter { it in chavesPresentes }
                if (prontosValidos.size != 4) {
                    Log.d(
                        START_TAG,
                        "mode=2x2 room=$codigoSala startBlocked ready=${prontosValidos.size} players=${jogadores.size}"
                    )
                    aIniciarJogo = false
                    publicarEstado()
                    _evento.value = Sala2x2Event.JogadoresNaoProntos
                    return@addOnSuccessListener
                }

                val equipaA = jogadores.take(2)
                val equipaB = jogadores.drop(2).take(2)
                Log.d(
                    HOST_REMOVAL_TAG,
                    "mode=2x2 room=$codigoSala startGame admin=$admin " +
                        "teamA=${equipaA.map { it.chave.maskedLogId() }} teamB=${equipaB.map { it.chave.maskedLogId() }} " +
                        "flow=${fluxoSala.firebaseValue} roomType=${origemSala.ifBlank { "<empty>" }} matchmaking=$salaMatchmaking " +
                        "category=${nomeCategoriaSala.ifBlank { "<empty>" }} statusBefore=${GameConstants.ESTADO_EM_ESPERA} " +
                        "cleanupBlocked=true method=iniciarJogo2x2"
                )

                jogoCompetitivoRepository.iniciarJogo2x2(
                    codigoSala,
                    equipaA,
                    equipaB,
                    nomeCategoriaSala,
                    categoriaTodasLabel
                )
                    .addOnSuccessListener {
                        Log.d(HOST_REMOVAL_TAG, "mode=2x2 room=$codigoSala statusAfter=${GameConstants.ESTADO_EM_JOGO}")
                    }
                    .addOnFailureListener { error ->
                        Log.w(HOST_REMOVAL_TAG, "mode=2x2 room=$codigoSala startFailed=${error.message}")
                        aIniciarJogo = false
                        publicarEstado()
                        _evento.value = Sala2x2Event.ErroIniciarJogo
                    }
            }
            .addOnFailureListener {
                aIniciarJogo = false
                publicarEstado()
                _evento.value = Sala2x2Event.ErroIniciarJogo
            }
    }

    private fun tentarIniciarMatchmakingSePronto(codigoSala: String) {
        val jogadores = jogadoresUnicos()
        if (!salaMatchmaking || !admin || aIniciarJogo || jogadores.size != 4) return
        val chavesPresentes = jogadores.map { it.chave }.toSet()
        val prontosValidos = prontos.filter { it in chavesPresentes }
        if (prontosValidos.size != 4) return

        aIniciarJogo = true
        publicarEstado()
        val equipaA = jogadores.take(2)
        val equipaB = jogadores.drop(2).take(2)
        jogoCompetitivoRepository.iniciarJogo2x2(
            codigoSala,
            equipaA,
            equipaB,
            nomeCategoriaSala,
            categoriaTodasLabel
        )
            .addOnFailureListener { error ->
                Log.w(HOST_REMOVAL_TAG, "mode=2x2 room=$codigoSala matchmakingStartFailed=${error.message}")
                aIniciarJogo = false
                publicarEstado()
                _evento.value = Sala2x2Event.ErroIniciarJogo
            }
    }

    fun sairDaSala(codigoSala: String, reason: String = "explicit_leave") {
        if (aIniciarJogo) {
            Log.d(
                HOST_REMOVAL_TAG,
                "mode=2x2 room=$codigoSala sairDaSala skipped cleanupTriggered=false reason=start_in_progress " +
                    "admin=$admin key=${chaveJogador.maskedLogId()}"
            )
            return
        }
        saidaManual = true
        Log.d(
            FLOW_TAG,
            "mode=2x2 room=$codigoSala cleanupRequested flow=${fluxoSala.firebaseValue} " +
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
            "mode=2x2 room=$codigoSala cleanupPath=matchmaking action=delete_room " +
                "key=${chaveJogador.maskedLogId()}"
        )
        jogoCompetitivoRepository.apagarSala(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
    }

    private fun sairDaSalaPrivada(codigoSala: String) {
        val action = if (admin) "delete_room" else "remove_player"
        Log.d(
            FLOW_TAG,
            "mode=2x2 room=$codigoSala cleanupPath=invite_private action=$action " +
                "key=${chaveJogador.maskedLogId()}"
        )
        if (admin) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
        } else {
            jogoCompetitivoRepository.removerJogador2x2(codigoSala, jogadorAtual, chaveJogador)
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

    private fun atualizarAdminEPublicar(codigoSala: String) {
        jogoCompetitivoRepository.verificarAdmin(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            jogadorAtual,
            chaveJogador
        ).addOnSuccessListener { isAdmin ->
            admin = isAdmin
            publicarEstado()
        }.addOnFailureListener {
            admin = false
            publicarEstado()
        }
    }

    private fun publicarEstado() {
        val jogadores = jogadoresUnicos()
        val equipaA = jogadores.take(2)
        val equipaB = jogadores.drop(2).take(2)
        val salaCompleta = jogadores.size == 4
        val jogadorPronto = chaveJogador.isNotBlank() && chaveJogador in prontos
        val jogadorPresente = jogadores.any { it.chave == chaveJogador }
        _estado.value = Sala2x2UiState(
            equipaA = equipaA.map { it.nomeComPronto() },
            equipaB = equipaB.map { it.nomeComPronto() },
            equipaADetalhe = equipaA.map { it.toSalaJogadorUiState(pronto = it.chave in prontos) },
            equipaBDetalhe = equipaB.map { it.toSalaJogadorUiState(pronto = it.chave in prontos) },
            podeIniciar = if (salaMatchmaking) {
                jogadorPresente && !jogadorPronto && !aIniciarJogo
            } else {
                admin && salaCompleta && !aIniciarJogo
            },
            codigoSalaVisivel = codigoSalaVisivel,
            textoCodigoSalaPrivado = textoCodigoSalaPrivado,
            matchmaking = salaMatchmaking,
            origemSala = origemSala,
            jogadorPronto = jogadorPronto,
            chaveJogadorAtual = chaveJogador,
            avatarJogadorAtual = jogadores.firstOrNull { it.chave == chaveJogador }?.avatar.orEmpty(),
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

    private fun JogoCompetitivoRepository.JogadorCompetitivo.nomeComPronto(): String {
        return if (salaMatchmaking) {
            "$nomeDisplay · ${if (chave in prontos) "Pronto" else "A aguardar"}"
        } else {
            nomeDisplay
        }
    }

    private fun jogadoresUnicos(): List<JogoCompetitivoRepository.JogadorCompetitivo> {
        return jogadoresUnicosDeLista(jogadoresNaSala)
    }

    private fun jogadoresUnicosDeLista(
        lista: List<JogoCompetitivoRepository.JogadorCompetitivo>
    ): List<JogoCompetitivoRepository.JogadorCompetitivo> {
        return lista
            .filterNot { it.chave == GameConstants.JOGADOR_ADMIN || it.estado == GameConstants.ESTADO_OFF }
            .fold(emptyList()) { acumulado, jogador ->
                val existenteIndex = acumulado.indexOfFirst { it.corresponde(jogador) }
                if (existenteIndex == -1) {
                    acumulado + jogador
                } else {
                    acumulado.toMutableList().apply {
                        this[existenteIndex] = this[existenteIndex].preferir(jogador)
                    }
                }
            }
    }

    private fun JogoCompetitivoRepository.JogadorCompetitivo.corresponde(
        outro: JogoCompetitivoRepository.JogadorCompetitivo
    ): Boolean {
        return identificadores().any { it in outro.identificadores() }
    }

    private fun JogoCompetitivoRepository.JogadorCompetitivo.preferir(
        outro: JogoCompetitivoRepository.JogadorCompetitivo
    ): JogoCompetitivoRepository.JogadorCompetitivo {
        return when {
            outro.uid.isNotBlank() && outro.chave == outro.uid -> outro
            uid.isNotBlank() && chave == uid -> this
            outro.uid.isNotBlank() && uid.isBlank() -> outro
            else -> this
        }
    }

    private fun JogoCompetitivoRepository.JogadorCompetitivo.identificadores(): List<String> {
        return listOf(chave, uid, nomeUtilizador, nomeJogador, nomeDisplay)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun List<JogoCompetitivoRepository.JogadorCompetitivo>.resumoEstados(): List<String> {
        return map { "${it.chave}:${it.estado.ifBlank { GameConstants.ESTADO_ON }}" }
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
        const val TAG = "MATCHMAKING_DEBUG"
        const val START_TAG = "INVITE_START_ROOT_CAUSE"
        const val HOST_REMOVAL_TAG = "HOST_REMOVAL_DEBUG"
        const val FLOW_TAG = "FLOW_SEPARATION_DEBUG"
    }
}

data class Sala2x2UiState(
    val equipaA: List<String>,
    val equipaB: List<String>,
    val equipaADetalhe: List<SalaJogadorUiState> = emptyList(),
    val equipaBDetalhe: List<SalaJogadorUiState> = emptyList(),
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

sealed class Sala2x2Event {
    data object JogoIniciado : Sala2x2Event()
    data object SalaEncerrada : Sala2x2Event()
    data object ErroIniciarJogo : Sala2x2Event()
    data object EntradaBloqueada : Sala2x2Event()
    data object JogadoresNaoProntos : Sala2x2Event()
    data object OponenteSaiu : Sala2x2Event()
}

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
