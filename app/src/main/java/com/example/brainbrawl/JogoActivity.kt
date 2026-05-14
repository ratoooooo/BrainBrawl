package com.example.brainbrawl

import android.content.Intent
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
import com.example.brainbrawl.databinding.ActivityJogoBinding
import com.example.brainbrawl.models.Pergunta
import com.example.brainbrawl.routes.UteisNavegacao.adicionarDadosJogador
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.UteisPerguntas.obterOpcoesAleatorias
import com.example.brainbrawl.viewmodels.JogoEvent
import com.example.brainbrawl.viewmodels.JogoPerguntaUiState
import com.example.brainbrawl.viewmodels.JogoResultadoDados
import com.example.brainbrawl.viewmodels.JogoSalaUiState
import com.example.brainbrawl.viewmodels.JogoViewModel

class JogoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityJogoBinding.inflate(layoutInflater) }
    private val viewModel by lazy {
        ViewModelProvider(this)[JogoViewModel::class.java]
    }

    private lateinit var codigoSala: String
    private lateinit var uid: String
    private lateinit var nomeJogador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeUtilizador: String
    private lateinit var perguntaAtual: Pergunta

    private var perguntaAtualIndex = 0
    private var tempoRestante = 20.0
    private var tempoDecorrido = false
    private var modoJogo: String? = null
    private var opcoesAtuais: List<String> = emptyList()
    private var admin = false
    private var adminPrimeiraPergunta = true
    private var progressBarAtivo = false
    private var somTocar = false
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val formatoDecimal = DecimalFormat("#.#")
    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: "Jogador"
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""

        configurarObservers()
        viewModel.iniciar(codigoSala, uid, nomeUtilizador, nomeJogador, nomeCategoria)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        pararSom()
        viewModel.removerListeners()
        super.onDestroy()
    }

    private fun configurarObservers() {
        viewModel.sala.observe(this) { estado ->
            atualizarInfoSala(estado)
        }
        viewModel.pergunta.observe(this) { estado ->
            mostrarPergunta(estado)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun atualizarInfoSala(estado: JogoSalaUiState) {
        admin = estado.admin
        modoJogo = estado.modoJogo
        binding.txtAdmin.apply {
            if (admin) {
                text = getString(R.string.admin_label)
                visibility = android.view.View.VISIBLE
            } else {
                visibility = android.view.View.GONE
            }
        }
    }

    private fun mostrarPergunta(estado: JogoPerguntaUiState) {
        handler.removeCallbacksAndMessages(null)
        pararSom()

        perguntaAtual = estado.pergunta
        perguntaAtualIndex = estado.indice
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

        desbloquearOpcoes()
        tempoDecorrido = true
        progressBarAtivo = true

        if (estado.admin) {
            bloquearAdminSempre()
            if (adminPrimeiraPergunta) {
                Toast.makeText(this, getString(R.string.admin_observar_respostas), Toast.LENGTH_SHORT).show()
                adminPrimeiraPergunta = false
            }
        } else {
            binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
            binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
            binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
            binding.btnOpcao4.setOnClickListener { verificarResposta(3) }
        }
    }

    private fun bloquearAdminSempre() {
        if (admin) {
            bloquearOpcoes()
        }
    }

    private fun bloquearOpcoes() {
        binding.btnOpcao1.isEnabled = false
        binding.btnOpcao2.isEnabled = false
        binding.btnOpcao3.isEnabled = false
        binding.btnOpcao4.isEnabled = false
        binding.btnOpcao1.setOnClickListener(null)
        binding.btnOpcao2.setOnClickListener(null)
        binding.btnOpcao3.setOnClickListener(null)
        binding.btnOpcao4.setOnClickListener(null)
    }

    private fun desbloquearOpcoes() {
        binding.btnOpcao1.isEnabled = true
        binding.btnOpcao2.isEnabled = true
        binding.btnOpcao3.isEnabled = true
        binding.btnOpcao4.isEnabled = true
    }

    private fun verificarResposta(numeroOpcao: Int) {
        if (admin) return

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
        val resultado = viewModel.enviarResposta(
            numeroOpcao,
            opcaoEscolhida,
            perguntaAtual.respostaCorreta,
            tempoRestante
        ) ?: return

        bloquearOpcoes()

        val indiceCorreto = opcoesAtuais.indexOf(perguntaAtual.respostaCorreta)
        val botaoCorreto = when (indiceCorreto) {
            0 -> binding.btnOpcao1
            1 -> binding.btnOpcao2
            2 -> binding.btnOpcao3
            3 -> binding.btnOpcao4
            else -> null
        }
        definirCorBotao(botaoCorreto!!, "#81C784")

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

        if (resultado.deveEliminar) {
            handler.postDelayed({
                tempoDecorrido = false
                progressBarAtivo = false
                handler.removeCallbacksAndMessages(null)
                pararSom()
                viewModel.eliminarJogador()
            }, 1200)
        }
    }

    private fun iniciarCronometroSincronizado(horaInicio: Long) {
        tempoDecorrido = true
        progressBarAtivo = true
        val tempoTotal = viewModel.tempoTotal()
        binding.pbTempo.max = tempoTotal.toInt()
        binding.pbTempo.progress = tempoTotal.toInt()

        val runnable = object : Runnable {
            override fun run() {
                val tempoAtual = viewModel.tempoServidorAtual()
                val tempoDecorridoSegundos = (tempoAtual - horaInicio) / 1000.0
                tempoRestante = tempoTotal - tempoDecorridoSegundos
                if (tempoRestante < 0) tempoRestante = 0.0

                if (tempoRestante <= 5 && tempoRestante > 0 && !somTocar) {
                    pararSom()
                    mediaPlayer = MediaPlayer.create(this@JogoActivity, R.raw.som)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    somTocar = true
                } else if ((tempoRestante > 5 || tempoRestante <= 0) && somTocar) {
                    pararSom()
                }

                if (tempoDecorrido) {
                    binding.txtCronometro.text = formatoDecimal.format(tempoRestante.coerceAtLeast(0.0))
                }
                if (progressBarAtivo) {
                    binding.pbTempo.progress = tempoRestante.coerceAtLeast(0.0).toInt()
                    if (tempoRestante <= 0) {
                        binding.pbTempo.progress = 0
                        progressBarAtivo = false
                        if (!viewModel.jaRespondeuPergunta()) {
                            verificarResposta(-1)
                        }
                        bloquearOpcoes()
                        handler.postDelayed({}, 3000)
                    }
                }

                if (tempoRestante > 0 && progressBarAtivo) {
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.post(runnable)
    }

    private fun iniciarCronometroAdmin(horaInicio: Long) {
        tempoDecorrido = true
        progressBarAtivo = true
        val tempoTotal = viewModel.tempoTotal()
        binding.pbTempo.max = tempoTotal.toInt()
        binding.pbTempo.progress = tempoTotal.toInt()

        val runnable = object : Runnable {
            override fun run() {
                val tempoAtual = viewModel.tempoServidorAtual()
                val tempoDecorridoSegundos = (tempoAtual - horaInicio) / 1000.0
                tempoRestante = tempoTotal - tempoDecorridoSegundos
                if (tempoRestante < 0) tempoRestante = 0.0

                if (tempoRestante <= 5 && tempoRestante > 0 && !somTocar) {
                    pararSom()
                    mediaPlayer = MediaPlayer.create(this@JogoActivity, R.raw.som)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    somTocar = true
                } else if ((tempoRestante > 5 || tempoRestante <= 0) && somTocar) {
                    pararSom()
                }

                if (tempoDecorrido) {
                    binding.txtCronometro.text = formatoDecimal.format(tempoRestante.coerceAtLeast(0.0))
                }
                if (progressBarAtivo) {
                    binding.pbTempo.progress = tempoRestante.coerceAtLeast(0.0).toInt()
                    if (tempoRestante <= 0) {
                        binding.pbTempo.progress = 0
                        progressBarAtivo = false
                        handler.postDelayed({
                            viewModel.adminTempoTerminou()
                        }, 3000)
                    }
                }

                if (tempoRestante > 0 && progressBarAtivo) {
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.post(runnable)
    }

    private fun finalizarJogo() {
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        pararSom()
        viewModel.finalizarJogo()
    }

    private fun tratarEvento(evento: JogoEvent) {
        when (evento) {
            is JogoEvent.IniciarCronometro -> {
                if (evento.admin) {
                    iniciarCronometroAdmin(evento.horaInicio)
                } else {
                    iniciarCronometroSincronizado(evento.horaInicio)
                }
            }
            is JogoEvent.ErroCarregarSala -> {
                Toast.makeText(this, getString(R.string.erro_carregar_sala_format, evento.mensagem), Toast.LENGTH_SHORT).show()
                finish()
            }
            is JogoEvent.ErroCarregarPerguntas -> {
                Toast.makeText(this, getString(R.string.erro_carregar_perguntas_format, evento.mensagem), Toast.LENGTH_SHORT).show()
                finalizarJogo()
            }
            is JogoEvent.ErroEstadoSala -> {
                Toast.makeText(this, getString(R.string.erro_estado_sala_format, evento.mensagem), Toast.LENGTH_SHORT).show()
                abrirMainAposErro()
            }
            JogoEvent.ErroVerificarJogadores -> {
                Toast.makeText(this, getString(R.string.erro_verificar_jogadores), Toast.LENGTH_SHORT).show()
                abrirMainAposErro()
            }
            JogoEvent.ErroEliminarJogador -> {
                Toast.makeText(this, getString(R.string.erro_eliminar_jogador), Toast.LENGTH_LONG).show()
            }
            JogoEvent.FinalizarJogo -> finalizarJogo()
            JogoEvent.MensagemFimEliminatorias -> {
                Toast.makeText(this, getString(R.string.jogo_terminado_um_jogador), Toast.LENGTH_LONG).show()
            }
            is JogoEvent.AbrirEsperaEliminado -> {
                Toast.makeText(this, getString(R.string.jogador_eliminado), Toast.LENGTH_LONG).show()
                abrirEsperaEliminadoActivity(evento.dados)
            }
            is JogoEvent.AbrirPontuacoes -> enviarPontuacaoActivity(evento.dados)
        }
    }

    private fun abrirMainAposErro() {
        val intent = Intent(this@JogoActivity, MainActivity::class.java)
        adicionarDadosJogador(intent, nomeUtilizador.ifBlank { null }, nomeJogador, uid.ifBlank { null })
        startActivity(intent)
        finish()
    }

    private fun abrirEsperaEliminadoActivity(dados: JogoResultadoDados) {
        viewModel.removerListeners()
        val intent = Intent(this, EsperaEliminadoActivity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, dados.codigoSala)
        intent.putExtra(IntentExtras.UID, dados.uid)
        intent.putExtra(IntentExtras.NOME_JOGADOR, dados.nomeJogador)
        intent.putExtra(IntentExtras.TOTAL_PONTOS, dados.totalPontos)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, dados.nomeCategoria)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, dados.nomeUtilizador)
        intent.putExtra(IntentExtras.MODO_JOGO, dados.modoJogo)
        intent.putExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, dados.numeroPerguntasCertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, dados.totalPerguntasCertas)
        intent.putExtra(IntentExtras.RESPOSTAS_CERTAS, dados.totalPerguntasCertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS, dados.totalPerguntas)
        startActivity(intent)
        finish()
    }

    private fun enviarPontuacaoActivity(dados: JogoResultadoDados) {
        val intent = Intent(this, PontuacoesActivity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, dados.codigoSala)
        intent.putExtra(IntentExtras.UID, dados.uid)
        intent.putExtra(IntentExtras.NOME_JOGADOR, dados.nomeJogador)
        intent.putExtra(IntentExtras.TOTAL_PONTOS, dados.totalPontos)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, dados.nomeCategoria)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, dados.nomeUtilizador)
        intent.putExtra(IntentExtras.MODO_JOGO, dados.modoJogo)
        intent.putExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, dados.numeroPerguntasCertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, dados.totalPerguntasCertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS, dados.totalPerguntas)
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
