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
import com.example.brainbrawl.utils.UteisPerguntas.obterOpcoesAleatorias
import com.example.brainbrawl.UteisJogo.tocarSom
import com.example.brainbrawl.routes.UteisNavegacao.enviarPontuacaoActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityJogo1x1Binding
import com.example.brainbrawl.repositories.JogoCompetitivoRepository
import com.example.brainbrawl.repositories.JogoCompetitivoRepository.ModoCompetitivo
import com.example.brainbrawl.services.ScoreCompetitivoService

class Jogo1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityJogo1x1Binding.inflate(layoutInflater)
    }
    private val jogoCompetitivoRepository = JogoCompetitivoRepository()
    private val scoreCompetitivoService = ScoreCompetitivoService()
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
    private val perguntas = mutableListOf<Pergunta>()
    private lateinit var categoria: String
    private var serverTimeOffset: Long = 0L
    private var offsetListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var podioListener: JogoCompetitivoRepository.ListenerHandle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pelo Intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        carregarOffsetServidor()

        // Lê a categoria REAL da sala do Firebase para garantir filtragem correta
        jogoCompetitivoRepository.carregarNomeCategoria(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            "Todas as categorias"
        ).addOnSuccessListener { nomeCategoria ->
            categoria = nomeCategoria
            prepararPerguntasEJogo()
        }.addOnFailureListener {
            Toast.makeText(this@Jogo1x1Activity, "Erro ao ler categoria!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun prepararPerguntasEJogo() {
        // Buscar ou criar perguntas
        jogoCompetitivoRepository.carregarOuCriarPerguntas(
            ModoCompetitivo.UM_CONTRA_UM,
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
            Toast.makeText(this@Jogo1x1Activity, mensagemErroPerguntas(it), Toast.LENGTH_SHORT).show()
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

            // Vermelho para a resposta errada
            definirCorBotao(botaoSelecionado, "#E57373")
            numeroPerguntasCertas = 0
        }

        // Delay para mostrar feedback antes da próxima pergunta
        val tempoAteProxima = ((tempoIniciado + 15000) - tempoServidorAtual()).coerceAtLeast(0)
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
        jogoCompetitivoRepository.guardarPontuacao1x1(codigoSala, nomeUtilizador, totalPontos)
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
        podioListener = jogoCompetitivoRepository.escutarPodio1x1(
            codigoSala,
            onPodioCompleto = {
                // Avança para o pódio (agora sim, ambos podem ver!)
                enviarPontuacaoActivity(
                    this@Jogo1x1Activity, codigoSala, GameConstants.MODO_1X1, nomeUtilizador, totalPontos, categoria, nomeUtilizador, totalPerguntascertas, numeroPerguntasCertas, perguntas.size
                )
            },
            onAguardar = {
                // Mostra mensagem de espera (podes melhorar para mostrar loading, etc)
                Toast.makeText(this@Jogo1x1Activity, "Aguarde que o adversário termine!", Toast.LENGTH_SHORT).show()
            }
        ) {
                Toast.makeText(this@Jogo1x1Activity, "Erro ao verificar pódio!", Toast.LENGTH_SHORT).show()
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
            ModoCompetitivo.UM_CONTRA_UM,
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
