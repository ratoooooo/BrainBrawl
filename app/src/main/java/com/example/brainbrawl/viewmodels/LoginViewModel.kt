package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.UteisValidacao

class LoginViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository(),
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _evento = MutableLiveData<LoginEvent?>()
    val evento: LiveData<LoginEvent?> = _evento

    fun verificarSessaoAtual() {
        val utilizador = authService.utilizadorAtual() ?: return
        val uid = utilizador.uid

        jogadorRepository.obterPerfil(uid)
            .addOnSuccessListener { perfil ->
                if (perfil == null) {
                    _evento.value = LoginEvent.ErroPerfilAuth
                    return@addOnSuccessListener
                }

                jogadorRepository.marcarOnline(uid)
                _evento.value = LoginEvent.LoginSucesso(
                    nomeUtilizador = perfil.nomeUtilizador,
                    uid = uid,
                    email = perfil.email.ifBlank { utilizador.email.orEmpty() }
                )
            }
            .addOnFailureListener {
                _evento.value = LoginEvent.ErroBanco
            }
    }

    fun entrar(identificador: String, password: String) {
        if (identificador.isBlank() || password.isBlank()) {
            _evento.value = LoginEvent.ValidacaoFalhou("Preencha todos os campos")
            return
        }

        if (identificador.isEmail()) {
            entrarComFirebaseAuth(identificador, password)
        } else {
            entrarLegado(identificador, password)
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

    private fun entrarComFirebaseAuth(email: String, password: String) {
        val erro = validarEmailPassword(email, password)
        if (erro != null) {
            _evento.value = LoginEvent.ValidacaoFalhou(erro)
            return
        }

        authService.entrar(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid.isNullOrBlank()) {
                    _evento.value = LoginEvent.ErroPerfilAuth
                    return@addOnSuccessListener
                }

                jogadorRepository.obterPerfil(uid)
                    .addOnSuccessListener { perfil ->
                        if (perfil == null) {
                            _evento.value = LoginEvent.ErroPerfilAuth
                        } else {
                            jogadorRepository.marcarOnline(uid)
                            _evento.value = LoginEvent.LoginSucesso(
                                nomeUtilizador = perfil.nomeUtilizador,
                                uid = uid,
                                email = perfil.email.ifBlank { email }
                            )
                        }
                    }
                    .addOnFailureListener {
                        _evento.value = LoginEvent.ErroBanco
                    }
            }
            .addOnFailureListener {
                _evento.value = LoginEvent.ErroAutenticacao
            }
    }

    private fun entrarLegado(nomeUtilizador: String, password: String) {
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

    private fun validarEmailPassword(email: String, password: String): String? {
        if (!email.isEmail()) {
            return "Insira um email válido"
        }
        if (password.length < 8 || password.length > 20) {
            return "A senha deve ter entre 8 e 20 caracteres"
        }
        return null
    }

    private fun String.isEmail(): Boolean {
        return matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
    }
}

sealed class LoginEvent {
    data class ValidacaoFalhou(val mensagem: String) : LoginEvent()
    data class LoginSucesso(
        val nomeUtilizador: String,
        val uid: String? = null,
        val email: String? = null
    ) : LoginEvent()
    data class ConvidadoSucesso(val nomeJogador: String) : LoginEvent()
    data object SenhaIncorreta : LoginEvent()
    data object ErroAutenticacao : LoginEvent()
    data object ErroPerfilAuth : LoginEvent()
    data object JogadorNaoEncontrado : LoginEvent()
    data object ErroBanco : LoginEvent()
    data object NomeConvidadoVazio : LoginEvent()
}
