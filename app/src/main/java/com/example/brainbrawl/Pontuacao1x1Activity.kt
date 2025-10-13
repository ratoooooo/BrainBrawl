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

    private var desforraListener: ValueEventListener? = null
    private var pontuacaoListener: ValueEventListener? = null
    private var novaSalaListener: ValueEventListener? = null

    private var adversario: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pelo Intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        totalRespostasCertas = intent.getIntExtra("totalRespostasCertas", 0)
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""

        // Chama a função para atualizar a pontuação do jogador
        carregarPontuacao1x1Realtime()
        // Chama a funçao para os jogadores jogarem novamente
        escutarNovaSalaDesforra()

        // Configura o botoa de voltar e desforra
        binding.btnVoltar.setOnClickListener {
            database.child("sala_1x1").child(codigoSala).removeValue()
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("nomeUtilizador", nomeUtilizador)
            })
            finish()
        }

        binding.btnDesforra.setOnClickListener {
            pedirDesforra()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removerListener(desforraListener, "jogadores")
        removerListener(pontuacaoListener, "pontuacoes")
        removerListener(novaSalaListener, "novaSalaDesforra")
    }

    // Chama a função para escutar a nova sala de desforra
    private fun escutarNovaSalaDesforra() {
        novaSalaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Guarda o novo código da sala
                val novaSala = snapshot.getValue(String::class.java)
                if (!novaSala.isNullOrEmpty()) {
                    // Envia de volta para a SalaDeEspera1x1Activity com o novo código da sala
                    val intent = Intent(this@Pontuacao1x1Activity, SalaDeEspera1x1Activity::class.java)
                    intent.putExtra("codigoSala", novaSala)
                    intent.putExtra("nomeUtilizador", nomeUtilizador)
                    intent.putExtra("nomeCategoria", nomeCategoria)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("sala_1x1").child(codigoSala).child("novaSalaDesforra")
            .addValueEventListener(novaSalaListener!!)
    }

    // Chama a fubção para pedir desforra
    private fun pedirDesforra() {
        database.child("sala_1x1").child(codigoSala)
            .child("jogadores").child(nomeUtilizador).child("desforra").setValue(true)

        if (desforraListener == null) {
            val ref = database.child("sala_1x1").child(codigoSala).child("jogadores")
            desforraListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var desforraAceita = false
                    var outroJogador: String? = null
                    // Verifica se o outro jogador aceitou a desforra
                    for (child in snapshot.children) {
                        if (child.key != nomeUtilizador &&
                            child.child("desforra").getValue(Boolean::class.java) == true) {
                            desforraAceita = true
                            outroJogador = child.key
                            break
                        }
                    }
                    if (desforraAceita && outroJogador != null) {
                        removerListener(desforraListener, "jogadores")
                        criarSalaDesforra(outroJogador)
                    } else {
                        Toast.makeText(this@Pontuacao1x1Activity, "Aguardando o outro jogador aceitar desforra", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(desforraListener!!)
        }
    }

    // Função para criar uma nova sala de desforra
    private fun criarSalaDesforra(adversario: String) {
        val novoCodigoSala = gerarCodigoSala()
        val salaRef = database.child("sala_1x1").child(novoCodigoSala)

        salaRef.child("jogadores").child(nomeUtilizador).setValue(true)
        salaRef.child("jogadores").child(adversario).setValue(true)
        salaRef.child("estado").setValue("em_espera")
        salaRef.child("nomeCategoria").setValue(nomeCategoria)
        salaRef.child("prontos").child(nomeUtilizador).setValue(true)
        salaRef.child("prontos").child(adversario).setValue(false)

        database.child("sala_1x1").child(codigoSala).child("jogadores").child(nomeUtilizador).child("desforra").removeValue()
        database.child("sala_1x1").child(codigoSala).child("jogadores").child(adversario).child("desforra").removeValue()

        database.child("sala_1x1").child(codigoSala).child("novaSalaDesforra").setValue(novoCodigoSala)
    }

    // Função para carregar a pontuação dos jogadores
    private fun carregarPontuacao1x1Realtime() {
        pontuacaoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jogadores = mutableListOf<Pair<String, Double>>()
                for (child in snapshot.children) {
                    val nome = child.key ?: "Desconhecido"
                    val pontos = child.getValue(Double::class.java) ?: 0.0
                    jogadores.add(Pair(nome, pontos))
                }
                jogadores.sortByDescending { it.second }

                if (jogadores.isNotEmpty()) {
                    binding.txtNomeJogador1.text = jogadores[0].first
                    binding.txtPontos1.text = jogadores[0].second.toInt().toString()
                    if (jogadores[0].first != nomeUtilizador) adversario = jogadores[0].first
                }
                if (jogadores.size > 1) {
                    binding.txtNomeJogador2.text = jogadores[1].first
                    binding.txtPontos2.text = jogadores[1].second.toInt().toString()
                    if (jogadores[1].first != nomeUtilizador) adversario = jogadores[1].first
                }
                if (jogadores.size <= 1) {
                    binding.txtNomeJogador2.text = "Aguardando adversário..."
                    binding.txtPontos2.text = ""
                }

                for ((index, jogador) in jogadores.withIndex()) {
                    val ficouEmPrimeiro = (index == 0)
                    atualizarEstatisticasJogador1x1(jogador.first, jogador.second, ficouEmPrimeiro)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Pontuacao1x1Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
            }
        }

        database.child("sala_1x1").child(codigoSala).child("pontuacoes")
            .addValueEventListener(pontuacaoListener!!)
    }

    private fun atualizarEstatisticasJogador1x1(
        jogadorNome: String,
        pontosObtidos: Double,
        ficouEmPrimeiro: Boolean
    ) {
        database.child("jogadores").child(jogadorNome)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Guarda os dados anteriores
                    val totalJogosAnterior = snapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                    val totalVitoriasAnterior = snapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                    val totalRespostasCertasAnterior = snapshot.child("totalRespostasCertas").getValue(Int::class.java) ?: 0
                    val totalPontuacaoAnterior = snapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                    val totalVitoriasModo1x1Anterior = snapshot.child("totalVitoriasModo1x1").getValue(Int::class.java) ?: 0

                    // Atualiza os dados do jogador
                    val novoTotalVitoriasModo1x1 = totalVitoriasModo1x1Anterior + if (ficouEmPrimeiro) 1 else 0
                    val novoTotalJogos = totalJogosAnterior + 1
                    val novoTotalVitorias = totalVitoriasAnterior + if (ficouEmPrimeiro) 1 else 0
                    val novaPontuacaoMaxima = maxOf(pontosObtidos, totalPontuacaoAnterior)

                    // Cria o mapa de atualizações
                    val updates = mapOf(
                        "totalJogos" to novoTotalJogos,
                        "totalVitorias" to novoTotalVitorias,
                        "totalRespostasCertas" to (totalRespostasCertasAnterior + totalRespostasCertas),
                        "totalVitoriasModo1x1" to novoTotalVitoriasModo1x1,
                        "pontuacao" to novaPontuacaoMaxima
                    )
                    // Atualiza os dados do jogador no Firebase
                    database.child("jogadores").child(jogadorNome).updateChildren(updates)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun removerListener(listener: ValueEventListener?, campo: String) {
        listener?.let {
            database.child("sala_1x1").child(codigoSala).child(campo).removeEventListener(it)
        }
    }
}
