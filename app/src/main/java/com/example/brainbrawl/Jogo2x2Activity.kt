package com.example.brainbrawl

import android.icu.text.DecimalFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.UteisJogo.definirCorBotao
import com.example.brainbrawl.UteisJogo.tocarSom
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityJogo2x2Binding
import com.example.brainbrawl.models.Pergunta
import com.example.brainbrawl.routes.UteisNavegacao.enviarPontuacaoActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.utils.UteisPerguntas.obterOpcoesAleatorias
import com.example.brainbrawl.viewmodels.EquipaCompetitivaUi
import com.example.brainbrawl.viewmodels.Jogo2x2Event
import com.example.brainbrawl.viewmodels.Jogo2x2ViewModel
import com.example.brainbrawl.viewmodels.JogoCompetitivoPerguntaUiState
import com.example.brainbrawl.viewmodels.JogoCompetitivoPontuacaoDados

class Jogo2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityJogo2x2Binding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[Jogo2x2ViewModel::class.java]
    }

    private lateinit var codigoSala: String
    private lateinit var uid: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeJogador: String
    private lateinit var perguntaAtual: Pergunta
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var avatar: String = ""
    private var origemSala: String = ""
    private var tempoTotalPergunta = GameConstants.COMPETITIVE_DEFAULT_QUESTION_TIME_SECONDS
    private var tempoTotalPerguntaMs = GameConstants.COMPETITIVE_DEFAULT_QUESTION_TIME_MS

    private var mediaPlayer: MediaPlayer? = null
    private var somTocar = false
    private var tempoRestante = GameConstants.COMPETITIVE_DEFAULT_QUESTION_TIME_SECONDS
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
        origemSala = intent.getStringExtra(IntentExtras.ORIGEM_SALA).orEmpty()
        if (origemSala == GameConstants.ORIGEM_MATCHMAKING) {
            tempoTotalPergunta = GameConstants.MATCHMAKING_QUESTION_TIME_SECONDS
            tempoTotalPerguntaMs = GameConstants.MATCHMAKING_QUESTION_TIME_MS
        }

        binding.txtCategoriaJogo.text = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)
            ?: getString(R.string.categoria5)
        Log.d(
            START_TAG,
            "mode=2x2 room=$codigoSala gameCreate uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()} " +
                "categoryIntent=${intent.getStringExtra(IntentExtras.NOME_CATEGORIA).orEmpty().ifBlank { "<empty>" }} " +
                "origin=${origemSala.ifBlank { "<empty>" }}"
        )
        configurarObservers()
        configurarBotoes()
        configurarBackBloqueado()
        viewModel.iniciar(
            codigoSala = codigoSala,
            uid = uid,
            nomeUtilizador = nomeUtilizador,
            nomeJogador = nomeJogador,
            playerKey = playerKey,
            tipoJogador = tipoJogador,
            avatar = avatar,
            origemSala = origemSala,
            categoriaPadrao = getString(R.string.categoria5),
            categoriaTodas = getString(R.string.categoria5),
            tempoTotalPergunta = tempoTotalPergunta
        )
    }

    private fun configurarBackBloqueado() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@Jogo2x2Activity, R.string.voltar_bloqueado_jogo, Toast.LENGTH_SHORT).show()
            }
        })
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
        viewModel.placar.observe(this) { estado ->
            atualizarPlacarEquipa(
                equipa = estado.equipaA,
                nome = getString(R.string.equipa_lusa),
                nomeView = binding.txtNomeEquipaA,
                pontosView = binding.txtPontosEquipaA,
                avatar1 = binding.imgEquipaA1,
                avatar2 = binding.imgEquipaA2
            )
            atualizarPlacarEquipa(
                equipa = estado.equipaB,
                nome = getString(R.string.os_descobridores),
                nomeView = binding.txtNomeEquipaB,
                pontosView = binding.txtPontosEquipaB,
                avatar1 = binding.imgEquipaB1,
                avatar2 = binding.imgEquipaB2
            )
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun atualizarPlacarEquipa(
        equipa: EquipaCompetitivaUi,
        nome: String,
        nomeView: android.widget.TextView,
        pontosView: android.widget.TextView,
        avatar1: ImageView,
        avatar2: ImageView
    ) {
        nomeView.text = nome
        pontosView.text = formatarPontos(equipa.pontuacao)
        avatar1.setImageResource(AvatarUtils.resolverAvatar(this, equipa.jogadores.getOrNull(0)?.avatar))
        avatar2.setImageResource(AvatarUtils.resolverAvatar(this, equipa.jogadores.getOrNull(1)?.avatar))
        avatar2.alpha = if (equipa.jogadores.getOrNull(1) == null) 0.42f else 1f
    }

    private fun configurarBotoes() {
        binding.questionAnswers.btnOpcao1.setOnClickListener { verificarResposta(0) }
        binding.questionAnswers.btnOpcao2.setOnClickListener { verificarResposta(1) }
        binding.questionAnswers.btnOpcao3.setOnClickListener { verificarResposta(2) }
        binding.questionAnswers.btnOpcao4.setOnClickListener { verificarResposta(3) }
    }

    private fun mostrarPergunta(estado: JogoCompetitivoPerguntaUiState) {
        handler.removeCallbacksAndMessages(null)
        perguntaAtual = estado.pergunta
        binding.txtProgresso.text = getString(R.string.progresso_pergunta, estado.indice + 1, estado.totalPerguntas)
        if (estado.categoria.isNotBlank()) {
            binding.txtCategoriaJogo.text = estado.categoria
        }
        Log.d(
            GAME_CATEGORY_TAG,
            "mode=2x2 room=$codigoSala uid=${uid.maskedLogId()} " +
                "categoryIntent=${intent.getStringExtra(IntentExtras.NOME_CATEGORIA).orEmpty().ifBlank { "<empty>" }} " +
                "categoryDisplayed=${binding.txtCategoriaJogo.text} questionIndex=${estado.indice + 1}/${estado.totalPerguntas}"
        )
        binding.questionAnswers.txtPergunta.text = perguntaAtual.pergunta

        opcoesAtuais = obterOpcoesAleatorias(perguntaAtual)
        binding.questionAnswers.btnOpcao1.text = opcoesAtuais[0]
        binding.questionAnswers.btnOpcao2.text = opcoesAtuais[1]
        binding.questionAnswers.btnOpcao3.text = opcoesAtuais[2]
        binding.questionAnswers.btnOpcao4.text = opcoesAtuais[3]

        definirCorBotao(binding.questionAnswers.btnOpcao1, "#FFFDF7")
        definirCorBotao(binding.questionAnswers.btnOpcao2, "#FFFDF7")
        definirCorBotao(binding.questionAnswers.btnOpcao3, "#FFFDF7")
        definirCorBotao(binding.questionAnswers.btnOpcao4, "#FFFDF7")

        tempoRestante = tempoTotalPergunta
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

        binding.questionAnswers.btnOpcao1.isEnabled = false
        binding.questionAnswers.btnOpcao2.isEnabled = false
        binding.questionAnswers.btnOpcao3.isEnabled = false
        binding.questionAnswers.btnOpcao4.isEnabled = false

        val botaoSelecionado = when (numeroOpcao) {
            0 -> binding.questionAnswers.btnOpcao1
            1 -> binding.questionAnswers.btnOpcao2
            2 -> binding.questionAnswers.btnOpcao3
            3 -> binding.questionAnswers.btnOpcao4
            else -> null
        }
        val opcaoEscolhida = if (numeroOpcao in 0..3) opcoesAtuais[numeroOpcao] else ""
        val indiceCorreto = opcoesAtuais.indexOf(perguntaAtual.respostaCorreta)
        val botaoCorreto = when (indiceCorreto) {
            0 -> binding.questionAnswers.btnOpcao1
            1 -> binding.questionAnswers.btnOpcao2
            2 -> binding.questionAnswers.btnOpcao3
            3 -> binding.questionAnswers.btnOpcao4
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

        val tempoAteProxima = ((tempoIniciado + tempoTotalPerguntaMs) - viewModel.tempoServidorAtual()).coerceAtLeast(0)
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
        val tempoTotal = tempoTotalPergunta
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
                    mediaPlayer = MediaPlayer.create(this@Jogo2x2Activity, R.raw.som)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    somTocar = true
                } else if ((tempoRestante > 5 || tempoRestante <= 0) && somTocar) {
                    pararSom()
                }

                if (primeiraAtualizacao) {
                    binding.questionAnswers.btnOpcao1.isEnabled = true
                    binding.questionAnswers.btnOpcao2.isEnabled = true
                    binding.questionAnswers.btnOpcao3.isEnabled = true
                    binding.questionAnswers.btnOpcao4.isEnabled = true
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

    private fun tratarEvento(evento: Jogo2x2Event) {
        when (evento) {
            is Jogo2x2Event.IniciarCronometro -> {
                tempoIniciado = evento.horaInicio
                iniciarCronometro(evento.horaInicio)
            }
            Jogo2x2Event.ErroLerCategoria -> {
                Log.w(START_TAG, "mode=2x2 room=$codigoSala finishReason=category_error uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()}")
                Toast.makeText(this, getString(R.string.erro_ler_categoria), Toast.LENGTH_SHORT).show()
                finish()
            }
            Jogo2x2Event.ErroCarregarEquipa -> {
                Log.w(START_TAG, "mode=2x2 room=$codigoSala finishReason=team_load_error uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()}")
                Toast.makeText(this, getString(R.string.erro_carregar_equipa), Toast.LENGTH_SHORT).show()
                finish()
            }
            is Jogo2x2Event.ErroPerguntas -> {
                Log.w(START_TAG, "mode=2x2 room=$codigoSala finishReason=questions_error uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()}")
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
                finish()
            }
            Jogo2x2Event.AguardarJogadores -> {
                Toast.makeText(this@Jogo2x2Activity, getString(R.string.aguarde_todos_terminarem), Toast.LENGTH_SHORT).show()
            }
            Jogo2x2Event.ErroPodio -> {
                Toast.makeText(this@Jogo2x2Activity, getString(R.string.erro_verificar_podio), Toast.LENGTH_SHORT).show()
            }
            Jogo2x2Event.FinalizarJogo -> finalizarJogo()
            is Jogo2x2Event.AbrirPontuacoes -> abrirPontuacoes(evento.dados)
        }
    }

    private fun abrirPontuacoes(dados: JogoCompetitivoPontuacaoDados) {
        viewModel.removerListeners()
        enviarPontuacaoActivity(
            this@Jogo2x2Activity,
            dados.codigoSala,
            dados.modoJogo,
            dados.nomeUtilizador,
            dados.totalPontos,
            dados.categoria,
            dados.nomeJogador,
            dados.totalPerguntasCertas,
            dados.numeroPerguntasCertas,
            dados.totalPerguntas,
            dados.equipa,
            uid = dados.uid,
            playerKey = dados.playerKey,
            tipoJogador = dados.tipoJogador,
            avatar = dados.avatar,
            categoriaCompetitiva = dados.categoriaCompetitiva,
            origemSala = dados.origemSala
        )
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

    private fun formatarPontos(valor: Double): String {
        return DecimalFormat("#,###").format(valor)
    }

    private companion object {
        const val START_TAG = "GameStart"
        const val GAME_CATEGORY_TAG = "GameCategory"
    }
}

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
