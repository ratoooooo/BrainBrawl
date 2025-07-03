// MANO O CODIGO NAO AVANÇA NA MESMA PARA A PROXIMA PERGUNTA EU NAO ENTENDO O PROBLEMA
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
import com.example.brainbrawl.UteisJogo.definirCorBotao
import com.example.brainbrawl.UteisJogo.obterOpcoesAleatorias
import com.example.brainbrawl.UteisJogo.tocarSom
import com.example.brainbrawl.databinding.ActivityJogoBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class JogoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityJogoBinding.inflate(layoutInflater) }

    private lateinit var codigoSala: String
    private lateinit var nomeJogador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeUtilizador: String
    private lateinit var perguntaAtual: Pergunta

    private var perguntaAtualIndex = 0
    private var totalPontos = 0.0
    private var numeroPerguntasCertas = 0
    private var totalPerguntascertas = 0
    private var bonus = 50

    private var tempoRestante = 20.0
    private var tempoIniciado: Long = 0
    private var modoJogo: String? = null
    private var opcoesAtuais: List<String> = emptyList()
    private var admin = false
    private var adminPrimeiraPergunta = true
    private var jaRespondeu = false
    private var acertouUltimaPergunta = false

    private var tempoDecorrido = false
    private var progressBarAtivo = false
    private var somTocar = false
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val formatoDecimal = DecimalFormat("#.#")
    private val perguntas = mutableListOf<Pergunta>()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""

        FirebaseDatabase.getInstance().reference.child("salas").child(codigoSala).child("admin")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomeAdmin = snapshot.getValue(String::class.java)
                    admin = (nomeAdmin == nomeUtilizador) || (nomeAdmin == nomeJogador)
                    database.child("salas").child(codigoSala).child("modoJogo")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                modoJogo = snapshot.getValue(String::class.java) ?: "classico"
                                carregarPerguntas()
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@JogoActivity, "Erro ao carregar modo de jogo: ${error.message}", Toast.LENGTH_SHORT).show()
                                modoJogo = "classico"
                                carregarPerguntas()
                            }
                        })
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@JogoActivity, "Erro ao identificar admin: ${error.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        pararSom()
    }

    private fun carregarPerguntas() {
        database.child("salas").child(codigoSala).child("perguntas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    perguntas.clear()
                    for (perguntaSnapshot in snapshot.children) {
                        val pergunta = perguntaSnapshot.getValue(Pergunta::class.java)
                        if (pergunta != null) perguntas.add(pergunta)
                    }
                    if (perguntas.isNotEmpty()) {
                        mostrarResposta()
                    } else {
                        finalizarJogo()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@JogoActivity, "Erro ao carregar perguntas: ${error.message}", Toast.LENGTH_SHORT).show()
                    finalizarJogo()
                }
            })
    }

    private fun mostrarResposta() {
        tempoIniciado = System.currentTimeMillis()
        if (perguntaAtualIndex >= perguntas.size) {
            finalizarJogo()
            return
        }
        handler.removeCallbacksAndMessages(null)
        pararSom()

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

        bloquearAdminSempre()
        jaRespondeu = false
        acertouUltimaPergunta = false
        tempoDecorrido = true
        progressBarAtivo = true

        if (!admin) {
            binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
            binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
            binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
            binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
        } else if (adminPrimeiraPergunta) {
            Toast.makeText(this, "Como admin, só pode observar as respostas.", Toast.LENGTH_SHORT).show()
            adminPrimeiraPergunta = false
        }

        iniciarCronometro()
    }

    private fun bloquearAdminSempre() {
        if (admin) {
            binding.btnOpcao1.isEnabled = false
            binding.btnOpcao2.isEnabled = false
            binding.btnOpcao3.isEnabled = false
            binding.btnOpcao4.isEnabled = false
            binding.btnOpcao1.setOnClickListener(null)
            binding.btnOpcao2.setOnClickListener(null)
            binding.btnOpcao3.setOnClickListener(null)
            binding.btnOpcao4.setOnClickListener(null)
        }
    }

    private fun verificarResposta(numeroOpcao: Int) {
        if (admin || jaRespondeu) return
        jaRespondeu = true

        if (numeroOpcao == -1) {
            tempoRestante = 0.0
            binding.txtCronometro.text = "0.0"
        }

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

        acertouUltimaPergunta = false

        if (botaoSelecionado != null && opcaoEscolhida == perguntaAtual.respostaCorreta) {
            tocarSom(this, R.raw.certo)
            somTocar = true
            definirCorBotao(botaoSelecionado, "#81C784")
            acertouUltimaPergunta = true
            numeroPerguntasCertas++
            totalPerguntascertas++
            atualizarPontuacao()
        } else if (botaoSelecionado != null) {
            tocarSom(this, R.raw.errado)
            somTocar = true
            definirCorBotao(botaoSelecionado, "#E57373")
            numeroPerguntasCertas = 0
            acertouUltimaPergunta = false
        } else if (numeroOpcao == -1) {
            acertouUltimaPergunta = false
        }

        val tempoTotal = if (modoJogo == "caotico") 10.0 else 20.0
        handler.postDelayed({
            if (modoJogo == "eliminatorias") {
                verificarFimEliminatoriasOuAvancar()
            } else {
                perguntaAtualIndex++
                mostrarResposta()
            }
        }, (tempoTotal * 1000 + 3000).toLong()) // Aguarda 13s ou 23s
    }

    private fun iniciarCronometro() {
        tempoDecorrido = true
        progressBarAtivo = true
        tempoRestante = if (modoJogo == "caotico") 10.0 else 20.0
        binding.pbTempo.max = tempoRestante.toInt()
        binding.pbTempo.progress = tempoRestante.toInt()
        binding.txtCronometro.text = formatoDecimal.format(tempoRestante)
        var primeiraAtualizacao = true
        tempoIniciado = System.currentTimeMillis()

        val runnable = object : Runnable {
            override fun run() {
                val tempoAtual = System.currentTimeMillis()
                tempoRestante = if (modoJogo == "caotico") {
                    10.0 - ((tempoAtual - tempoIniciado) / 1000.0)
                } else {
                    20.0 - ((tempoAtual - tempoIniciado) / 1000.0)
                }

                if (tempoRestante <= 5 && tempoRestante > 0 && !somTocar) {
                    pararSom()
                    mediaPlayer = MediaPlayer.create(this@JogoActivity, R.raw.som)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    somTocar = true
                } else if ((tempoRestante > 5 || tempoRestante <= 0) && somTocar) {
                    pararSom()
                }

                if (primeiraAtualizacao && !admin) {
                    binding.btnOpcao1.isEnabled = true
                    binding.btnOpcao2.isEnabled = true
                    binding.btnOpcao3.isEnabled = true
                    binding.btnOpcao4.isEnabled = true
                    primeiraAtualizacao = false
                }

                if (tempoDecorrido) {
                    binding.txtCronometro.text = formatoDecimal.format(tempoRestante.coerceAtLeast(0.0))
                }
                if (progressBarAtivo) {
                    binding.pbTempo.progress = tempoRestante.coerceAtLeast(0.0).toInt()
                    if (tempoRestante <= 0) {
                        binding.pbTempo.progress = 0
                        progressBarAtivo = false
                        if (!jaRespondeu) {
                            verificarResposta(-1) // Trata como resposta errada se não respondeu
                        }
                        // Desativa os botões quando o tempo acabar
                        binding.btnOpcao1.isEnabled = false
                        binding.btnOpcao2.isEnabled = false
                        binding.btnOpcao3.isEnabled = false
                        binding.btnOpcao4.isEnabled = false

                        // NOVO: Se for admin, avança automaticamente para a próxima pergunta
                        if (admin) {
                            handler.postDelayed({
                                perguntaAtualIndex++
                                mostrarResposta()
                            }, 2000) // Espera 2 segundos antes de avançar, ajusta se quiseres
                        }
                    }
                }

                if (tempoRestante > 0 && progressBarAtivo) {
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.post(runnable)
    }

    private fun verificarFimEliminatoriasOuAvancar() {
        if (!admin && !acertouUltimaPergunta) {
            eliminarJogador()
            return
        }
        database.child("salas").child(codigoSala).child("jogadores")
            .get()
            .addOnSuccessListener { snapshot ->
                val jogadoresRestantes = snapshot.children
                    .mapNotNull { it.key }
                    .filter { nome ->
                        nome != nomeUtilizador && nome != nomeJogador
                    }
                if (jogadoresRestantes.size <= 1) {
                    Toast.makeText(this@JogoActivity, "Jogo terminado! Só resta um jogador.", Toast.LENGTH_LONG).show()
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
                intent.putExtra("nomeJogador", nomeJogador)
                startActivity(intent)
                finish()
            }
    }

    private fun eliminarJogador() {
        if (admin) return
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        pararSom()
        database.child("salas").child(codigoSala).child("jogadores")
            .child(nomeJogador).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Você foi eliminado!", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("nomeUtilizador", nomeUtilizador)
                intent.putExtra("nomeJogador", nomeJogador)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao eliminar jogador. Tente novamente.", Toast.LENGTH_LONG).show()
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

    private fun finalizarJogo() {
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        pararSom()
        if (perguntaAtualIndex >= perguntas.size) {
            if (!admin) {
                database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador)
                    .child("pontuacao").setValue(totalPontos)
                database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador)
                    .child("totalRespostasCertas").setValue(totalPerguntascertas)
            }
            database.child("salas").child(codigoSala).child("estado")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        enviarPontuacaoActivity()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@JogoActivity, "Erro ao verificar estado da sala: ${error.message}", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@JogoActivity, MainActivity::class.java)
                        intent.putExtra("nomeUtilizador", nomeUtilizador)
                        intent.putExtra("nomeJogador", nomeJogador)
                        startActivity(intent)
                        finish()
                    }
                })
            return
        }

        if (modoJogo == "eliminatorias") {
            verificarFimEliminatoriasOuAvancar()
        } else {
            perguntaAtualIndex++
            mostrarResposta()
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
        intent.putExtra("numeroPerguntasCertas", numeroPerguntasCertas)
        intent.putExtra("totalPerguntascertas", totalPerguntascertas)
        intent.putExtra("totalPerguntas", perguntas.size)
        startActivity(intent)
        finish()
    }

    private fun pararSom() {
        if (somTocar) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            somTocar = false
        }
    }
}