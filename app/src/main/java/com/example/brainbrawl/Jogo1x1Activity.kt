package com.example.brainbrawl

import android.icu.text.DecimalFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.UteisJogo.definirCorBotao
import com.example.brainbrawl.UteisJogo.tocarSom
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityJogo1x1Binding
import com.example.brainbrawl.models.Pergunta
import com.example.brainbrawl.routes.UteisNavegacao.enviarPontuacaoActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.UteisPerguntas.obterOpcoesAleatorias
import com.example.brainbrawl.viewmodels.Jogo1x1Event
import com.example.brainbrawl.viewmodels.Jogo1x1ViewModel
import com.example.brainbrawl.viewmodels.JogoCompetitivoPerguntaUiState
import com.example.brainbrawl.viewmodels.JogoCompetitivoPontuacaoDados

class Jogo1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityJogo1x1Binding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[Jogo1x1ViewModel::class.java]
    }

    private lateinit var codigoSala: String
    private lateinit var uid: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeJogador: String
    private lateinit var perguntaAtual: Pergunta
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var avatar: String = ""

    private var mediaPlayer: MediaPlayer? = null
    private var somTocar = false
    private var tempoRestante = 15.0
    private var tempoDecorrido = false
    private var progressBarAtivo = false
    private var tempoIniciado: Long = 0
    private var opcoesAtuais: List<String> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private val formatoDecimal = DecimalFormat("#.#")
    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        avatar = intent.getStringExtra(IntentExtras.AVATAR) ?: ""

        configurarObservers()
        configurarBotoes()
        viewModel.iniciar(
            codigoSala = codigoSala,
            uid = uid,
            nomeUtilizador = nomeUtilizador,
            nomeJogador = nomeJogador,
            playerKey = playerKey,
            tipoJogador = tipoJogador,
            avatar = avatar,
            categoriaPadrao = "Todas as categorias",
            categoriaTodas = getString(R.string.categoria5)
        )
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        viewModel.removerListeners()
        super.onDestroy()
    }

    private fun configurarObservers() {
        viewModel.pergunta.observe(this) { estado ->
            mostrarPergunta(estado)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun configurarBotoes() {
        binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
        binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
        binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
        binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
    }

    private fun mostrarPergunta(estado: JogoCompetitivoPerguntaUiState) {
        handler.removeCallbacksAndMessages(null)
        perguntaAtual = estado.pergunta
        binding.txtProgresso.text = getString(R.string.progresso_pergunta, estado.indice + 1, estado.totalPerguntas)
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
    }

    private fun verificarResposta(numeroOpcao: Int) {
        if (somTocar) {
            pararSom()
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

        val resultado = viewModel.responder(
            numeroOpcao,
            opcaoEscolhida,
            perguntaAtual.respostaCorreta,
            tempoRestante
        )
        if (resultado.acertou && botaoSelecionado != null) {
            tocarSom(this, R.raw.certo)
            somTocar = true
            definirCorBotao(botaoSelecionado, "#81C784")
            if (resultado.bonusAplicado > 0) {
                Toast.makeText(
                    this,
                    getString(R.string.bonus_sequencia_format, resultado.bonusAplicado),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (botaoSelecionado != null) {
            tocarSom(this, R.raw.errado)
            somTocar = true
            definirCorBotao(botaoSelecionado, "#E57373")
        }

        val tempoAteProxima = ((tempoIniciado + 15000) - viewModel.tempoServidorAtual()).coerceAtLeast(0)
        handler.postDelayed({
            viewModel.avancarPergunta()
        }, tempoAteProxima + 1200)
    }

    private fun finalizarJogo() {
        pararSom()
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        viewModel.finalizarJogo()
    }

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
                val tempoAtual = viewModel.tempoServidorAtual()
                val tempoDecorridoSegundos = (tempoAtual - horaInicio) / 1000.0
                tempoRestante = tempoTotal - tempoDecorridoSegundos
                if (tempoRestante < 0) tempoRestante = 0.0

                if (tempoRestante <= 5 && tempoRestante > 0 && !somTocar) {
                    mediaPlayer = MediaPlayer.create(this@Jogo1x1Activity, R.raw.som)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    somTocar = true
                } else if ((tempoRestante > 5 || tempoRestante <= 0) && somTocar) {
                    pararSom()
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

    private fun tratarEvento(evento: Jogo1x1Event) {
        when (evento) {
            is Jogo1x1Event.IniciarCronometro -> {
                tempoIniciado = evento.horaInicio
                iniciarCronometro(evento.horaInicio)
            }
            Jogo1x1Event.ErroLerCategoria -> {
                Toast.makeText(this, getString(R.string.erro_ler_categoria), Toast.LENGTH_SHORT).show()
                finish()
            }
            is Jogo1x1Event.ErroPerguntas -> {
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
                finish()
            }
            Jogo1x1Event.ErroGuardarPontuacao -> {
                Toast.makeText(this, getString(R.string.erro_guardar_pontuacao), Toast.LENGTH_SHORT).show()
                finish()
            }
            Jogo1x1Event.AguardarAdversario -> {
                Toast.makeText(this@Jogo1x1Activity, getString(R.string.aguarde_adversario_terminar), Toast.LENGTH_SHORT).show()
            }
            Jogo1x1Event.ErroPodio -> {
                Toast.makeText(this@Jogo1x1Activity, getString(R.string.erro_verificar_podio), Toast.LENGTH_SHORT).show()
            }
            Jogo1x1Event.FinalizarJogo -> finalizarJogo()
            is Jogo1x1Event.AbrirPontuacoes -> abrirPontuacoes(evento.dados)
        }
    }

    private fun abrirPontuacoes(dados: JogoCompetitivoPontuacaoDados) {
        viewModel.removerListeners()
        enviarPontuacaoActivity(
            this@Jogo1x1Activity,
            dados.codigoSala,
            dados.modoJogo,
            dados.nomeUtilizador,
            dados.totalPontos,
            dados.categoria,
            dados.nomeJogador,
            dados.totalPerguntasCertas,
            dados.numeroPerguntasCertas,
            dados.totalPerguntas,
            uid = dados.uid,
            playerKey = dados.playerKey,
            tipoJogador = dados.tipoJogador,
            avatar = dados.avatar
        )
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
