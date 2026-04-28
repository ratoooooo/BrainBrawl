package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.databinding.ActivityAmigosBinding
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.repositories.JogadorRepository

class AmigosActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityAmigosBinding.inflate(layoutInflater)
    }
    private val amigosRepository = AmigosRepository()
    private val jogadorRepository = JogadorRepository()
    // Variáveis para armazenar os dados dos amigos, convites e pedidos de amizade
    private var nomeUtilizador: String = ""
    private val amigos = mutableListOf<String>()
    private lateinit var amigoAdapter: AmigoAdapter
    private val avataresAmigos = mutableListOf<String>()
    private val estadoAmigos = mutableListOf<String>()

    private val convitesRecebidos = mutableListOf<Convite1x1>()
    private lateinit var conviteAdapter: ConviteAdapter

    private val pedidosAmizadeRecebidos = mutableListOf<String>()
    private lateinit var pedidoAdapter: PedidoAmizadeAdapter
    private var amigosListenerHandle: AmigosRepository.ListenerHandle? = null
    private var pedidosListenerHandle: AmigosRepository.ListenerHandle? = null
    private var convitesListenerHandle: AmigosRepository.ListenerHandle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os valores passados pela Intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""

        // Configura o adaptador para a lista de amigos
        amigoAdapter = AmigoAdapter(amigos, avataresAmigos, estadoAmigos, nomeUtilizador)
        binding.recyclerAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerAmigos.adapter = amigoAdapter

        // Configura o adaptador para a lista de convites recebidos
        conviteAdapter = ConviteAdapter(convitesRecebidos) { convite ->
            aceitarConvite(convite)
        }
        binding.recyclerConvites.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvites.adapter = conviteAdapter

        pedidoAdapter = PedidoAmizadeAdapter(pedidosAmizadeRecebidos) { nomeOutro ->
            aceitarPedidoAmizade(nomeOutro)
        }
        binding.recyclerPedidosAmizade.layoutManager = LinearLayoutManager(this)
        binding.recyclerPedidosAmizade.adapter = pedidoAdapter

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
            if (nomeNovoAmigo.isEmpty() || nomeNovoAmigo == nomeUtilizador) return@setOnClickListener
            if (amigos.contains(nomeNovoAmigo)) {
                Toast.makeText(this, "Já é teu amigo!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            amigosRepository.enviarPedidoAmizade(nomeUtilizador, nomeNovoAmigo)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pedido de amizade enviado!", Toast.LENGTH_SHORT).show()
                    binding.layoutAddAmigo.visibility = android.view.View.GONE
                    binding.edtPesquisar.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao enviar pedido", Toast.LENGTH_SHORT).show()
                }
        }
        // Configurar o botão para voltar ao MainActivity
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            nomeUtilizador.let { intent.putExtra("nomeUtilizador", it) }
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        iniciarListenersSociais()
    }

    override fun onStop() {
        removerListenersSociais()
        super.onStop()
    }

    override fun onDestroy() {
        removerListenersSociais()
        super.onDestroy()
    }

    private fun iniciarListenersSociais() {
        if (nomeUtilizador.isEmpty() || amigosListenerHandle != null) return

        amigosListenerHandle = amigosRepository.observarAmigos(
            nomeUtilizador,
            onAmigosAlterados = { nomesAmigos ->
                atualizarListaAmigos(nomesAmigos)
            }
        )
        pedidosListenerHandle = amigosRepository.observarPedidosRecebidos(
            nomeUtilizador,
            onPedidosAlterados = { pedidos ->
                atualizarPedidosAmizadeRecebidos(pedidos)
            }
        )
        convitesListenerHandle = amigosRepository.observarConvitesRecebidos(
            nomeUtilizador,
            getString(R.string.categoria5),
            onConvitesAlterados = { convites ->
                atualizarConvitesRecebidos(convites)
            }
        )
    }

    private fun removerListenersSociais() {
        amigosRepository.removerListener(amigosListenerHandle)
        amigosRepository.removerListener(pedidosListenerHandle)
        amigosRepository.removerListener(convitesListenerHandle)
        amigosListenerHandle = null
        pedidosListenerHandle = null
        convitesListenerHandle = null
    }

    // Função para atualizar a lista de amigos
    private fun atualizarListaAmigos(nomesAmigos: List<String>) {
        // Limpa as listas de amigos, avatares e estados
        amigos.clear()
        avataresAmigos.clear()
        estadoAmigos.clear()
        amigos.add(nomeUtilizador)
        avataresAmigos.add("avatar_1_playstore")
        estadoAmigos.add("on")
        /// Buscra os dados do utilizador
        jogadorRepository.obterPerfil(nomeUtilizador)
            .addOnSuccessListener { perfil ->
                // Guardar o nome e o estado do utilizador
                val avatarAtual = perfil?.avatar ?: "avatar_1_playstore"
                val estadoAtual = perfil?.estado ?: "on"
                avataresAmigos[0] = avatarAtual
                estadoAmigos[0] = estadoAtual

                val amigosTemp = nomesAmigos.filter { it != nomeUtilizador }
                val avataresTemp = MutableList(amigosTemp.size) { "avatar_1_playstore" }
                val estadosTemp = MutableList(amigosTemp.size) { "off" }
                if (amigosTemp.isEmpty()) {
                    amigoAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }
                var loaded = 0
                // Percoirrer os amigos e buscar os dados de cada um
                amigosTemp.forEachIndexed { index, nomeAmigo ->
                    // Buscar o avatar e o estado do amigo
                    jogadorRepository.obterPerfil(nomeAmigo)
                        .addOnSuccessListener { perfilAmigo ->
                            avataresTemp[index] = perfilAmigo?.avatar ?: "avatar_1_playstore"
                            estadosTemp[index] = perfilAmigo?.estado ?: "off"
                            loaded++
                            if (loaded == amigosTemp.size) {
                                amigos.addAll(amigosTemp)
                                avataresAmigos.addAll(avataresTemp)
                                estadoAmigos.addAll(estadosTemp)
                                amigoAdapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener {
                            loaded++
                            if (loaded == amigosTemp.size) {
                                amigos.addAll(amigosTemp)
                                avataresAmigos.addAll(avataresTemp)
                                estadoAmigos.addAll(estadosTemp)
                                amigoAdapter.notifyDataSetChanged()
                            }
                        }
                    }
            }
    }

    // Função para atualizar os convites recebidos
    private fun atualizarConvitesRecebidos(convites: List<Convite1x1>) {
        // Limpa a lista de convites recebidos
        convitesRecebidos.clear()
        convitesRecebidos.addAll(convites)
        binding.txtConvites.visibility = if (convitesRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerConvites.visibility = if (convitesRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        // Notifica o adaptador que os dados foram alterados
        conviteAdapter.notifyDataSetChanged()
    }

    // Função para atualizar os pedidos de amizade recebidos
    private fun atualizarPedidosAmizadeRecebidos(pedidos: List<String>) {
        // Limpa a lista de pedidos de amizade recebidos
        pedidosAmizadeRecebidos.clear()
        pedidosAmizadeRecebidos.addAll(pedidos)
        binding.txtPedidosAmizade.visibility = if (pedidosAmizadeRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerPedidosAmizade.visibility = if (pedidosAmizadeRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        pedidoAdapter.notifyDataSetChanged()
    }

    // Função para pesquisar um utilizador
    private fun pesquisarUtilizador(nome: String) {
        if (nome.isEmpty() || nome == nomeUtilizador) {
            binding.layoutAddAmigo.visibility = android.view.View.GONE
            return
        }
        // Pwscar na base de dados se o utilizador existe
        amigosRepository.pesquisarJogadorParaAdicionar(nome).addOnSuccessListener { existe ->
            if (existe) {
                if (amigos.contains(nome)) {
                    binding.layoutAddAmigo.visibility = android.view.View.GONE
                    Toast.makeText(this, "$nome já é teu amigo!", Toast.LENGTH_SHORT).show()
                } else {
                    binding.layoutAddAmigo.visibility = android.view.View.VISIBLE
                    binding.btnAdicionarAmigo.text = "Adicionar $nome"
                }
            } else {
                binding.layoutAddAmigo.visibility = android.view.View.GONE
                Toast.makeText(this, "Utilizador não encontrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função para aceitar um convite 1x1 ou 2x2
    private fun aceitarConvite(convite: Convite1x1) {
        // Atualizar o estado do convite na base de dados
        amigosRepository.aceitarConvite(nomeUtilizador, convite.nomeAmigo)
        Toast.makeText(this, "Convite aceite!", Toast.LENGTH_SHORT).show()

        // Redirecionar para a sala de espera correspondente
        val intent = when (convite.modo) {
            "2x2" -> Intent(this, SalaDeEspera2x2Activity::class.java)
            else -> Intent(this, SalaDeEspera1x1Activity::class.java)
        }
        nomeUtilizador.let { intent.putExtra("nomeUtilizador", it) }
        convite.codigoSala.let { intent.putExtra("codigoSala", it) }
        intent.putExtra("nomeCategoria", convite.nomeCategoria)
        startActivity(intent)
        finish()
    }

    // Função para aceitar um pedido de amizade
    private fun aceitarPedidoAmizade(nomeOutro: String) {
        // Atualizar o estado do pedido de amizade na base de dados
        amigosRepository.aceitarPedidoAmizade(nomeUtilizador, nomeOutro)
            .addOnSuccessListener {
                Toast.makeText(this, "Amizade aceite!", Toast.LENGTH_SHORT).show()
            }
    }
}
