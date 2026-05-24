package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityAmigosBinding
import com.example.brainbrawl.models.Convite
import com.example.brainbrawl.models.PedidoAmizade
import com.example.brainbrawl.models.UtilizadorSocial
import com.example.brainbrawl.routes.BottomNavHelper
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.AmigosEvent
import com.example.brainbrawl.viewmodels.AmigosListaUiState
import com.example.brainbrawl.viewmodels.AmigosViewModel

class AmigosActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityAmigosBinding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[AmigosViewModel::class.java]
    }
    private val authService = AuthService()
    // Variáveis para armazenar os dados dos amigos, convites e pedidos de amizade
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private val amigosOnline = mutableListOf<UtilizadorSocial>()
    private lateinit var amigoOnlineAdapter: AmigoAdapter
    private val avataresOnline = mutableListOf<String>()
    private val estadosOnline = mutableListOf<String>()
    private var abaAtiva = AbaAmigos.AMIGOS
    private var ultimoResultadoPesquisa: String = ""

    private val convitesRecebidos = mutableListOf<Convite>()
    private lateinit var conviteAdapter: ConviteAdapter

    private val pedidosAmizadeRecebidos = mutableListOf<PedidoAmizade>()
    private lateinit var pedidoAdapter: PedidoAmizadeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os valores passados pela Intent
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: ""
        val email = intent.getStringExtra(IntentExtras.EMAIL) ?: authService.utilizadorAtual()?.email ?: ""
        BottomNavHelper.instalar(this, BottomNavHelper.Item.AMIGOS, uid, nomeUtilizador, nomeJogador, email)
        amigoOnlineAdapter = AmigoAdapter(amigosOnline, avataresOnline, estadosOnline, nomeUtilizador, uid)
        binding.recyclerAmigosOnline.layoutManager = LinearLayoutManager(this)
        binding.recyclerAmigosOnline.adapter = amigoOnlineAdapter

        // Configura o adaptador para a lista de convites recebidos
        conviteAdapter = ConviteAdapter(
            convitesRecebidos,
            onAceitarClick = { convite -> aceitarConvite(convite) },
            onRecusarClick = { convite -> viewModel.recusarConvite(uid, nomeUtilizador, convite) }
        )
        binding.recyclerConvites.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvites.adapter = conviteAdapter

        pedidoAdapter = PedidoAmizadeAdapter(
            pedidosAmizadeRecebidos,
            onAceitarClick = { pedido -> aceitarPedidoAmizade(pedido) },
            onRecusarClick = { pedido -> viewModel.recusarPedidoAmizade(uid, nomeUtilizador, pedido) }
        )
        binding.recyclerPedidosAmizade.layoutManager = LinearLayoutManager(this)
        binding.recyclerPedidosAmizade.adapter = pedidoAdapter

        configurarObservers()
        configurarTabs()

        // Configurar o botao de pesquisa
        binding.btnPesquisar.setOnClickListener {
            if (binding.layoutPesquisa.visibility == View.VISIBLE) {
                val nomePesquisa = binding.edtPesquisar.text.toString().trim()
                if (nomePesquisa.isNotBlank()) pesquisarUtilizador(nomePesquisa) else alternarPesquisa()
            } else {
                alternarPesquisa(focar = true)
            }
        }
        // Configurar botao de pesquisa
        binding.edtPesquisar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                executarPesquisa()
                true
            } else false
        }
        binding.btnExecutarPesquisa.setOnClickListener { executarPesquisa() }
        //Configurar botao de adicionar amigo
        binding.btnAdicionarAmigo.setOnClickListener {
            val nomeNovoAmigo = ultimoResultadoPesquisa.ifBlank { binding.edtPesquisar.text.toString().trim() }
            viewModel.enviarPedidoAmizade(uid, nomeUtilizador, nomeNovoAmigo)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.iniciarListenersSociais(uid, nomeUtilizador, getString(R.string.categoria5))
    }

    override fun onStop() {
        viewModel.removerListenersSociais()
        super.onStop()
    }

    override fun onDestroy() {
        viewModel.removerListenersSociais()
        super.onDestroy()
    }

    private fun configurarObservers() {
        viewModel.amigos.observe(this) { estado ->
            atualizarListaAmigos(estado)
        }
        viewModel.pedidos.observe(this) { pedidos ->
            atualizarPedidosAmizadeRecebidos(pedidos)
        }
        viewModel.convites.observe(this) { convites ->
            atualizarConvitesRecebidos(convites)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun tratarEvento(evento: AmigosEvent) {
        when (evento) {
            AmigosEvent.PesquisaOculta -> {
                limparResultadoPesquisa()
            }
            is AmigosEvent.JogadorEncontrado -> {
                ultimoResultadoPesquisa = evento.nome
                binding.layoutAddAmigo.visibility = View.VISIBLE
                binding.txtPesquisaEstado.visibility = View.GONE
                binding.txtResultadoPesquisaNome.text = evento.nome
                binding.txtResultadoPesquisaEstado.text = getString(R.string.jogador_encontrado)
                binding.btnAdicionarAmigo.text = getString(R.string.adicionar)
            }
            is AmigosEvent.JogadorJaAmigo -> {
                binding.layoutAddAmigo.visibility = View.GONE
                binding.txtPesquisaEstado.visibility = View.VISIBLE
                binding.txtPesquisaEstado.text = getString(R.string.ja_e_teu_amigo_format, evento.nome)
            }
            AmigosEvent.JogadorNaoEncontrado -> {
                binding.layoutAddAmigo.visibility = View.GONE
                binding.txtPesquisaEstado.visibility = View.VISIBLE
                binding.txtPesquisaEstado.text = getString(R.string.jogador_nao_encontrado)
            }
            AmigosEvent.PedidoJaAmigo -> {
                Toast.makeText(this, R.string.ja_e_teu_amigo, Toast.LENGTH_SHORT).show()
            }
            AmigosEvent.PedidoEnviado -> {
                Toast.makeText(this, R.string.pedido_amizade_enviado, Toast.LENGTH_SHORT).show()
                limparResultadoPesquisa()
                binding.edtPesquisar.text.clear()
            }
            AmigosEvent.ErroEnviarPedido -> {
                Toast.makeText(this, R.string.erro_enviar_pedido, Toast.LENGTH_SHORT).show()
            }
            AmigosEvent.PedidoAceite -> {
                Toast.makeText(this, R.string.amizade_aceite, Toast.LENGTH_SHORT).show()
            }
            AmigosEvent.PedidoRecusado,
            AmigosEvent.ConviteRecusado,
            AmigosEvent.ConviteRemovido -> Unit
            is AmigosEvent.ConviteAceite -> abrirSalaConvite(evento.convite)
            AmigosEvent.ConviteExpirado -> {
                Toast.makeText(this, R.string.convite_expirado, Toast.LENGTH_SHORT).show()
            }
            AmigosEvent.ErroAceitarConvite -> {
                Toast.makeText(this, R.string.erro_aceitar_convite, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função para atualizar a lista de amigos
    private fun atualizarListaAmigos(estado: AmigosListaUiState) {
        // Limpa as listas de amigos, avatares e estados
        amigosOnline.clear()
        avataresOnline.clear()
        estadosOnline.clear()

        estado.utilizadores.forEachIndexed { index, amigo ->
            val estadoAmigo = estado.estados.getOrNull(index) ?: GameConstants.ESTADO_OFF
            val avatar = estado.avatares.getOrNull(index) ?: "avatar_1_playstore"
            if (estadoAmigo == GameConstants.ESTADO_ON) {
                amigosOnline.add(amigo)
                avataresOnline.add(avatar)
                estadosOnline.add(estadoAmigo)
            }
        }

        amigoOnlineAdapter.notifyDataSetChanged()

        binding.txtAmigosOnline.text = getString(R.string.amigos_online_count, amigosOnline.size)
        binding.recyclerAmigosOnline.visibility = if (amigosOnline.isEmpty()) View.GONE else View.VISIBLE
        binding.txtAmigosVazio.visibility = if (amigosOnline.isEmpty()) View.VISIBLE else View.GONE
    }

    // Função para atualizar os convites recebidos
    private fun atualizarConvitesRecebidos(convites: List<Convite>) {
        // Limpa a lista de convites recebidos
        convitesRecebidos.clear()
        convitesRecebidos.addAll(convites)
        binding.txtConvites.visibility = if (convitesRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerConvites.visibility = if (convitesRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        // Notifica o adaptador que os dados foram alterados
        conviteAdapter.notifyDataSetChanged()
    }

    // Função para atualizar os pedidos de amizade recebidos
    private fun atualizarPedidosAmizadeRecebidos(pedidos: List<PedidoAmizade>) {
        // Limpa a lista de pedidos de amizade recebidos
        pedidosAmizadeRecebidos.clear()
        pedidosAmizadeRecebidos.addAll(pedidos)
        binding.txtPedidosAmizade.visibility = if (pedidosAmizadeRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerPedidosAmizade.visibility = if (pedidosAmizadeRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        pedidoAdapter.notifyDataSetChanged()
    }

    private fun configurarTabs() {
        binding.tabAmigos.setOnClickListener { mostrarAba(AbaAmigos.AMIGOS) }
        binding.tabPedidos.setOnClickListener { mostrarAba(AbaAmigos.PEDIDOS) }
        binding.tabConvites.setOnClickListener { mostrarAba(AbaAmigos.CONVITES) }
        mostrarAba(AbaAmigos.AMIGOS)
    }

    private fun mostrarAba(aba: AbaAmigos) {
        abaAtiva = aba
        binding.containerAmigos.visibility = if (aba == AbaAmigos.AMIGOS) View.VISIBLE else View.GONE
        binding.containerPedidos.visibility = if (aba == AbaAmigos.PEDIDOS) View.VISIBLE else View.GONE
        binding.containerConvites.visibility = if (aba == AbaAmigos.CONVITES) View.VISIBLE else View.GONE
        aplicarEstadoTab(binding.tabAmigos, binding.iconTabAmigos, binding.txtTabAmigos, aba == AbaAmigos.AMIGOS)
        aplicarEstadoTab(binding.tabPedidos, binding.iconTabPedidos, binding.txtTabPedidos, aba == AbaAmigos.PEDIDOS)
        aplicarEstadoTab(binding.tabConvites, binding.iconTabConvites, binding.txtTabConvites, aba == AbaAmigos.CONVITES)
    }

    private fun aplicarEstadoTab(tab: LinearLayout, icon: ImageView, texto: TextView, ativo: Boolean) {
        tab.background = ContextCompat.getDrawable(
            this,
            if (ativo) R.drawable.bg_luso_segment_selected else R.drawable.bg_luso_segment_unselected
        )
        val iconColor = ContextCompat.getColor(this, if (ativo) R.color.bb_luso_gold else R.color.bb_luso_navy)
        val textColor = ContextCompat.getColor(this, if (ativo) R.color.bb_primary_text else R.color.bb_luso_navy)
        icon.setColorFilter(iconColor)
        texto.setTextColor(textColor)
    }

    private fun alternarPesquisa(focar: Boolean = false) {
        val mostrar = binding.layoutPesquisa.visibility != View.VISIBLE
        binding.layoutPesquisa.visibility = if (mostrar) View.VISIBLE else View.GONE
        if (!mostrar) limparResultadoPesquisa()
        if (mostrar && focar) {
            binding.edtPesquisar.requestFocus()
        }
    }

    private fun executarPesquisa() {
        val nome = binding.edtPesquisar.text.toString().trim()
        if (nome.isBlank()) {
            binding.layoutAddAmigo.visibility = View.GONE
            binding.txtPesquisaEstado.visibility = View.VISIBLE
            binding.txtPesquisaEstado.text = getString(R.string.digite_nome_utilizador)
            return
        }
        pesquisarUtilizador(nome)
    }

    private fun limparResultadoPesquisa() {
        ultimoResultadoPesquisa = ""
        binding.layoutAddAmigo.visibility = View.GONE
        binding.txtPesquisaEstado.visibility = View.GONE
        binding.txtPesquisaEstado.text = ""
    }

    // Função para pesquisar um utilizador
    private fun pesquisarUtilizador(nome: String) {
        // Pesquisar na base de dados se o utilizador existe
        viewModel.pesquisarUtilizador(uid, nomeUtilizador, nome)
    }

    // Função para aceitar um convite 1x1 ou 2x2
    private fun aceitarConvite(convite: Convite) {
        viewModel.aceitarConvite(uid, nomeUtilizador, convite)
    }

    private fun abrirSalaConvite(convite: Convite) {
        Toast.makeText(this, R.string.convite_aceite, Toast.LENGTH_SHORT).show()
        // Redirecionar para a sala de espera correspondente
        val intent = when (convite.modo) {
            GameConstants.MODO_2X2 -> Intent(this, SalaDeEspera2x2Activity::class.java)
            else -> Intent(this, SalaDeEspera1x1Activity::class.java)
        }
        val nomeAtual = nomeUtilizador.ifBlank { convite.destinatarioNome }
        nomeAtual.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
        convite.codigoSala.let { intent.putExtra(IntentExtras.CODIGO_SALA, it) }
        intent.putExtra(IntentExtras.NOME_CATEGORIA, convite.nomeCategoria)
        startActivity(intent)
        finish()
    }

    // Função para aceitar um pedido de amizade
    private fun aceitarPedidoAmizade(pedido: PedidoAmizade) {
        // Atualizar o estado do pedido de amizade na base de dados
        viewModel.aceitarPedidoAmizade(uid, nomeUtilizador, pedido)
    }

    private enum class AbaAmigos {
        AMIGOS,
        PEDIDOS,
        CONVITES
    }
}
