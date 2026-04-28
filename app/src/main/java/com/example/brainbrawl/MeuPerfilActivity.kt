package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisConquistas.jogosBadges
import com.example.brainbrawl.UteisConquistas.respostasBadges
import com.example.brainbrawl.UteisConquistas.vitoriaBadges
import com.example.brainbrawl.databinding.ActivityMeuPerfilBinding
import com.example.brainbrawl.repositories.JogadorRepository

class MeuPerfilActivity : AppCompatActivity() {

    // Usa o mesmo binding/layout do perfil do amigo
    private val binding by lazy { ActivityMeuPerfilBinding.inflate(layoutInflater) }
    private val jogadorRepository = JogadorRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guarda o nome do utilizador passado pelo Intent
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: return
        Toast.makeText(this, "A abrir perfil de $nomeUtilizador", Toast.LENGTH_SHORT).show()

        // Vai buscar os dados do próprio utilizador à base de dados
        jogadorRepository.obterPerfil(nomeUtilizador).addOnSuccessListener { perfil ->
            if (perfil != null) {
                val estado = perfil.estado
                val password = perfil.password
                val pontuacao = perfil.estatisticas.pontuacao
                val taxaAcertos = perfil.estatisticas.taxaAcertos
                val totalJogos = perfil.estatisticas.totalJogos
                val totalVitorias = perfil.estatisticas.totalVitorias
                val respostasCertas = perfil.estatisticas.totalRespostasCertas
                val totalVitoriasModo1x1 = perfil.estatisticas.totalVitoriasModo1x1
                val totalVitoriasModo2x2 = perfil.estatisticas.totalVitoriasModo2x2
                val totalVitoriasModoSolo = perfil.estatisticas.totalVitoriasModoSolo

                // Mostra badges se atingir thresholds
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

                // // Guarda o avatar do utilizador
                val nomeAvatar = perfil.avatar
                val resId = resources.getIdentifier(nomeAvatar, "drawable", packageName)
                binding.imgAvatarAmigo.setImageResource(resId)

                // Mostra os dados do perfil
                binding.txtNomeAmigo.text = nomeUtilizador
                binding.txtPontuacao.text = "Pontuação: $pontuacao"
                binding.txtTotalJogos.text = "Total de Jogos: $totalJogos"
                binding.txtTotalVitorias.text = "Total de Vitórias: $totalVitorias"
                binding.txtTaxaAcertos.text = "Taxa de Acertos: ${"%.1f".format(taxaAcertos)}%"
                binding.txtDetalhesPerfil.text = listOf(
                    "Estado: $estado",
                    "Password: ${if (password.isBlank()) "não definida" else "definida"}",
                    "Respostas certas: $respostasCertas",
                    "Vitórias 1x1: $totalVitoriasModo1x1",
                    "Vitórias 2x2: $totalVitoriasModo2x2",
                    "Vitórias Solo: $totalVitoriasModoSolo"
                ).joinToString("\n")

                binding.btnVoltarPerfil.setOnClickListener {
                    finish()
                }
            }
        }
    }

    // Função utilitária para determinar que badge mostrar
    private fun getBadgeDrawable(value: Int, thresholds: List<Pair<Int, Int>>): Int? {
        return thresholds.firstOrNull { value >= it.first }?.second
    }
}
