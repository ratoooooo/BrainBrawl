package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.repositories.JogoRepository

class EsperaEliminadoViewModel(
    private val jogoRepository: JogoRepository = JogoRepository()
) : ViewModel() {

    private val _evento = MutableLiveData<EsperaEliminadoEvent?>()
    val evento: LiveData<EsperaEliminadoEvent?> = _evento
    private val _ranking = MutableLiveData<List<RankingParcialEliminadoUi>>(emptyList())
    val ranking: LiveData<List<RankingParcialEliminadoUi>> = _ranking

    private var estadoListener: JogoRepository.ListenerHandle? = null
    private var jogadoresListener: JogoRepository.ListenerHandle? = null

    fun escutarFimJogo(codigoSala: String) {
        if (codigoSala.isBlank()) {
            _evento.value = EsperaEliminadoEvent.DadosInvalidos
            return
        }

        removerListener()
        estadoListener = jogoRepository.escutarEstadoSala(
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_TERMINADO) {
                    _evento.value = EsperaEliminadoEvent.JogoTerminado
                }
            },
            onErro = {
                _evento.value = EsperaEliminadoEvent.ErroAguardarFim
            }
        )
        jogadoresListener = jogoRepository.escutarJogadoresEliminatorias(
            codigoSala = codigoSala,
            onJogadoresAlterados = { jogadores ->
                val rankingParcial = jogadores
                    .filter { jogador ->
                        jogador.chave != GameConstants.JOGADOR_ADMIN &&
                            jogador.nome != GameConstants.JOGADOR_ADMIN
                    }
                    .sortedWith(compareBy<JogoRepository.JogadorEliminatorias> {
                        it.estado == GameConstants.ESTADO_ELIMINADO
                    }.thenByDescending { it.pontos })
                    .mapIndexed { index, jogador ->
                        val estadoTexto = when (jogador.estado) {
                            GameConstants.ESTADO_ELIMINADO -> "Eliminado"
                            GameConstants.ESTADO_TERMINADO -> "Terminou"
                            else -> "Em jogo"
                        }
                        val temProgresso = jogador.estado == GameConstants.ESTADO_ELIMINADO ||
                            jogador.estado == GameConstants.ESTADO_TERMINADO ||
                            jogador.pontos > 0.0 ||
                            jogador.respostasCertas > 0 ||
                            jogador.perguntasRespondidas > 0
                        RankingParcialEliminadoUi(
                            posicao = index + 1,
                            nome = jogador.nome,
                            avatar = jogador.avatar,
                            estado = estadoTexto,
                            detalhe = if (temProgresso) {
                                "${jogador.pontos.toInt()} pts • ${jogador.respostasCertas} certas"
                            } else {
                                "Em jogo"
                            },
                            ativo = jogador.estado != GameConstants.ESTADO_ELIMINADO,
                            destaque = index < 3
                        )
                    }
                _ranking.value = rankingParcial
            },
            onErro = {
                _ranking.value = emptyList()
            }
        )
    }

    fun removerListener() {
        jogoRepository.removerListener(estadoListener)
        estadoListener = null
        jogoRepository.removerListener(jogadoresListener)
        jogadoresListener = null
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListener()
        super.onCleared()
    }
}

sealed class EsperaEliminadoEvent {
    data object DadosInvalidos : EsperaEliminadoEvent()
    data object ErroAguardarFim : EsperaEliminadoEvent()
    data object JogoTerminado : EsperaEliminadoEvent()
}

data class RankingParcialEliminadoUi(
    val posicao: Int,
    val nome: String,
    val avatar: String,
    val estado: String,
    val detalhe: String,
    val ativo: Boolean,
    val destaque: Boolean
)
