package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.PontuacaoRepository

class Pontuacao1x1ViewModel(
    private val pontuacaoRepository: PontuacaoRepository = PontuacaoRepository()
) : ViewModel() {

    private val _estadoDesforra = MutableLiveData<Pontuacao1x1DesforraUiState>()
    val estadoDesforra: LiveData<Pontuacao1x1DesforraUiState> = _estadoDesforra

    private val _evento = MutableLiveData<Pontuacao1x1Event?>()
    val evento: LiveData<Pontuacao1x1Event?> = _evento

    private var codigoSala: String = ""
    private var nomeCategoria: String = ""
    private var jogadorAtual: PontuacaoRepository.JogadorDesforra? = null
    private var novaSalaListener: PontuacaoRepository.ListenerHandle? = null
    private var desforraListener: PontuacaoRepository.ListenerHandle? = null
    private var aCriarDesforra = false
    private var navegacaoEmitida = false

    fun iniciar(codigoSala: String, nomeCategoria: String) {
        if (this.codigoSala == codigoSala) return
        this.codigoSala = codigoSala
        this.nomeCategoria = nomeCategoria
        _estadoDesforra.value = Pontuacao1x1DesforraUiState()
        observarNovaSala()
    }

    fun atualizarJogadorAtual(jogador: PontuacaoRepository.JogadorDesforra) {
        val mudou = jogadorAtual?.chave != jogador.chave
        jogadorAtual = jogador
        if (mudou) {
            observarDesforras()
        }
    }

    fun pedirDesforra() {
        val jogador = jogadorAtual ?: run {
            _evento.value = Pontuacao1x1Event.MostrarMensagem("Ainda a carregar dados da partida.")
            return
        }
        _estadoDesforra.value = Pontuacao1x1DesforraUiState("A aguardar adversário...", desforraPedida = true)
        pontuacaoRepository.marcarDesforra1x1(codigoSala, jogador.chave)
            .addOnFailureListener {
                _estadoDesforra.value = Pontuacao1x1DesforraUiState()
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao pedir desforra.")
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }

    fun removerListeners() {
        pontuacaoRepository.removerListener(novaSalaListener)
        novaSalaListener = null
        pontuacaoRepository.removerListener(desforraListener)
        desforraListener = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun observarNovaSala() {
        pontuacaoRepository.removerListener(novaSalaListener)
        novaSalaListener = pontuacaoRepository.escutarNovaSalaDesforra1x1(
            codigoSala = codigoSala,
            onNovaSala = { novaSala ->
                emitirAbrirSala(novaSala)
            },
            onErro = {
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao observar desforra.")
            }
        )
    }

    private fun observarDesforras() {
        val jogador = jogadorAtual ?: return
        pontuacaoRepository.removerListener(desforraListener)
        desforraListener = pontuacaoRepository.escutarDesforra1x1(
            codigoSala = codigoSala,
            chaveJogadorAtual = jogador.chave,
            onAdversarioAceitou = { adversario ->
                criarDesforra(jogador, adversario)
            },
            onAguardar = {
                _estadoDesforra.value = Pontuacao1x1DesforraUiState("A aguardar adversário...", desforraPedida = true)
            },
            onErro = {
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao observar pedidos de desforra.")
            }
        )
    }

    private fun criarDesforra(
        jogador: PontuacaoRepository.JogadorDesforra,
        adversario: PontuacaoRepository.JogadorDesforra
    ) {
        if (aCriarDesforra || navegacaoEmitida) return
        aCriarDesforra = true
        _estadoDesforra.value = Pontuacao1x1DesforraUiState("Desforra aceite. A criar nova sala...", desforraPedida = true)
        pontuacaoRepository.criarOuObterSalaDesforra1x1(codigoSala, jogador, adversario, nomeCategoria)
            .addOnSuccessListener { novaSala ->
                aCriarDesforra = false
                emitirAbrirSala(novaSala)
            }
            .addOnFailureListener {
                aCriarDesforra = false
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao criar desforra.")
            }
    }

    private fun emitirAbrirSala(codigoNovaSala: String) {
        if (navegacaoEmitida) return
        navegacaoEmitida = true
        _evento.value = Pontuacao1x1Event.AbrirNovaSalaDesforra(codigoNovaSala)
    }
}

data class Pontuacao1x1DesforraUiState(
    val mensagem: String = "",
    val desforraPedida: Boolean = false
)

sealed class Pontuacao1x1Event {
    data class AbrirNovaSalaDesforra(val codigoSala: String) : Pontuacao1x1Event()
    data class MostrarMensagem(val mensagem: String) : Pontuacao1x1Event()
}
