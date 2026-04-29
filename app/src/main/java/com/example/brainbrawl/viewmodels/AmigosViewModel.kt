package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.models.Convite
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.repositories.JogadorRepository

class AmigosViewModel(
    private val amigosRepository: AmigosRepository = AmigosRepository(),
    private val jogadorRepository: JogadorRepository = JogadorRepository()
) : ViewModel() {

    private val _amigos = MutableLiveData<AmigosListaUiState>()
    val amigos: LiveData<AmigosListaUiState> = _amigos

    private val _pedidos = MutableLiveData<List<String>>()
    val pedidos: LiveData<List<String>> = _pedidos

    private val _convites = MutableLiveData<List<Convite>>()
    val convites: LiveData<List<Convite>> = _convites

    private val _evento = MutableLiveData<AmigosEvent?>()
    val evento: LiveData<AmigosEvent?> = _evento

    private var amigosListenerHandle: AmigosRepository.ListenerHandle? = null
    private var pedidosListenerHandle: AmigosRepository.ListenerHandle? = null
    private var convitesListenerHandle: AmigosRepository.ListenerHandle? = null
    private var nomesAmigosAtuais: List<String> = emptyList()

    fun carregarListaAmigos(nomeUtilizador: String) {
        if (nomeUtilizador.isEmpty()) return

        amigosRepository.carregarListaAmigos(nomeUtilizador)
            .addOnSuccessListener { nomesAmigos ->
                atualizarListaAmigos(nomeUtilizador, nomesAmigos)
            }
    }

    fun iniciarListenersSociais(nomeUtilizador: String, nomeCategoriaPadrao: String) {
        if (nomeUtilizador.isEmpty() || amigosListenerHandle != null) return

        amigosListenerHandle = amigosRepository.observarAmigos(
            nomeUtilizador,
            onAmigosAlterados = { nomesAmigos ->
                atualizarListaAmigos(nomeUtilizador, nomesAmigos)
            }
        )
        pedidosListenerHandle = amigosRepository.observarPedidosRecebidos(
            nomeUtilizador,
            onPedidosAlterados = { pedidosRecebidos ->
                _pedidos.value = pedidosRecebidos
            }
        )
        convitesListenerHandle = amigosRepository.observarConvitesRecebidos(
            nomeUtilizador,
            nomeCategoriaPadrao,
            onConvitesAlterados = { convitesRecebidos ->
                _convites.value = convitesRecebidos
            }
        )
    }

    fun removerListenersSociais() {
        amigosRepository.removerListener(amigosListenerHandle)
        amigosRepository.removerListener(pedidosListenerHandle)
        amigosRepository.removerListener(convitesListenerHandle)
        amigosListenerHandle = null
        pedidosListenerHandle = null
        convitesListenerHandle = null
    }

    fun pesquisarUtilizador(nomeUtilizador: String, nomePesquisa: String) {
        if (nomePesquisa.isEmpty() || nomePesquisa == nomeUtilizador) {
            _evento.value = AmigosEvent.PesquisaOculta
            return
        }

        amigosRepository.pesquisarJogadorParaAdicionar(nomePesquisa).addOnSuccessListener { existe ->
            if (existe) {
                if (nomesAmigosAtuais.contains(nomePesquisa)) {
                    _evento.value = AmigosEvent.JogadorJaAmigo(nomePesquisa)
                } else {
                    _evento.value = AmigosEvent.JogadorEncontrado(nomePesquisa)
                }
            } else {
                _evento.value = AmigosEvent.JogadorNaoEncontrado
            }
        }
    }

    fun enviarPedidoAmizade(nomeUtilizador: String, nomeNovoAmigo: String) {
        if (nomeNovoAmigo.isEmpty() || nomeNovoAmigo == nomeUtilizador) return

        if (nomesAmigosAtuais.contains(nomeNovoAmigo)) {
            _evento.value = AmigosEvent.PedidoJaAmigo
            return
        }

        amigosRepository.enviarPedidoAmizade(nomeUtilizador, nomeNovoAmigo)
            .addOnSuccessListener {
                _evento.value = AmigosEvent.PedidoEnviado
            }
            .addOnFailureListener {
                _evento.value = AmigosEvent.ErroEnviarPedido
            }
    }

    fun aceitarPedidoAmizade(nomeUtilizador: String, nomeOutro: String) {
        amigosRepository.aceitarPedidoAmizade(nomeUtilizador, nomeOutro)
            .addOnSuccessListener {
                _evento.value = AmigosEvent.PedidoAceite
            }
    }

    fun recusarPedidoAmizade(nomeUtilizador: String, nomeOutro: String) {
        amigosRepository.recusarPedidoAmizade(nomeUtilizador, nomeOutro)
            .addOnSuccessListener {
                _evento.value = AmigosEvent.PedidoRecusado
            }
    }

    fun carregarPedidosRecebidos(nomeUtilizador: String) {
        if (nomeUtilizador.isEmpty()) return

        amigosRepository.carregarPedidosRecebidos(nomeUtilizador)
            .addOnSuccessListener { pedidosRecebidos ->
                _pedidos.value = pedidosRecebidos
            }
    }

    fun carregarConvitesRecebidos(nomeUtilizador: String, nomeCategoriaPadrao: String) {
        if (nomeUtilizador.isEmpty()) return

        amigosRepository.carregarConvitesRecebidos(nomeUtilizador, nomeCategoriaPadrao)
            .addOnSuccessListener { convitesRecebidos ->
                _convites.value = convitesRecebidos
            }
    }

    fun aceitarConvite(nomeUtilizador: String, convite: Convite) {
        amigosRepository.aceitarConvite(nomeUtilizador, convite.nomeAmigo)
    }

    fun recusarConvite(nomeUtilizador: String, convite: Convite) {
        amigosRepository.recusarConvite(nomeUtilizador, convite.nomeAmigo)
            .addOnSuccessListener {
                _evento.value = AmigosEvent.ConviteRecusado
            }
    }

    fun removerConvite(nomeUtilizador: String, convite: Convite) {
        amigosRepository.removerConvite(nomeUtilizador, convite.nomeAmigo)
            .addOnSuccessListener {
                _evento.value = AmigosEvent.ConviteRemovido
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListenersSociais()
        super.onCleared()
    }

    private fun atualizarListaAmigos(nomeUtilizador: String, nomesAmigos: List<String>) {
        val nomesBase = mutableListOf(nomeUtilizador)
        val avataresBase = mutableListOf(AVATAR_PADRAO)
        val estadosBase = mutableListOf(ESTADO_ON)

        jogadorRepository.obterPerfil(nomeUtilizador)
            .addOnSuccessListener { perfil ->
                avataresBase[0] = perfil?.avatar ?: AVATAR_PADRAO
                estadosBase[0] = perfil?.estado ?: ESTADO_ON

                val amigosTemp = nomesAmigos.filter { it != nomeUtilizador }
                val avataresTemp = MutableList(amigosTemp.size) { AVATAR_PADRAO }
                val estadosTemp = MutableList(amigosTemp.size) { ESTADO_OFF }

                if (amigosTemp.isEmpty()) {
                    publicarAmigos(nomesBase, avataresBase, estadosBase)
                    return@addOnSuccessListener
                }

                var loaded = 0
                amigosTemp.forEachIndexed { index, nomeAmigo ->
                    jogadorRepository.obterPerfil(nomeAmigo)
                        .addOnSuccessListener { perfilAmigo ->
                            avataresTemp[index] = perfilAmigo?.avatar ?: AVATAR_PADRAO
                            estadosTemp[index] = perfilAmigo?.estado ?: ESTADO_OFF
                            loaded++
                            if (loaded == amigosTemp.size) {
                                publicarAmigos(
                                    nomesBase + amigosTemp,
                                    avataresBase + avataresTemp,
                                    estadosBase + estadosTemp
                                )
                            }
                        }
                        .addOnFailureListener {
                            loaded++
                            if (loaded == amigosTemp.size) {
                                publicarAmigos(
                                    nomesBase + amigosTemp,
                                    avataresBase + avataresTemp,
                                    estadosBase + estadosTemp
                                )
                            }
                        }
                }
            }
    }

    private fun publicarAmigos(nomes: List<String>, avatares: List<String>, estados: List<String>) {
        nomesAmigosAtuais = nomes
        _amigos.value = AmigosListaUiState(nomes, avatares, estados)
    }

    private companion object {
        const val AVATAR_PADRAO = "avatar_1_playstore"
        const val ESTADO_ON = "on"
        const val ESTADO_OFF = "off"
    }
}

data class AmigosListaUiState(
    val nomes: List<String>,
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
}
