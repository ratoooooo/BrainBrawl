package com.example.brainbrawl

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.brainbrawl.databinding.ActivityPontuacaoBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PontuacoesActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityPontuacaoBinding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeJogador: String
    private var totalPontos: Double = 0.0
    private var respostasCertas: Int = 0
    private var totalPerguntas: Int = 1
    private var totalRespostasCertas = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        respostasCertas = intent.getIntExtra("respostasCertas", 0)
        totalPerguntas = intent.getIntExtra("totalPerguntas", 1)
        totalRespostasCertas = intent.getIntExtra("totalPerguntascertas", 0)

        // Chamar a função para carregar pontuação da sala
        carregarPontuacaoSala()

        // Configurar o botao de voltar
        binding.btnVoltar.setOnClickListener {
            // Redirecionar para a MainActivity e passar o nome do utilizador
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            startActivity(intent)
            finish()
        }
    }

    // Função para carrear a pontuação da sala
    private fun carregarPontuacaoSala() {
        database.child("salas").child(codigoSala).child("jogadores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val jogadores = mutableListOf<Triple<String, Double, Int>>() // Jogador, Pontos, totalPerguntasCertas
                    for (childSnapshot in snapshot.children) {
                        val jogadorNome = childSnapshot.key ?: "Desconhecido"
                        if (jogadorNome == "admin" || jogadorNome.isEmpty()) {
                            // Ignorar admin ou nomes vazios para o pódio
                            continue
                        }
                        val pontos = childSnapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                        val certas = childSnapshot.child("totalPerguntasCertas").getValue(Int::class.java) ?: 0
                        jogadores.add(Triple(jogadorNome, pontos, certas))
                    }
                    // Ordenar apenas para fins de visualização do pódio
                    jogadores.sortByDescending { it.second }
                    val maxCertas = jogadores.maxOfOrNull { it.third } ?: 0
                    val mvps = jogadores.filter { it.third == maxCertas && maxCertas > 0 }.map { it.first }

                    // Mostrar pódio normalmente
                    binding.layoutPodio.removeAllViews()
                    val inflater = LayoutInflater.from(this@PontuacoesActivity)
                    for ((index, jogador) in jogadores.withIndex()) {
                        val view = inflater.inflate(R.layout.item_podio, binding.layoutPodio, false)
                        val txtMedalha = view.findViewById<TextView>(R.id.txt_medalha)
                        val txtNome = view.findViewById<TextView>(R.id.txt_nome_jogador)
                        val txtPontos = view.findViewById<TextView>(R.id.txt_pontos)
                        when (index) {
                            0 -> { txtMedalha.text = "🥇"; txtMedalha.setTextColor("#FFC400".toColorInt()) }
                            1 -> { txtMedalha.text = "🥈"; txtMedalha.setTextColor("#b0b0b0".toColorInt()) }
                            2 -> { txtMedalha.text = "🥉"; txtMedalha.setTextColor("#ad7e54".toColorInt()) }
                            else -> { txtMedalha.text = "${index+1}"; txtMedalha.setTextColor("#222".toColorInt()) }
                        }
                        val isMVP = mvps.contains(jogador.first)
                        val mvpTag = if (isMVP) " 🏆 MVP" else ""
                        txtNome.text = jogador.first + mvpTag
                        txtPontos.text = jogador.second.toInt().toString()
                        binding.layoutPodio.addView(view)
                    }
                    if (jogadores.isEmpty()) {
                        val textView = TextView(this@PontuacoesActivity)
                        textView.text = "Sem jogadores na sala."
                        textView.textSize = 16f
                        textView.setTextColor(Color.BLACK)
                        binding.layoutPodio.addView(textView)
                    }

                    // ATUALIZAR TODOS OS JOGADORES DA SALA
                    for ((index, jogador) in jogadores.withIndex()) {
                        val ficouEmPrimeiro = (index == 0)
                        atualizarEstatisticasJogadorParaUtilizador(
                            jogadorNome = jogador.first,
                            pontuacao = jogador.second,
                            respostasCertas = jogador.third,
                            totalPerguntas = 1,
                            ficouEmPrimeiro = ficouEmPrimeiro
                        )
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    binding.layoutPodio.removeAllViews()
                    val textView = TextView(this@PontuacoesActivity)
                    textView.text = "Erro ao carregar resultados"
                    textView.textSize = 16f
                    textView.setTextColor(Color.BLACK)
                    binding.layoutPodio.addView(textView)
                }
            })
    }

    // Função para atualizar estatísticas e pontuação
    private fun atualizarEstatisticasJogadorParaUtilizador(jogadorNome: String, pontuacao: Double, respostasCertas: Int, totalPerguntas: Int, ficouEmPrimeiro: Boolean) {
        if (jogadorNome.isEmpty()) return
        database.child("jogadores").child(jogadorNome).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Guarda os valores guardados anteriormente
                val totalJogosAnterior = snapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitoriasAnterior = snapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val taxaAcertosAnterior = snapshot.child("taxaAcertos").getValue(Double::class.java) ?: 0.0
                val totalVitoriasModoSoloAnterior = snapshot.child("totalVitoriasModoSolo").getValue(Int::class.java) ?: 0
                val pontuacaoAnterior = snapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0

                // Calcula os novos valores
                val novoTotalJogos = totalJogosAnterior + 1
                val novoTotalVitorias = totalVitoriasAnterior + if (ficouEmPrimeiro) 1 else 0
                val percentagemEsteJogo = if (totalPerguntas > 0) (respostasCertas.toDouble() / totalPerguntas) * 100 else 0.0
                val novaTaxa = if (totalJogosAnterior == 0) percentagemEsteJogo else ((taxaAcertosAnterior * totalJogosAnterior) + percentagemEsteJogo) / novoTotalJogos
                val novoTotalVitoriasModoSolo = totalVitoriasModoSoloAnterior + if (ficouEmPrimeiro) 1 else 0
                val novaPontuacaoMaxima = if (pontuacao > pontuacaoAnterior) pontuacao else pontuacaoAnterior

                // Cria um mapa de atualizações
                val updates = mapOf(
                    "totalJogos" to novoTotalJogos,
                    "totalVitorias" to novoTotalVitorias,
                    "taxaAcertos" to novaTaxa,
                    "totalVitoriasModoSolo" to novoTotalVitoriasModoSolo,
                    "pontuacao" to novaPontuacaoMaxima
                )
                database.child("jogadores").child(jogadorNome).updateChildren(updates)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }}