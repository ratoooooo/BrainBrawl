package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private val amigos = mutableListOf<UtilizadorSocial>()
    private lateinit var amigoAdapter: AmigoAdapter
    private val avataresAmigos = mutableListOf<String>()
    private val estadoAmigos = mutableListOf<String>()

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
        binding.btnVoltar.visibility = android.view.View.GONE

        // Configura o adaptador para a lista de amigos
        amigoAdapter = AmigoAdapter(amigos, avataresAmigos, estadoAmigos, nomeUtilizador, uid)
        binding.recyclerAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerAmigos.adapter = amigoAdapter

        // Configura o adaptador para a lista de convites recebidos
        conviteAdapter = ConviteAdapter(convitesRecebidos) { convite ->
            aceitarConvite(convite)
        }
        binding.recyclerConvites.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvites.adapter = conviteAdapter

        pedidoAdapter = PedidoAmizadeAdapter(pedidosAmizadeRecebidos) { pedido ->
            aceitarPedidoAmizade(pedido)
        }
        binding.recyclerPedidosAmizade.layoutManager = LinearLayoutManager(this)
        binding.recyclerPedidosAmizade.adapter = pedidoAdapter

        configurarObservers()

        // Configurar o botao de pesquisa
        binding.btnPesquisar.setOnClickListener {
            val nomePesquisa = binding.edtPesquisar.text.toString().trim()
            pesquisarUtilizador(nomePesquisa)
        }
        // Configurar botao de pesquisa
        binding.edtPesquisar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                pesquisarUtilizador(binding.edtPesquisar.text.toString().trim())
                true
            } else false
        }
        //Configurar botao de adicionar amigo
        binding.btnAdicionarAmigo.setOnClickListener {
            val nomeNovoAmigo = binding.edtPesquisar.text.toString().trim()
            viewModel.enviarPedidoAmizade(uid, nomeUtilizador, nomeNovoAmigo)
        }
        // Configurar o botão para voltar ao MainActivity
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            nomeUtilizador.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
            startActivity(intent)
            finish()
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
                binding.layoutAddAmigo.visibility = android.view.View.GONE
            }
            is AmigosEvent.JogadorEncontrado -> {
                binding.layoutAddAmigo.visibility = android.view.View.VISIBLE
                binding.btnAdicionarAmigo.text = getString(R.string.adicionar_nome_format, evento.nome)
            }
            is AmigosEvent.JogadorJaAmigo -> {
                binding.layoutAddAmigo.visibility = android.view.View.GONE
                Toast.makeText(this, getString(R.string.ja_e_teu_amigo_format, evento.nome), Toast.LENGTH_SHORT).show()
            }
            AmigosEvent.JogadorNaoEncontrado -> {
                binding.layoutAddAmigo.visibility = android.view.View.GONE
                Toast.makeText(this, R.string.utilizador_nao_encontrado, Toast.LENGTH_SHORT).show()
            }
            AmigosEvent.PedidoJaAmigo -> {
                Toast.makeText(this, R.string.ja_e_teu_amigo, Toast.LENGTH_SHORT).show()
            }
            AmigosEvent.PedidoEnviado -> {
                Toast.makeText(this, R.string.pedido_amizade_enviado, Toast.LENGTH_SHORT).show()
                binding.layoutAddAmigo.visibility = android.view.View.GONE
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
        }
    }

    // Função para atualizar a lista de amigos
    private fun atualizarListaAmigos(estado: AmigosListaUiState) {
        // Limpa as listas de amigos, avatares e estados
        amigos.clear()
        avataresAmigos.clear()
        estadoAmigos.clear()
        amigos.addAll(estado.utilizadores)
        avataresAmigos.addAll(estado.avatares)
        estadoAmigos.addAll(estado.estados)
        amigoAdapter.notifyDataSetChanged()
        binding.recyclerAmigos.visibility = if (amigos.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        binding.txtAmigosVazio.visibility = if (amigos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
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

    // Função para pesquisar um utilizador
    private fun pesquisarUtilizador(nome: String) {
        // Pesquisar na base de dados se o utilizador existe
        viewModel.pesquisarUtilizador(uid, nomeUtilizador, nome)
    }

    // Função para aceitar um convite 1x1 ou 2x2
    private fun aceitarConvite(convite: Convite) {
        // Atualizar o estado do convite na base de dados
        viewModel.aceitarConvite(uid, nomeUtilizador, convite)
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
}
