package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.utils.UteisValidacao

class LoginViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository()
) : ViewModel() {

    private val _evento = MutableLiveData<LoginEvent?>()
    val evento: LiveData<LoginEvent?> = _evento

    fun entrar(nomeUtilizador: String, password: String) {
        val erro = UteisValidacao.validarCampos(nomeUtilizador, password)
        if (erro != null) {
            _evento.value = LoginEvent.ValidacaoFalhou(erro)
            return
        }

        jogadorRepository.obterPerfil(nomeUtilizador)
            .addOnSuccessListener { perfil ->
                if (perfil == null) {
                    _evento.value = LoginEvent.JogadorNaoEncontrado
                    return@addOnSuccessListener
                }

                val inputHash = UteisValidacao.hashPassword(password)
                if (perfil.password == inputHash) {
                    jogadorRepository.marcarOnline(nomeUtilizador)
                    _evento.value = LoginEvent.LoginSucesso(nomeUtilizador)
                } else {
                    _evento.value = LoginEvent.SenhaIncorreta
                }
            }
            .addOnFailureListener {
                _evento.value = LoginEvent.ErroBanco
            }
    }

    fun entrarComoConvidado(nomeJogador: String) {
        if (nomeJogador.isEmpty()) {
            _evento.value = LoginEvent.NomeConvidadoVazio
            return
        }
        _evento.value = LoginEvent.ConvidadoSucesso(nomeJogador)
    }

    fun consumirEvento() {
        _evento.value = null
    }
}

sealed class LoginEvent {
    data class ValidacaoFalhou(val mensagem: String) : LoginEvent()
    data class LoginSucesso(val nomeUtilizador: String) : LoginEvent()
    data class ConvidadoSucesso(val nomeJogador: String) : LoginEvent()
    data object SenhaIncorreta : LoginEvent()
    data object JogadorNaoEncontrado : LoginEvent()
    data object ErroBanco : LoginEvent()
    data object NomeConvidadoVazio : LoginEvent()
}
