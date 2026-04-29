package com.example.brainbrawl

import com.example.brainbrawl.models.Pergunta
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
import com.example.brainbrawl.UteisNavegacao.enviarPontuacaoActivity
import com.example.brainbrawl.databinding.ActivityJogo2x2Binding
import com.example.brainbrawl.repositories.JogoCompetitivoRepository
import com.example.brainbrawl.repositories.JogoCompetitivoRepository.ModoCompetitivo
import com.example.brainbrawl.services.ScoreCompetitivoService

class Jogo2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityJogo2x2Binding.inflate(layoutInflater)
    }
    private val jogoCompetitivoRepository = JogoCompetitivoRepository()
    private val scoreCompetitivoService = ScoreCompetitivoService()
    // Variáveis para dados do jogo e jogador
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var perguntaAtual: Pergunta
    private lateinit var categoria: String

    // Variáveis de lógica de jogo
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
    // Total de perguntas respondidas corretamente
    private var totalPerguntascertas = 0

    private val handler = Handler(Looper.getMainLooper())
    private val formatoDecimal = DecimalFormat("#.#")
    private val perguntas = mutableListOf<Pergunta>()
    private var serverTimeOffset: Long = 0L
    private var offsetListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var podioListener: JogoCompetitivoRepository.ListenerHandle? = null

    // Variável para saber a equipa do jogador ("A" ou "B")
    private var equipaDoJogador: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //Guardar os dados passados pelo Intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        carregarOffsetServidor()

        // Lê a categoria REAL da sala do Firebase para garantir filtragem correta
        jogoCompetitivoRepository.carregarNomeCategoria(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            getString(R.string.categoria5)
        ).addOnSuccessListener { nomeCategoria ->
            categoria = nomeCategoria
            identificarEquipaECarregarPerguntas()
        }.addOnFailureListener {
            Toast.makeText(this@Jogo2x2Activity, "Erro ao ler categoria!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun identificarEquipaECarregarPerguntas() {
        // Verifica a equipa do jogador na sala
        jogoCompetitivoRepository.identificarEquipa2x2(codigoSala, nomeUtilizador)
            .addOnSuccessListener { equipa ->
                equipaDoJogador = equipa
                carregarOuCriarPerguntas()
            }
            .addOnFailureListener {
                Toast.makeText(this@Jogo2x2Activity, "Erro ao carregar equipa!", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    // Função que carrega as perguntas da sala ou as cria se for o primeiro jogador
    private fun carregarOuCriarPerguntas() {
        jogoCompetitivoRepository.carregarOuCriarPerguntas(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            categoria,
            getString(R.string.categoria5)
        ).addOnSuccessListener { perguntasCarregadas ->
            perguntas.clear()
            perguntas.addAll(perguntasCarregadas)
            if (perguntas.isNotEmpty()) {
                mostrarPergunta()
            }
            configurarBotoes()
        }.addOnFailureListener {
            Toast.makeText(this@Jogo2x2Activity, mensagemErroPerguntas(it), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Libertar recursos do handler e do media player
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        jogoCompetitivoRepository.removerListener(offsetListener)
        jogoCompetitivoRepository.removerListener(podioListener)
    }

    // Função que configura os listeners dos botões de opções
    private fun configurarBotoes() {
        binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
        binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
        binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
        binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
    }

    // Função que a pergunta atual e redefine o cronómetro
    private fun mostrarPergunta() {
        if (perguntaAtualIndex >= perguntas.size) {
            finalizarJogo()
            return
        }
        handler.removeCallbacksAndMessages(null)
        perguntaAtual = perguntas[perguntaAtualIndex]
        binding.txtProgresso.text = "Pergunta ${perguntaAtualIndex + 1}/${perguntas.size}"
        binding.txtPergunta.text = perguntaAtual.pergunta

        // Obter opções aleatórias para a pergunta atual
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

        tempoRestante = 15.0
        sincronizarInicioPergunta {
            tempoIniciado = it
            iniciarCronometro(it)
        }
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

        // Desativar botões após resposta
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

        // Define a cor do botão correto
        definirCorBotao(botaoCorreto!!, "#81C784")

        // Atualiza estatísticas de respostas
        totalPerguntasRespondidas++
        if (botaoSelecionado != null && opcaoEscolhida == perguntaAtual.respostaCorreta) {
            // Chamar a função para tocar som de resposta correta
            tocarSom(this, R.raw.certo)
            somTocar = true
            definirCorBotao(botaoSelecionado, "#81C784")

            // Guardar o numero de respostas certas em squencia
            numeroPerguntasCertas++
            // Guardar o total de perguntas certas ao longo do jogo
            totalPerguntascertas++
            val resultadoPontuacao = scoreCompetitivoService.calcularPontuacao(
                tempoRestante,
                numeroPerguntasCertas,
                bonus
            )
            if (resultadoPontuacao.bonusAplicado > 0) {
                Toast.makeText(
                    this,
                    "Bónus de sequência! +${resultadoPontuacao.bonusAplicado} pontos",
                    Toast.LENGTH_SHORT
                ).show()
            }
            totalPontos += resultadoPontuacao.pontos
        } else if (botaoSelecionado != null) {
            // Chamar a função para tocar som de resposta errada
            tocarSom(this, R.raw.errado)
            somTocar = true

            definirCorBotao(botaoSelecionado, "#E57373") // Vermelho para errada
            numeroPerguntasCertas = 0
        }

        // Guarda resposta do jogador na base de dados
        jogoCompetitivoRepository.guardarResposta2x2(codigoSala, nomeUtilizador, perguntaAtualIndex, opcaoEscolhida)

        // Delay para mostrar feedback antes da próxima pergunta
        val tempoAteProxima = ((tempoIniciado + 15000) - tempoServidorAtual()).coerceAtLeast(0)
        handler.postDelayed({
            perguntaAtualIndex++
            mostrarPergunta()
        }, tempoAteProxima + 1200)
    }

    // Função para finalizar o jogo e guardar pontuação
    private fun finalizarJogo() {
        pararSom()
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)

        // Guarda pontuação do jogador no nó da sua equipa
        if (equipaDoJogador == "A" || equipaDoJogador == "B") {
            jogoCompetitivoRepository.guardarResultado2x2(
                codigoSala,
                equipaDoJogador,
                nomeUtilizador,
                totalPontos,
                totalPerguntascertas
            )
        }

        // Aguarda até todas as pontuações estarem guardadas!
        aguardarPodioCompleto()
    }

    private fun aguardarPodioCompleto() {
        podioListener = jogoCompetitivoRepository.escutarPodio2x2(
            codigoSala,
            onPodioCompleto = {
                // Avança para a activity do pódio
                enviarPontuacaoActivity(
                    this@Jogo2x2Activity,
                    codigoSala,
                    "2x2",
                    nomeUtilizador,
                    totalPontos,
                    categoria,
                    nomeUtilizador,
                    totalPerguntascertas,
                    numeroPerguntasCertas,
                    perguntas.size,
                    equipaDoJogador
                )
                finish()
            },
            onAguardar = {
                Toast.makeText(
                    this@Jogo2x2Activity,
                    "Aguarde que todos terminem!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        ) {
                Toast.makeText(this@Jogo2x2Activity, "Erro ao verificar pódio!", Toast.LENGTH_SHORT).show()
        }
    }

    // Função que inicia o cronómetro visual e sonoro da pergunta de forma sincronizada
    private fun iniciarCronometro(horaInicioSincronizada: Long) {
        tempoDecorrido = true
        progressBarAtivo = true
        val tempoTotal = 15.0
        binding.pbTempo.max = tempoTotal.toInt()
        binding.pbTempo.progress = tempoTotal.toInt()
        var primeiraAtualizacao = true

        val horaInicio = horaInicioSincronizada
                    val runnable = object : Runnable {
                        override fun run() {
                            val tempoAtual = tempoServidorAtual()
                            val tempoDecorridoSegundos = (tempoAtual - horaInicio) / 1000.0
                            tempoRestante = tempoTotal - tempoDecorridoSegundos
                            if (tempoRestante < 0) tempoRestante = 0.0

                            // Toca som nos últimos 5 segundos
                            if (tempoRestante <= 5 && tempoRestante > 0 && !somTocar) {
                                mediaPlayer = MediaPlayer.create(this@Jogo2x2Activity, R.raw.som)
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

    private fun carregarOffsetServidor() {
        offsetListener = jogoCompetitivoRepository.escutarOffsetServidor(
            onOffsetAlterado = { offset ->
                serverTimeOffset = offset
            }
        )
    }

    private fun tempoServidorAtual(): Long = System.currentTimeMillis() + serverTimeOffset

    private fun mensagemErroPerguntas(erro: Exception): String {
        return if (erro.message?.contains("buscar perguntas", ignoreCase = true) == true) {
            "Erro ao buscar perguntas!"
        } else {
            "Erro ao carregar perguntas"
        }
    }

    private fun sincronizarInicioPergunta(onReady: (Long) -> Unit) {
        jogoCompetitivoRepository.sincronizarInicioPergunta(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            perguntaAtualIndex,
            tempoServidorAtual()
        ).addOnSuccessListener { inicio ->
            onReady(inicio)
        }.addOnFailureListener {
            onReady(tempoServidorAtual())
        }
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
