package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
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
    private var estadoListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var salaListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var jogadoresNaSala: List<String> = emptyList()
    private var admin = false
    private var nomeUtilizador = ""
    private var saidaManual = false

    fun iniciar(codigoSala: String, nomeUtilizador: String) {
        this.nomeUtilizador = nomeUtilizador
        saidaManual = false
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala, nomeUtilizador)
        jogoCompetitivoRepository.obterAdmin(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .addOnSuccessListener { nomeAdmin ->
                admin = if (nomeAdmin.isNullOrBlank()) {
                    jogadoresNaSala.firstOrNull() == nomeUtilizador
                } else {
                    nomeAdmin == nomeUtilizador
                }
                publicarEstadoEGuardarEquipas(codigoSala)
            }
    }

    fun observarJogadores(codigoSala: String) {
        removerJogadoresListener()
        jogadoresListener = jogoCompetitivoRepository.escutarJogadores(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onJogadoresAlterados = { nomes ->
                jogadoresNaSala = nomes
                publicarEstadoEGuardarEquipas(codigoSala)
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
                if (!saidaManual && !existe) {
                    _evento.value = Sala2x2Event.SalaEncerrada
                }
            }
        )
    }

    fun iniciarJogo(codigoSala: String) {
        if (admin && jogadoresNaSala.size == 4) {
            jogoCompetitivoRepository.atualizarEstadoSala(
                ModoCompetitivo.DOIS_CONTRA_DOIS,
                codigoSala,
                GameConstants.ESTADO_EM_JOGO
            )
        }
    }

    fun sairDaSala(codigoSala: String) {
        saidaManual = true
        if (admin) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
        } else {
            jogoCompetitivoRepository.removerJogador2x2(codigoSala, nomeUtilizador)
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

    private fun publicarEstadoEGuardarEquipas(codigoSala: String) {
        val equipaA = jogadoresNaSala.take(2)
        val equipaB = jogadoresNaSala.drop(2).take(2)
        val salaCompleta = jogadoresNaSala.size == 4
        _estado.value = Sala2x2UiState(
            equipaA = equipaA,
            equipaB = equipaB,
            podeIniciar = admin && salaCompleta
        )

        if (admin && salaCompleta) {
            jogoCompetitivoRepository.guardarEquipas2x2(codigoSala, equipaA, equipaB)
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

data class Sala2x2UiState(
    val equipaA: List<String>,
    val equipaB: List<String>,
    val podeIniciar: Boolean
)

sealed class Sala2x2Event {
    data object JogoIniciado : Sala2x2Event()
    data object SalaEncerrada : Sala2x2Event()
}
