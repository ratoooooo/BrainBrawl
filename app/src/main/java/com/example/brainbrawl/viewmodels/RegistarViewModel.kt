package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.utils.UteisValidacao

class RegistarViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository()
) : ViewModel() {

    private val _evento = MutableLiveData<RegistarEvent?>()
    val evento: LiveData<RegistarEvent?> = _evento

    fun registar(nomeUtilizador: String, password: String, avatarSelecionadoIndex: Int) {
        val erro = UteisValidacao.validarCampos(nomeUtilizador, password)
        if (erro != null) {
            _evento.value = RegistarEvent.ValidacaoFalhou(erro)
            return
        }

        jogadorRepository.verificarJogadorExiste(nomeUtilizador)
            .addOnSuccessListener { existe ->
                if (existe) {
                    _evento.value = RegistarEvent.JogadorJaExiste
                } else {
                    criarJogador(nomeUtilizador, password, avatarSelecionadoIndex)
                }
            }
            .addOnFailureListener { exception ->
                _evento.value = RegistarEvent.ErroVerificarJogador(exception.message.orEmpty())
            }
    }

    private fun criarJogador(nomeUtilizador: String, password: String, avatarSelecionadoIndex: Int) {
        val hashedPassword = UteisValidacao.hashPassword(password)
        val nomeAvatar = "avatar_${avatarSelecionadoIndex + 1}_playstore"
        jogadorRepository.criarJogador(nomeUtilizador, hashedPassword, nomeAvatar)
            .addOnSuccessListener {
                _evento.value = RegistarEvent.RegistoSucesso(nomeUtilizador)
            }
            .addOnFailureListener { exception ->
                _evento.value = RegistarEvent.ErroCriarJogador(exception.message.orEmpty())
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }
}

sealed class RegistarEvent {
    data class ValidacaoFalhou(val mensagem: String) : RegistarEvent()
    data class RegistoSucesso(val nomeUtilizador: String) : RegistarEvent()
    data class ErroVerificarJogador(val mensagem: String) : RegistarEvent()
    data class ErroCriarJogador(val mensagem: String) : RegistarEvent()
    data object JogadorJaExiste : RegistarEvent()
}
