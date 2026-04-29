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

    private var estadoListener: JogoRepository.ListenerHandle? = null

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
    }

    fun removerListener() {
        jogoRepository.removerListener(estadoListener)
        estadoListener = null
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
