package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.utils.UteisConquistas.jogosBadges
import com.example.brainbrawl.utils.UteisConquistas.respostasBadges
import com.example.brainbrawl.utils.UteisConquistas.vitoriaBadges
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPerfilAmigoBinding
import com.example.brainbrawl.viewmodels.PerfilAmigoEvent
import com.example.brainbrawl.viewmodels.PerfilAmigoUiState
import com.example.brainbrawl.viewmodels.PerfilAmigoViewModel

class PerfilAmigoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPerfilAmigoBinding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[PerfilAmigoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pelo Intent
        val nomeAmigo = intent.getStringExtra(IntentExtras.NOME_AMIGO) ?: "Amigo Desconhecido"
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""

        binding.btnVoltarPerfil.setOnClickListener {
            val intent = Intent(this, AmigosActivity::class.java)
            intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
            startActivity(intent)
            finish()
        }

        viewModel.perfil.observe(this) { perfil ->
            mostrarPerfil(perfil, nomeUtilizador)
        }
        viewModel.evento.observe(this) { evento ->
            if (evento == PerfilAmigoEvent.AmigoRemovido) {
                Toast.makeText(this, "Amigo removido com sucesso!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AmigosActivity::class.java)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                startActivity(intent)
                viewModel.consumirEvento()
                finish()
            }
        }
        viewModel.carregarPerfil(nomeAmigo)
    }

    private fun mostrarPerfil(perfil: PerfilAmigoUiState, nomeUtilizador: String) {
        if (!perfil.perfilExiste) {
            binding.imgAvatarAmigo.setImageResource(R.drawable.avatar_1_playstore)
            binding.txtNomeAmigo.text = perfil.nome
            binding.txtPontuacao.text = "Pontuação: 0"
            binding.txtTotalJogos.text = "Total de Jogos: 0"
            binding.txtTotalVitorias.text = "Total de Vitórias: 0"
            binding.txtTaxaAcertos.text = "Taxa de Acertos: 0.0%"
            return
        }

        // Atualizar os badges de conquistas
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

        // Mostrar os dados do amigo no layout
        binding.txtNomeAmigo.text = perfil.nome
        binding.txtPontuacao.text = "Pontuação: ${perfil.pontuacao}"
        binding.txtTotalJogos.text = "Total de Jogos: ${perfil.totalJogos}"
        binding.txtTotalVitorias.text = "Total de Vitórias: ${perfil.totalVitorias}"
        binding.txtTaxaAcertos.text = "Taxa de Acertos: ${"%.1f".format(perfil.taxaAcertos)}%"

        if (perfil.perfilExiste) {
            binding.btnRemoverAmigo.setOnClickListener {
                viewModel.removerAmigo(nomeUtilizador, perfil.nome)
                binding.btnRemoverAmigo.isEnabled = false
            }
        }
    }


    @DrawableRes
    private fun getBadgeDrawable(value: Int, thresholds: List<Pair<Int, Int>>): Int? {
        return thresholds.firstOrNull { value >= it.first }?.second
    }
}
