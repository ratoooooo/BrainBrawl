package com.example.brainbrawl.viewmodels

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
    private var estadoListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var salaListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var jogadoresNaSala: List<JogoCompetitivoRepository.JogadorCompetitivo> = emptyList()
    private var admin = false
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()
    private var chaveJogador = ""
    private var saidaManual = false

    fun iniciar(codigoSala: String, uid: String, nomeUtilizador: String, nomeJogador: String) {
        jogadorAtual = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador)
        chaveJogador = jogadorAtual.chaveSala
        saidaManual = false
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.UM_CONTRA_UM, codigoSala, jogadorAtual)
            .addOnSuccessListener { jogadorNaSala ->
                chaveJogador = jogadorNaSala.chave
                jogoCompetitivoRepository.marcarPronto1x1(codigoSala, chaveJogador)
                atualizarAdmin(codigoSala)
                publicarEstado()
            }
    }

    fun observarJogadores(codigoSala: String) {
        removerJogadoresListener()
        jogadoresListener = jogoCompetitivoRepository.escutarJogadores(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            onJogadoresAlterados = { jogadores ->
                jogadoresNaSala = jogadores
                atualizarAdmin(codigoSala)
                publicarEstado()
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
                if (!saidaManual && !existe) {
                    _evento.value = Sala1x1Event.SalaEncerrada
                }
            }
        )
    }

    fun verificarProntosEAvancar(codigoSala: String) {
        if (!(admin && jogadoresNaSala.size == 2)) {
            _evento.value = Sala1x1Event.AguardarAdversario
            return
        }

        jogoCompetitivoRepository.obterProntos1x1(codigoSala)
            .addOnSuccessListener { prontos ->
                if (prontos.size == 2 && jogadoresNaSala.size == 2) {
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

    fun sairDaSala(codigoSala: String) {
        saidaManual = true
        if (admin) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
        } else {
            jogoCompetitivoRepository.removerJogador1x1(codigoSala, jogadorAtual, chaveJogador)
        }
    }

    fun removerListeners() {
        removerJogadoresListener()
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
        _estado.value = SalaCompetitivaUiState(
            jogadores = jogadoresNaSala.map { it.nomeDisplay },
            admin = admin,
            podeIniciar = admin && jogadoresNaSala.size == 2
        )
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

    private fun removerEstadoListener() {
        jogoCompetitivoRepository.removerListener(estadoListener)
        estadoListener = null
    }

    private fun removerSalaListener() {
        jogoCompetitivoRepository.removerListener(salaListener)
        salaListener = null
    }
}

data class SalaCompetitivaUiState(
    val jogadores: List<String>,
    val admin: Boolean,
    val podeIniciar: Boolean
)

sealed class Sala1x1Event {
    data object JogoIniciado : Sala1x1Event()
    data object SalaEncerrada : Sala1x1Event()
    data object AguardarAdversario : Sala1x1Event()
    data object JogadoresNaoProntos : Sala1x1Event()
}
