package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.databinding.ActivityAmigosBinding
import com.google.firebase.database.FirebaseDatabase

class AmigosActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityAmigosBinding.inflate(layoutInflater)
    }
    private val database = FirebaseDatabase.getInstance().reference
    private var nomeUtilizador: String = ""
    private val amigos = mutableListOf<String>()
    private lateinit var amigoAdapter: AmigoAdapter

    // Lista de convites recebidos
    private val convitesRecebidos = mutableListOf<Convite1x1>()
    private lateinit var conviteAdapter: ConviteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""

        // Adapter para amigos
        amigoAdapter = AmigoAdapter(amigos, nomeUtilizador)
        binding.recyclerAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerAmigos.adapter = amigoAdapter

        // Adapter para convites recebidos
        conviteAdapter = ConviteAdapter(convitesRecebidos) { convite ->
            aceitarConvite(convite)
        }
        binding.recyclerConvites.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvites.adapter = conviteAdapter

        // Carregar amigos e convites
        carregarListaAmigos()
        carregarConvitesRecebidos()

        // Pesquisa ao clicar na lupa
        binding.btnPesquisar.setOnClickListener {
            val nomePesquisa = binding.edtPesquisar.text.toString().trim()
            pesquisarUtilizador(nomePesquisa)
        }

        // Pesquisa ao carregar em "Pesquisar" no teclado
        binding.edtPesquisar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                pesquisarUtilizador(binding.edtPesquisar.text.toString().trim())
                true
            } else false
        }

        // Botão de adicionar amigo
        binding.btnAdicionarAmigo.setOnClickListener {
            val nomeNovoAmigo = binding.edtPesquisar.text.toString().trim()
            if (nomeNovoAmigo.isEmpty() || nomeNovoAmigo == nomeUtilizador) return@setOnClickListener
            database.child("jogadores").child(nomeUtilizador).child("amigos").child(nomeNovoAmigo).setValue(true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Amigo adicionado!", Toast.LENGTH_SHORT).show()
                    binding.layoutAddAmigo.visibility = android.view.View.GONE
                    binding.edtPesquisar.text.clear()
                    carregarListaAmigos()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao adicionar amigo", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Carregar amigos da base de dados
    private fun carregarListaAmigos() {
        amigos.clear()
        database.child("jogadores").child(nomeUtilizador).child("amigos")
            .get().addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    amigos.add(child.key ?: "")
                }
                amigoAdapter.notifyDataSetChanged()
            }
    }

    // Carregar convites recebidos da base de dados
    private fun carregarConvitesRecebidos() {
        convitesRecebidos.clear()
        database.child("jogadores").child(nomeUtilizador).child("convites_recebidos")
            .get().addOnSuccessListener { snapshot ->
                // Percorrer os convites recebidos
                for (convite in snapshot.children) {
                    val nomeAmigo = convite.key ?: continue
                    val estado = convite.child("estado").getValue(String::class.java) ?: ""
                    val salaId = convite.child("salaId").getValue(String::class.java) ?: ""
                    val modo = convite.child("modo").getValue(String::class.java) ?: "1x1"
                    if (estado == "pendente") {
                        convitesRecebidos.add(Convite1x1(nomeAmigo, salaId, modo))
                    }
                }
                // Mostrar apenas se houver convites recebidos
                binding.txtConvites.visibility = if (convitesRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.recyclerConvites.visibility = if (convitesRecebidos.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                conviteAdapter.notifyDataSetChanged()
            }
    }

    // Função utilizada para pesquisar por jogador
    private fun pesquisarUtilizador(nome: String) {
        if (nome.isEmpty() || nome == nomeUtilizador) {
            binding.layoutAddAmigo.visibility = android.view.View.GONE
            return
        }
        database.child("jogadores").child(nome).get().addOnSuccessListener { snapshot ->
            // Verifica se o utilizador existe
            if (snapshot.exists()) {
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

    // Função para aceitar pedido
    private fun aceitarConvite(convite: Convite1x1) {
        // Atualiza o estado do convite para aceite
        database.child("jogadores").child(nomeUtilizador).child("convites_recebidos").child(convite.nomeAmigo).child("estado").setValue("aceite")
        database.child("jogadores").child(convite.nomeAmigo).child("convites_enviados").child(nomeUtilizador).child("estado").setValue("aceite")
        Toast.makeText(this, "Convite aceite!", Toast.LENGTH_SHORT).show()

        // Redirecionamento correto para 1x1 ou 2x2
        val intent = when (convite.modo) {
            "2x2" -> Intent(this, SalaDeEspera2x2Activity::class.java)
            else -> Intent(this, SalaDeEspera1x1Activity::class.java)
        }
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("salaId", convite.salaId)
        startActivity(intent)
        finish()
    }
}