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
import com.example.brainbrawl.Uteis.definirCorBotao
import com.example.brainbrawl.Uteis.obterOpcoesAleatorias
import com.example.brainbrawl.Uteis.tocarSom
import com.example.brainbrawl.databinding.ActivityJogoBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class JogoActivity : AppCompatActivity() {
    // Acessar elementos do layout
    private val binding by lazy { ActivityJogoBinding.inflate(layoutInflater) }

    // Variáveis globais de estado do jogo
    private lateinit var codigoSala: String
    private lateinit var nomeJogador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeUtilizador: String
    private lateinit var perguntaAtual: Pergunta

    // Som de contagem decrescente
    private var mediaPlayer: MediaPlayer? = null
    // Flag para controlar o som
    private var somTocar = false
    // Índice da pergunta atual
    private var perguntaAtualIndex = 0
    // Pontuação total do jogador
    private var totalPontos = 0.0
    // Tempo restante para responder
    private var tempoRestante = 20.0
    // Flag para saber se o tempo está a contar
    private var tempoDecorrido = false
    // Flag para ativar/desativar a barra de progresso
    private var progressBarAtivo = false
    // Momento em que começou a pergunta
    private var tempoIniciado: Long = 0
    // Opções embaralhadas apresentadas
    private var opcoesAtuais: List<String> = emptyList()
    // Modo de jogo ("classico", "caotico", "eliminatorias")
    private var modoJogo: String? = null
    // Flag para saber se é admin
    private var admin = false
    // Respostas certas seguidas
    private var numeroPerguntasCertas = 0
    // Total de perguntas respondidas corretamente
    private var totalPerguntascertas = 0
    // Valor base do bónus de streak
    private var bonus = 50
    // Handler para o cronómetro
    private val handler = Handler(Looper.getMainLooper())
    // Formato para mostrar tempo
    private val formatoDecimal = DecimalFormat("#.#")
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    // Lista de perguntas desta sala
    private val perguntas = mutableListOf<Pergunta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Recuperar dados do intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        if (nomeUtilizador != "") {
            nomeJogador = intent.getStringExtra("nomeJogador") ?: nomeUtilizador
        } else {
            nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        }
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""
        admin = intent.getBooleanExtra("admin", false)

        // Buscar o modo de jogo da sala e configurar o UI
        database.child("salas").child(codigoSala).child("modoJogo")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    modoJogo = snapshot.getValue(String::class.java) ?: "classico"
                    carregarPerguntas()
                    configurarUIPorAdmin()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@JogoActivity, "Erro ao carregar modo de jogo: ${error.message}", Toast.LENGTH_SHORT).show()
                    modoJogo = "classico"
                    carregarPerguntas()
                    configurarUIPorAdmin()
                }
            })
    }

    // Libertar recursos ao destruir a activity
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Configura os botões e visibilidade consoante se é admin
// Configura os botões e visibilidade consoante se é admin
    private fun configurarUIPorAdmin() {
        if (admin) {
            // O admin apenas vê as opções, mas não pode responder
            binding.btnOpcao1.isEnabled = false
            binding.btnOpcao2.isEnabled = false
            binding.btnOpcao3.isEnabled = false
            binding.btnOpcao4.isEnabled = false
        } else {
            // Jogador: ativa os botões (serão ativados/desativados durante o jogo)
            binding.btnOpcao1.isEnabled = true
            binding.btnOpcao2.isEnabled = true
            binding.btnOpcao3.isEnabled = true
            binding.btnOpcao4.isEnabled = true

            binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
            binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
            binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
            binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
        }
        // Para garantir que não há listeners ativos para o admin:
        if (admin) {
            binding.btnOpcao1.setOnClickListener(null)
            binding.btnOpcao2.setOnClickListener(null)
            binding.btnOpcao3.setOnClickListener(null)
            binding.btnOpcao4.setOnClickListener(null)
        }
    }

    // Carrega as perguntas desta sala do Firebase
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

    // Mostra a próxima pergunta e inicia cronómetro
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

        // Definir tempo consoante modo de jogo
        tempoRestante = if (modoJogo == "caotico") 10.0 else 20.0
        iniciarCronometro()
    }

    // Verifica a resposta do jogador, contabiliza pontos e prepara próxima pergunta
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

        // Desativar botões após resposta
        binding.btnOpcao1.isEnabled = false
        binding.btnOpcao2.isEnabled = false
        binding.btnOpcao3.isEnabled = false
        binding.btnOpcao4.isEnabled = false

        // Identificar botões selecionado e correto
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
        definirCorBotao(botaoCorreto!!, "#81C784") // verde para resposta certa

        // Só contam para estatísticas quem não for admin
        if (!admin) {
            if (botaoSelecionado != null && opcaoEscolhida == perguntaAtual.respostaCorreta) {
                // Chamar a função para tocar som de resposta correta
                tocarSom(this, R.raw.certo)
                somTocar = true

                definirCorBotao(botaoSelecionado, "#81C784")
                // Guardar o numero de respostas certas em squencia
                numeroPerguntasCertas++
                // Guardar o total de perguntas certas ao longo do jogo
                totalPerguntascertas++
                atualizarPontuacao()
            } else if (botaoSelecionado != null) {
                // Chamar a função para tocar som de resposta errada
                tocarSom(this, R.raw.errado)
                somTocar = true

                definirCorBotao(botaoSelecionado, "#E57373") // vermelho para errada
                numeroPerguntasCertas = 0
            }
        }

        // Calcular tempo até próxima pergunta (inclui um delay de 3 segundos para feedback visual)
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

    // Finaliza o jogo e envia para o ecrã de pontuação
    private fun finalizarJogo() {
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)

        if (perguntaAtualIndex >= perguntas.size) {
            // Grava a pontuação final na base de dados
            database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador)
                .child("pontuacao").setValue(totalPontos)
            // Gracva o total de perguntas certas
            database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador)
                .child("totalRespostasCertas").setValue(totalPerguntascertas)

            // Verifica estado da sala antes de enviar para pontuações
            database.child("salas").child(codigoSala).child("estado")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        enviarPontuacaoActivity()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@JogoActivity, "Erro ao verificar estado da sala: ${error.message}", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@JogoActivity, MainActivity::class.java)
                        intent.putExtra("nomeUtilizador", nomeUtilizador)
                        startActivity(intent)
                        finish()
                    }
                })
            return
        }

        // Lógica específica para modo eliminatórias (avança ou termina se só sobrar 1)
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

    // Atualiza pontuação do jogador consoante rapidez e streaks
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

        // Bónus de sequência de respostas certas
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

    // Inicia o cronómetro visual e sonoro
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
                // Tocar som ao entrar nos últimos 5 segundos
                if(tempoRestante <= 5 && !somTocar) {
                    mediaPlayer = MediaPlayer.create(this@JogoActivity, R.raw.som)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    somTocar = true
                } else if (tempoRestante > 5 && somTocar) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    somTocar = false
                }

                // Ativar botões apenas na primeira atualização de cada pergunta
                if (primeiraAtualizacao && !admin) {
                    binding.btnOpcao1.isEnabled = true
                    binding.btnOpcao2.isEnabled = true
                    binding.btnOpcao3.isEnabled = true
                    binding.btnOpcao4.isEnabled = true
                    primeiraAtualizacao = false
                }

                // Atualizar tempo restante
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
                // Se o tempo acabar, marca como errada
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

    // Elimina o jogador (modo eliminatórias) e devolve ao ecrã principal
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

    // Envia o jogador para o ecrã de pontuações no fim do jogo
    private fun enviarPontuacaoActivity() {
        val intent = Intent(this, PontuacoesActivity::class.java)
        //codigo da sala
        codigoSala?.let { intent.putExtra("codigoSala", it) }
        // O nome do jogador  que jogou o jogo (quando não é registado)
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
        // A pontuação total do jogador no jogo
        totalPontos.let { intent.putExtra("totalPontos", it) }
        // A categoria das perguntas deste jogo
        nomeCategoria.let { intent.putExtra("nomeCategoria", it) }
        // O nome do utilizador que jogou o jogo (quando é registado)
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        // O modo de jogo
        modoJogo?.let { intent.putExtra("modoJogo", it) }
        // Número de respostas certas seguidas ao longo do jogo
        numeroPerguntasCertas?.let { intent.putExtra("numeroPerguntasCertas", it) }
        // Total de respostas certas do jogador neste jogo
        totalPerguntascertas?.let { intent.putExtra("totalPerguntascertas", it) }
        // Total de perguntas do jogo (útil para calcular percentagens)
        perguntas.size.let { intent.putExtra("totalPerguntas", it) }
        startActivity(intent)
        finish()
    }
}