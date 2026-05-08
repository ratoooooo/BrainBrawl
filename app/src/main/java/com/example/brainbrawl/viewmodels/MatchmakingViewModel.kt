package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.MatchmakingPlayer
import com.example.brainbrawl.models.MatchmakingResult
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.repositories.MatchmakingRepository

class MatchmakingViewModel(
    private val matchmakingRepository: MatchmakingRepository = MatchmakingRepository(),
    private val jogadorRepository: JogadorRepository = JogadorRepository()
) : ViewModel() {

    private val _estado = MutableLiveData<MatchmakingUiState>()
    val estado: LiveData<MatchmakingUiState> = _estado

    private val _evento = MutableLiveData<MatchmakingEvent?>()
    val evento: LiveData<MatchmakingEvent?> = _evento

    private var filaListener: MatchmakingRepository.ListenerHandle? = null
    private var resultadoListener: MatchmakingRepository.ListenerHandle? = null
    private var jogadorAtual: MatchmakingPlayer? = null
    private var modo: String = GameConstants.MODO_1X1
    private var nomeCategoria: String = ""
    private var navegacaoIniciada = false
    private var aCancelar = false

    fun iniciar(
        uid: String,
        nomeUtilizador: String?,
        nomeJogador: String?,
        modoJogo: String,
        nomeCategoria: String
    ) {
        if (jogadorAtual != null) return

        modo = if (modoJogo == GameConstants.MODO_2X2) GameConstants.MODO_2X2 else GameConstants.MODO_1X1
        this.nomeCategoria = nomeCategoria
        _estado.value = MatchmakingUiState(
            modo = modo,
            limite = limiteJogadores(),
            estadoTexto = "A preparar matchmaking..."
        )

        prepararIdentidade(uid, nomeUtilizador.orEmpty(), nomeJogador.orEmpty())
    }

    fun cancelar() {
        val jogador = jogadorAtual
        if (aCancelar || navegacaoIniciada) {
            return
        }
        if (jogador == null) {
            _evento.value = MatchmakingEvent.VoltarMain(dadosNavegacao())
            return
        }

        aCancelar = true
        matchmakingRepository.cancelar(jogador.playerKey, modo)
            .addOnSuccessListener { removeuFila ->
                aCancelar = false
                if (removeuFila) {
                    _evento.value = MatchmakingEvent.VoltarMain(dadosNavegacao())
                } else {
                    _evento.value = MatchmakingEvent.MostrarMensagem("Partida já encontrada.")
                }
            }
            .addOnFailureListener {
                aCancelar = false
                _evento.value = MatchmakingEvent.MostrarMensagem("Erro ao cancelar matchmaking.")
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }

    fun removerListeners() {
        matchmakingRepository.removerListener(filaListener)
        filaListener = null
        matchmakingRepository.removerListener(resultadoListener)
        resultadoListener = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun prepararIdentidade(uid: String, nomeUtilizador: String, nomeJogador: String) {
        if (uid.isNotBlank()) {
            val nomeDisplay = nomeUtilizador.ifBlank { nomeJogador.ifBlank { uid } }
            jogadorRepository.obterAvatar(uid)
                .addOnSuccessListener { avatar ->
                    entrarComJogador(
                        MatchmakingPlayer(
                            playerKey = uid,
                            uid = uid,
                            tipoJogador = GameConstants.TIPO_JOGADOR_AUTH,
                            nomeUtilizador = nomeUtilizador,
                            nomeJogador = nomeJogador,
                            nomeDisplay = nomeDisplay,
                            avatar = avatar,
                            timestampEntrada = System.currentTimeMillis()
                        )
                    )
                }
                .addOnFailureListener {
                    entrarComJogador(
                        MatchmakingPlayer(
                            playerKey = uid,
                            uid = uid,
                            tipoJogador = GameConstants.TIPO_JOGADOR_AUTH,
                            nomeUtilizador = nomeUtilizador,
                            nomeJogador = nomeJogador,
                            nomeDisplay = nomeDisplay,
                            avatar = AVATAR_PADRAO,
                            timestampEntrada = System.currentTimeMillis()
                        )
                    )
                }
            return
        }

        val nomeGuest = nomeJogador.ifBlank { "Convidado" }
        val guestKey = gerarGuestKey(nomeGuest)
        entrarComJogador(
            MatchmakingPlayer(
                playerKey = guestKey,
                uid = "",
                tipoJogador = GameConstants.TIPO_JOGADOR_GUEST,
                nomeUtilizador = "",
                nomeJogador = nomeGuest,
                nomeDisplay = nomeGuest,
                avatar = AVATAR_PADRAO,
                timestampEntrada = System.currentTimeMillis()
            )
        )
    }

    private fun entrarComJogador(jogador: MatchmakingPlayer) {
        jogadorAtual = jogador
        _estado.value = MatchmakingUiState(
            modo = modo,
            limite = limiteJogadores(),
            jogadores = listOf(jogador),
            estadoTexto = "À procura de jogadores..."
        )

        matchmakingRepository.entrarNaFila(jogador, modo)
            .addOnSuccessListener {
                observarFila()
                observarResultado()
            }
            .addOnFailureListener {
                _evento.value = MatchmakingEvent.MostrarMensagem("Erro ao entrar na fila.")
            }
    }

    private fun observarFila() {
        matchmakingRepository.removerListener(filaListener)
        filaListener = matchmakingRepository.observarFila(
            modo = modo,
            onFila = { jogadores ->
                _estado.value = MatchmakingUiState(
                    modo = modo,
                    limite = limiteJogadores(),
                    jogadores = jogadores,
                    estadoTexto = "À procura de jogadores..."
                )
                tentarCriarMatchSePossivel(jogadores)
            },
            onErro = {
                _evento.value = MatchmakingEvent.MostrarMensagem("Erro ao observar fila.")
            }
        )
    }

    private fun observarResultado() {
        val jogador = jogadorAtual ?: return
        matchmakingRepository.removerListener(resultadoListener)
        resultadoListener = matchmakingRepository.observarResultado(
            modo = modo,
            playerKey = jogador.playerKey,
            onResultado = { resultado ->
                abrirSala(resultado)
            },
            onErro = {
                _evento.value = MatchmakingEvent.MostrarMensagem("Erro ao observar resultado.")
            }
        )
    }

    private fun tentarCriarMatchSePossivel(jogadores: List<MatchmakingPlayer>) {
        val jogador = jogadorAtual ?: return
        if (navegacaoIniciada || jogadores.size < limiteJogadores()) return

        matchmakingRepository.tentarCriarMatch(modo, nomeCategoria, jogador.playerKey)
            .addOnFailureListener {
                _evento.value = MatchmakingEvent.MostrarMensagem("Erro ao criar partida.")
            }
    }

    private fun abrirSala(resultado: MatchmakingResult) {
        if (navegacaoIniciada) return
        navegacaoIniciada = true
        removerListeners()
        val jogador = jogadorAtual ?: return
        matchmakingRepository.consumirResultado(modo, jogador.playerKey)
        val dados = MatchmakingNavegacaoDados(
            codigoSala = resultado.codigoSala,
            modo = resultado.modo,
            nomeCategoria = resultado.nomeCategoria,
            uid = jogador.uid,
            nomeUtilizador = jogador.nomeUtilizador,
            nomeJogador = jogador.nomeJogador,
            playerKey = jogador.playerKey,
            tipoJogador = jogador.tipoJogador,
            avatar = jogador.avatar
        )
        _evento.value = if (resultado.modo == GameConstants.MODO_2X2) {
            MatchmakingEvent.AbrirSala2x2(dados)
        } else {
            MatchmakingEvent.AbrirSala1x1(dados)
        }
    }

    private fun dadosNavegacao(): MatchmakingNavegacaoDados {
        val jogador = jogadorAtual
        return MatchmakingNavegacaoDados(
            codigoSala = "",
            modo = modo,
            nomeCategoria = nomeCategoria,
            uid = jogador?.uid.orEmpty(),
            nomeUtilizador = jogador?.nomeUtilizador.orEmpty(),
            nomeJogador = jogador?.nomeJogador.orEmpty(),
            playerKey = jogador?.playerKey.orEmpty(),
            tipoJogador = jogador?.tipoJogador.orEmpty(),
            avatar = jogador?.avatar.orEmpty()
        )
    }

    private fun limiteJogadores(): Int {
        return if (modo == GameConstants.MODO_2X2) 4 else 2
    }

    private fun gerarGuestKey(nome: String): String {
        val sanitizado = nome.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "convidado" }
            .take(24)
        return "guest_${sanitizado}_${System.currentTimeMillis()}"
    }

    private companion object {
        const val AVATAR_PADRAO = "avatar_1_playstore"
    }
}

data class MatchmakingUiState(
    val modo: String = GameConstants.MODO_1X1,
    val limite: Int = 2,
    val jogadores: List<MatchmakingPlayer> = emptyList(),
    val estadoTexto: String = ""
)

data class MatchmakingNavegacaoDados(
    val codigoSala: String,
    val modo: String,
    val nomeCategoria: String,
    val uid: String,
    val nomeUtilizador: String,
    val nomeJogador: String,
    val playerKey: String,
    val tipoJogador: String,
    val avatar: String
)

sealed class MatchmakingEvent {
    data class AbrirSala1x1(val dados: MatchmakingNavegacaoDados) : MatchmakingEvent()
    data class AbrirSala2x2(val dados: MatchmakingNavegacaoDados) : MatchmakingEvent()
    data class VoltarMain(val dados: MatchmakingNavegacaoDados) : MatchmakingEvent()
    data class MostrarMensagem(val mensagem: String) : MatchmakingEvent()
}
