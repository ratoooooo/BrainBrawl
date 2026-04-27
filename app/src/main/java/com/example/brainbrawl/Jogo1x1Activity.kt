package com.example.brainbrawl

import Pergunta
import android.icu.text.DecimalFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisJogo.atualizarPontuacao
import com.example.brainbrawl.UteisJogo.definirCorBotao
import com.example.brainbrawl.UteisJogo.obterOpcoesAleatorias
import com.example.brainbrawl.UteisJogo.tocarSom
import com.example.brainbrawl.UteisNavegacao.enviarPontuacaoActivity
import com.example.brainbrawl.databinding.ActivityJogo1x1Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

class Jogo1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityJogo1x1Binding.inflate(layoutInflater)
    }
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var perguntaAtual: Pergunta

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
    // Total de perguntas respondidas corretamente
    private var totalPerguntascertas = 0
    private var bonus = 50

    private val handler = Handler(Looper.getMainLooper())
    private val formatoDecimal = DecimalFormat("#.#")
    private val database = FirebaseDatabase.getInstance().reference
    private val perguntas = mutableListOf<Pergunta>()
    private lateinit var categoria: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pelo Intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""

        // Lê a categoria REAL da sala do Firebase para garantir filtragem correta
        database.child("sala_1x1").child(codigoSala).child("nomeCategoria")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoria = snapshot.getValue(String::class.java) ?: "Todas as categorias"
                    prepararPerguntasEJogo()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Jogo1x1Activity, "Erro ao ler categoria!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun prepararPerguntasEJogo() {
        // Buscar ou criar perguntas
        database.child("sala_1x1").child(codigoSala).child("perguntas")
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
                            database.child("sala_1x1").child(codigoSala).child("perguntas")
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
                                        // Volta a escutar até existirem perguntas
                                        database.child("sala_1x1").child(codigoSala).child("perguntas")
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
                                                    Toast.makeText(this@Jogo1x1Activity, "Erro ao carregar perguntas", Toast.LENGTH_SHORT).show()
                                                    finish()
                                                }
                                            })
                                    }
                                })
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Jogo1x1Activity, "Erro ao carregar perguntas", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Libertar recursos do handler e do media player
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Função que configura os listeners dos botões de opções
    private fun configurarBotoes() {
        binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
        binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
        binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
        binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
    }

    // Função que busca as perguntas aleatórias da categoria selecionada ou de todas as categorias
    private fun buscarPerguntasAleatorias(onComplete: (List<Pergunta>) -> Unit) {
        val categoriasRef = database.child("categorias")
        if (categoria == getString(R.string.categoria5) || categoria.isEmpty()) {
            // Busca perguntas de todas as categorias
            categoriasRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val todas = mutableListOf<Pergunta>()
                    for (categoriaSnapshot in snapshot.children) {
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
                    // Embaralha e seleciona 8 perguntas aleatórias
                    val escolhidas = todas.shuffled().take(8)
                    onComplete(escolhidas)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Jogo1x1Activity, "Erro ao buscar perguntas!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
        } else {
            // Busca perguntas só da categoria escolhida
            categoriasRef.child(categoria).child("perguntas")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val perguntas = mutableListOf<Pergunta>()
                        for (perguntaSnapshot in snapshot.children) {
                            val pergunta = perguntaSnapshot.child("pergunta").getValue(String::class.java)
                            val respostaCorreta = perguntaSnapshot.child("respostaCorreta").getValue(String::class.java)
                            val opcoesSnapshot = perguntaSnapshot.child("opcoes").children
                            val opcoes = mutableListOf<String>()
                            opcoesSnapshot.forEach { opcao ->
                                opcoes.add(opcao.getValue(String::class.java) ?: "")
                            }
                            if (pergunta != null && respostaCorreta != null && opcoes.size == 4) {
                                perguntas.add(Pergunta(pergunta, respostaCorreta, opcoes))
                            }
                        }
                        val escolhidas = perguntas.shuffled().take(8)
                        onComplete(escolhidas)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@Jogo1x1Activity, "Erro ao buscar perguntas!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                })
        }
    }

    // Função que a pergunta atual e redefine o cronómetro
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

        // Reset visual dos botões
        definirCorBotao(binding.btnOpcao1, "#E0E0E0")
        definirCorBotao(binding.btnOpcao2, "#E0E0E0")
        definirCorBotao(binding.btnOpcao3, "#E0E0E0")
        definirCorBotao(binding.btnOpcao4, "#E0E0E0")

        // Atualiza a hora de início no Firebase
        database.child("sala_1x1").child(codigoSala).child("perguntaHoraInicio")
            .setValue(tempoIniciado)

        tempoRestante = 15.0
        iniciarCronometro()
    }

    // Função que verefica se a resposta está correta e atualiza pontuação
    private fun verificarResposta(numeroOpcao: Int) {
        // Parar som se estiver a tocar
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

        // Verde para a resposta correta
        definirCorBotao(botaoCorreto!!, "#81C784")

        // Estatísticas
        totalPerguntasRespondidas++
        if (botaoSelecionado != null && opcaoEscolhida == perguntaAtual.respostaCorreta) {
            // Chamar a função para tocar som de resposta correta
            tocarSom(this, R.raw.certo)
            somTocar = true

            definirCorBotao(botaoSelecionado, "#81C784")
            numeroPerguntasCertas++
            totalPerguntascertas++
            val pontos = atualizarPontuacao(this, tempoRestante, numeroPerguntasCertas, bonus)
            totalPontos += pontos
        } else if (botaoSelecionado != null) {
            // Chamar a função para tocar som de resposta errada
            tocarSom(this, R.raw.errado)
            somTocar = true

            // Vermelho para a resposta errada
            definirCorBotao(botaoSelecionado, "#E57373")
            numeroPerguntasCertas = 0
        }

        // Delay para mostrar feedback antes da próxima pergunta
        val tempoAteProxima = ((tempoIniciado + 15000) - System.currentTimeMillis()).coerceAtLeast(0)
        handler.postDelayed({
            perguntaAtualIndex++
            mostrarPergunta()
        }, tempoAteProxima + 1200)
    }

    // Função para finalizar o jogo e guardar pontuação
    private fun finalizarJogo() {
        if (mediaPlayer != null) {
            pararSom()
        }

        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)

        // Guarda a tua pontuação
        database.child("sala_1x1").child(codigoSala)
            .child("pontuacoes").child(nomeUtilizador)
            .setValue(totalPontos)
            .addOnSuccessListener {
                // Listener para aguardar que ambos terminem
                aguardarPodioCompleto()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao guardar pontuação!", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun aguardarPodioCompleto() {
        val pontuacoesRef = database.child("sala_1x1").child(codigoSala).child("pontuacoes")
        pontuacoesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount >= 2) { // ambos terminaram!
                    // Remove o listener antes de avançar
                    pontuacoesRef.removeEventListener(this)
                    // Avança para o pódio (agora sim, ambos podem ver!)
                    enviarPontuacaoActivity(
                        this@Jogo1x1Activity, codigoSala, "1x1", nomeUtilizador, totalPontos, categoria, nomeUtilizador, totalPerguntascertas, numeroPerguntasCertas, perguntas.size
                    )
                } else {
                    // Mostra mensagem de espera (podes melhorar para mostrar loading, etc)
                    Toast.makeText(this@Jogo1x1Activity, "Aguarde que o adversário termine!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Jogo1x1Activity, "Erro ao verificar pódio!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Função que inicia o cronómetro visual e sonoro da pergunta de forma sincronizada
    private fun iniciarCronometro() {
        tempoDecorrido = true
        progressBarAtivo = true
        val tempoTotal = 15.0
        binding.pbTempo.max = tempoTotal.toInt()
        binding.pbTempo.progress = tempoTotal.toInt()
        var primeiraAtualizacao = true

        // Obtém a hora de início sincronizada do Firebase
        database.child("sala_1x1").child(codigoSala).child("perguntaHoraInicio")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val horaInicio = snapshot.getValue(Long::class.java) ?: System.currentTimeMillis()
                    val runnable = object : Runnable {
                        override fun run() {
                            val tempoAtual = System.currentTimeMillis()
                            val tempoDecorridoSegundos = (tempoAtual - horaInicio) / 1000.0
                            tempoRestante = tempoTotal - tempoDecorridoSegundos
                            if (tempoRestante < 0) tempoRestante = 0.0

                            // Toca som nos últimos 5 segundos
                            if (tempoRestante <= 5 && tempoRestante > 0 && !somTocar) {
                                mediaPlayer = MediaPlayer.create(this@Jogo1x1Activity, R.raw.som)
                                mediaPlayer?.isLooping = true
                                mediaPlayer?.start()
                                somTocar = true
                            } else if ((tempoRestante > 5 || tempoRestante <= 0) && somTocar) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                somTocar = false
                            }

                            // Ativar botões só na primeira atualização
                            if (primeiraAtualizacao) {
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
                                    if (tempoDecorrido) {
                                        verificarResposta(-1)
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

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Jogo1x1Activity, "Erro ao sincronizar cronómetro: ${error.message}", Toast.LENGTH_SHORT).show()
                    // Fallback para cronómetro local se a sincronização falhar
                    tempoIniciado = System.currentTimeMillis()
                    val runnable = object : Runnable {
                        override fun run() {
                            val tempoAtual = System.currentTimeMillis()
                            tempoRestante = 15.0 - ((tempoAtual - tempoIniciado) / 1000.0)
                            if (tempoRestante < 0) tempoRestante = 0.0

                            // Toca som nos últimos 5 segundos
                            if (tempoRestante <= 5 && tempoRestante > 0 && !somTocar) {
                                mediaPlayer = MediaPlayer.create(this@Jogo1x1Activity, R.raw.som)
                                mediaPlayer?.isLooping = true
                                mediaPlayer?.start()
                                somTocar = true
                            } else if ((tempoRestante > 5 || tempoRestante <= 0) && somTocar) {
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

                            if (tempoDecorrido) {
                                binding.txtCronometro.text = formatoDecimal.format(tempoRestante.coerceAtLeast(0.0))
                            }
                            if (progressBarAtivo) {
                                binding.pbTempo.progress = tempoRestante.coerceAtLeast(0.0).toInt()
                                if (tempoRestante <= 0) {
                                    binding.pbTempo.progress = 0
                                    progressBarAtivo = false
                                    if (tempoDecorrido) {
                                        verificarResposta(-1)
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
            })
    }

    // Função para parar e libertar o media player
    private fun pararSom() {
        if (somTocar) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            somTocar = false
        }
    }
}
