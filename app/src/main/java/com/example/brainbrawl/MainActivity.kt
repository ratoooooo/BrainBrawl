package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMainBinding
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.services.AuthService

class MainActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    // Acessar a base de dados
    private val jogadorRepository = JogadorRepository()
    private val authService = AuthService()
    // Variáveis para armazenar informações do utilizador e da sala
    private var nomeCategoria: String? = null
    private var codigoSala: String? = null
    private var uid: String? = null
    private var email: String? = null
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var modoJogo: String? = null
    private var admin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Recuperar dados do savedInstanceState ou do intent se for a primeira vez
        uid = savedInstanceState?.getString(IntentExtras.UID)
            ?: intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
        email = savedInstanceState?.getString(IntentExtras.EMAIL)
            ?: intent.getStringExtra(IntentExtras.EMAIL)
            ?: authService.utilizadorAtual()?.email
        nomeUtilizador = savedInstanceState?.getString(IntentExtras.NOME_UTILIZADOR)
            ?: intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = savedInstanceState?.getString(IntentExtras.NOME_JOGADOR)
            ?: intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        nomeCategoria = savedInstanceState?.getString(IntentExtras.NOME_CATEGORIA)
            ?: intent.getStringExtra(IntentExtras.NOME_CATEGORIA)
        codigoSala = savedInstanceState?.getString(IntentExtras.CODIGO_SALA)
            ?: intent.getStringExtra(IntentExtras.CODIGO_SALA)
        modoJogo = savedInstanceState?.getString(IntentExtras.MODO_JOGO)
            ?: intent.getStringExtra(IntentExtras.MODO_JOGO)

        if (nomeUtilizador.isNullOrBlank() && !uid.isNullOrBlank()) {
            jogadorRepository.obterPerfil(uid.orEmpty())
                .addOnSuccessListener { perfil ->
                    perfil ?: return@addOnSuccessListener
                    nomeUtilizador = perfil.nomeUtilizador
                    email = email ?: perfil.email
                    atualizarBoasVindas()
                }
        }

        atualizarBoasVindas()


        // BConfigurar Botões
        binding.btnCriarSala.setOnClickListener {
            val intent = Intent(this, EscolherModoActivity::class.java)
            if (nomeUtilizador != null) {
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                intent.putExtra(IntentExtras.ADMIN, true)
            } else if (nomeJogador != null) {
                intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                intent.putExtra(IntentExtras.ADMIN, true)
            }
            adicionarAuthExtras(intent)
            startActivity(intent)
        }

        binding.btnEntrarSala.setOnClickListener {
            val intent = Intent(this, SalaDeEsperaActivity::class.java)
            // Passa o nome do utilizador ou jogador (se já estiver preenchido)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            adicionarAuthExtras(intent)
            startActivity(intent)
        }

        binding.btnExplorarCategorias.setOnClickListener {
            val intent = Intent(this, ExplorarCategoriasActivity::class.java)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            adicionarAuthExtras(intent)
            startActivity(intent)
        }

        binding.btnRanking.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            adicionarAuthExtras(intent)
            startActivity(intent)
        }

        // Botão para voltar ao ecrã de login
        binding.btnVoltar.setOnClickListener {
            // Mudar estado do jogador para 'off' no Firebase, só se for utilizador autenticado
            (uid ?: nomeUtilizador)?.let {
                jogadorRepository.marcarOffline(it)
            }
            authService.terminarSessao()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun atualizarBoasVindas() {
        if (nomeUtilizador != null) {
            binding.txtBoasVindas.text = "Bem-vindo, $nomeUtilizador!"
            binding.btnAddAmigo.visibility = View.VISIBLE
            binding.btnAddAmigo.setOnClickListener {
                val intent = Intent(this, AmigosActivity::class.java)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                adicionarAuthExtras(intent)
                startActivity(intent)
            }
        } else if (nomeJogador != null) {
            binding.txtBoasVindas.text = "Bem-vindo, $nomeJogador!"
            binding.btnAddAmigo.visibility = View.GONE
        } else {
            binding.txtBoasVindas.text = "Bem-vindo!"
            binding.btnAddAmigo.visibility = View.GONE
        }
    }

    private fun adicionarAuthExtras(intent: Intent) {
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        email?.let { intent.putExtra(IntentExtras.EMAIL, it) }
    }

    // Guardar estado da activity para rotações/dispositivo
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(IntentExtras.UID, uid)
        outState.putString(IntentExtras.EMAIL, email)
        outState.putString(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        outState.putString(IntentExtras.NOME_JOGADOR, nomeJogador)
        outState.putString(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        outState.putString(IntentExtras.CODIGO_SALA, codigoSala)
        outState.putString(IntentExtras.MODO_JOGO, modoJogo)
    }
}
