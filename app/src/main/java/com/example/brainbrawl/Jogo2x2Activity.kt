package com.example.brainbrawl

import Pergunta
import android.content.Intent
import android.icu.text.DecimalFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityJogo2x2Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

class Jogo2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityJogo2x2Binding.inflate(layoutInflater)
    }
    private lateinit var salaId: String
    private lateinit var nomeUtilizador: String
    private lateinit var perguntaAtual: Pergunta
    private lateinit var categoria: String

    private var mediaPlayer: MediaPlayer? = null
    private var somTocar = false
    private var perguntaAtualIndex = 0
    private var totalPontos = 0.0
    private var tempoRestante = 15.0
    private var tempoDecorrido = false
    private var progressBarAtivo = false
    private var tempoIniciado: Long = 0
    private var opcoesAtuais: List<String> = emptyList()
    private var numeroPerguntasCertas = 0
    private var totalPerguntasRespondidas = 0
    private var bonus = 50

    private val handler = Handler(Looper.getMainLooper())
    private val formatoDecimal = DecimalFormat("#.#")
    private val database = FirebaseDatabase.getInstance().reference
    private val perguntas = mutableListOf<Pergunta>()

    // Para saber a equipa do jogador
    private var equipaDoJogador: String = "" // "A" ou "B"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        salaId = intent.getStringExtra("salaId") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        categoria = intent.getStringExtra("categoria") ?: "Todas as categorias"

        // --- Identifica a equipa deste jogador ---
        database.child("sala_2x2").child(salaId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val equipaA = snapshot.child("equipaA").children.mapNotNull { it.key }
                val equipaB = snapshot.child("equipaB").children.mapNotNull { it.key }
                equipaDoJogador = when {
                    equipaA.contains(nomeUtilizador) -> "A"
                    equipaB.contains(nomeUtilizador) -> "B"
                    else -> ""
                }
                carregarOuCriarPerguntas()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Jogo2x2Activity, "Erro ao carregar equipa!", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun carregarOuCriarPerguntas() {
        // Busca ou cria perguntas (idêntico ao 1x1, mas usa sala_2x2)
        database.child("sala_2x2").child(salaId).child("perguntas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    perguntas.clear()
                    if (snapshot.exists()) {
                        for (perguntaSnapshot in snapshot.children) {
                            val pergunta = perguntaSnapshot.getValue(Pergunta::class.java)
                            if (pergunta != null) perguntas.add(pergunta)
                        }
                        if (perguntas.isNotEmpty()) {
                            mostrarPergunta()
                        }
                        configurarBotoes()
                    } else {
                        // Só o primeiro a entrar cria as perguntas - garante justiça com transaction!
                        buscarPerguntasAleatorias { perguntasAleatorias ->
                            database.child("sala_2x2").child(salaId).child("perguntas")
                                .runTransaction(object : Transaction.Handler {
                                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                                        if (currentData.value == null) {
                                            currentData.value = perguntasAleatorias
                                            return Transaction.success(currentData)
                                        }
                                        return Transaction.abort()
                                    }
                                    override fun onComplete(
                                        databaseError: DatabaseError?,
                                        committed: Boolean,
                                        currentData: DataSnapshot?
                                    ) {
                                        database.child("sala_2x2").child(salaId).child("perguntas")
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    perguntas.clear()
                                                    for (perguntaSnapshot in snapshot.children) {
                                                        val pergunta = perguntaSnapshot.getValue(Pergunta::class.java)
                                                        if (pergunta != null) perguntas.add(pergunta)
                                                    }
                                                    if (perguntas.isNotEmpty()) {
                                                        mostrarPergunta()
                                                    }
                                                    configurarBotoes()
                                                }
                                                override fun onCancelled(error: DatabaseError) {
                                                    Toast.makeText(this@Jogo2x2Activity, "Erro ao carregar perguntas", Toast.LENGTH_SHORT).show()
                                                    finish()
                                                }
                                            })
                                    }
                                })
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Jogo2x2Activity, "Erro ao carregar perguntas", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun configurarBotoes() {
        binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
        binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
        binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
        binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
    }

    private fun buscarPerguntasAleatorias(onComplete: (List<Pergunta>) -> Unit) {
        val categoriaEscolhida = categoria
        database.child("categorias")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val todas = mutableListOf<Pergunta>()
                    for (categoriaSnapshot in snapshot.children) {
                        // Só busca perguntas da categoria selecionada, se houver
                        if (categoriaEscolhida == "Todas as categorias" || categoriaEscolhida.isEmpty() ||
                            categoriaSnapshot.key == categoriaEscolhida) {
                            val perguntasSnapshot = categoriaSnapshot.child("perguntas")
                            for (perguntaSnapshot in perguntasSnapshot.children) {
                                val pergunta = perguntaSnapshot.child("pergunta").getValue(String::class.java)
                                val respostaCorreta = perguntaSnapshot.child("respostaCorreta").getValue(String::class.java)
                                val opcoesSnapshot = perguntaSnapshot.child("opcoes").children
                                val opcoes = mutableListOf<String>()
                                opcoesSnapshot.forEach { opcao ->
                                    opcoes.add(opcao.getValue(String::class.java) ?: "")
                                }
                                if (pergunta != null && respostaCorreta != null && opcoes.size == 4) {
                                    todas.add(Pergunta(pergunta, respostaCorreta, opcoes))
                                }
                            }
                        }
                    }
                    val escolhidas = todas.shuffled().take(15)
                    onComplete(escolhidas)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Jogo2x2Activity, "Erro ao buscar perguntas!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun obterOpcoesAleatorias(pergunta: Pergunta): List<String> {
        val opcoes = pergunta.opcoes.toMutableList()
        opcoes.shuffle()
        return opcoes
    }

    private fun mostrarPergunta() {
        tempoIniciado = System.currentTimeMillis()
        if (perguntaAtualIndex >= perguntas.size) {
            finalizarJogo()
            return
        }
        handler.removeCallbacksAndMessages(null)
        perguntaAtual = perguntas[perguntaAtualIndex]
        binding.txtProgresso.text = "Pergunta ${perguntaAtualIndex + 1}/${perguntas.size}"
        binding.txtPergunta.text = perguntaAtual.pergunta

        opcoesAtuais = obterOpcoesAleatorias(perguntaAtual)
        binding.btnOpcao1.text = opcoesAtuais[0]
        binding.btnOpcao2.text = opcoesAtuais[1]
        binding.btnOpcao3.text = opcoesAtuais[2]
        binding.btnOpcao4.text = opcoesAtuais[3]

        definirCorBotao(binding.btnOpcao1, "#E0E0E0")
        definirCorBotao(binding.btnOpcao2, "#E0E0E0")
        definirCorBotao(binding.btnOpcao3, "#E0E0E0")
        definirCorBotao(binding.btnOpcao4, "#E0E0E0")

        tempoRestante = 15.0
        iniciarCronometro()
    }

    private fun definirCorBotao(botao: android.widget.Button, cor: String) {
        botao.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(cor)
        )
    }

    private fun verificarResposta(numeroOpcao: Int) {
        if (somTocar) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            somTocar = false
        }

        tempoDecorrido = false
        if (numeroOpcao == -1) {
            tempoRestante = 0.0
            binding.txtCronometro.text = "0.0"
        }

        binding.btnOpcao1.isEnabled = false
        binding.btnOpcao2.isEnabled = false
        binding.btnOpcao3.isEnabled = false
        binding.btnOpcao4.isEnabled = false

        val botaoSelecionado = when (numeroOpcao) {
            0 -> binding.btnOpcao1
            1 -> binding.btnOpcao2
            2 -> binding.btnOpcao3
            3 -> binding.btnOpcao4
            else -> null
        }

        val opcaoEscolhida = if (numeroOpcao in 0..3) opcoesAtuais[numeroOpcao] else ""
        val indiceCorreto = opcoesAtuais.indexOf(perguntaAtual.respostaCorreta)
        val botaoCorreto = when (indiceCorreto) {
            0 -> binding.btnOpcao1
            1 -> binding.btnOpcao2
            2 -> binding.btnOpcao3
            3 -> binding.btnOpcao4
            else -> null
        }

        definirCorBotao(botaoCorreto!!, "#81C784")

        // Estatísticas
        totalPerguntasRespondidas++
        if (botaoSelecionado != null && opcaoEscolhida == perguntaAtual.respostaCorreta) {
            definirCorBotao(botaoSelecionado, "#81C784")
            numeroPerguntasCertas++
            atualizarPontuacao()
        } else if (botaoSelecionado != null) {
            definirCorBotao(botaoSelecionado, "#E57373")
            numeroPerguntasCertas = 0
        }

        // Guarda resposta do jogador no nó da sala
        val respostaRef = database.child("sala_2x2").child(salaId)
            .child("respostas").child(nomeUtilizador).child(perguntaAtualIndex.toString())
        respostaRef.setValue(opcaoEscolhida)

        val tempoAteProxima = ((tempoIniciado + 15000) - System.currentTimeMillis()).coerceAtLeast(0)
        handler.postDelayed({
            perguntaAtualIndex++
            mostrarPergunta()
        }, tempoAteProxima + 1200)
    }

    private fun finalizarJogo() {
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)

        // Guarda pontuação do jogador no nó da sua equipa
        if (equipaDoJogador == "A" || equipaDoJogador == "B") {
            database.child("sala_2x2").child(salaId)
                .child("pontuacoes_${equipaDoJogador}")
                .child(nomeUtilizador)
                .setValue(totalPontos)
        }

        enviarPontuacaoActivity()
    }

    private fun atualizarPontuacao() {
        val tempoUsado = (15 - tempoRestante).toInt()
        var pontuacao = (15 - tempoUsado) * 10

        if (numeroPerguntasCertas == 2) {
            pontuacao += bonus
            Toast.makeText(this, "Bónus de sequência! +$bonus pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas == 3) {
            pontuacao += bonus + 25
            Toast.makeText(this, "Bónus de sequência! +${bonus + 25} pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas >= 4) {
            pontuacao += bonus + 100
            Toast.makeText(this, "Bónus de sequência! +${bonus + 50} pontos", Toast.LENGTH_SHORT).show()
        }
        totalPontos += pontuacao
    }

    private fun iniciarCronometro() {
        tempoDecorrido = true
        progressBarAtivo = true
        binding.pbTempo.max = 15
        tempoRestante = 15.0
        binding.pbTempo.progress = tempoRestante.toInt()
        binding.txtCronometro.text = formatoDecimal.format(tempoRestante)
        var primeiraAtualizacao = true
        tempoIniciado = System.currentTimeMillis()
        val runnable = object : Runnable {
            override fun run() {

                if (tempoRestante <= 5 && !somTocar) {
                    mediaPlayer = MediaPlayer.create(this@Jogo2x2Activity, R.raw.som)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    somTocar = true
                } else if (tempoRestante > 5 && somTocar) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    somTocar = false
                }

                if (primeiraAtualizacao) {
                    binding.btnOpcao1.isEnabled = true
                    binding.btnOpcao2.isEnabled = true
                    binding.btnOpcao3.isEnabled = true
                    binding.btnOpcao4.isEnabled = true
                    primeiraAtualizacao = false
                }
                val tempoAtual = System.currentTimeMillis()
                tempoRestante = 15.0 - ((tempoAtual - tempoIniciado) / 1000.0)
                if (tempoDecorrido) {
                    binding.txtCronometro.text = formatoDecimal.format(tempoRestante)
                }
                if (progressBarAtivo) {
                    binding.pbTempo.progress = tempoRestante.toInt()
                    if (tempoRestante <= 0) {
                        binding.pbTempo.progress = 0
                        progressBarAtivo = false
                    }
                }
                if (tempoRestante <= 0 && tempoDecorrido) {
                    tempoRestante = 0.0
                    binding.txtCronometro.text = "0.0"
                    tempoDecorrido = false
                    verificarResposta(-1)
                } else if (progressBarAtivo) {
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.postDelayed(runnable, 200)
    }

    private fun enviarPontuacaoActivity() {
        // Redireciona para o ecrã de pontuação (adapta a activity se necessário)
        val intent = Intent(this, Pontuacao2x2Activity::class.java)
        intent.putExtra("codigoSala", salaId)
        intent.putExtra("nomeJogador", nomeUtilizador)
        intent.putExtra("totalPontos", totalPontos)
        intent.putExtra("nomeCategoria", categoria)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("modoJogo", "2x2")
        intent.putExtra("admin", false)
        intent.putExtra("respostasCertas", numeroPerguntasCertas)
        intent.putExtra("totalPerguntas", perguntas.size)
        intent.putExtra("equipa", equipaDoJogador)
        startActivity(intent)
        finish()
    }
}