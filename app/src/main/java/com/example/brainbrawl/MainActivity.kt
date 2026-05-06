package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.config.GameConstants
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

        atualizarBoasVindas()
        carregarPerfilPrincipal()


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

        binding.btnMatchmaking1x1.setOnClickListener {
            abrirMatchmaking(GameConstants.MODO_1X1)
        }

        binding.btnMatchmaking2x2.setOnClickListener {
            abrirMatchmaking(GameConstants.MODO_2X2)
        }

        binding.btnRanking.setOnClickListener {
            abrirRanking()
        }

        binding.btnRankingNav.setOnClickListener {
            abrirRanking()
        }

        binding.btnHistorico.setOnClickListener {
            val intent = Intent(this, HistoricoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            adicionarAuthExtras(intent)
            startActivity(intent)
        }

        binding.avatarFrame.setOnClickListener {
            abrirPerfil()
        }

        binding.btnPerfil.setOnClickListener {
            abrirPerfil()
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
            binding.txtBoasVindas.text = nomeUtilizador
            binding.btnAddAmigo.visibility = View.VISIBLE
            binding.btnAddAmigo.setOnClickListener {
                val intent = Intent(this, AmigosActivity::class.java)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                adicionarAuthExtras(intent)
                startActivity(intent)
            }
        } else if (nomeJogador != null) {
            binding.txtBoasVindas.text = nomeJogador
            binding.btnAddAmigo.visibility = View.GONE
        } else {
            binding.txtBoasVindas.text = "Jogador"
            binding.btnAddAmigo.visibility = View.GONE
        }
    }

    private fun carregarPerfilPrincipal() {
        val identificador = uid?.takeIf { it.isNotBlank() }
            ?: nomeUtilizador?.takeIf { it.isNotBlank() }
            ?: return

        jogadorRepository.obterPerfil(identificador)
            .addOnSuccessListener { perfil ->
                perfil ?: return@addOnSuccessListener

                if (uid.isNullOrBlank()) {
                    uid = perfil.uid.takeIf { it.isNotBlank() }
                }
                nomeUtilizador = perfil.nomeUtilizador.takeIf { it.isNotBlank() } ?: nomeUtilizador
                email = email ?: perfil.email.takeIf { it.isNotBlank() }

                binding.txtBoasVindas.text = perfil.nomeUtilizador.ifBlank { nomeUtilizador ?: "Jogador" }
                binding.txtNivel.text = "Nível ${perfil.estatisticas.nivel}"
                binding.txtLevelBadge.text = perfil.estatisticas.nivel.toString()
                binding.txtXp.text = "${perfil.estatisticas.xpNoNivelAtual} / ${perfil.estatisticas.xpNecessarioProximoNivel} XP"
                binding.progressXp.max = perfil.estatisticas.xpNecessarioProximoNivel.coerceAtLeast(1)
                binding.progressXp.progress = perfil.estatisticas.xpNoNivelAtual.coerceAtLeast(0)

                val avatarRes = resources.getIdentifier(perfil.avatar, "drawable", packageName)
                    .takeIf { it != 0 }
                    ?: R.drawable.avatar_1_playstore
                binding.imgAvatar.setImageResource(avatarRes)
                atualizarBoasVindas()
            }
    }

    private fun abrirRanking() {
        val intent = Intent(this, RankingActivity::class.java)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        adicionarAuthExtras(intent)
        startActivity(intent)
    }

    private fun abrirPerfil() {
        if (uid.isNullOrBlank() && nomeUtilizador.isNullOrBlank()) {
            Toast.makeText(this, "Perfil disponível apenas para contas com sessão iniciada.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MeuPerfilActivity::class.java)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        adicionarAuthExtras(intent)
        startActivity(intent)
    }

    private fun adicionarAuthExtras(intent: Intent) {
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        email?.let { intent.putExtra(IntentExtras.EMAIL, it) }
    }

    private fun abrirMatchmaking(modo: String) {
        val intent = Intent(this, MatchmakingActivity::class.java)
        intent.putExtra(IntentExtras.MODO_JOGO, modo)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, getString(R.string.categoria5))
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        adicionarAuthExtras(intent)
        startActivity(intent)
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
