package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityPontuacaoBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PontuacoesActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPontuacaoBinding.inflate(layoutInflater)
    }

    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeJogador: String
    private var totalPontos: Double = 0.0
    private var respostasCertas: Int = 0
    private var totalPerguntas: Int = 1

    private val database = FirebaseDatabase.getInstance().reference
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        respostasCertas = intent.getIntExtra("respostasCertas", 0)
        totalPerguntas = intent.getIntExtra("totalPerguntas", 1)

        carregarPontuacaoSala()

        // Atualizar estatísticas para jogadores registados
        if (nomeUtilizador.isNotEmpty()) {
            val ficouEmPrimeiro = false
            atualizarEstatisticasJogador(respostasCertas, totalPerguntas, ficouEmPrimeiro)
        }

        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            startActivity(intent)
            finish()
        }
    }

    private fun carregarPontuacaoSala() {
        database.child("salas").child(codigoSala).child("jogadores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val jogadores = mutableListOf<Pair<String, Double>>()
                    for (childSnapshot in snapshot.children) {
                        val jogadorNome = childSnapshot.key ?: "Desconhecido"
                        val jogadorPontos = if (childSnapshot.child("pontuacao").exists()) {
                            childSnapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                        } else {
                            childSnapshot.getValue(Double::class.java) ?: 0.0
                        }
                        jogadores.add(Pair(jogadorNome, jogadorPontos))
                    }
                    jogadores.sortByDescending { it.second }

                    // Limpar o layout antes de adicionar as pontuações
                    binding.layoutPodio.removeAllViews()

                    // Adicionar as posições dinamicamente (até ao número de jogadores)
                    for ((index, jogador) in jogadores.withIndex()) {
                        val posicao = index + 1
                        val nome = jogador.first
                        val pontos = jogador.second

                        // Escolhe o emoji para o pódio (top 3) e mostra número nas restantes
                        val prefixo = when (posicao) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "$posicao"
                        }

                        // Cria uma TextView simples para cada linha do pódio
                        val textView = android.widget.TextView(this@PontuacoesActivity)
                        textView.text = "$prefixo  $nome  -  $pontos"
                        textView.textSize = 18f
                        textView.setPadding(0, 8, 0, 8)
                        textView.setTextColor(android.graphics.Color.BLACK)
                        textView.setTypeface(null, android.graphics.Typeface.BOLD)
                        binding.layoutPodio.addView(textView)
                    }

                    // Se não houver jogadores
                    if (jogadores.isEmpty()) {
                        val textView = android.widget.TextView(this@PontuacoesActivity)
                        textView.text = "Sem jogadores na sala."
                        textView.textSize = 16f
                        textView.setTextColor(android.graphics.Color.BLACK)
                        binding.layoutPodio.addView(textView)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    binding.layoutPodio.removeAllViews()
                    val textView = android.widget.TextView(this@PontuacoesActivity)
                    textView.text = "Erro ao carregar resultados"
                    textView.textSize = 16f
                    textView.setTextColor(android.graphics.Color.BLACK)
                    binding.layoutPodio.addView(textView)
                }
            })
    }

    private fun atualizarEstatisticasJogador(respostasCertas: Int, totalPerguntas: Int, ficouEmPrimeiro: Boolean) {
        if (nomeUtilizador.isEmpty()) return
        database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalJogosAnterior = snapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitoriasAnterior = snapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val taxaAcertosAnterior = snapshot.child("taxaAcertos").getValue(Double::class.java) ?: 0.0

                val novoTotalJogos = totalJogosAnterior + 1
                val novoTotalVitorias = totalVitoriasAnterior + if (ficouEmPrimeiro) 1 else 0

                val percentagemEsteJogo = if (totalPerguntas > 0) (respostasCertas.toDouble() / totalPerguntas) * 100 else 0.0

                val novaTaxa = if (totalJogosAnterior == 0) {
                    percentagemEsteJogo
                } else {
                    ((taxaAcertosAnterior * totalJogosAnterior) + percentagemEsteJogo) / novoTotalJogos
                }

                val updates = mapOf(
                    "totalJogos" to novoTotalJogos,
                    "totalVitorias" to novoTotalVitorias,
                    "taxaAcertos" to novaTaxa
                )
                database.child("jogadores").child(nomeUtilizador).updateChildren(updates)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}