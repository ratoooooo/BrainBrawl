package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        val nomeAmigo = intent.getStringExtra("nomeAmigo") ?: "Amigo Desconhecido"
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""

        database.child("jogadores").child(nomeAmigo).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val pontuacao = dataSnapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                val totalJogos = dataSnapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitorias = dataSnapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val taxaVitorias = if (totalJogos > 0) ((totalVitorias.toDouble() / totalJogos) * 100).toInt() else 0

                binding.txtNomeAmigo.text = nomeAmigo
                binding.txtPontuacao.text = "Pontuação: $pontuacao"
                binding.txtTotalJogos.text = "Total de Jogos: $totalJogos"
                binding.txtTotalVitorias.text = "Total de Vitórias: $totalVitorias"
                binding.txtTaxaAcertos.text = "Taxa de Vitória: $taxaVitorias%"

                // Remover amigo
                binding.btnRemoverAmigo.setOnClickListener {
                    database.child("jogadores").child(nomeUtilizador).child("amigos")
                        .child(nomeAmigo).removeValue().addOnSuccessListener {
                            Toast.makeText(this, "Amigo removido com sucesso!", Toast.LENGTH_SHORT).show()
                            intent = Intent(this, AmigosActivity::class.java)
                            intent.putExtra("nomeUtilizador", nomeUtilizador)
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Erro ao remover amigo.", Toast.LENGTH_SHORT).show()
                        }
                }

                // Voltar ao perfil
                binding.btnVoltarPerfil.setOnClickListener {
                    var intent = Intent(this, AmigosActivity::class.java)
                    intent.putExtra("nomeUtilizador", nomeUtilizador)
                    finish()
                }
            }
            else {
                binding.txtNomeAmigo.text = nomeAmigo
                binding.txtPontuacao.text = "Pontuação: 0"
                binding.txtTotalJogos.text = "Total de Jogos: 0"
                binding.txtTotalVitorias.text = "Total de Vitórias: 0"
                binding.txtTaxaAcertos.text = "Taxa de Vitória: 0%"
            }
        }
    }
}