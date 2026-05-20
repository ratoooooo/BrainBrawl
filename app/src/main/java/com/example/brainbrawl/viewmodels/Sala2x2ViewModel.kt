package com.example.brainbrawl.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
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
    private var codigoSalaVisivel = false
    private var textoCodigoSalaPrivado = "A carregar sala..."
    private var picoJogadoresPresentes = 0

    fun iniciar(
        codigoSala: String,
        uid: String,
        nomeUtilizador: String,
        nomeJogador: String,
        playerKey: String = "",
        tipoJogador: String = "",
        avatar: String = ""
    ) {
        jogadorAtual = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador, playerKey, tipoJogador, avatar)
        chaveJogador = jogadorAtual.chaveSala
        Log.d(
            TAG,
            "Sala2x2 iniciar: codigo=$codigoSala uid=$uid playerKey=$playerKey " +
                "chaveInicial=$chaveJogador tipo=$tipoJogador"
        )
        saidaManual = false
        aIniciarJogo = false
        salaConfirmada = false
        picoJogadoresPresentes = 0
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala, jogadorAtual)
            .addOnSuccessListener { jogadorNaSala ->
                chaveJogador = jogadorNaSala.chave
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
                salaMatchmaking = info.origem == GameConstants.ORIGEM_MATCHMAKING
                codigoSalaVisivel = info.codigoVisivel
                textoCodigoSalaPrivado = info.textoPrivado
                publicarEstado()
                onComplete()
            }
            .addOnFailureListener {
                codigoSalaVisivel = true
                textoCodigoSalaPrivado = ""
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
                    Log.d(
                        TAG,
                        "Sala2x2 queda de presenca: codigo=$codigoSala matchmaking=$salaMatchmaking " +
                            "presentes=${presentes.size} pico=$picoJogadoresPresentes " +
                            "cleanupIntentional=false"
                    )
                    _evento.value = Sala2x2Event.OponenteSaiu
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
                Log.d(TAG, "Sala2x2 prontos: codigo=$codigoSala valores=$prontosAtualizados")
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
                if (estado == GameConstants.ESTADO_EM_JOGO) {
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
                } else if (salaConfirmada && !saidaManual) {
                    _evento.value = Sala2x2Event.SalaEncerrada
                }
            }
        )
    }

    fun iniciarJogo(codigoSala: String) {
        if (salaMatchmaking) {
            marcarProntoMatchmaking(codigoSala)
            return
        }
        iniciarJogoSeCompleto(codigoSala)
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
        if (!admin || jogadores.size != 4 || aIniciarJogo) return
        aIniciarJogo = true
        publicarEstado()

        jogoCompetitivoRepository.obterProntos2x2(codigoSala)
            .addOnSuccessListener { prontos ->
                val chavesPresentes = jogadores.map { it.chave }.toSet()
                val prontosValidos = prontos.filter { it in chavesPresentes }
                if (prontosValidos.size != 4) {
                    aIniciarJogo = false
                    publicarEstado()
                    _evento.value = Sala2x2Event.JogadoresNaoProntos
                    return@addOnSuccessListener
                }

                val equipaA = jogadores.take(2)
                val equipaB = jogadores.drop(2).take(2)

                jogoCompetitivoRepository.guardarEquipas2x2(codigoSala, equipaA, equipaB)
                    .addOnSuccessListener {
                        jogoCompetitivoRepository.atualizarEstadoSala(
                            ModoCompetitivo.DOIS_CONTRA_DOIS,
                            codigoSala,
                            GameConstants.ESTADO_EM_JOGO
                        ).addOnFailureListener {
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
        jogoCompetitivoRepository.guardarEquipas2x2(codigoSala, equipaA, equipaB)
            .addOnSuccessListener {
                jogoCompetitivoRepository.atualizarEstadoSala(
                    ModoCompetitivo.DOIS_CONTRA_DOIS,
                    codigoSala,
                    GameConstants.ESTADO_EM_JOGO
                ).addOnFailureListener {
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

    fun sairDaSala(codigoSala: String) {
        saidaManual = true
        Log.d(
            TAG,
            "Sala2x2 sairDaSala: codigo=$codigoSala matchmaking=$salaMatchmaking " +
                "admin=$admin chave=$chaveJogador cleanupIntentional=true"
        )
        if (salaMatchmaking) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
        } else if (admin) {
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
            podeIniciar = if (salaMatchmaking) {
                jogadorPresente && !jogadorPronto && !aIniciarJogo
            } else {
                admin && salaCompleta && !aIniciarJogo
            },
            codigoSalaVisivel = codigoSalaVisivel,
            textoCodigoSalaPrivado = textoCodigoSalaPrivado,
            matchmaking = salaMatchmaking,
            jogadorPronto = jogadorPronto
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
    }
}

data class Sala2x2UiState(
    val equipaA: List<String>,
    val equipaB: List<String>,
    val podeIniciar: Boolean,
    val codigoSalaVisivel: Boolean = true,
    val textoCodigoSalaPrivado: String = "",
    val matchmaking: Boolean = false,
    val jogadorPronto: Boolean = false
)

sealed class Sala2x2Event {
    data object JogoIniciado : Sala2x2Event()
    data object SalaEncerrada : Sala2x2Event()
    data object ErroIniciarJogo : Sala2x2Event()
    data object EntradaBloqueada : Sala2x2Event()
    data object JogadoresNaoProntos : Sala2x2Event()
    data object OponenteSaiu : Sala2x2Event()
}
