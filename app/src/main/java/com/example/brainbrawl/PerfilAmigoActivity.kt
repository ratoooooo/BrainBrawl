package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityPerfilAmigoBinding
import com.google.firebase.database.FirebaseDatabase

class PerfilAmigoActivity : AppCompatActivity() {
    /// Acessar os elementos do layout
    private val binding by lazy {
        ActivityPerfilAmigoBinding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        val nomeAmigo = intent.getStringExtra("nomeAmigo") ?: "Amigo Desconhecido"
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""

        database.child("jogadores").child(nomeAmigo).get().addOnSuccessListener { dataSnapshot ->
            // Verifica se o amigo existe na base de dados
            if (dataSnapshot.exists()) {
                // Guardar os dados do amigo
                val pontuacao = dataSnapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                val totalJogos = dataSnapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitorias = dataSnapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val taxaVitorias = if (totalJogos > 0) ((totalVitorias.toDouble() / totalJogos) * 100).toInt() else 0

                // Mostrar os dados do amigo no layout
                binding.txtNomeAmigo.text = nomeAmigo
                binding.txtPontuacao.text = "Pontuação: $pontuacao"
                binding.txtTotalJogos.text = "Total de Jogos: $totalJogos"
                binding.txtTotalVitorias.text = "Total de Vitórias: $totalVitorias"
                binding.txtTaxaAcertos.text = "Taxa de Vitória: $taxaVitorias%"

                // Configurar o botão para remover amigo
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

                // Configurar o botão de voltar
                binding.btnVoltarPerfil.setOnClickListener {
                    val intent = Intent(this, AmigosActivity::class.java)
                    intent.putExtra("nomeUtilizador", nomeUtilizador)
                    startActivity(intent)
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