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
    private val database = FirebaseDatabase.getInstance().reference

    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeCategoria: String
    private var equipa: String? = null
    private var totalPontos: Double = 0.0
    private var totalRespostasCertas: Int = 0

    private var pontuacaoListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Obter dados do intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: "Todas as categorias"
        equipa = intent.getStringExtra("equipa")
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        totalRespostasCertas = intent.getIntExtra("totalRespostasCertas", 0)

        carregarPontuacao2x2Realtime()

        if (nomeUtilizador.isNotEmpty()) {
            atualizarEstatisticasJogador()
        }

        binding.btnVoltar.setOnClickListener {
            database.child("sala_2x2").child(codigoSala).removeValue()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removerListenerPontuacao()
    }

    private fun removerListenerPontuacao() {
        pontuacaoListener?.let {
            database.child("sala_2x2").child(codigoSala).removeEventListener(it)
            pontuacaoListener = null
        }
    }

    private fun carregarPontuacao2x2Realtime() {
        val salaRef = database.child("sala_2x2").child(codigoSala)
        pontuacaoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Vai buscar todas as pontuações das duas equipas
                val equipaASnapshot = snapshot.child("equipaA")
                val equipaBSnapshot = snapshot.child("equipaB")
                val pontuacoesA = snapshot.child("pontuacoes_A")
                val pontuacoesB = snapshot.child("pontuacoes_B")

                val equipaAList = mutableListOf<Pair<String, Double>>()
                for (player in equipaASnapshot.children) {
                    val nome = player.key ?: ""
                    val pontos = pontuacoesA.child(nome).getValue(Double::class.java) ?: 0.0
                    equipaAList.add(Pair(nome, pontos))
                }
                val equipaBList = mutableListOf<Pair<String, Double>>()
                for (player in equipaBSnapshot.children) {
                    val nome = player.key ?: ""
                    val pontos = pontuacoesB.child(nome).getValue(Double::class.java) ?: 0.0
                    equipaBList.add(Pair(nome, pontos))
                }

                // Ordena cada equipa por pontuação decrescente
                val equipaAOrdenada = equipaAList.sortedByDescending { it.second }
                val equipaBOrdenada = equipaBList.sortedByDescending { it.second }

                // Calcular total de pontos de cada equipa
                val totalA = equipaAOrdenada.sumOf { it.second }
                val totalB = equipaBOrdenada.sumOf { it.second }

                // Pódio: equipa vencedora primeiro, depois a outra equipa
                val podio = if (totalA >= totalB)
                    equipaAOrdenada + equipaBOrdenada
                else
                    equipaBOrdenada + equipaAOrdenada

                // Preenche os 4 lugares do layout (ajusta para o teu binding)
                binding.txtNomeJogador1.text = podio.getOrNull(0)?.first ?: ""
                binding.txtPontos1.text = podio.getOrNull(0)?.second?.toInt()?.toString() ?: ""
                binding.txtNomeJogador2.text = podio.getOrNull(1)?.first ?: ""
                binding.txtPontos2.text = podio.getOrNull(1)?.second?.toInt()?.toString() ?: ""
                binding.txtNomeJogador3.text = podio.getOrNull(2)?.first ?: ""
                binding.txtPontos3.text = podio.getOrNull(2)?.second?.toInt()?.toString() ?: ""
                binding.txtNomeJogador4.text = podio.getOrNull(3)?.first ?: ""
                binding.txtPontos4.text = podio.getOrNull(3)?.second?.toInt()?.toString() ?: ""

                // Mostra equipa vencedora
                val textoVencedor = when {
                    totalA > totalB -> "Vitória da Equipa A!"
                    totalB > totalA -> "Vitória da Equipa B!"
                    else -> "Empate!"
                }
                Toast.makeText(this@Pontuacao2x2Activity, textoVencedor, Toast.LENGTH_SHORT).show()

                // Opcional: mostra mensagem se alguém ainda não terminou
                if (podio.size < 4) {
                    Toast.makeText(this@Pontuacao2x2Activity, "Aguardando todos os jogadores terminarem...", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        salaRef.addValueEventListener(pontuacaoListener!!)
    }

    private fun atualizarEstatisticasJogador() {
        database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pontuacaoGuardada = snapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                val totalRespostasCertasAnterior = snapshot.child("totalRespostasCertas").getValue(Int::class.java) ?: 0
                val totalJogosAnterior = snapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitoriasAnterior = snapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val totalVitoriasModo2x2Anterior = snapshot.child("totalVitoriasModo2x2").getValue(Int::class.java) ?: 0

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
                        val novoTotalVitoriasModo2x2 = totalVitoriasModo2x2Anterior + if (ganhou) 1 else 0

                        val updates = mapOf(
                            "pontuacao" to if (totalPontos > pontuacaoGuardada) totalPontos else pontuacaoGuardada,
                            "totalRespostasCertas" to novoTotalRespostasCertas,
                            "totalJogos" to novoTotalJogos,
                            "totalVitorias" to novoTotalVitorias,
                            "totalVitoriasModo2x2" to novoTotalVitoriasModo2x2
                        )
                        database.child("jogadores").child(nomeUtilizador).updateChildren(updates)

                        if (totalPontos > pontuacaoGuardada) {
                            Toast.makeText(this@Pontuacao2x2Activity, "NOVO RECORD!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Pontuacao2x2Activity, "Erro ao carregar estatísticas", Toast.LENGTH_SHORT).show()
            }
        })
    }
}