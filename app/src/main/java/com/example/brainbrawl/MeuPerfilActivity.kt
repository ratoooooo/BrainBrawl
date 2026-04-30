package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMeuPerfilBinding
import com.example.brainbrawl.services.AuthService
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
        if (uid.isBlank() && nomeUtilizador.isBlank()) return

        viewModel.perfil.observe(this) { perfil ->
            mostrarPerfil(perfil)
        }
        viewModel.carregarPerfil(uid, nomeUtilizador)
    }

    private fun mostrarPerfil(perfil: MeuPerfilUiState) {
        // Mostra badges se atingir thresholds
        getBadgeDrawable(perfil.totalJogos, jogosBadges)?.let {
            binding.imgTotalJogos.setImageResource(it)
        } ?: run {
            binding.imgTotalJogos.visibility = View.GONE
        }

        getBadgeDrawable(perfil.totalVitorias, vitoriaBadges)?.let {
            binding.imgTotalVitorias.setImageResource(it)
        } ?: run {
            binding.imgTotalVitorias.visibility = View.GONE
        }

        getBadgeDrawable(perfil.totalRespostasCertas, respostasBadges)?.let {
            binding.imgTotalRespostasCertas.setImageResource(it)
        } ?: run {
            binding.imgTotalRespostasCertas.visibility = View.GONE
        }

        val resId = resources.getIdentifier(perfil.avatar, "drawable", packageName)
        binding.imgAvatarAmigo.setImageResource(resId)

        // Mostra os dados do perfil
        binding.txtNomeAmigo.text = perfil.nome
        binding.txtPontuacao.text = "Recorde de Pontuação: ${perfil.recordePontuacao.toInt()} pontos"
        binding.txtNivel.text = "Nível ${perfil.nivel}"
        binding.txtXpProgress.text = "${perfil.xpNoNivelAtual} / ${perfil.xpNecessarioProximoNivel} XP"
        binding.txtTotalJogos.text = "Total de Jogos: ${perfil.totalJogos}"
        binding.txtTotalVitorias.text = "Total de Vitórias: ${perfil.totalVitorias}"
        binding.txtTaxaAcertos.text = "Taxa de Acertos: ${"%.1f".format(perfil.taxaAcertos)}%"

        binding.btnVoltarPerfil.setOnClickListener {
            finish()
        }
    }

    // Função utilitária para determinar que badge mostrar
    private fun getBadgeDrawable(value: Int, thresholds: List<Pair<Int, Int>>): Int? {
        return thresholds.firstOrNull { value >= it.first }?.second
    }
}
