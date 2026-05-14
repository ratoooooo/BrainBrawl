package com.example.brainbrawl

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMeuPerfilBinding
import com.example.brainbrawl.databinding.ItemBadgeBinding
import com.example.brainbrawl.models.Badge
import com.example.brainbrawl.routes.BottomNavHelper
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.viewmodels.MeuPerfilUiState
import com.example.brainbrawl.viewmodels.MeuPerfilViewModel

class MeuPerfilActivity : AppCompatActivity() {

    // Usa o mesmo binding/layout do perfil do amigo
    private val binding by lazy { ActivityMeuPerfilBinding.inflate(layoutInflater) }
    private val viewModel by lazy {
        ViewModelProvider(this)[MeuPerfilViewModel::class.java]
    }
    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: ""
        val email = intent.getStringExtra(IntentExtras.EMAIL) ?: authService.utilizadorAtual()?.email ?: ""
        BottomNavHelper.instalar(this, BottomNavHelper.Item.PERFIL, uid, nomeUtilizador, nomeJogador, email)

        binding.btnVoltarPerfil.visibility = View.GONE
        binding.btnVoltarPerfil.setOnClickListener {
            finish()
        }

        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            Toast.makeText(this, R.string.perfil_indisponivel, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.perfil.observe(this) { perfil ->
            mostrarPerfil(perfil)
        }
        viewModel.carregarPerfil(uid, nomeUtilizador)
    }

    private fun mostrarPerfil(perfil: MeuPerfilUiState) {
        binding.imgAvatarAmigo.setImageResource(AvatarUtils.resolverAvatar(this, perfil.avatar))

        binding.txtNomeAmigo.text = perfil.nome
        binding.txtPontuacao.text = getString(R.string.pontuacao_format, perfil.pontuacao.toInt())
        binding.txtRecordePontuacao.text = getString(
            R.string.recorde_pontuacao_format,
            perfil.recordePontuacao.toInt()
        )
        binding.txtNivel.text = getString(R.string.nivel_format, perfil.nivel)
        binding.txtXpProgress.text = getString(
            R.string.xp_progress_format,
            perfil.xpNoNivelAtual,
            perfil.xpNecessarioProximoNivel
        )
        binding.txtTotalJogos.text = getString(R.string.total_de_jogos_format, perfil.totalJogos)
        binding.txtTotalVitorias.text = getString(R.string.total_de_vitorias_format, perfil.totalVitorias)
        binding.txtTotalDerrotas.text = getString(R.string.total_de_derrotas_format, perfil.totalDerrotas)
        binding.txtTotalRespostasCertas.text = getString(
            R.string.total_de_respostas_certas_format,
            perfil.totalRespostasCertas
        )
        binding.txtTaxaVitoria.text = getString(R.string.taxa_de_vitoria_format, perfil.taxaVitoria)
        binding.txtTaxaAcertos.text = getString(R.string.taxa_de_acertos_format, perfil.taxaAcertos)
        binding.txtXpTotal.text = getString(R.string.xp_total_format, perfil.xpTotal)
        binding.txtConquistasEstado.text = if (perfil.conquistasPersistentesAtivas) {
            getString(R.string.conquistas_client_side)
        } else {
            getString(R.string.conquistas_sem_persistencia)
        }
        mostrarBadges(perfil.badges)
    }

    private fun mostrarBadges(badges: List<Badge>) {
        val colunas = if (resources.configuration.screenWidthDp >= 360) 3 else 2
        binding.gridConquistas.columnCount = colunas
        binding.gridConquistas.removeAllViews()

        badges.forEach { badge ->
            val itemBinding = ItemBadgeBinding.inflate(layoutInflater, binding.gridConquistas, false)
            itemBinding.imgBadge.setImageResource(resolverBadgeDrawable(badge))
            itemBinding.imgBadge.imageAlpha = if (badge.desbloqueada) 255 else 95
            itemBinding.imgBadge.contentDescription = badge.descricao
            itemBinding.txtBadgeNome.text = badge.nome
            itemBinding.txtBadgeEstado.text = if (badge.desbloqueada) {
                getString(R.string.conquista_desbloqueada)
            } else {
                getString(R.string.conquista_bloqueada)
            }
            itemBinding.txtBadgeProgresso.text = getString(
                R.string.badge_progresso_format,
                badge.progressoAtual,
                badge.objetivo
            )
            itemBinding.root.alpha = if (badge.desbloqueada) 1f else 0.72f

            binding.gridConquistas.addView(
                itemBinding.root,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                    setGravity(Gravity.FILL_HORIZONTAL)
                }
            )
        }
    }

    private fun resolverBadgeDrawable(badge: Badge): Int {
        // getIdentifier e intencional aqui: os PNGs dos badges podem ser importados depois
        // sem criar referencias R.drawable estaticas que quebrariam o build enquanto faltam assets.
        val badgeRes = resources.getIdentifier(badge.drawableName, "drawable", packageName)
        if (badgeRes != 0) return badgeRes

        val fallbackName = if (badge.desbloqueada) BADGE_DEFAULT else BADGE_LOCKED
        val fallbackRes = resources.getIdentifier(fallbackName, "drawable", packageName)
        if (fallbackRes != 0) return fallbackRes

        val defaultRes = resources.getIdentifier(BADGE_DEFAULT, "drawable", packageName)
        if (defaultRes != 0) return defaultRes

        return if (badge.desbloqueada) R.drawable.ic_trophy else R.drawable.ic_lock
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val BADGE_DEFAULT = "badge_default"
        const val BADGE_LOCKED = "badge_locked"
    }
}
