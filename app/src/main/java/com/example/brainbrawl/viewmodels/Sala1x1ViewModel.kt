package com.example.brainbrawl.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
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
    private var codigoSalaVisivel = false
    private var textoCodigoSalaPrivado = "A carregar sala..."
    private var picoJogadoresPresentes = 0
    private var aVerificarProntos = false

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
            "Sala1x1 iniciar: codigo=$codigoSala uid=$uid playerKey=$playerKey " +
                "chaveInicial=$chaveJogador tipo=$tipoJogador"
        )
        saidaManual = false
        picoJogadoresPresentes = 0
        salaConfirmada = false
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.UM_CONTRA_UM, codigoSala, jogadorAtual)
            .addOnSuccessListener { jogadorNaSala ->
                chaveJogador = jogadorNaSala.chave
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
                    Log.d(
                        TAG,
                        "Sala1x1 queda de presenca: codigo=$codigoSala matchmaking=$salaMatchmaking " +
                            "presentes=${presentes.size} pico=$picoJogadoresPresentes " +
                            "cleanupIntentional=false"
                    )
                    _evento.value = Sala1x1Event.OponenteSaiu
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
                Log.d(TAG, "Sala1x1 prontos: codigo=$codigoSala valores=$prontosAtualizados")
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
                if (estado == GameConstants.ESTADO_EM_JOGO) {
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
                } else if (salaConfirmada && !saidaManual) {
                    _evento.value = Sala1x1Event.SalaEncerrada
                }
            }
        )
    }

    fun acaoPrincipal(codigoSala: String) {
        if (salaMatchmaking) {
            marcarProntoMatchmaking(codigoSala)
        } else {
            verificarProntosEAvancar(codigoSala)
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
            _evento.value = Sala1x1Event.AguardarAdversario
            return
        }

        aVerificarProntos = true
        jogoCompetitivoRepository.obterProntos1x1(codigoSala)
            .addOnCompleteListener { aVerificarProntos = false }
            .addOnSuccessListener { prontos ->
                val chavesPresentes = jogadoresNaSala.map { it.chave }.toSet()
                val prontosValidos = prontos.filter { it in chavesPresentes }
                if (prontosValidos.size == 2 && jogadoresNaSala.size == 2) {
                    jogoCompetitivoRepository.atualizarEstadoSala(
                        ModoCompetitivo.UM_CONTRA_UM,
                        codigoSala,
                        GameConstants.ESTADO_EM_JOGO
                    )
                } else {
                    _evento.value = Sala1x1Event.JogadoresNaoProntos
                }
            }
    }

    private fun tentarIniciarMatchmakingSePronto(codigoSala: String) {
        if (!salaMatchmaking || !admin || aVerificarProntos || jogadoresNaSala.size != 2) return
        val chavesPresentes = jogadoresNaSala.map { it.chave }.toSet()
        val prontosValidos = prontos.filter { it in chavesPresentes }
        if (prontosValidos.size != 2) return

        aVerificarProntos = true
        jogoCompetitivoRepository.atualizarEstadoSala(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            GameConstants.ESTADO_EM_JOGO
        ).addOnCompleteListener {
            aVerificarProntos = false
        }
    }

    fun sairDaSala(codigoSala: String) {
        saidaManual = true
        Log.d(
            TAG,
            "Sala1x1 sairDaSala: codigo=$codigoSala matchmaking=$salaMatchmaking " +
                "admin=$admin chave=$chaveJogador cleanupIntentional=true"
        )
        if (salaMatchmaking) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
        } else if (admin) {
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
        _estado.value = SalaCompetitivaUiState(
            jogadores = jogadoresNaSala.map { jogador ->
                if (salaMatchmaking) {
                    "${jogador.nomeDisplay} · ${if (jogador.chave in prontos) "Pronto" else "A aguardar"}"
                } else {
                    jogador.nomeDisplay
                }
            },
            admin = admin,
            podeIniciar = if (salaMatchmaking) {
                jogadorPresente && !jogadorPronto
            } else {
                admin && jogadoresNaSala.size == 2
            },
            codigoSalaVisivel = codigoSalaVisivel,
            textoCodigoSalaPrivado = textoCodigoSalaPrivado,
            matchmaking = salaMatchmaking,
            jogadorPronto = jogadorPronto
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
        const val TAG = "MATCHMAKING_DEBUG"
    }
}

data class SalaCompetitivaUiState(
    val jogadores: List<String>,
    val admin: Boolean,
    val podeIniciar: Boolean,
    val codigoSalaVisivel: Boolean = true,
    val textoCodigoSalaPrivado: String = "",
    val matchmaking: Boolean = false,
    val jogadorPronto: Boolean = false
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
