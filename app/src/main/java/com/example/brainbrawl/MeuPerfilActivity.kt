package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisConquistas.jogosBadges
import com.example.brainbrawl.UteisConquistas.respostasBadges
import com.example.brainbrawl.UteisConquistas.vitoriaBadges
import com.example.brainbrawl.databinding.ActivityMeuPerfilBinding
import com.google.firebase.database.FirebaseDatabase

class MeuPerfilActivity : AppCompatActivity() {

    // Usa o mesmo binding/layout do perfil do amigo
    private val binding by lazy { ActivityMeuPerfilBinding.inflate(layoutInflater) }
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guarda o nome do utilizador passado pelo Intent
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: return
        Toast.makeText(this, "A abrir perfil de $nomeUtilizador", Toast.LENGTH_SHORT).show()

        // Vai buscar os dados do próprio utilizador à base de dados
        database.child("jogadores").child(nomeUtilizador).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val pontuacao = dataSnapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                val totalJogos = dataSnapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitorias = dataSnapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val respostasCertas = dataSnapshot.child("totalRespostasCertas").getValue(Int::class.java) ?: 0
                val taxaVitorias = if (totalJogos > 0) ((totalVitorias.toDouble() / totalJogos) * 100).toInt() else 0

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
                val nomeAvatar = dataSnapshot.child("avatar").getValue(String::class.java) ?: "avatar_1_playstore"
                val resId = resources.getIdentifier(nomeAvatar, "drawable", packageName)
                binding.imgAvatarAmigo.setImageResource(resId)

                // Mostra os dados do perfil
                binding.txtNomeAmigo.text = nomeUtilizador
                binding.txtPontuacao.text = "Pontuação: $pontuacao"
                binding.txtTotalJogos.text = "Total de Jogos: $totalJogos"
                binding.txtTotalVitorias.text = "Total de Vitórias: $totalVitorias"
                binding.txtTaxaAcertos.text = "Taxa de Vitória: $taxaVitorias%"

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