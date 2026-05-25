package com.example.brainbrawl.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.repositories.SalaRepository
import com.example.brainbrawl.utils.CodigoSalaUtils
import com.example.brainbrawl.utils.UteisValidacao

class SalaGrupoViewModel(
    private val salaRepository: SalaRepository = SalaRepository(),
    private val jogadorRepository: JogadorRepository = JogadorRepository()
) : ViewModel() {

    private val _entrada = MutableLiveData<SalaEntradaEvent?>()
    val entrada: LiveData<SalaEntradaEvent?> = _entrada

    private val _jogadores = MutableLiveData<SalaGrupoJogadoresUiState>()
    val jogadores: LiveData<SalaGrupoJogadoresUiState> = _jogadores

    private val _evento = MutableLiveData<SalaGrupoEvent?>()
    val evento: LiveData<SalaGrupoEvent?> = _evento

    private var jogadoresListener: SalaRepository.ListenerHandle? = null
    private var estadoListener: SalaRepository.ListenerHandle? = null
    private var salaListener: SalaRepository.ListenerHandle? = null
    private var saidaManual = false
    private var nomeAtual: String = ""
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()
    private var admin = false
    private var salaConfirmada = false

    fun criarSala(codigoSala: String, dadosSala: Map<String, Any>) {
        salaRepository.criarSala(codigoSala, dadosSala)
    }

    fun entrarEmSala(codigoSala: String, nomeJogador: String, uid: String, nomeUtilizador: String?) {
        val codigoNormalizado = CodigoSalaUtils.normalizarCodigo(codigoSala)
        if (codigoNormalizado.isEmpty()) {
            _entrada.value = SalaEntradaEvent.CodigoVazio
            return
        }
        if (!CodigoSalaUtils.codigoTemCaracteresValidos(codigoNormalizado)) {
            _entrada.value = SalaEntradaEvent.CodigoInvalido
            return
        }

        val erro = UteisValidacao.validarCampos(nomeJogador)
        if (erro != null) {
            _entrada.value = SalaEntradaEvent.ValidacaoFalhou(erro)
            return
        }

        val jogador = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador)
        salaRepository.procurarSalaPorCodigo(codigoNormalizado, jogador)
            .addOnSuccessListener { resultado ->
                if (!resultado.existe) {
                    _entrada.value = SalaEntradaEvent.CodigoInvalido
                    return@addOnSuccessListener
                }

                if (resultado.jogadorJaExiste) {
                    _entrada.value = SalaEntradaEvent.NomeJaExiste
                    return@addOnSuccessListener
                }

                if (!nomeUtilizador.isNullOrEmpty()) {
                    jogadorRepository.obterAvatar(uid.ifBlank { nomeUtilizador })
                        .addOnSuccessListener { avatar ->
                            adicionarJogadorComAvatar(jogador, codigoNormalizado, avatar, nomeUtilizador)
                        }
                        .addOnFailureListener {
                            adicionarJogadorComAvatar(jogador, codigoNormalizado, AVATAR_PADRAO, nomeUtilizador)
                        }
                } else {
                    adicionarJogadorComAvatar(jogador, codigoNormalizado, AVATAR_PADRAO, null)
                }
            }
            .addOnFailureListener { error ->
                _entrada.value = SalaEntradaEvent.ErroVerificarSala(error.message.orEmpty())
            }
    }

    fun iniciarSala(codigoSala: String, jogador: JogadorSalaIdentidade, admin: Boolean) {
        this.jogadorAtual = jogador
        this.nomeAtual = jogador.nomeDisplay
        this.admin = admin
        saidaManual = false
        salaConfirmada = false
        salaRepository.garantirJogadorNaSala(codigoSala, jogador, admin)
    }

    fun observarJogadores(codigoSala: String) {
        removerJogadoresListener()
        jogadoresListener = salaRepository.escutarJogadoresDaSala(
            codigoSala,
            onJogadoresAlterados = { jogadoresSala ->
                val nomes = jogadoresSala.map { it.nome }
                val participantes = participantesAtivos(jogadoresSala)
                val emFalta = (MINIMO_JOGADORES_GRUPO - participantes.size).coerceAtLeast(0)
                val podeIniciar = admin && emFalta == 0
                _jogadores.value = SalaGrupoJogadoresUiState(
                    nomes = nomes,
                    jogadores = participantes.map { jogador ->
                        Log.d(
                            WAITING_ROOM_AVATAR_TAG,
                            "bind roomGroup playerKey=${jogador.chave.maskedLogId()} uid=${jogador.uid.maskedLogId()} " +
                                "username=${jogador.nome} avatar=${jogador.avatar.ifBlank { "<empty>" }} " +
                                "source=salas/$codigoSala/jogadores/${jogador.chave}/avatar"
                        )
                        SalaGrupoJogadorUiState(
                            nome = jogador.nome,
                            avatar = jogador.avatar.ifBlank { AVATAR_PADRAO },
                            estado = jogador.estado
                        )
                    },
                    podeIniciar = podeIniciar,
                    jogadoresMinimosAtuais = participantes.size,
                    jogadoresMinimosNecessarios = MINIMO_JOGADORES_GRUPO,
                    jogadoresEmFalta = emFalta
                )
            },
            onErro = {
                _evento.value = SalaGrupoEvent.ErroCarregarJogadores
            }
        )
    }

    fun observarEstadoSala(codigoSala: String) {
        removerEstadoListener()
        estadoListener = salaRepository.escutarEstadoDaSala(
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_EM_JOGO) {
                    _evento.value = SalaGrupoEvent.JogoIniciado
                }
            },
            onErro = {
                _evento.value = SalaGrupoEvent.ErroEscutarEstado
            }
        )
    }

    fun observarSalaApagada(codigoSala: String) {
        removerSalaListener()
        salaListener = salaRepository.escutarSalaApagada(
            codigoSala,
            onSalaExisteAlterada = { existe ->
                if (existe) {
                    salaConfirmada = true
                } else if (salaConfirmada && !saidaManual) {
                    _evento.value = SalaGrupoEvent.SalaEncerrada
                }
            }
        )
    }

    fun validarEIniciarJogo(codigoSala: String) {
        salaRepository.obterJogadoresDaSala(codigoSala)
            .addOnSuccessListener { jogadores ->
                if (participantesAtivos(jogadores).size < MINIMO_JOGADORES_GRUPO) {
                    _evento.value = SalaGrupoEvent.JogadoresInsuficientes
                    return@addOnSuccessListener
                }
                salaRepository.atualizarEstadoSala(codigoSala, GameConstants.ESTADO_EM_JOGO)
            }
            .addOnFailureListener {
                _evento.value = SalaGrupoEvent.ErroValidarJogadores
            }
    }

    fun sairDaSala(codigoSala: String, jogador: JogadorSalaIdentidade, admin: Boolean) {
        saidaManual = true
        if (admin) {
            salaRepository.apagarSala(codigoSala)
        } else {
            salaRepository.removerJogadorDaSala(codigoSala, jogador)
        }
    }

    fun removerListeners() {
        removerJogadoresListener()
        removerEstadoListener()
        removerSalaListener()
    }

    fun consumirEntrada() {
        _entrada.value = null
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun adicionarJogadorComAvatar(
        jogador: JogadorSalaIdentidade,
        codigoSala: String,
        avatar: String,
        nomeUtilizador: String?
    ) {
        val jogadorData = jogador.toFirebaseMap(isHostOnly = false, avatar = avatar)
        salaRepository.adicionarJogadorASala(codigoSala, jogador, jogadorData)
        _entrada.value = SalaEntradaEvent.JogadorAdicionado(
            codigoSala = codigoSala,
            nomeJogador = jogador.nomeDisplay,
            uid = jogador.uid,
            nomeUtilizador = nomeUtilizador
        )
    }

    private fun participantesAtivos(jogadores: List<SalaRepository.JogadorSala>): List<SalaRepository.JogadorSala> {
        return jogadores
            .filter { jogador ->
                val ePlaceholderAdmin = jogador.chave == GameConstants.JOGADOR_ADMIN &&
                    jogador.nome == GameConstants.JOGADOR_ADMIN
                !ePlaceholderAdmin && jogador.estado != GameConstants.ESTADO_OFF && !jogador.isHostOnly
            }
            .distinctBy { jogador ->
                jogador.uid
                    .ifBlank { jogador.nomeUtilizador }
                    .ifBlank { jogador.nomeJogador }
                    .ifBlank { jogador.chave }
            }
    }

    private fun removerJogadoresListener() {
        salaRepository.removerListener(jogadoresListener)
        jogadoresListener = null
    }

    private fun removerEstadoListener() {
        salaRepository.removerListener(estadoListener)
        estadoListener = null
    }

    private fun removerSalaListener() {
        salaRepository.removerListener(salaListener)
        salaListener = null
    }

    private companion object {
        const val AVATAR_PADRAO = "avatar_1_playstore"
        const val MINIMO_JOGADORES_GRUPO = 2
        const val WAITING_ROOM_AVATAR_TAG = "WaitingRoomAvatar"
    }
}

data class SalaGrupoJogadoresUiState(
    val nomes: List<String>,
    val jogadores: List<SalaGrupoJogadorUiState> = emptyList(),
    val podeIniciar: Boolean,
    val jogadoresMinimosAtuais: Int = 0,
    val jogadoresMinimosNecessarios: Int = 2,
    val jogadoresEmFalta: Int = 2
)

data class SalaGrupoJogadorUiState(
    val nome: String,
    val avatar: String,
    val estado: String
)

sealed class SalaEntradaEvent {
    data object CodigoVazio : SalaEntradaEvent()
    data object CodigoInvalido : SalaEntradaEvent()
    data object NomeJaExiste : SalaEntradaEvent()
    data class ValidacaoFalhou(val mensagem: String) : SalaEntradaEvent()
    data class ErroVerificarSala(val mensagem: String) : SalaEntradaEvent()
    data class JogadorAdicionado(
        val codigoSala: String,
        val nomeJogador: String,
        val uid: String,
        val nomeUtilizador: String?
    ) : SalaEntradaEvent()
}

sealed class SalaGrupoEvent {
    data object ErroCarregarJogadores : SalaGrupoEvent()
    data object ErroEscutarEstado : SalaGrupoEvent()
    data object ErroValidarJogadores : SalaGrupoEvent()
    data object JogadoresInsuficientes : SalaGrupoEvent()
    data object JogoIniciado : SalaGrupoEvent()
    data object SalaEncerrada : SalaGrupoEvent()
}

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
