package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.repositories.SalaRepository
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

    fun criarSala(codigoSala: String, dadosSala: Map<String, Any>) {
        salaRepository.criarSala(codigoSala, dadosSala)
    }

    fun entrarEmSala(codigoSala: String, nomeJogador: String, uid: String, nomeUtilizador: String?) {
        if (codigoSala.isEmpty()) {
            _entrada.value = SalaEntradaEvent.CodigoVazio
            return
        }

        val erro = UteisValidacao.validarCampos(nomeJogador)
        if (erro != null) {
            _entrada.value = SalaEntradaEvent.ValidacaoFalhou(erro)
            return
        }

        val jogador = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador)
        salaRepository.procurarSalaPorCodigo(codigoSala, jogador)
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
                            adicionarJogadorComAvatar(jogador, codigoSala, avatar, nomeUtilizador)
                        }
                        .addOnFailureListener {
                            adicionarJogadorComAvatar(jogador, codigoSala, AVATAR_PADRAO, nomeUtilizador)
                        }
                } else {
                    adicionarJogadorComAvatar(jogador, codigoSala, AVATAR_PADRAO, null)
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
        salaRepository.garantirJogadorNaSala(codigoSala, jogador, admin)
    }

    fun observarJogadores(codigoSala: String) {
        removerJogadoresListener()
        jogadoresListener = salaRepository.escutarJogadoresDaSala(
            codigoSala,
            onJogadoresAlterados = { jogadoresSala ->
                val nomes = jogadoresSala.map { it.nome }
                val jogadoresInfo = jogadoresSala.associate { it.nome to it.isHostOnly }
                val podeIniciar = admin && jogadoresReais(nomes, jogadoresInfo).size >= MINIMO_JOGADORES_GRUPO
                _jogadores.value = SalaGrupoJogadoresUiState(nomes, podeIniciar)
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
                if (!saidaManual && !existe) {
                    _evento.value = SalaGrupoEvent.SalaEncerrada
                }
            }
        )
    }

    fun validarEIniciarJogo(codigoSala: String) {
        salaRepository.obterJogadoresDaSala(codigoSala)
            .addOnSuccessListener { jogadores ->
                if (jogadoresReais(jogadores.map { it.nome }, jogadores.associate { it.nome to it.isHostOnly }).size < MINIMO_JOGADORES_GRUPO) {
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

    private fun jogadoresReais(nomes: List<String>, jogadoresInfo: Map<String, Boolean>): List<String> {
        return nomes.filter { jogador ->
            jogador != nomeAtual && jogador != GameConstants.JOGADOR_ADMIN && jogadoresInfo[jogador] != true
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
        const val MINIMO_JOGADORES_GRUPO = 1
    }
}

data class SalaGrupoJogadoresUiState(
    val nomes: List<String>,
    val podeIniciar: Boolean
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
