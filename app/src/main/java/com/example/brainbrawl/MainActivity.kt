package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMainBinding
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.viewmodels.MainInput
import com.example.brainbrawl.viewmodels.MainViewModel

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private var nomeCategoria: String? = null
    private var codigoSala: String? = null
    private var uid: String? = null
    private var email: String? = null
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var modoJogo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        uid = savedInstanceState?.getString(IntentExtras.UID)
            ?: intent.getStringExtra(IntentExtras.UID)
        email = savedInstanceState?.getString(IntentExtras.EMAIL)
            ?: intent.getStringExtra(IntentExtras.EMAIL)
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

        configurarObservers()
        configurarClicks()
        viewModel.iniciar(
            MainInput(
                uid = uid.orEmpty(),
                email = email.orEmpty(),
                nomeUtilizador = nomeUtilizador.orEmpty(),
                nomeJogador = nomeJogador.orEmpty(),
                nomeCategoria = nomeCategoria.orEmpty(),
                codigoSala = codigoSala.orEmpty(),
                modoJogo = modoJogo.orEmpty()
            )
        )
    }

    override fun onStart() {
        super.onStart()
        viewModel.iniciarNotificacoes(getString(R.string.categoria5))
    }

    override fun onStop() {
        viewModel.pararNotificacoes()
        super.onStop()
    }

    private fun configurarObservers() {
        viewModel.uiState.observe(this) { state ->
            uid = state.uid.ifBlank { null }
            email = state.email.ifBlank { null }
            nomeUtilizador = state.nomeUtilizador.ifBlank { null }
            nomeJogador = state.nomeJogador.ifBlank { null }
            nomeCategoria = state.nomeCategoria.ifBlank { null }
            codigoSala = state.codigoSala.ifBlank { null }
            modoJogo = state.modoJogo.ifBlank { null }

            binding.txtBoasVindas.text = state.boasVindas
            binding.btnAddAmigo.visibility = if (state.amigosVisivel) View.VISIBLE else View.GONE
            binding.txtNivel.text = getString(R.string.nivel_format, state.nivel)
            binding.txtLevelBadge.text = state.nivel.toString()
            binding.txtXp.text = getString(
                R.string.xp_progress_format,
                state.xpNoNivelAtual,
                state.xpNecessarioProximoNivel
            )
            binding.progressXp.max = state.xpNecessarioProximoNivel.coerceAtLeast(1)
            binding.progressXp.progress = state.xpNoNivelAtual.coerceAtLeast(0)
            if (state.avatar.isNotBlank()) {
                binding.imgAvatar.setImageResource(AvatarUtils.resolverAvatar(this, state.avatar))
            }
            val matchmakingVisivel = state.uid.isNotBlank()
            binding.btnMatchmaking1x1.visibility = if (matchmakingVisivel) View.VISIBLE else View.GONE
            binding.btnMatchmaking2x2.visibility = if (matchmakingVisivel) View.VISIBLE else View.GONE
            binding.btnMatchmaking1x1.isEnabled = matchmakingVisivel
            binding.btnMatchmaking2x2.isEnabled = matchmakingVisivel
            atualizarBadgeNotificacoes(state.notificacoesPendentes, state.amigosVisivel)
        }
    }

    private fun configurarClicks() {
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

        binding.btnRanking.setOnClickListener { abrirRanking() }
        binding.btnRankingNav.setOnClickListener { abrirRanking() }
        binding.btnMatchmaking1x1.setOnClickListener { abrirMatchmaking(GameConstants.MODO_1X1) }
        binding.btnMatchmaking2x2.setOnClickListener { abrirMatchmaking(GameConstants.MODO_2X2) }

        binding.btnHistorico.setOnClickListener {
            val intent = Intent(this, HistoricoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            adicionarAuthExtras(intent)
            startActivity(intent)
        }

        binding.avatarFrame.setOnClickListener { abrirPerfil() }
        binding.btnPerfil.setOnClickListener { abrirPerfil() }
        binding.btnAddAmigo.setOnClickListener { abrirAmigos() }

        binding.btnVoltar.setOnClickListener {
            viewModel.terminarSessao()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun atualizarBadgeNotificacoes(total: Int, amigosVisivel: Boolean) {
        val mostrar = total > 0 && amigosVisivel
        binding.notificationBadgeAmigos.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.notificationBadgeAmigos.text = when {
            total > 9 -> "9+"
            total > 0 -> total.toString()
            else -> ""
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
            Toast.makeText(this, R.string.perfil_requer_conta, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MeuPerfilActivity::class.java)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        adicionarAuthExtras(intent)
        startActivity(intent)
    }

    private fun abrirAmigos() {
        val utilizador = nomeUtilizador
        if (utilizador.isNullOrBlank()) {
            Toast.makeText(this, R.string.amigos_requer_conta, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, AmigosActivity::class.java)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, utilizador)
        adicionarAuthExtras(intent)
        startActivity(intent)
    }

    private fun abrirMatchmaking(modo: String) {
        val uidAtual = uid
        if (uidAtual.isNullOrBlank()) {
            Toast.makeText(this, R.string.matchmaking_requer_conta, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MatchmakingActivity::class.java)
        intent.putExtra(IntentExtras.MODO_JOGO, modo)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria ?: getString(R.string.categoria5))
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        adicionarAuthExtras(intent)
        startActivity(intent)
    }

    private fun adicionarAuthExtras(intent: Intent) {
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        email?.let { intent.putExtra(IntentExtras.EMAIL, it) }
    }

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
