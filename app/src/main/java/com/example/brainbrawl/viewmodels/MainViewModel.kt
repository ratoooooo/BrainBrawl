package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.services.AuthService

class MainViewModel(
    private val jogadorRepository: JogadorRepository = JogadorRepository(),
    private val amigosRepository: AmigosRepository = AmigosRepository(),
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _uiState = MutableLiveData(MainUiState())
    val uiState: LiveData<MainUiState> = _uiState

    private var pedidosNotificacoesListener: AmigosRepository.ListenerHandle? = null
    private var convitesNotificacoesListener: AmigosRepository.ListenerHandle? = null
    private var notificacoesAtivas = false
    private var notificacoesResolucaoEmCurso = false
    private var notificacoesIdentificador = ""
    private var nomeCategoriaPadraoConvites = "Todas as categorias"
    private var pedidosPendentes = 0
    private var convitesPendentes = 0

    fun iniciar(input: MainInput) {
        val utilizadorAtual = authService.utilizadorAtual()
        val state = MainUiState(
            uid = input.uid.ifBlank { utilizadorAtual?.uid.orEmpty() },
            email = input.email.ifBlank { utilizadorAtual?.email.orEmpty() },
            nomeUtilizador = input.nomeUtilizador,
            nomeJogador = input.nomeJogador,
            nomeCategoria = input.nomeCategoria,
            codigoSala = input.codigoSala,
            modoJogo = input.modoJogo
        ).comBoasVindas()

        _uiState.value = state
        carregarPerfilPrincipal(state)
    }

    fun iniciarNotificacoes(nomeCategoriaPadrao: String) {
        nomeCategoriaPadraoConvites = nomeCategoriaPadrao.ifBlank { nomeCategoriaPadraoConvites }
        notificacoesAtivas = true
        iniciarListenersNotificacoesSociais()
    }

    fun pararNotificacoes() {
        notificacoesAtivas = false
        removerListenersNotificacoesSociais()
    }

    fun terminarSessao() {
        val state = _uiState.value ?: MainUiState()
        (state.uid.ifBlank { state.nomeUtilizador }).takeIf { it.isNotBlank() }?.let {
            jogadorRepository.marcarOffline(it)
        }
        authService.terminarSessao()
    }

    override fun onCleared() {
        removerListenersNotificacoesSociais()
        super.onCleared()
    }

    private fun carregarPerfilPrincipal(state: MainUiState) {
        val identificador = state.uid.takeIf { it.isNotBlank() }
            ?: state.nomeUtilizador.takeIf { it.isNotBlank() }
            ?: return

        jogadorRepository.obterPerfil(identificador)
            .addOnSuccessListener { perfil ->
                if (perfil != null) {
                    aplicarPerfilPrincipal(perfil)
                    return@addOnSuccessListener
                }
                if (state.email.isNotBlank()) {
                    jogadorRepository.obterPerfilPorEmail(state.email)
                        .addOnSuccessListener { perfilPorEmail ->
                            perfilPorEmail?.let { aplicarPerfilPrincipal(it) }
                        }
                }
            }
    }

    private fun aplicarPerfilPrincipal(perfil: JogadorRepository.PerfilJogador) {
        val atual = _uiState.value ?: MainUiState()
        val novoState = atual.copy(
            uid = atual.uid.ifBlank { perfil.uid },
            email = atual.email.ifBlank { perfil.email },
            nomeUtilizador = perfil.nomeUtilizador.ifBlank { atual.nomeUtilizador },
            boasVindas = perfil.nomeUtilizador.ifBlank { atual.nomeUtilizador.ifBlank { "Jogador" } },
            amigosVisivel = true,
            nivel = perfil.estatisticas.nivel,
            xpNoNivelAtual = perfil.estatisticas.xpNoNivelAtual,
            xpNecessarioProximoNivel = perfil.estatisticas.xpNecessarioProximoNivel.coerceAtLeast(1),
            avatar = perfil.avatar
        )
        _uiState.value = novoState
        if (notificacoesAtivas) iniciarListenersNotificacoesSociais()
    }

    private fun iniciarListenersNotificacoesSociais() {
        val state = _uiState.value ?: MainUiState()
        val identificador = state.uid.takeIf { it.isNotBlank() }
            ?: state.nomeUtilizador.takeIf { it.isNotBlank() }
            ?: run {
                publicarNotificacoes(0, 0)
                return
            }
        if (!state.amigosVisivel) {
            publicarNotificacoes(0, 0)
            return
        }

        val chave = "${state.uid}|${state.nomeUtilizador}"
        val jaAtivo = pedidosNotificacoesListener != null || convitesNotificacoesListener != null
        if ((jaAtivo || notificacoesResolucaoEmCurso) && notificacoesIdentificador == chave) return

        removerListenersNotificacoesSociais(limparBadge = false)
        notificacoesIdentificador = chave
        notificacoesResolucaoEmCurso = true

        amigosRepository.resolverUtilizador(identificador, state.nomeUtilizador)
            .addOnSuccessListener { utilizador ->
                notificacoesResolucaoEmCurso = false
                if (!notificacoesAtivas || notificacoesIdentificador != chave) return@addOnSuccessListener
                if (utilizador == null) {
                    publicarNotificacoes(0, 0)
                    return@addOnSuccessListener
                }

                pedidosNotificacoesListener = amigosRepository.observarPedidosRecebidos(
                    utilizador,
                    onPedidosAlterados = { pedidos ->
                        pedidosPendentes = pedidos.size
                        publicarNotificacoes(pedidosPendentes, convitesPendentes)
                    },
                    onErro = { publicarNotificacoes(0, convitesPendentes) }
                )
                convitesNotificacoesListener = amigosRepository.observarConvitesRecebidos(
                    utilizador,
                    nomeCategoriaPadraoConvites,
                    onConvitesAlterados = { convites ->
                        convitesPendentes = convites.size
                        publicarNotificacoes(pedidosPendentes, convitesPendentes)
                    },
                    onErro = { publicarNotificacoes(pedidosPendentes, 0) }
                )
            }
            .addOnFailureListener {
                notificacoesResolucaoEmCurso = false
                publicarNotificacoes(0, 0)
            }
    }

    private fun removerListenersNotificacoesSociais(limparBadge: Boolean = true) {
        amigosRepository.removerListener(pedidosNotificacoesListener)
        amigosRepository.removerListener(convitesNotificacoesListener)
        pedidosNotificacoesListener = null
        convitesNotificacoesListener = null
        notificacoesResolucaoEmCurso = false
        notificacoesIdentificador = ""
        if (limparBadge) publicarNotificacoes(0, 0)
    }

    private fun publicarNotificacoes(pedidos: Int, convites: Int) {
        pedidosPendentes = pedidos
        convitesPendentes = convites
        val atual = _uiState.value ?: MainUiState()
        _uiState.value = atual.copy(notificacoesPendentes = pedidos + convites)
    }
}

data class MainInput(
    val uid: String = "",
    val email: String = "",
    val nomeUtilizador: String = "",
    val nomeJogador: String = "",
    val nomeCategoria: String = "",
    val codigoSala: String = "",
    val modoJogo: String = ""
)

data class MainUiState(
    val uid: String = "",
    val email: String = "",
    val nomeUtilizador: String = "",
    val nomeJogador: String = "",
    val nomeCategoria: String = "",
    val codigoSala: String = "",
    val modoJogo: String = "",
    val boasVindas: String = "Jogador",
    val amigosVisivel: Boolean = false,
    val nivel: Int = 1,
    val xpNoNivelAtual: Int = 0,
    val xpNecessarioProximoNivel: Int = 300,
    val avatar: String = "",
    val notificacoesPendentes: Int = 0
) {
    fun comBoasVindas(): MainUiState {
        return when {
            nomeUtilizador.isNotBlank() -> copy(boasVindas = nomeUtilizador, amigosVisivel = true)
            nomeJogador.isNotBlank() -> copy(boasVindas = nomeJogador, amigosVisivel = false, notificacoesPendentes = 0)
            else -> copy(boasVindas = "Jogador", amigosVisivel = false, notificacoesPendentes = 0)
        }
    }
}
