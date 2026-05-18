package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.models.Convite
import com.example.brainbrawl.models.PedidoAmizade
import com.example.brainbrawl.models.UtilizadorSocial
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.repositories.JogadorRepository

class AmigosViewModel(
    private val amigosRepository: AmigosRepository = AmigosRepository(),
    private val jogadorRepository: JogadorRepository = JogadorRepository()
) : ViewModel() {

    private val _amigos = MutableLiveData<AmigosListaUiState>()
    val amigos: LiveData<AmigosListaUiState> = _amigos

    private val _pedidos = MutableLiveData<List<PedidoAmizade>>()
    val pedidos: LiveData<List<PedidoAmizade>> = _pedidos

    private val _convites = MutableLiveData<List<Convite>>()
    val convites: LiveData<List<Convite>> = _convites

    private val _evento = MutableLiveData<AmigosEvent?>()
    val evento: LiveData<AmigosEvent?> = _evento

    private var amigosListenerHandle: AmigosRepository.ListenerHandle? = null
    private var pedidosListenerHandle: AmigosRepository.ListenerHandle? = null
    private var convitesListenerHandle: AmigosRepository.ListenerHandle? = null
    private var utilizadorAtual: UtilizadorSocial? = null
    private var amigosAtuais: List<UtilizadorSocial> = emptyList()

    fun carregarListaAmigos(uid: String, nomeUtilizador: String) {
        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.carregarListaAmigos(utilizador)
                .addOnSuccessListener { amigos ->
                    atualizarListaAmigos(utilizador, amigos)
                }
        }
    }

    fun iniciarListenersSociais(uid: String, nomeUtilizador: String, nomeCategoriaPadrao: String) {
        if ((uid.isBlank() && nomeUtilizador.isBlank()) || amigosListenerHandle != null) return

        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            if (amigosListenerHandle != null) return@resolverUtilizadorAtual

            amigosListenerHandle = amigosRepository.observarAmigos(
                utilizador,
                onAmigosAlterados = { amigos ->
                    atualizarListaAmigos(utilizador, amigos)
                }
            )
            pedidosListenerHandle = amigosRepository.observarPedidosRecebidos(
                utilizador,
                onPedidosAlterados = { pedidosRecebidos ->
                    _pedidos.value = pedidosRecebidos
                }
            )
            convitesListenerHandle = amigosRepository.observarConvitesRecebidos(
                utilizador,
                nomeCategoriaPadrao,
                onConvitesAlterados = { convitesRecebidos ->
                    _convites.value = convitesRecebidos
                }
            )
        }
    }

    fun removerListenersSociais() {
        amigosRepository.removerListener(amigosListenerHandle)
        amigosRepository.removerListener(pedidosListenerHandle)
        amigosRepository.removerListener(convitesListenerHandle)
        amigosListenerHandle = null
        pedidosListenerHandle = null
        convitesListenerHandle = null
    }

    fun pesquisarUtilizador(uid: String, nomeUtilizador: String, nomePesquisa: String) {
        if (nomePesquisa.isEmpty()) {
            _evento.value = AmigosEvent.PesquisaOculta
            return
        }

        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.pesquisarJogadorParaAdicionar(nomePesquisa).addOnSuccessListener { jogador ->
                when {
                    jogador == null -> _evento.value = AmigosEvent.JogadorNaoEncontrado
                    jogador.corresponde(utilizador) -> _evento.value = AmigosEvent.PesquisaOculta
                    amigosAtuais.any { it.corresponde(jogador) } -> {
                        _evento.value = AmigosEvent.JogadorJaAmigo(jogador.nomeDisplay)
                    }
                    else -> _evento.value = AmigosEvent.JogadorEncontrado(jogador.nomeDisplay)
                }
            }
        }
    }

    fun enviarPedidoAmizade(uid: String, nomeUtilizador: String, nomeNovoAmigo: String) {
        if (nomeNovoAmigo.isEmpty()) return

        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.pesquisarJogadorParaAdicionar(nomeNovoAmigo).addOnSuccessListener { amigo ->
                when {
                    amigo == null -> _evento.value = AmigosEvent.JogadorNaoEncontrado
                    amigo.corresponde(utilizador) -> _evento.value = AmigosEvent.PesquisaOculta
                    amigosAtuais.any { it.corresponde(amigo) } -> _evento.value = AmigosEvent.PedidoJaAmigo
                    else -> {
                        amigosRepository.enviarPedidoAmizade(utilizador, amigo)
                            .addOnSuccessListener {
                                _evento.value = AmigosEvent.PedidoEnviado
                            }
                            .addOnFailureListener {
                                _evento.value = AmigosEvent.ErroEnviarPedido
                            }
                    }
                }
            }.addOnFailureListener {
                _evento.value = AmigosEvent.ErroEnviarPedido
            }
        }
    }

    fun aceitarPedidoAmizade(uid: String, nomeUtilizador: String, pedido: PedidoAmizade) {
        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.aceitarPedidoAmizade(utilizador, pedido)
                .addOnSuccessListener {
                    _evento.value = AmigosEvent.PedidoAceite
                }
        }
    }

    fun recusarPedidoAmizade(uid: String, nomeUtilizador: String, pedido: PedidoAmizade) {
        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.recusarPedidoAmizade(utilizador, pedido)
                .addOnSuccessListener {
                    _evento.value = AmigosEvent.PedidoRecusado
                }
        }
    }

    fun carregarPedidosRecebidos(uid: String, nomeUtilizador: String) {
        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.carregarPedidosRecebidos(utilizador)
                .addOnSuccessListener { pedidosRecebidos ->
                    _pedidos.value = pedidosRecebidos
                }
        }
    }

    fun carregarConvitesRecebidos(uid: String, nomeUtilizador: String, nomeCategoriaPadrao: String) {
        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.carregarConvitesRecebidos(utilizador, nomeCategoriaPadrao)
                .addOnSuccessListener { convitesRecebidos ->
                    _convites.value = convitesRecebidos
                }
        }
    }

    fun aceitarConvite(uid: String, nomeUtilizador: String, convite: Convite) {
        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.verificarSalaConviteExiste(convite)
                .addOnSuccessListener { salaExiste ->
                    if (!salaExiste) {
                        amigosRepository.removerConvite(utilizador, convite)
                        _evento.value = AmigosEvent.ConviteExpirado
                        return@addOnSuccessListener
                    }

                    amigosRepository.aceitarConvite(utilizador, convite)
                        .addOnSuccessListener {
                            _evento.value = AmigosEvent.ConviteAceite(convite)
                        }
                        .addOnFailureListener {
                            _evento.value = AmigosEvent.ErroAceitarConvite
                        }
                }
                .addOnFailureListener {
                    _evento.value = AmigosEvent.ErroAceitarConvite
                }
        }
    }

    fun recusarConvite(uid: String, nomeUtilizador: String, convite: Convite) {
        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.recusarConvite(utilizador, convite)
                .addOnSuccessListener {
                    _evento.value = AmigosEvent.ConviteRecusado
                }
        }
    }

    fun removerConvite(uid: String, nomeUtilizador: String, convite: Convite) {
        resolverUtilizadorAtual(uid, nomeUtilizador) { utilizador ->
            amigosRepository.removerConvite(utilizador, convite)
                .addOnSuccessListener {
                    _evento.value = AmigosEvent.ConviteRemovido
                }
        }
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListenersSociais()
        super.onCleared()
    }

    private fun resolverUtilizadorAtual(
        uid: String,
        nomeUtilizador: String,
        onSuccess: (UtilizadorSocial) -> Unit
    ) {
        val atual = utilizadorAtual
        if (atual != null && atual.corresponde(uid, nomeUtilizador)) {
            onSuccess(atual)
            return
        }

        val identificador = uid.ifBlank { nomeUtilizador }
        amigosRepository.resolverUtilizador(identificador, nomeUtilizador)
            .addOnSuccessListener { utilizador ->
                val resolvidoBase = utilizador ?: UtilizadorSocial(
                    uid = uid,
                    nomeUtilizador = nomeUtilizador,
                    chavePerfil = identificador,
                    chaveOrigem = nomeUtilizador
                )
                val resolvido = if (resolvidoBase.uid.isBlank() && uid.isNotBlank()) {
                    resolvidoBase.copy(uid = uid)
                } else {
                    resolvidoBase
                }
                utilizadorAtual = resolvido
                onSuccess(resolvido)
            }
    }

    private fun atualizarListaAmigos(utilizador: UtilizadorSocial, amigos: List<UtilizadorSocial>) {
        val amigosTemp = amigos.filterNot { it.corresponde(utilizador) }
        val avataresTemp = MutableList(amigosTemp.size) { AVATAR_PADRAO }
        val estadosTemp = MutableList(amigosTemp.size) { ESTADO_OFF }

        if (amigosTemp.isEmpty()) {
            publicarAmigos(emptyList(), emptyList(), emptyList())
            return
        }

        var loaded = 0
        amigosTemp.forEachIndexed { index, amigo ->
            jogadorRepository.obterPerfil(amigo.chavePrimaria)
                .addOnSuccessListener { perfilAmigo ->
                    avataresTemp[index] = perfilAmigo?.avatar ?: AVATAR_PADRAO
                    estadosTemp[index] = perfilAmigo?.estado ?: ESTADO_OFF
                    loaded++
                    if (loaded == amigosTemp.size) {
                        publicarAmigos(amigosTemp, avataresTemp, estadosTemp)
                    }
                }
                .addOnFailureListener {
                    loaded++
                    if (loaded == amigosTemp.size) {
                        publicarAmigos(amigosTemp, avataresTemp, estadosTemp)
                    }
                }
        }
    }

    private fun publicarAmigos(
        utilizadores: List<UtilizadorSocial>,
        avatares: List<String>,
        estados: List<String>
    ) {
        amigosAtuais = utilizadores
        _amigos.value = AmigosListaUiState(utilizadores, avatares, estados)
    }

    private companion object {
        const val AVATAR_PADRAO = "avatar_1_playstore"
        const val ESTADO_ON = "on"
        const val ESTADO_OFF = "off"
    }
}

data class AmigosListaUiState(
    val utilizadores: List<UtilizadorSocial>,
    val avatares: List<String>,
    val estados: List<String>
)

sealed class AmigosEvent {
    data object PesquisaOculta : AmigosEvent()
    data class JogadorEncontrado(val nome: String) : AmigosEvent()
    data class JogadorJaAmigo(val nome: String) : AmigosEvent()
    data object JogadorNaoEncontrado : AmigosEvent()
    data object PedidoJaAmigo : AmigosEvent()
    data object PedidoEnviado : AmigosEvent()
    data object ErroEnviarPedido : AmigosEvent()
    data object PedidoAceite : AmigosEvent()
    data object PedidoRecusado : AmigosEvent()
    data object ConviteRecusado : AmigosEvent()
    data object ConviteRemovido : AmigosEvent()
    data class ConviteAceite(val convite: Convite) : AmigosEvent()
    data object ConviteExpirado : AmigosEvent()
    data object ErroAceitarConvite : AmigosEvent()
}
