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
                    // Mapa para armazenar os jogadores e as suas pontuações
                    val jogadores = mutableListOf<Triple<String, Double, Int>>() // Jogador, Pontos, totalPerguntasCertas
                    // Percorrer os filhos do snapshot para obter os jogadores e as suas pontuações
                    for (childSnapshot in snapshot.children) {
                        val jogadorNome = childSnapshot.key ?: "Desconhecido"
                        val pontos = childSnapshot.child("pontuacao").getValue(Double::class.java) ?: 0.0
                        val certas = childSnapshot.child("totalPerguntasCertas").getValue(Int::class.java) ?: 0
                        jogadores.add(Triple(jogadorNome, pontos, certas))
                    }
                    // Ordenar os jogadores por pontuação (maior para menor)
                    jogadores.sortByDescending { it.second }

                    // Guardar o máximo de respostas certas
                    val maxCertas = jogadores.maxOfOrNull { it.third } ?: 0
                    // Guardar o(s) MVP(s) (jogador(es) com mais respostas certas)
                    val mvps = jogadores.filter { it.third == maxCertas && maxCertas > 0 }.map { it.first }

                    // Limpar o layout antes de adicionar as pontuações
                    binding.layoutPodio.removeAllViews()
                    val inflater = LayoutInflater.from(this@PontuacoesActivity)

                    // Mostrar a lista de jogadores e suas pontuações usando o item_podio.xml
                    for ((index, jogador) in jogadores.withIndex()) {
                        // Inflar o layout customizado para cada linha do pódio
                        val view = inflater.inflate(R.layout.item_podio, binding.layoutPodio, false)
                        val txtMedalha = view.findViewById<TextView>(R.id.txt_medalha)
                        val txtNome = view.findViewById<TextView>(R.id.txt_nome_jogador)
                        val txtPontos = view.findViewById<TextView>(R.id.txt_pontos)

                        // Definir medalha e cor consoante a posição
                        when (index) {
                            0 -> { txtMedalha.text = "🥇"; txtMedalha.setTextColor("#FFC400".toColorInt()) }
                            1 -> { txtMedalha.text = "🥈"; txtMedalha.setTextColor("#b0b0b0".toColorInt()) }
                            2 -> { txtMedalha.text = "🥉"; txtMedalha.setTextColor("#ad7e54".toColorInt()) }
                            else -> { txtMedalha.text = "${index+1}"; txtMedalha.setTextColor("#222".toColorInt()) }
                        }

                        // Adicionar tag de MVP se aplicável
                        val isMVP = mvps.contains(jogador.first)
                        val mvpTag = if (isMVP) " 🏆 MVP" else ""
                        txtNome.text = jogador.first + mvpTag

                        // Mostrar pontos
                        txtPontos.text = jogador.second.toInt().toString()

                        // Adicionar a view ao layout do pódio
                        binding.layoutPodio.addView(view)
                    }

                    // Se não houver jogadores
                    if (jogadores.isEmpty()) {
                        val textView = TextView(this@PontuacoesActivity)
                        textView.text = "Sem jogadores na sala."
                        textView.textSize = 16f
                        textView.setTextColor(Color.BLACK)
                        binding.layoutPodio.addView(textView)
                    }

                    // Atualizar estatísticas para jogadores registados
                    if (nomeUtilizador.isNotEmpty()) {
                        // Verifica se ficou em primeiro lugar
                        val ficouEmPrimeiro = jogadores.firstOrNull()?.first == nomeUtilizador
                        atualizarEstatisticasJogador(respostasCertas, totalPerguntas, ficouEmPrimeiro)
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

    // Função para atualizar as estatísticas do jogador
    private fun atualizarEstatisticasJogador(respostasCertas: Int, totalPerguntas: Int, ficouEmPrimeiro: Boolean) {
        if (nomeUtilizador.isEmpty()) return
        database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Guardar os valores que estão guardados na base de dados
                val totalJogosAnterior = snapshot.child("totalJogos").getValue(Int::class.java) ?: 0
                val totalVitoriasAnterior = snapshot.child("totalVitorias").getValue(Int::class.java) ?: 0
                val taxaAcertosAnterior = snapshot.child("taxaAcertos").getValue(Double::class.java) ?: 0.0
                val totalVitoriasModoSoloAnterior = snapshot.child("totalVitoriasModoSolo").getValue(Int::class.java) ?: 0 // Novo campo

                // Calcular os novos valores
                val novoTotalJogos = totalJogosAnterior + 1
                val novoTotalVitorias = totalVitoriasAnterior + if (ficouEmPrimeiro) 1 else 0
                val percentagemEsteJogo = if (totalPerguntas > 0) (respostasCertas.toDouble() / totalPerguntas) * 100 else 0.0

                val novaTaxa = if (totalJogosAnterior == 0) {
                    percentagemEsteJogo
                } else {
                    ((taxaAcertosAnterior * totalJogosAnterior) + percentagemEsteJogo) / novoTotalJogos
                }

                val novoTotalVitoriasModoSolo = totalVitoriasModoSoloAnterior + if (ficouEmPrimeiro) 1 else 0

                // Criar um mapa com os novos valores
                val updates = mapOf(
                    "totalJogos" to novoTotalJogos,
                    "totalVitorias" to novoTotalVitorias,
                    "taxaAcertos" to novaTaxa,
                    "totalVitoriasModoSolo" to novoTotalVitoriasModoSolo
                )
                database.child("jogadores").child(nomeUtilizador).updateChildren(updates)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}