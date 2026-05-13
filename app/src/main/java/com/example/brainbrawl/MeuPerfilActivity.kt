package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMeuPerfilBinding
import com.example.brainbrawl.routes.BottomNavHelper
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.utils.UteisConquistas.jogosBadges
import com.example.brainbrawl.utils.UteisConquistas.respostasBadges
import com.example.brainbrawl.utils.UteisConquistas.vitoriaBadges
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
        // Mostra badges se atingir thresholds
        getBadgeDrawable(perfil.totalJogos, jogosBadges)?.let {
            binding.imgTotalJogos.visibility = View.VISIBLE
            binding.imgTotalJogos.setImageResource(it)
        } ?: run {
            binding.imgTotalJogos.visibility = View.GONE
        }

        getBadgeDrawable(perfil.totalVitorias, vitoriaBadges)?.let {
            binding.imgTotalVitorias.visibility = View.VISIBLE
            binding.imgTotalVitorias.setImageResource(it)
        } ?: run {
            binding.imgTotalVitorias.visibility = View.GONE
        }

        getBadgeDrawable(perfil.totalRespostasCertas, respostasBadges)?.let {
            binding.imgTotalRespostasCertas.visibility = View.VISIBLE
            binding.imgTotalRespostasCertas.setImageResource(it)
        } ?: run {
            binding.imgTotalRespostasCertas.visibility = View.GONE
        }

        binding.imgAvatarAmigo.setImageResource(AvatarUtils.resolverAvatar(this, perfil.avatar))

        // Mostra os dados do perfil
        binding.txtNomeAmigo.text = perfil.nome
        binding.txtPontuacao.text = getString(R.string.recorde_pontuacao_format, perfil.recordePontuacao.toInt())
        binding.txtNivel.text = getString(R.string.nivel_format, perfil.nivel)
        binding.txtXpProgress.text = getString(
            R.string.xp_progress_format,
            perfil.xpNoNivelAtual,
            perfil.xpNecessarioProximoNivel
        )
        binding.txtTotalJogos.text = getString(R.string.total_de_jogos_format, perfil.totalJogos)
        binding.txtTotalVitorias.text = getString(R.string.total_de_vitorias_format, perfil.totalVitorias)
        binding.txtTaxaAcertos.text = getString(R.string.taxa_de_acertos_format, perfil.taxaAcertos)
    }

    // Função utilitária para determinar que badge mostrar
    private fun getBadgeDrawable(value: Int, thresholds: List<Pair<Int, Int>>): Int? {
        return thresholds.firstOrNull { value >= it.first }?.second
    }
}
