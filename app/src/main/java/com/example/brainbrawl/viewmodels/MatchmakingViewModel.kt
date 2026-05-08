package com.example.brainbrawl.viewmodels

import android.util.Log
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
    private var aCriarMatch = false

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

        val nomeGuest = nomeJogador.ifBlank { nomeUtilizador.ifBlank { "Convidado" } }
        if (nomeGuest.isBlank()) {
            _estado.value = _estado.value?.copy(estadoTexto = "Nome de convidado inválido.")
            _evento.value = MatchmakingEvent.MostrarMensagem("Nome de convidado inválido.")
            return
        }
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
        if (jogador.playerKey.isBlank()) {
            Log.e(TAG, "PlayerKey vazio ao entrar em matchmaking: modo=$modo tipo=${jogador.tipoJogador}")
            _estado.value = _estado.value?.copy(estadoTexto = "Não foi possível identificar o jogador.")
            _evento.value = MatchmakingEvent.MostrarMensagem("Não foi possível identificar o jogador.")
            return
        }
        jogadorAtual = jogador
        _estado.value = MatchmakingUiState(
            modo = modo,
            limite = limiteJogadores(),
            jogadores = listOf(jogador),
            estadoTexto = "À procura de jogadores..."
        )

        matchmakingRepository.entrarNaFila(jogador, modo)
            .addOnSuccessListener {
                Log.d(TAG, "Entrou na fila: modo=$modo playerKey=${jogador.playerKey} tipo=${jogador.tipoJogador}")
                observarFila()
                observarResultado()
            }
            .addOnFailureListener { erro ->
                Log.e(TAG, "Erro ao entrar na fila: modo=$modo playerKey=${jogador.playerKey} tipo=${jogador.tipoJogador}", erro)
                val mensagem = mensagemErroEntradaFila(erro)
                _estado.value = _estado.value?.copy(estadoTexto = mensagem)
                _evento.value = MatchmakingEvent.MostrarMensagem(mensagem)
            }
    }

    private fun observarFila() {
        matchmakingRepository.removerListener(filaListener)
        filaListener = matchmakingRepository.observarFila(
            modo = modo,
            onFila = { jogadores ->
                val jogador = jogadorAtual
                val jogadorEmCriacao = jogador != null &&
                    jogadores.any {
                        it.playerKey == jogador.playerKey && it.estado == GameConstants.ESTADO_ENCONTRADO
                    }
                val jogadoresAguardando = jogadores.filter { it.estado == GameConstants.ESTADO_AGUARDANDO }
                Log.d(TAG, "Fila observada: modo=$modo total=${jogadores.size} aguardando=${jogadoresAguardando.size}")
                _estado.value = MatchmakingUiState(
                    modo = modo,
                    limite = limiteJogadores(),
                    jogadores = jogadores,
                    estadoTexto = if (jogadorEmCriacao || aCriarMatch) "A criar sala..." else "À procura de jogadores..."
                )
                tentarCriarMatchSePossivel(jogadoresAguardando)
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
        if (navegacaoIniciada || aCancelar || aCriarMatch || jogadores.size < limiteJogadores()) return

        aCriarMatch = true
        _estado.value = _estado.value?.copy(estadoTexto = "A criar sala...")
        Log.d(TAG, "A tentar criar match: modo=$modo criador=${jogador.playerKey} jogadores=${jogadores.size}")
        matchmakingRepository.tentarCriarMatch(modo, nomeCategoria, jogador.playerKey)
            .addOnCompleteListener {
                aCriarMatch = false
            }
            .addOnSuccessListener { match ->
                if (match == null && !navegacaoIniciada) {
                    _estado.value = _estado.value?.copy(estadoTexto = "À procura de jogadores...")
                }
            }
            .addOnFailureListener { erro ->
                Log.e(TAG, "Erro ao criar sala/match: modo=$modo criador=${jogador.playerKey}", erro)
                _estado.value = _estado.value?.copy(estadoTexto = "Erro ao criar sala. Pode cancelar e tentar novamente.")
                _evento.value = MatchmakingEvent.MostrarMensagem("Erro ao criar sala.")
            }
    }

    private fun abrirSala(resultado: MatchmakingResult) {
        if (navegacaoIniciada) return
        val jogador = jogadorAtual ?: return

        navegacaoIniciada = true
        Log.d(TAG, "Resultado recebido: modo=${resultado.modo} sala=${resultado.codigoSala} playerKey=${jogador.playerKey}")
        matchmakingRepository.verificarJogadorNaSala(resultado.modo, resultado.codigoSala, jogador.playerKey)
            .addOnSuccessListener { jogadorNaSala ->
                if (!jogadorNaSala) {
                    Log.w(
                        TAG,
                        "Resultado antigo/invalido ignorado: modo=${resultado.modo} " +
                            "sala=${resultado.codigoSala} playerKey=${jogador.playerKey}"
                    )
                    matchmakingRepository.consumirResultado(modo, jogador.playerKey)
                    matchmakingRepository.cancelarOnDisconnect(jogador.playerKey, modo)
                    navegacaoIniciada = false
                    _evento.value = MatchmakingEvent.MostrarMensagem("Resultado antigo ou sala cheia ignorado.")
                    return@addOnSuccessListener
                }

                removerListeners()
                matchmakingRepository.cancelarOnDisconnect(jogador.playerKey, modo)
                matchmakingRepository.consumirResultado(modo, jogador.playerKey)
                Log.d(TAG, "A navegar para sala: modo=${resultado.modo} sala=${resultado.codigoSala} playerKey=${jogador.playerKey}")
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
            .addOnFailureListener { erro ->
                Log.e(TAG, "Erro ao confirmar sala antes de navegar: modo=${resultado.modo} sala=${resultado.codigoSala}", erro)
                navegacaoIniciada = false
                _evento.value = MatchmakingEvent.MostrarMensagem("Erro ao confirmar sala.")
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

    private fun mensagemErroEntradaFila(erro: Exception): String {
        val texto = erro.message.orEmpty()
        return when {
            jogadorAtual?.playerKey.isNullOrBlank() -> "Não foi possível identificar o jogador."
            "permission" in texto.lowercase() || "denied" in texto.lowercase() ->
                "Sem permissão para entrar na fila. Verifica as regras Firebase."
            else -> "Erro ao entrar na fila."
        }
    }

    private companion object {
        const val AVATAR_PADRAO = "avatar_1_playstore"
        const val TAG = "MatchmakingVM"
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
