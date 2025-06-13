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
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityPontuacao1x1Binding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var codigoSala: String
    private lateinit var nomeJogador: String
    private lateinit var nomeUtilizador: String
    private var totalPontos: Double = 0.0
    private var totalRespostasCertas: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        totalRespostasCertas = intent.getIntExtra("totalRespostasCertas", 0)

        // Chamar a função para carregar pontuação da sala 1x1
        carregarPontuacao1x1()

        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            database.child("sala_1x1").child(codigoSala).removeValue()
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("nomeUtilizador", nomeUtilizador)
            })
            finish()
        }
    }

    // Função para carregar a pontuação da sala 1x1
    private fun carregarPontuacao1x1() {
        database.child("sala_1x1").child(codigoSala).child("pontuacoes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val jogadores = mutableListOf<Pair<String, Double>>() // Jogador, Pontos
                    for (child in snapshot.children) {
                        val nome = child.key ?: "Desconhecido"
                        val pontos = child.getValue(Double::class.java) ?: 0.0
                        jogadores.add(Pair(nome, pontos))
                    }
                    // Ordena os jogadores por pontuação
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

                    // Atualizar estatísticas do jogador registado
                    if (nomeUtilizador.isNotEmpty()) {
                        val ficouEmPrimeiro = jogadores.isNotEmpty() && jogadores[0].first == nomeUtilizador
                        atualizarEstatisticasJogador(ficouEmPrimeiro)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Pontuacao1x1Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Função para atualizar as estatísticas do jogador registado
    private fun atualizarEstatisticasJogador(ficouEmPrimeiro: Boolean) {
        database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalJogosAnterior = snapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitoriasAnterior = snapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val totalRespostasCertasAnterior = snapshot.child("totalRespostasCertas").getValue(Int::class.java) ?: 0

                val novoTotalJogos = totalJogosAnterior + 1
                val novoTotalVitorias = totalVitoriasAnterior + if (ficouEmPrimeiro) 1 else 0
                val novoTotalRespostasCertas = totalRespostasCertasAnterior + totalRespostasCertas

                val updates = mapOf(
                    "totalJogos" to novoTotalJogos,
                    "totalVitorias" to novoTotalVitorias,
                    "totalRespostasCertas" to novoTotalRespostasCertas
                )
                database.child("jogadores").child(nomeUtilizador).updateChildren(updates)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}