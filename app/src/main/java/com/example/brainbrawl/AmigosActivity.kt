package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.databinding.ActivityAmigosBinding
import com.example.brainbrawl.repositories.JogadorRepository
import com.google.firebase.database.FirebaseDatabase

class AmigosActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityAmigosBinding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
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

        // Chamar as funções para carregar a lista de amigos
        carregarListaAmigos()
        // Carregar convites e pedidos de amizade
        carregarConvitesRecebidos()
        // Carregar pedidos de amizade recebidos
        carregarPedidosAmizadeRecebidos()

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
            val pedido = mapOf("estado" to "pendente")
            database.child("jogadores").child(nomeNovoAmigo).child("pedidos_amizade").child(nomeUtilizador).setValue(pedido)
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

    // Função para carregar a lista de amigos
    private fun carregarListaAmigos() {
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

                // Buscar os amigos do utilizador
                database.child("jogadores").child(nomeUtilizador).child("amigos")
                    .get().addOnSuccessListener amigosListener@ { snapshot ->
                        val amigosTemp = mutableListOf<String>()
                        val avataresTemp = mutableListOf<String>()
                        val estadosTemp = mutableListOf<String>()
                        val children = snapshot.children.toList()
                        if (children.isEmpty()) {
                            amigoAdapter.notifyDataSetChanged()
                            return@amigosListener
                        }
                        var loaded = 0
                        // Percoirrer os amigos e buscar os dados de cada um
                        for (child in children) {
                            val nomeAmigo = child.key ?: continue
                            if (nomeAmigo == nomeUtilizador) continue
                            amigosTemp.add(nomeAmigo)
                            // Buscar o avatar e o estado do amigo
                            jogadorRepository.obterPerfil(nomeAmigo)
                                .addOnSuccessListener { perfilAmigo ->
                                    val avatarAmigo = perfilAmigo?.avatar ?: "avatar_1_playstore"
                                    val estadoAmigo = perfilAmigo?.estado ?: "off"
                                    avataresTemp.add(avatarAmigo)
                                    estadosTemp.add(estadoAmigo)
                                    loaded++
                                    if (loaded == children.size) {
                                        amigos.addAll(amigosTemp)
                                        avataresAmigos.addAll(avataresTemp)
                                        estadoAmigos.addAll(estadosTemp)
                                        amigoAdapter.notifyDataSetChanged()
                                    }
                                }
                        }
                        if (children.isEmpty()) {
                            amigoAdapter.notifyDataSetChanged()
                        }
                    }
            }
    }

    // Função para carregar os convites recebidos
    private fun carregarConvitesRecebidos() {
        // Limpa a lista de convites recebidos
        convitesRecebidos.clear()
        // Busca os convites recebidos do utilizador
        database.child("jogadores").child(nomeUtilizador).child("convites_recebidos")
            .get().addOnSuccessListener { snapshot ->
                // Percorre os convites recebidos e adiciona-os à lista
                for (convite in snapshot.children) {
                    val nomeAmigo = convite.key ?: continue
                    val estado = convite.child("estado").getValue(String::class.java) ?: ""
                    val codigoSala = convite.child("codigoSala").getValue(String::class.java) ?: ""
                    val modo = convite.child("modo").getValue(String::class.java) ?: "1x1"
                    val nomeCategoria = convite.child("nomeCategoria").getValue(String::class.java) ?: getString(R.string.categoria5)
                    if (estado == "pendente") {
                        convitesRecebidos.add(Convite1x1(nomeAmigo, codigoSala, modo, nomeCategoria))
                    }
                }
                binding.txtConvites.visibility = if (convitesRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.recyclerConvites.visibility = if (convitesRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                // Notifica o adaptador que os dados foram alterados
                conviteAdapter.notifyDataSetChanged()
            }
    }

    // Função para carregar os pedidos de amizade recebidos
    private fun carregarPedidosAmizadeRecebidos() {
        // Limpa a lista de pedidos de amizade recebidos
        pedidosAmizadeRecebidos.clear()
        // Busca os pedidos de amizade recebidos do utilizador
        database.child("jogadores").child(nomeUtilizador).child("pedidos_amizade")
            .get().addOnSuccessListener { snapshot ->
                // Percorre os pedidos de amizade recebidos e adiciona-os à lista
                for (pedido in snapshot.children) {
                    val nomeOutro = pedido.key ?: continue
                    val estado = pedido.child("estado").getValue(String::class.java) ?: ""
                    if (estado == "pendente") {
                        pedidosAmizadeRecebidos.add(nomeOutro)
                    }
                }
                binding.txtPedidosAmizade.visibility = if (pedidosAmizadeRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.recyclerPedidosAmizade.visibility = if (pedidosAmizadeRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                pedidoAdapter.notifyDataSetChanged()
            }
    }

    // Função para pesquisar um utilizador
    private fun pesquisarUtilizador(nome: String) {
        if (nome.isEmpty() || nome == nomeUtilizador) {
            binding.layoutAddAmigo.visibility = android.view.View.GONE
            return
        }
        // Pwscar na base de dados se o utilizador existe
        jogadorRepository.obterPerfil(nome).addOnSuccessListener { perfil ->
            if (perfil != null) {
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
        database.child("jogadores").child(nomeUtilizador).child("convites_recebidos").child(convite.nomeAmigo).child("estado").setValue("aceite")
        database.child("jogadores").child(convite.nomeAmigo).child("convites_enviados").child(nomeUtilizador).child("estado").setValue("aceite")
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
        database.child("jogadores").child(nomeUtilizador).child("amigos").child(nomeOutro).setValue(true)
        database.child("jogadores").child(nomeOutro).child("amigos").child(nomeUtilizador).setValue(true)
        database.child("jogadores").child(nomeUtilizador).child("pedidos_amizade").child(nomeOutro).removeValue()
        database.child("jogadores").child(nomeOutro).child("pedidos_amizade").child(nomeUtilizador).removeValue()
        Toast.makeText(this, "Amizade aceite!", Toast.LENGTH_SHORT).show()
        carregarListaAmigos()
        carregarPedidosAmizadeRecebidos()
    }
}
