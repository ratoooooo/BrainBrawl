package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.UteisValidacao

class RegistarViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository(),
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _evento = MutableLiveData<RegistarEvent?>()
    val evento: LiveData<RegistarEvent?> = _evento

    fun registar(
        nomeUtilizador: String,
        email: String,
        password: String,
        avatarSelecionadoIndex: Int
    ) {
        val erroNome = UteisValidacao.validarCampos(nomeUtilizador, password)
        if (erroNome != null) {
            _evento.value = RegistarEvent.ValidacaoFalhou(erroNome)
            return
        }

        val erroAuth = validarEmailPassword(email, password)
        if (erroAuth != null) {
            _evento.value = RegistarEvent.ValidacaoFalhou(erroAuth)
            return
        }

        jogadorRepository.verificarJogadorExiste(nomeUtilizador)
            .addOnSuccessListener { existe ->
                if (existe) {
                    _evento.value = RegistarEvent.JogadorJaExiste
                } else {
                    criarContaAuth(nomeUtilizador, email, password, avatarSelecionadoIndex)
                }
            }
            .addOnFailureListener { exception ->
                _evento.value = RegistarEvent.ErroVerificarJogador(exception.message.orEmpty())
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }

    private fun criarContaAuth(
        nomeUtilizador: String,
        email: String,
        password: String,
        avatarSelecionadoIndex: Int
    ) {
        authService.registar(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid.isNullOrBlank()) {
                    _evento.value = RegistarEvent.ErroCriarAuth("UID inválido.")
                    return@addOnSuccessListener
                }

                criarPerfil(uid, nomeUtilizador, email, avatarSelecionadoIndex)
            }
            .addOnFailureListener { exception ->
                _evento.value = RegistarEvent.ErroCriarAuth(exception.message.orEmpty())
            }
    }

    private fun criarPerfil(
        uid: String,
        nomeUtilizador: String,
        email: String,
        avatarSelecionadoIndex: Int
    ) {
        val nomeAvatar = "avatar_${avatarSelecionadoIndex + 1}_playstore"
        jogadorRepository.criarPerfilAutenticado(uid, nomeUtilizador, email, nomeAvatar)
            .addOnSuccessListener {
                _evento.value = RegistarEvent.RegistoSucesso(
                    nomeUtilizador = nomeUtilizador,
                    uid = uid,
                    email = email
                )
            }
            .addOnFailureListener { exception ->
                _evento.value = RegistarEvent.ErroCriarJogador(exception.message.orEmpty())
            }
    }

    private fun validarEmailPassword(email: String, password: String): String? {
        if (email.isBlank()) {
            return "Preencha todos os campos"
        }
        if (!email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            return "Insira um email válido"
        }
        if (password.length < 8 || password.length > 20) {
            return "A senha deve ter entre 8 e 20 caracteres"
        }
        return null
    }
}

sealed class RegistarEvent {
    data class ValidacaoFalhou(val mensagem: String) : RegistarEvent()
    data class RegistoSucesso(
        val nomeUtilizador: String,
        val uid: String,
        val email: String
    ) : RegistarEvent()
    data class ErroVerificarJogador(val mensagem: String) : RegistarEvent()
    data class ErroCriarAuth(val mensagem: String) : RegistarEvent()
    data class ErroCriarJogador(val mensagem: String) : RegistarEvent()
    data object JogadorJaExiste : RegistarEvent()
}
