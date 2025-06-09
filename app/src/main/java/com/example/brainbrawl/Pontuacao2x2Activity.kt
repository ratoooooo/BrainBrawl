package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityPontuacaoMultiBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Pontuacao2x2Activity : AppCompatActivity() {

    private val binding by lazy {
        ActivityPontuacaoMultiBinding.inflate(layoutInflater)
    }

    private lateinit var codigoSala: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeJogador: String
    private lateinit var nomeUtilizador: String

    private var totalPontos: Double = 0.0
    private var admin = false

    private val database = FirebaseDatabase.getInstance().reference

    // Novos dados do jogo
    private var respostasCertas: Int = 0
    private var totalPerguntas: Int = 1
    private var equipa: String? = null // para 2x2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        admin = intent.getBooleanExtra("admin", false)
        equipa = intent.getStringExtra("equipa")

        carregarPontuacao2x2()

        if (nomeUtilizador.isNotEmpty()) {
            adicionarPontuacaoJogadorRegistado()
        }

        binding.btnVoltar.setOnClickListener {
            database.child("sala_2x2").child(codigoSala).removeValue()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            startActivity(intent)
            finish()
        }
    }

    // Lógica para mostrar o pódio 2x2, sempre exibindo os dois da equipa vencedora primeiro!
    private fun carregarPontuacao2x2() {
        val equipaA = mutableListOf<Pair<String, Double>>() // Jogador, Pontos
        val equipaB = mutableListOf<Pair<String, Double>>() // Jogador, Pontos
        val salaRef = database.child("sala_2x2").child(codigoSala)
        salaRef.child("pontuacoes_A").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshotA: DataSnapshot) {
                for (child in snapshotA.children) {
                    val nome = child.key ?: "Desconhecido"
                    val pontos = child.getValue(Double::class.java) ?: 0.0
                    equipaA.add(Pair(nome, pontos))
                }
                salaRef.child("pontuacoes_B").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshotB: DataSnapshot) {
                        for (child in snapshotB.children) {
                            val nome = child.key ?: "Desconhecido"
                            val pontos = child.getValue(Double::class.java) ?: 0.0
                            equipaB.add(Pair(nome, pontos))
                        }
                        equipaA.sortByDescending { it.second }
                        equipaB.sortByDescending { it.second }

                        val totalA = equipaA.sumOf { it.second }
                        val totalB = equipaB.sumOf { it.second }

                        // Lista final: equipa vencedora primeiro, depois perdedora
                        val podio = if (totalA >= totalB)
                            equipaA + equipaB
                        else
                            equipaB + equipaA

                        // Preenche os 4 lugares do layout (podes ajustar para menos/jogadores faltantes)
                        if (podio.size > 0) {
                            binding.txtNomeJogador1.text = podio.getOrNull(0)?.first ?: ""
                            binding.txtPontos1.text = podio.getOrNull(0)?.second?.toInt()?.toString() ?: ""
                        }
                        if (podio.size > 1) {
                            binding.txtNomeJogador2.text = podio.getOrNull(1)?.first ?: ""
                            binding.txtPontos2.text = podio.getOrNull(1)?.second?.toInt()?.toString() ?: ""
                        }
                        if (podio.size > 2) {
                            binding.txtNomeJogador3.text = podio.getOrNull(2)?.first ?: ""
                            binding.txtPontos3.text = podio.getOrNull(2)?.second?.toInt()?.toString() ?: ""
                        }
                        if (podio.size > 3) {
                            binding.txtNomeJogador4.text = podio.getOrNull(3)?.first ?: ""
                            binding.txtPontos4.text = podio.getOrNull(3)?.second?.toInt()?.toString() ?: ""
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun adicionarPontuacaoJogadorRegistado() {
        database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pontuacaoGuardada = snapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                if (totalPontos > pontuacaoGuardada) {
                    database.child("jogadores").child(nomeUtilizador).child("pontuacao").setValue(totalPontos)
                    Toast.makeText(this@Pontuacao2x2Activity, "NOVO RECORD!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Pontuacao2x2Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
            }
        })
    }
}