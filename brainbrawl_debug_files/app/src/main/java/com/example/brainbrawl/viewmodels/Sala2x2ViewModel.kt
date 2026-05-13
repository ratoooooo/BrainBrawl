package com.example.brainbrawl.viewmodels

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
    private var estadoListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var salaListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var jogadoresNaSala: List<JogoCompetitivoRepository.JogadorCompetitivo> = emptyList()
    private var admin = false
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()
    private var chaveJogador = ""
    private var saidaManual = false
    private var aIniciarJogo = false
    private var salaConfirmada = false

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
        saidaManual = false
        aIniciarJogo = false
        salaConfirmada = false
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala, jogadorAtual)
            .addOnSuccessListener { jogadorNaSala ->
                chaveJogador = jogadorNaSala.chave
                atualizarAdminEPublicar(codigoSala)
            }
    }

    fun observarJogadores(codigoSala: String) {
        removerJogadoresListener()
        jogadoresListener = jogoCompetitivoRepository.escutarJogadores(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onJogadoresAlterados = { jogadores ->
                jogadoresNaSala = jogadores
                atualizarAdminEPublicar(codigoSala)
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
        val jogadores = jogadoresUnicos()
        if (!admin || jogadores.size != 4 || aIniciarJogo) return
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
        if (admin) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
        } else {
            jogoCompetitivoRepository.removerJogador2x2(codigoSala, jogadorAtual, chaveJogador)
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
        _estado.value = Sala2x2UiState(
            equipaA = equipaA.map { it.nomeDisplay },
            equipaB = equipaB.map { it.nomeDisplay },
            podeIniciar = admin && salaCompleta && !aIniciarJogo
        )
    }

    private fun jogadoresUnicos(): List<JogoCompetitivoRepository.JogadorCompetitivo> {
        return jogadoresNaSala
            .filterNot { it.chave == GameConstants.JOGADOR_ADMIN }
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
    data object ErroIniciarJogo : Sala2x2Event()
}
