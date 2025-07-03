package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisSala.gerarCodigoSala
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
    private lateinit var nomeUtilizador: String
    private var totalPontos: Double = 0.0
    private var totalRespostasCertas: Int = 0
    private var nomeCategoria: String = ""

    // Listener para desforra em tempo real
    private var desforraListener: ValueEventListener? = null
    private var pontuacaoListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        totalRespostasCertas = intent.getIntExtra("totalRespostasCertas", 0)
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""

        carregarPontuacao1x1Realtime()

        binding.btnVoltar.setOnClickListener {
            database.child("sala_1x1").child(codigoSala).removeValue()
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("nomeUtilizador", nomeUtilizador)
            })
            finish()
        }

        // Ao pedir desforra, vai para uma nova sala de espera, com novo código
        binding.btnDesforra.setOnClickListener {
            pedirDesforra()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removerListenerDesforra()
        removerListenerPontuacao()
    }

    private fun pedirDesforra() {
        // Marca pedido de desforra no Firebase
        database.child("sala_1x1").child(codigoSala).child("jogadores").child(nomeUtilizador).child("desforra").setValue(true)
        // Adiciona listener para saber quando o outro jogador aceitar
        if (desforraListener == null) {
            val desforraRef = database.child("sala_1x1").child(codigoSala).child("jogadores")
            desforraListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var desforraAceita = false
                    for (child in snapshot.children) {
                        if (child.key != nomeUtilizador && child.child("desforra").getValue(Boolean::class.java) == true) {
                            desforraAceita = true
                            break
                        }
                    }
                    if (desforraAceita) {
                        removerListenerDesforra()
                        iniciarJogoDesforra()
                    } else {
                        Toast.makeText(this@Pontuacao1x1Activity, "Aguardando o outro jogador aceitar desforra", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            desforraRef.addValueEventListener(desforraListener as ValueEventListener)
        }
    }

    private fun removerListenerDesforra() {
        desforraListener?.let {
            database.child("sala_1x1").child(codigoSala).child("jogadores").removeEventListener(it)
            desforraListener = null
        }
    }

    private fun removerListenerPontuacao() {
        pontuacaoListener?.let {
            database.child("sala_1x1").child(codigoSala).child("pontuacoes").removeEventListener(it)
            pontuacaoListener = null
        }
    }

    // Listener em tempo real para garantir que ambos os jogadores veem o pódio assim que ambos terminam
    private fun carregarPontuacao1x1Realtime() {
        pontuacaoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jogadores = mutableListOf<Pair<String, Double>>() // Jogador, Pontos
                for (child in snapshot.children) {
                    val nome = child.key ?: "Desconhecido"
                    val pontos = child.getValue(Double::class.java) ?: 0.0
                    jogadores.add(Pair(nome, pontos))
                }
                jogadores.sortByDescending { it.second }

                if (jogadores.isNotEmpty()) {
                    binding.txtNomeJogador1.text = jogadores[0].first
                    binding.txtPontos1.text = jogadores[0].second.toInt().toString()
                }
                if (jogadores.size > 1) {
                    binding.txtNomeJogador2.text = jogadores[1].first
                    binding.txtPontos2.text = jogadores[1].second.toInt().toString()
                }
                if (jogadores.size <= 1) {
                    binding.txtNomeJogador2.text = "Aguardando adversário..."
                    binding.txtPontos2.text = ""
                }

                if (nomeUtilizador.isNotEmpty() && jogadores.isNotEmpty()) {
                    val ficouEmPrimeiro = jogadores[0].first == nomeUtilizador
                    atualizarEstatisticasJogador(ficouEmPrimeiro)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Pontuacao1x1Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
            }
        }
        database.child("sala_1x1").child(codigoSala).child("pontuacoes")
            .addValueEventListener(pontuacaoListener!!)
    }

    private fun atualizarEstatisticasJogador(ficouEmPrimeiro: Boolean) {
        database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalJogosAnterior = snapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitoriasAnterior = snapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val totalRespostasCertasAnterior = snapshot.child("totalRespostasCertas").getValue(Int::class.java) ?: 0

                // Novo campo para vitórias em 1x1
                val totalVitoriasModo1x1Anterior = snapshot.child("totalVitoriasModo1x1").getValue(Int::class.java) ?: 0
                val novoTotalVitoriasModo1x1 = totalVitoriasModo1x1Anterior + if (ficouEmPrimeiro) 1 else 0

                val novoTotalJogos = totalJogosAnterior + 1
                val novoTotalVitorias = totalVitoriasAnterior + if (ficouEmPrimeiro) 1 else 0
                val novoTotalRespostasCertas = totalRespostasCertasAnterior + totalRespostasCertas

                val updates = mapOf(
                    "totalJogos" to novoTotalJogos,
                    "totalVitorias" to novoTotalVitorias,
                    "totalRespostasCertas" to novoTotalRespostasCertas,
                    "totalVitoriasModo1x1" to novoTotalVitoriasModo1x1
                )
                database.child("jogadores").child(nomeUtilizador).updateChildren(updates)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun iniciarJogoDesforra() {
        // Limpa a sala antiga
        database.child("sala_1x1").child(codigoSala).removeValue()
        // Gera um novo código de sala e volta para a sala de espera
        val novoCodigoSala = gerarCodigoSala()
        val intent = Intent(this, SalaDeEspera1x1Activity::class.java)
        nomeUtilizador.let { intent.putExtra("nomeUtilizador", it) }
        nomeCategoria.let { intent.putExtra("nomeCategoria", it) }
        novoCodigoSala.let { intent.putExtra("codigoSala", it) }
        startActivity(intent)
        finish()
    }
}