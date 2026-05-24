package com.example.brainbrawl

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityConquistasBinding
import com.example.brainbrawl.models.Badge
import com.example.brainbrawl.models.BadgeFamily
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.BadgeGridRenderer
import com.example.brainbrawl.viewmodels.MeuPerfilViewModel
import com.example.brainbrawl.viewmodels.PerfilAmigoViewModel

class ConquistasActivity : AppCompatActivity() {
    private val binding by lazy { ActivityConquistasBinding.inflate(layoutInflater) }
    private val authService = AuthService()
    private val meuPerfilViewModel by lazy {
        ViewModelProvider(this)[MeuPerfilViewModel::class.java]
    }
    private val perfilAmigoViewModel by lazy {
        ViewModelProvider(this)[PerfilAmigoViewModel::class.java]
    }
    private var badgesAtuais: List<Badge> = emptyList()
    private var filtroAtual = FiltroConquistas.TODAS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnVoltarConquistas.bringToFront()
        binding.btnVoltarConquistas.setOnClickListener { finish() }
        configurarTabs()

        val uidAmigo = intent.getStringExtra(IntentExtras.UID_AMIGO).orEmpty()
        val nomeAmigo = intent.getStringExtra(IntentExtras.NOME_AMIGO).orEmpty()
        if (uidAmigo.isNotBlank() || nomeAmigo.isNotBlank()) {
            carregarConquistasAmigo(uidAmigo, nomeAmigo)
        } else {
            carregarConquistasProprias()
        }
    }

    private fun carregarConquistasProprias() {
        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid.orEmpty()
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR).orEmpty()
        meuPerfilViewModel.perfil.observe(this) { perfil ->
            binding.txtSubtituloConquistas.text = getString(R.string.conquistas_todas_subtitulo)
            atualizarBadges(perfil.badges)
        }
        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            Toast.makeText(this, R.string.perfil_indisponivel, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        meuPerfilViewModel.carregarPerfil(uid, nomeUtilizador)
    }

    private fun carregarConquistasAmigo(uidAmigo: String, nomeAmigo: String) {
        perfilAmigoViewModel.perfil.observe(this) { perfil ->
            binding.txtSubtituloConquistas.text = getString(R.string.conquistas_amigo_publicas)
            atualizarBadges(perfil.badges)
        }
        perfilAmigoViewModel.carregarPerfil(uidAmigo.ifBlank { nomeAmigo }, nomeAmigo)
    }

    private fun configurarTabs() {
        listOf(
            binding.tabTodas to FiltroConquistas.TODAS,
            binding.tabCombate to FiltroConquistas.COMBATE,
            binding.tabExploracao to FiltroConquistas.EXPLORACAO,
            binding.tabSocial to FiltroConquistas.SOCIAL
        ).forEach { (tab, filtro) ->
            tab.setOnClickListener {
                filtroAtual = filtro
                atualizarTabs()
                renderizarBadges()
            }
        }
        atualizarTabs()
    }

    private fun atualizarTabs() {
        listOf(
            binding.tabTodas to FiltroConquistas.TODAS,
            binding.tabCombate to FiltroConquistas.COMBATE,
            binding.tabExploracao to FiltroConquistas.EXPLORACAO,
            binding.tabSocial to FiltroConquistas.SOCIAL
        ).forEach { (tab, filtro) ->
            val ativo = filtro == filtroAtual
            tab.background = ContextCompat.getDrawable(
                this,
                if (ativo) R.drawable.bg_luso_segment_selected else R.drawable.bg_luso_segment_unselected
            )
            tab.setTextColor(
                ContextCompat.getColor(this, if (ativo) R.color.bb_primary_text else R.color.bb_luso_navy)
            )
        }
    }

    private fun atualizarBadges(badges: List<Badge>) {
        badgesAtuais = badges
        val desbloqueadas = badges.count { it.desbloqueada }
        binding.txtResumoConquistas.text = getString(
            R.string.conquistas_progress_format,
            desbloqueadas,
            badges.size
        )
        binding.progressConquistas.max = badges.size.coerceAtLeast(1)
        binding.progressConquistas.progress = desbloqueadas
        renderizarBadges()
    }

    private fun renderizarBadges() {
        val filtradas = badgesAtuais.filter { filtroAtual.aceita(it) }
        BadgeGridRenderer.renderPlain(
            this,
            layoutInflater,
            binding.gridConquistas,
            filtradas.filter { it.desbloqueada }
        )
        BadgeGridRenderer.renderPlain(
            this,
            layoutInflater,
            binding.gridConquistasBloqueadas,
            filtradas.filterNot { it.desbloqueada }
        )
    }

    private enum class FiltroConquistas {
        TODAS,
        COMBATE,
        EXPLORACAO,
        SOCIAL;

        fun aceita(badge: Badge): Boolean {
            return when (this) {
                TODAS -> true
                COMBATE -> badge.familia == BadgeFamily.VT
                EXPLORACAO -> badge.familia == BadgeFamily.PJ || badge.familia == BadgeFamily.RC
                SOCIAL -> badge.familia == BadgeFamily.XP || badge.familia == BadgeFamily.CR
            }
        }
    }
}
