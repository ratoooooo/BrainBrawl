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
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityPontuacaoMultiBinding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var codigoSala: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeJogador: String
    private lateinit var nomeUtilizador: String
    private var totalPontos: Double = 0.0
    private var admin = false

    // Novos dados do jogo
    private var respostasCertas: Int = 0
    private var totalPerguntas: Int = 1
    private var equipa: String? = null
    private var totalRespostasCertas: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        admin = intent.getBooleanExtra("admin", false)
        equipa = intent.getStringExtra("equipa")
        respostasCertas = intent.getIntExtra("respostasCertas", 0)
        totalPerguntas = intent.getIntExtra("totalPerguntas", 1)
        totalRespostasCertas = intent.getIntExtra("totalRespostasCertas", 0)

        // Chamar a função para carregar pontuação da sala 2x2
        carregarPontuacao2x2()

        // Atualizar estatísticas do jogador registado (após carregar o pódio)
        if (nomeUtilizador.isNotEmpty()) {
            atualizarEstatisticasJogador()
        }

        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            database.child("sala_2x2").child(codigoSala).removeValue()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            startActivity(intent)
            finish()
        }
    }

    // Função para carregar a pontuação da sala 2x2
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
                        // Ordenar as equipas por pontos
                        equipaA.sortByDescending { it.second }
                        equipaB.sortByDescending { it.second }

                        // Calcular o total de pontos de cada equipa
                        val totalA = equipaA.sumOf { it.second }
                        val totalB = equipaB.sumOf { it.second }

                        // Apresentar as equipas e os seus pontos
                        val podio = if (totalA >= totalB)
                            equipaA + equipaB
                        else
                            equipaB + equipaA

                        // Preenche os 4 lugares do layout
                        if (podio.isNotEmpty()) {
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

    // Função para atualizar as estatísticas do jogador registado
    private fun atualizarEstatisticasJogador() {
        database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pontuacaoGuardada = snapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                val totalRespostasCertasAnterior = snapshot.child("totalRespostasCertas").getValue(Int::class.java) ?: 0
                val totalJogosAnterior = snapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitoriasAnterior = snapshot.child("totalVitorias").getValue(Int::class.java) ?: 0

                val novoTotalRespostasCertas = totalRespostasCertasAnterior + totalRespostasCertas
                val novoTotalJogos = totalJogosAnterior + 1

                // Verifica se a equipa ganhou (pontos da equipa vs equipa adversária)
                val salaRef = database.child("sala_2x2").child(codigoSala)
                salaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(salaSnapshot: DataSnapshot) {
                        val equipaA = salaSnapshot.child("pontuacoes_A").children.map { it.getValue(Double::class.java) ?: 0.0 }
                        val equipaB = salaSnapshot.child("pontuacoes_B").children.map { it.getValue(Double::class.java) ?: 0.0 }
                        val totalA = equipaA.sum()
                        val totalB = equipaB.sum()

                        var ganhou = false
                        if (equipa == "A" && totalA >= totalB) ganhou = true
                        if (equipa == "B" && totalB > totalA) ganhou = true

                        val novoTotalVitorias = totalVitoriasAnterior + if (ganhou) 1 else 0

                        // Atualizar tudo na base de dados
                        val updates = mapOf(
                            "pontuacao" to if (totalPontos > pontuacaoGuardada) totalPontos else pontuacaoGuardada,
                            "totalRespostasCertas" to novoTotalRespostasCertas,
                            "totalJogos" to novoTotalJogos,
                            "totalVitorias" to novoTotalVitorias
                        )
                        database.child("jogadores").child(nomeUtilizador).updateChildren(updates)

                        // Notifica se for novo record
                        if (totalPontos > pontuacaoGuardada) {
                            Toast.makeText(this@Pontuacao2x2Activity, "NOVO RECORD!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Pontuacao2x2Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
            }
        })
    }
}