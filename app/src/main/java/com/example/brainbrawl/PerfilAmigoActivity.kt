package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisConquistas.jogosBadges
import com.example.brainbrawl.UteisConquistas.respostasBadges
import com.example.brainbrawl.UteisConquistas.vitoriaBadges
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPerfilAmigoBinding
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.repositories.JogadorRepository

class PerfilAmigoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPerfilAmigoBinding.inflate(layoutInflater)
    }
    private val amigosRepository = AmigosRepository()
    private val jogadorRepository = JogadorRepository()

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

        // Aceder ao perfil do amigo
        jogadorRepository.obterPerfil(nomeAmigo).addOnSuccessListener { perfil ->
            // Verifica se o perfil do amigo existe
            if (perfil != null) {
                // Guardar os dados do amigo
                val pontuacao = perfil.estatisticas.pontuacao
                val taxaAcertos = perfil.estatisticas.taxaAcertos
                val totalJogos = perfil.estatisticas.totalJogos
                val totalVitorias = perfil.estatisticas.totalVitorias
                val respostasCertas = perfil.estatisticas.totalRespostasCertas

                // Atualizar os badges de conquistas
                getBadgeDrawable(totalJogos, jogosBadges)?.let {
                    binding.imgTotalJogos.setImageResource(it)
                } ?: run {
                    binding.imgTotalJogos.visibility = View.GONE
                }

                getBadgeDrawable(totalVitorias, vitoriaBadges)?.let {
                    binding.imgTotalVitorias.setImageResource(it)
                } ?: run {
                    binding.imgTotalVitorias.visibility = View.GONE
                }

                getBadgeDrawable(respostasCertas, respostasBadges)?.let {
                    binding.imgTotalRespostasCertas.setImageResource(it)
                } ?: run {
                    binding.imgTotalRespostasCertas.visibility = View.GONE
                }

                val nomeAvatar = perfil.avatar
                val resId = resources.getIdentifier(nomeAvatar, "drawable", packageName)
                binding.imgAvatarAmigo.setImageResource(resId)

                // Mostrar os dados do amigo no layout
                binding.txtNomeAmigo.text = nomeAmigo
                binding.txtPontuacao.text = "Pontuação: $pontuacao"
                binding.txtTotalJogos.text = "Total de Jogos: $totalJogos"
                binding.txtTotalVitorias.text = "Total de Vitórias: $totalVitorias"
                binding.txtTaxaAcertos.text = "Taxa de Acertos: ${"%.1f".format(taxaAcertos)}%"

                binding.btnRemoverAmigo.setOnClickListener {
                    amigosRepository.removerAmigo(nomeUtilizador, nomeAmigo)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Amigo removido com sucesso!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, AmigosActivity::class.java)
                            intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                            startActivity(intent)
                            finish()
                        }
                    binding.btnRemoverAmigo.isEnabled = false
                }
            } else {
                binding.imgAvatarAmigo.setImageResource(R.drawable.avatar_1_playstore)
                binding.txtNomeAmigo.text = nomeAmigo
                binding.txtPontuacao.text = "Pontuação: 0"
                binding.txtTotalJogos.text = "Total de Jogos: 0"
                binding.txtTotalVitorias.text = "Total de Vitórias: 0"
                binding.txtTaxaAcertos.text = "Taxa de Acertos: 0.0%"
            }
        }
    }


    @DrawableRes
    private fun getBadgeDrawable(value: Int, thresholds: List<Pair<Int, Int>>): Int? {
        return thresholds.firstOrNull { value >= it.first }?.second
    }
}
