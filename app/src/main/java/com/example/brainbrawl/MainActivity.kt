package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityMainBinding
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    // Variáveis para armazenar informações do utilizador e da sala
    private var nomeCategoria: String? = null
    private var codigoSala: String? = null
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var modoJogo: String? = null
    private var admin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Recuperar dados do savedInstanceState ou do intent se for a primeira vez
        nomeUtilizador = savedInstanceState?.getString("nomeUtilizador")
            ?: intent.getStringExtra("nomeUtilizador")
        nomeJogador = savedInstanceState?.getString("nomeJogador")
            ?: intent.getStringExtra("nomeJogador")
        nomeCategoria = savedInstanceState?.getString("nomeCategoria")
            ?: intent.getStringExtra("nomeCategoria")
        codigoSala = savedInstanceState?.getString("codigoSala")
            ?: intent.getStringExtra("codigoSala")
        modoJogo = savedInstanceState?.getString("modoJogo")
            ?: intent.getStringExtra("modoJogo")

        // Se o utilizador estiver autenticado, mostrar mensagem de boas-vindas e botão de amigos
        if (nomeUtilizador != null) {
            binding.txtBoasVindas.text = "Bem-vindo, $nomeUtilizador!"
            binding.btnAddAmigo.visibility = View.VISIBLE
            binding.btnAddAmigo.setOnClickListener {
                val intent = Intent(this, AmigosActivity::class.java)
                intent.putExtra("nomeUtilizador", nomeUtilizador)
                startActivity(intent)
            }
        } else if (nomeJogador != null) {
            binding.txtBoasVindas.text = "Bem-vindo, $nomeJogador!"
            binding.btnAddAmigo.visibility = View.GONE
        } else {
            binding.txtBoasVindas.text = "Bem-vindo!"
            binding.btnAddAmigo.visibility = View.GONE
        }


        // BConfigurar Botões
        binding.btnCriarSala.setOnClickListener {
            val intent = Intent(this, EscolherModoActivity::class.java)
            if (nomeUtilizador != null) {
                intent.putExtra("nomeUtilizador", nomeUtilizador)
                intent.putExtra("admin", true)
            } else if (nomeJogador != null) {
                intent.putExtra("nomeJogador", nomeJogador)
                intent.putExtra("admin", true)
            }
            startActivity(intent)
        }

        binding.btnEntrarSala.setOnClickListener {
            val intent = Intent(this, SalaDeEsperaActivity::class.java)
            // Passa o nome do utilizador ou jogador (se já estiver preenchido)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            startActivity(intent)
        }

        binding.btnExplorarCategorias.setOnClickListener {
            val intent = Intent(this, ExplorarCategoriasActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            startActivity(intent)
        }

        // Botão para voltar ao ecrã de login
        binding.btnVoltar.setOnClickListener {
            // Mudar estado do jogador para 'off' no Firebase, só se for utilizador autenticado
            nomeUtilizador?.let {
                database.child("jogadores").child(it).child("estado").setValue("off")
            }
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Guardar estado da activity para rotações/dispositivo
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("nomeUtilizador", nomeUtilizador)
        outState.putString("nomeJogador", nomeJogador)
        outState.putString("nomeCategoria", nomeCategoria)
        outState.putString("codigoSala", codigoSala)
        outState.putString("modoJogo", modoJogo)
    }
}
