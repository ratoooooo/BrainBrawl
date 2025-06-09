package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityPontuacao1x1Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Pontuacao1x1Activity : AppCompatActivity() {

    private val binding by lazy {
        ActivityPontuacao1x1Binding.inflate(layoutInflater)
    }
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var codigoSala: String
    private lateinit var nomeJogador: String
    private lateinit var nomeUtilizador: String
    private var totalPontos: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)

        carregarPodio1x1()

        binding.btnVoltar.setOnClickListener {
            database.child("sala_1x1").child(codigoSala).removeValue()
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("nomeUtilizador", nomeUtilizador)
            })
            finish()
        }
    }

    private fun carregarPodio1x1() {
        database.child("sala_1x1").child(codigoSala).child("pontuacoes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val jogadores = mutableListOf<Pair<String, Double>>()
                    for (child in snapshot.children) {
                        val nome = child.key ?: "Desconhecido"
                        val pontos = child.getValue(Double::class.java) ?: 0.0
                        jogadores.add(Pair(nome, pontos))
                    }
                    jogadores.sortByDescending { it.second }

                    // Preenche o layout
                    if (jogadores.isNotEmpty()) {
                        binding.txtNomeJogador1.text = jogadores[0].first
                        binding.txtPontos1.text = jogadores[0].second.toInt().toString()
                    }
                    if (jogadores.size > 1) {
                        binding.txtNomeJogador2.text = jogadores[1].first
                        binding.txtPontos2.text = jogadores[1].second.toInt().toString()
                    }
                    // Se só tem 1 jogador
                    if (jogadores.size <= 1) {
                        binding.txtNomeJogador2.text = ""
                        binding.txtPontos2.text = ""
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Pontuacao1x1Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun adicionarPontuacaoJogadorRegistado() {
        database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pontuacaoGuardada = snapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                if (totalPontos > pontuacaoGuardada) {
                    database.child("jogadores").child(nomeUtilizador).child("pontuacao").setValue(totalPontos)
                    Toast.makeText(this@Pontuacao1x1Activity, "NOVO RECORD!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Pontuacao1x1Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
            }
        })
    }
}