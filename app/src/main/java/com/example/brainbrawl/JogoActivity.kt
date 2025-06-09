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
import com.example.brainbrawl.databinding.ActivityJogoBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class JogoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityJogoBinding.inflate(layoutInflater)
    }

    private lateinit var codigoSala: String
    private lateinit var nomeJogador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeUtilizador: String
    private lateinit var perguntaAtual: Pergunta

    private var mediaPlayer: MediaPlayer? = null
    private var somTocar = false
    private var perguntaAtualIndex = 0
    private var totalPontos = 0.0
    private var tempoRestante = 20.0
    private var tempoDecorrido = false
    private var progressBarAtivo = false
    private var tempoIniciado: Long = 0
    private var opcoesAtuais: List<String> = emptyList()
    private var modoJogo: String? = null
    private var admin = false
    // Numero de perguntas certas e total de perguntas respondidas
    private var numeroPerguntasCertas = 0
    private var totalPerguntasRespondidas = 0
    private var bonus = 50

    private val handler = Handler(Looper.getMainLooper())
    private val formatoDecimal = DecimalFormat("#.#")
    private val database = FirebaseDatabase.getInstance().reference
    private val perguntas = mutableListOf<Pergunta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        if (nomeUtilizador != "") {
            nomeJogador = intent.getStringExtra("nomeJogador") ?: nomeUtilizador
        } else {
            nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        }
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""
        admin = intent.getBooleanExtra("admin", false)

        database.child("salas").child(codigoSala).child("modoJogo")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    modoJogo = snapshot.getValue(String::class.java) ?: "classico"
                    carregarPerguntas()

                    if (admin) {
                        binding.btnOpcao1.visibility = android.view.View.GONE
                        binding.btnOpcao2.visibility = android.view.View.GONE
                        binding.btnOpcao3.visibility = android.view.View.GONE
                        binding.btnOpcao4.visibility = android.view.View.GONE
                    }
                    binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
                    binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
                    binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
                    binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@JogoActivity, "Erro ao carregar modo de jogo", Toast.LENGTH_SHORT).show()
                    modoJogo = "classico"
                    carregarPerguntas()
                    if (admin) {
                        binding.btnOpcao1.visibility = android.view.View.GONE
                        binding.btnOpcao2.visibility = android.view.View.GONE
                        binding.btnOpcao3.visibility = android.view.View.GONE
                        binding.btnOpcao4.visibility = android.view.View.GONE
                    }
                    binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
                    binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
                    binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
                    binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
                }
            })
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun carregarPerguntas() {
        database.child("salas").child(codigoSala).child("perguntas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    perguntas.clear()
                    for (perguntaSnapshot in snapshot.children) {
                        val pergunta = perguntaSnapshot.getValue(Pergunta::class.java)
                        if (pergunta != null) {
                            perguntas.add(pergunta)
                        }
                    }
                    if (perguntas.isNotEmpty()) {
                        mostrarResposta()
                    } else {
                        finalizarJogo()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@JogoActivity, "Erro ao carregar perguntas", Toast.LENGTH_SHORT).show()
                    finalizarJogo()
                }
            })
    }

    private fun obterOpcoesAleatorias(pergunta: Pergunta): List<String> {
        val opcoes = pergunta.opcoes.toMutableList()
        opcoes.shuffle()
        return opcoes
    }

    private fun mostrarResposta() {
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

        if (modoJogo == "caotico") {
            tempoRestante = 10.0
        } else {
            tempoRestante = 20.0
        }
        iniciarCronometro()
    }

    private fun definirCorBotao(botao: android.widget.Button, cor: String) {
        botao.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(cor)
        )
    }

    private fun verificarResposta(numeroOpcao: Int) {
        // Desligar o som se estiver tocando
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

        // Só contam para estatísticas quem não for admin
        if (!admin) {
            totalPerguntasRespondidas++
            if (botaoSelecionado != null && opcaoEscolhida == perguntaAtual.respostaCorreta) {
                definirCorBotao(botaoSelecionado, "#81C784")
                numeroPerguntasCertas++
                atualizarPontuacao()
            } else if (botaoSelecionado != null) {
                definirCorBotao(botaoSelecionado, "#E57373")
                numeroPerguntasCertas = 0
            }
        }

        val tempoBase = if (modoJogo == "caotico") 10000 else 20000
        val tempoAteProxima = ((tempoIniciado + tempoBase) - System.currentTimeMillis()).coerceAtLeast(0)
        handler.postDelayed({
            if (!admin && modoJogo == "eliminatorias") {
                eliminarJogador()
            } else {
                perguntaAtualIndex++
                mostrarResposta()
            }
        }, tempoAteProxima + 3000)
    }

    private fun finalizarJogo() {
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)

        if (perguntaAtualIndex >= perguntas.size) {
            database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador)
                .child("pontuacao").setValue(totalPontos)

            database.child("salas").child(codigoSala).child("estado")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        enviarPontuacaoActivity()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@JogoActivity, "Erro ao verificar estado da sala.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@JogoActivity, MainActivity::class.java)
                        intent.putExtra("nomeUtilizador", nomeUtilizador)
                        startActivity(intent)
                        finish()
                    }
                })
            return
        }

        if (!admin && modoJogo == "eliminatorias") {
            database.child("salas").child(codigoSala).child("jogadores")
                .get()
                .addOnSuccessListener { snapshot ->
                    val numeroJogadores = snapshot.childrenCount
                    if (numeroJogadores <= 1) {
                        Toast.makeText(this@JogoActivity, "Jogo terminado! Apenas um jogador restante.", Toast.LENGTH_LONG).show()
                        enviarPontuacaoActivity()
                    } else {
                        perguntaAtualIndex++
                        mostrarResposta()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this@JogoActivity, "Erro ao verificar jogadores.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@JogoActivity, MainActivity::class.java)
                    intent.putExtra("nomeUtilizador", nomeUtilizador)
                    startActivity(intent)
                    finish()
                }
        } else {
            perguntaAtualIndex++
            mostrarResposta()
        }
    }

    private fun atualizarPontuacao() {
        val tempoUsado = if (modoJogo == "caotico") {
            (10 - tempoRestante).toInt()
        } else {
            (20 - tempoRestante).toInt()
        }

        var pontuacao: Int

        if (modoJogo == "caotico") {
            pontuacao = (10 - tempoUsado) * 30
        } else {
            pontuacao = (20 - tempoUsado) * 10
        }

        if (numeroPerguntasCertas == 2) {
            pontuacao += bonus
            Toast.makeText(this, "Bônus de sequência! +$bonus pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas == 3) {
            pontuacao += bonus + 25
            Toast.makeText(this, "Bônus de sequência! +${bonus + 25} pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas >= 4) {
            pontuacao += bonus + 50
            Toast.makeText(this, "Bônus de sequência! +${bonus + 50} pontos", Toast.LENGTH_SHORT).show()
        }
        totalPontos += pontuacao
    }

    private fun iniciarCronometro() {
        tempoDecorrido = true
        progressBarAtivo = true
        if (modoJogo == "caotico") {
            binding.pbTempo.max = 10
            tempoRestante = 10.0
        } else {
            binding.pbTempo.max = 20
            tempoRestante = 20.0
        }
        binding.pbTempo.progress = tempoRestante.toInt()
        binding.txtCronometro.text = formatoDecimal.format(tempoRestante)
        var primeiraAtualizacao = true
        tempoIniciado = System.currentTimeMillis()
        val runnable = object : Runnable {
            override fun run() {

                if(tempoRestante <= 5 && !somTocar)
                {
                    mediaPlayer = MediaPlayer.create(this@JogoActivity, R.raw.som)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    somTocar = true
                }
                else if (tempoRestante > 5 && somTocar) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    somTocar = false
                }

                if (primeiraAtualizacao && !admin) {
                    binding.btnOpcao1.isEnabled = true
                    binding.btnOpcao2.isEnabled = true
                    binding.btnOpcao3.isEnabled = true
                    binding.btnOpcao4.isEnabled = true
                    primeiraAtualizacao = false
                }
                val tempoAtual = System.currentTimeMillis()
                if (modoJogo == "caotico") {
                    tempoRestante = 10.0 - ((tempoAtual - tempoIniciado) / 1000.0)
                } else {
                    tempoRestante = 20.0 - ((tempoAtual - tempoIniciado) / 1000.0)
                }
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

    private fun eliminarJogador() {
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        database.child("salas").child(codigoSala).child("jogadores")
            .child(nomeJogador).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Você foi eliminado!", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("nomeUtilizador", nomeUtilizador)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao eliminar jogador. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun enviarPontuacaoActivity() {
        val intent = Intent(this, PontuacoesActivity::class.java)
        intent.putExtra("codigoSala", codigoSala)
        intent.putExtra("nomeJogador", nomeJogador)
        intent.putExtra("totalPontos", totalPontos)
        intent.putExtra("nomeCategoria", nomeCategoria)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("admin", admin)
        intent.putExtra("respostasCertas", numeroPerguntasCertas)
        intent.putExtra("totalPerguntas", perguntas.size)
        startActivity(intent)
        finish()
    }

}