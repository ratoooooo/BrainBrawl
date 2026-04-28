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
import com.example.brainbrawl.UteisFirebase.doubleValue
import com.example.brainbrawl.UteisFirebase.intValue
import com.example.brainbrawl.databinding.ActivityPerfilAmigoBinding
import com.google.firebase.database.FirebaseDatabase

class PerfilAmigoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPerfilAmigoBinding.inflate(layoutInflater)
    }
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pelo Intent
        val nomeAmigo = intent.getStringExtra("nomeAmigo") ?: "Amigo Desconhecido"
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""

        binding.btnVoltarPerfil.setOnClickListener {
            val intent = Intent(this, AmigosActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            startActivity(intent)
            finish()
        }

        // Aceder ao perfil do amigo
        database.child("jogadores").child(nomeAmigo).get().addOnSuccessListener { dataSnapshot ->
            // Verifica se o perfil do amigo existe
            if (dataSnapshot.exists()) {
                // Guardar os dados do amigo
                val pontuacao = dataSnapshot.child("pontuacao").doubleValue()
                val taxaAcertos = dataSnapshot.child("taxaAcertos").doubleValue()
                val totalJogos = dataSnapshot.child("totalJogos").intValue()
                val totalVitorias = dataSnapshot.child("totalVitorias").intValue()
                val respostasCertas = dataSnapshot.child("totalRespostasCertas").intValue()

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

                val nomeAvatar = dataSnapshot.child("avatar").getValue(String::class.java) ?: "avatar_1_playstore"
                val resId = resources.getIdentifier(nomeAvatar, "drawable", packageName)
                binding.imgAvatarAmigo.setImageResource(resId)

                // Mostrar os dados do amigo no layout
                binding.txtNomeAmigo.text = nomeAmigo
                binding.txtPontuacao.text = "Pontuação: $pontuacao"
                binding.txtTotalJogos.text = "Total de Jogos: $totalJogos"
                binding.txtTotalVitorias.text = "Total de Vitórias: $totalVitorias"
                binding.txtTaxaAcertos.text = "Taxa de Acertos: ${"%.1f".format(taxaAcertos)}%"

                binding.btnRemoverAmigo.setOnClickListener {
                    database.child("jogadores").child(nomeUtilizador).child("amigos")
                        .child(nomeAmigo).removeValue().addOnSuccessListener {
                            Toast.makeText(this, "Amigo removido com sucesso!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, AmigosActivity::class.java)
                            intent.putExtra("nomeUtilizador", nomeUtilizador)
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
