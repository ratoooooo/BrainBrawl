package com.example.brainbrawl

import com.example.brainbrawl.models.Pergunta
import android.content.Intent
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
import com.example.brainbrawl.routes.UteisNavegacao.adicionarDadosJogador
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityJogoBinding
import com.example.brainbrawl.repositories.JogoRepository
import com.example.brainbrawl.services.GameService
import com.example.brainbrawl.services.ScoreService

class JogoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityJogoBinding.inflate(layoutInflater) }

    // Variáveis para guardar informações da sala e do jogador
    private lateinit var codigoSala: String
    private lateinit var nomeJogador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeUtilizador: String
    private lateinit var perguntaAtual: Pergunta

    // Variáveis de estado do jogo
    private var perguntaAtualIndex = 0
    private var totalPontos = 0.0
    private var numeroPerguntasCertas = 0
    private var totalPerguntascertas = 0
    private var bonus = 50

    // Variáveis relacionadas com o tempo
    private var tempoRestante = 20.0
    private var tempoIniciado: Long = 0
    private var tempoDecorrido = false

    // Variáveis de controlo do jogo
    private var modoJogo: String? = null
    private var opcoesAtuais: List<String> = emptyList()
    private var admin = false
    private var adminPrimeiraPergunta = true // Para mostrar a mensagem de admin apenas uma vez
    private var jaRespondeu = false
    private var acertouUltimaPergunta = false

    // Variáveis de controlo da UI e som
    private var progressBarAtivo = false
    private var somTocar = false
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val formatoDecimal = DecimalFormat("#.#")
    private val perguntas = mutableListOf<Pergunta>()
    private val jogoRepository = JogoRepository()
    private val gameService = GameService()
    private val scoreService = ScoreService()
    private var serverTimeOffset: Long = 0L

    // Listeners do Firebase para serem removidos quando a atividade é destruída
    private var perguntaIndexListener: JogoRepository.ListenerHandle? = null
    private var serverTimeOffsetListener: JogoRepository.ListenerHandle? = null
    private var estadoSalaListener: JogoRepository.ListenerHandle? = null
    private var adminAdvanceHandler: Runnable? = null
    private var eliminacaoEmCurso = false
    private var navegacaoPontuacoesIniciada = false

    // Função chamada quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Obter os dados passados da atividade anterior (Intent)
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: "Jogador"
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        carregarOffsetServidor()

        jogoRepository.obterInfoSala(codigoSala, nomeUtilizador, nomeJogador)
            .addOnSuccessListener { infoSala ->
                admin = infoSala.admin
                modoJogo = infoSala.modoJogo

                // Mostra ou esconde a indicação "Admin"
                binding.txtAdmin.apply {
                    if (admin) {
                        text = "Admin"
                        visibility = android.view.View.VISIBLE
                    } else {
                        visibility = android.view.View.GONE
                    }
                }

                escutarFimEliminatorias()
                carregarPerguntas()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar sala: ${erro.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove todos os callbacks do handler
        handler.removeCallbacksAndMessages(null)
        // Para qualquer som que esteja a tocar
        pararSom()
        // Remove os listeners do Firebase
        removerListeners()
    }

    // Função para remover os listeners do Firebase e evitar memory leaks
    private fun removerListeners() {
        jogoRepository.removerListener(perguntaIndexListener)
        perguntaIndexListener = null
        jogoRepository.removerListener(serverTimeOffsetListener)
        serverTimeOffsetListener = null
        jogoRepository.removerListener(estadoSalaListener)
        estadoSalaListener = null
        adminAdvanceHandler?.let {
            handler.removeCallbacks(it)
        }
    }

    // Função para carregar as perguntas do Firebase
    private fun carregarPerguntas() {
        jogoRepository.carregarPerguntas(codigoSala)
            .addOnSuccessListener { perguntasCarregadas ->
                perguntas.clear()
                perguntas.addAll(perguntasCarregadas)
                if (perguntas.isNotEmpty()) {
                    if (admin) {
                        // Se for admin, controla o fluxo do jogo
                        mostrarRespostaAdmin()
                    } else {
                        // Se for jogador, escuta por atualizações do admin
                        escutarIndicePergunta()
                    }
                } else {
                    // Se não houver perguntas, finaliza o jogo
                    finalizarJogo()
                }
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this@JogoActivity, "Erro ao carregar perguntas: ${erro.message}", Toast.LENGTH_SHORT).show()
                finalizarJogo()
            }
    }

    // Função para os jogadores escutarem as mudanças no índice da pergunta
    private fun escutarIndicePergunta() {
        perguntaIndexListener = jogoRepository.escutarIndicePergunta(
            codigoSala,
            onIndiceAlterado = { novoIndex ->
                // Se o índice mudou, atualiza a pergunta
                if (novoIndex != perguntaAtualIndex) {
                    perguntaAtualIndex = novoIndex
                    if (perguntaAtualIndex < perguntas.size) {
                        // Chama a função
                        mostrarRespostaJogador()
                    } else {
                        finalizarJogo()
                    }
                }
            }
        )

        // Garante que a primeira pergunta é mostrada corretamente
        jogoRepository.obterIndicePergunta(codigoSala)
            .addOnSuccessListener { idx ->
                perguntaAtualIndex = idx
                if (perguntaAtualIndex < perguntas.size) {
                    mostrarRespostaJogador()
                } else {
                    finalizarJogo()
                }
            }
    }

    // Função para preparar e mostrar a interface da pergunta para o jogador
    private fun mostrarRespostaJogador() {
        handler.removeCallbacksAndMessages(null)
        pararSom()
        perguntaAtual = perguntas[perguntaAtualIndex]

        // Atualiza a UI com os dados da pergunta atual
        binding.txtProgresso.text = "Pergunta ${perguntaAtualIndex + 1}/${perguntas.size}"
        binding.txtPergunta.text = perguntaAtual.pergunta

        opcoesAtuais = obterOpcoesAleatorias(perguntaAtual)
        binding.btnOpcao1.text = opcoesAtuais[0]
        binding.btnOpcao2.text = opcoesAtuais[1]
        binding.btnOpcao3.text = opcoesAtuais[2]
        binding.btnOpcao4.text = opcoesAtuais[3]

        // Restaura a cor e estado dos botões
        definirCorBotao(binding.btnOpcao1, "#E0E0E0")
        definirCorBotao(binding.btnOpcao2, "#E0E0E0")
        definirCorBotao(binding.btnOpcao3, "#E0E0E0")
        definirCorBotao(binding.btnOpcao4, "#E0E0E0")

        binding.btnOpcao1.isEnabled = true
        binding.btnOpcao2.isEnabled = true
        binding.btnOpcao3.isEnabled = true
        binding.btnOpcao4.isEnabled = true

        // Define os listeners para os cliques nos botões de opção
        binding.btnOpcao1.setOnClickListener { verificarResposta(0) }
        binding.btnOpcao2.setOnClickListener { verificarResposta(1) }
        binding.btnOpcao3.setOnClickListener { verificarResposta(2) }
        binding.btnOpcao4.setOnClickListener { verificarResposta(3) }

        // Reinicia as variáveis de controlo da pergunta
        jaRespondeu = false
        acertouUltimaPergunta = false
        tempoDecorrido = true
        progressBarAtivo = true

        // Obtém a hora de início da pergunta definida pelo admin para sincronizar o cronómetro
        jogoRepository.obterHoraInicioPergunta(codigoSala)
            .addOnSuccessListener { horaInicio ->
                iniciarCronometroSincronizado(horaInicio ?: tempoServidorAtual())
            }
            .addOnFailureListener {
                iniciarCronometroSincronizado(tempoServidorAtual())
            }
    }

    // Função para  mostrar a interface da pergunta para o admin
    private fun mostrarRespostaAdmin() {
        handler.removeCallbacksAndMessages(null)
        pararSom()
        if (perguntaAtualIndex >= perguntas.size) {
            finalizarJogo()
            return
        }
        perguntaAtual = perguntas[perguntaAtualIndex]

        binding.txtProgresso.text = "Pergunta ${perguntaAtualIndex + 1}/${perguntas.size}"
        binding.txtPergunta.text = perguntaAtual.pergunta

        // Guardar as opções das perguntas atuais
        opcoesAtuais = obterOpcoesAleatorias(perguntaAtual)
        binding.btnOpcao1.text = opcoesAtuais[0]
        binding.btnOpcao2.text = opcoesAtuais[1]
        binding.btnOpcao3.text = opcoesAtuais[2]
        binding.btnOpcao4.text = opcoesAtuais[3]

        // Chama as funções para definir a cor dos botões
        definirCorBotao(binding.btnOpcao1, "#E0E0E0")
        definirCorBotao(binding.btnOpcao2, "#E0E0E0")
        definirCorBotao(binding.btnOpcao3, "#E0E0E0")
        definirCorBotao(binding.btnOpcao4, "#E0E0E0")

        // O admin não pode responder, apenas observar
        bloquearAdminSempre()
        jaRespondeu = false
        acertouUltimaPergunta = false
        tempoDecorrido = true
        progressBarAtivo = true

        if (adminPrimeiraPergunta) {
            Toast.makeText(this, "Como admin, só pode observar as respostas.", Toast.LENGTH_SHORT).show()
            adminPrimeiraPergunta = false
        }

        // Atualiza o índice da pergunta e a hora de início no Firebase para todos os jogadores
        jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)

        // Limpa as respostas da pergunta anterior no Firebase
        jogoRepository.limparRespostasPergunta(codigoSala)

        // Inicia o cronómetro do lado do admin
        jogoRepository.obterHoraInicioPergunta(codigoSala)
            .addOnSuccessListener { horaInicio ->
                iniciarCronometroAdmin(horaInicio ?: tempoServidorAtual())
            }
            .addOnFailureListener {
                iniciarCronometroAdmin(tempoServidorAtual())
            }
    }

    // Função para desativar os botões de resposta para o admin
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

    // Função para verificar se a resposta do jogador está correta
    private fun verificarResposta(numeroOpcao: Int) {
        // Impede respostas múltiplas ou do admin
        if (admin || jaRespondeu) return
        jaRespondeu = true

        // Inidcar que o tempo acavou
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
        // Chama a função para definir a cor do botão correto
        definirCorBotao(botaoCorreto!!, "#81C784")

        acertouUltimaPergunta = false

        // Se a resposta estiver correta
        if (botaoSelecionado != null && opcaoEscolhida == perguntaAtual.respostaCorreta) {
            tocarSom(this, R.raw.certo)
            somTocar = true
            definirCorBotao(botaoSelecionado, "#81C784")
            acertouUltimaPergunta = true
            numeroPerguntasCertas++
            totalPerguntascertas++
            atualizarPontuacao()
        } else if (botaoSelecionado != null) {
            // Se a resposta estiver errada
            tocarSom(this, R.raw.errado)
            somTocar = true
            // Pinta a resposta errada a vermelho
            definirCorBotao(botaoSelecionado, "#E57373")
            // Reinicia a contagem de respostas certas seguidas
            numeroPerguntasCertas = 0
            // Se o jogador errou, não conta para o total de respostas certas
            acertouUltimaPergunta = false
        } else if (numeroOpcao == -1) {
            // Se o tempo acabou
            acertouUltimaPergunta = false
        }

        // Regista a resposta do jogador no Firebase
        jogoRepository.registarResposta(codigoSala, nomeJogador, acertouUltimaPergunta)

        if (modoJogo == GameConstants.MODO_ELIMINATORIAS && !admin && !acertouUltimaPergunta) {
            handler.postDelayed({
                eliminarJogador()
            }, 1200)
        }

    }

    // Função para iniciar o cronómetro sincronizado para os jogadores
    private fun iniciarCronometroSincronizado(horaInicio: Long) {
        tempoDecorrido = true
        progressBarAtivo = true
        val tempoTotal = gameService.tempoTotal(modoJogo)
        binding.pbTempo.max = tempoTotal.toInt()
        binding.pbTempo.progress = tempoTotal.toInt()

        val runnable = object : Runnable {
            override fun run() {
                val tempoAtual = tempoServidorAtual()
                val tempoDecorridoSegundos = (tempoAtual - horaInicio) / 1000.0
                tempoRestante = tempoTotal - tempoDecorridoSegundos
                if (tempoRestante < 0) tempoRestante = 0.0

                // Toca um som de aviso quando restam 5 segundos
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
                        // Quando o tempo acaba
                        binding.pbTempo.progress = 0
                        progressBarAtivo = false
                        if (!jaRespondeu) {
                            // Chama a verificação com -1 para indicar tempo esgotado
                            verificarResposta(-1)
                        }
                        // Desativa os botões
                        binding.btnOpcao1.isEnabled = false
                        binding.btnOpcao2.isEnabled = false
                        binding.btnOpcao3.isEnabled = false
                        binding.btnOpcao4.isEnabled = false

                        // O jogador espera que o admin avance para a próxima pergunta
                        handler.postDelayed({
                        }, 3000)
                    }
                }

                if (tempoRestante > 0 && progressBarAtivo) {
                    handler.postDelayed(this, 200) // Atualiza a cada 200ms
                }
            }
        }
        handler.post(runnable)
    }

    // Função para iniciar o cronómetro para o admin
    private fun iniciarCronometroAdmin(horaInicio: Long) {
        tempoDecorrido = true
        progressBarAtivo = true
        val tempoTotal = gameService.tempoTotal(modoJogo)
        binding.pbTempo.max = tempoTotal.toInt()
        binding.pbTempo.progress = tempoTotal.toInt()

        adminAdvanceHandler = object : Runnable {
            override fun run() {
                val tempoAtual = tempoServidorAtual()
                val tempoDecorridoSegundos = (tempoAtual - horaInicio) / 1000.0
                tempoRestante = tempoTotal - tempoDecorridoSegundos
                if (tempoRestante < 0) tempoRestante = 0.0

                // Toca um som de aviso quando restam 5 segundos
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
                        // Após 3 segundos, o admin avança para a próxima pergunta
                        handler.postDelayed({
                            if (modoJogo == GameConstants.MODO_ELIMINATORIAS) {
                                verificarFimEliminatoriasOuAvancar()
                            } else {
                                perguntaAtualIndex++
                                // Atualiza o índice da pergunta no Firebase para todos
                                jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
                                // Mostra a próxima pergunta para o admin
                                mostrarRespostaAdmin()
                            }
                        }, 3000)
                    }
                }

                if (tempoRestante > 0 && progressBarAtivo) {
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.post(adminAdvanceHandler!!)
    }

    private fun carregarOffsetServidor() {
        serverTimeOffsetListener = jogoRepository.escutarOffsetServidor(
            onOffsetAlterado = { offset ->
                serverTimeOffset = offset
            }
        )
    }

    private fun tempoServidorAtual(): Long = System.currentTimeMillis() + serverTimeOffset

    // Função para verificar se o modo "Eliminatórias" deve terminar
    private fun verificarFimEliminatoriasOuAvancar() {
        if (!admin && !acertouUltimaPergunta) {
            eliminarJogador()
            return
        }
        jogoRepository.obterJogadoresEliminatorias(codigoSala)
            .addOnSuccessListener { jogadores ->
                // Conta quantos jogadores ainda estão na sala
                val jogadoresRestantes = jogadores
                    .filter { jogador ->
                        jogador.nome != GameConstants.JOGADOR_ADMIN &&
                            !jogador.isHostOnly &&
                            jogador.estado != GameConstants.ESTADO_ELIMINADO
                    }
                    .map { it.nome }
                if (gameService.deveTerminarEliminatorias(jogadoresRestantes)) {
                    // Se restar apenas um jogador, o jogo termina
                    Toast.makeText(this@JogoActivity, "Jogo terminado! Só resta um jogador.", Toast.LENGTH_LONG).show()
                    terminarEliminatoriasEEnviar()
                } else {
                    // Caso contrário, avança para a próxima pergunta
                    perguntaAtualIndex++
                    jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
                    if (admin) mostrarRespostaAdmin()
                }
            }
            .addOnFailureListener {
                // Em caso de erro, volta para o menu principal
                Toast.makeText(this@JogoActivity, "Erro ao verificar jogadores.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@JogoActivity, MainActivity::class.java)
                adicionarDadosJogador(intent, nomeUtilizador.ifBlank { null }, nomeJogador)
                startActivity(intent)
                finish()
            }
    }

    // Função para eliminar um jogador no modo "Eliminatórias"
    private fun eliminarJogador() {
        if (admin || eliminacaoEmCurso) return
        eliminacaoEmCurso = true
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        pararSom()
        // Mantém o jogador na sala para preservar pontuação e pódio, mas marca-o como eliminado.
        jogoRepository.marcarJogadorEliminado(
            codigoSala,
            nomeJogador,
            totalPontos,
            totalPerguntascertas
        )
            .addOnSuccessListener {
                Toast.makeText(this, "Você foi eliminado!", Toast.LENGTH_LONG).show()
                abrirEsperaEliminadoActivity()
            }
            .addOnFailureListener {
                eliminacaoEmCurso = false
                Toast.makeText(this, "Erro ao eliminar jogador. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    // Função para calcular e atualizar a pontuação do jogador
    private fun atualizarPontuacao() {
        val resultado = scoreService.calcularPontuacao(
            modoJogo = modoJogo,
            tempoRestante = tempoRestante,
            numeroPerguntasCertas = numeroPerguntasCertas,
            bonus = bonus
        )
        if (resultado.bonusAplicado > 0) {
            Toast.makeText(this, "Bónus de sequência! +${resultado.bonusAplicado} pontos", Toast.LENGTH_SHORT).show()
        }
        totalPontos += resultado.pontos
    }

    // Função para finalizar o jogo
    private fun finalizarJogo() {
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        pararSom()
        removerListeners()
        if (perguntaAtualIndex >= perguntas.size) {
            if (modoJogo == GameConstants.MODO_ELIMINATORIAS && admin) {
                terminarEliminatoriasEEnviar()
                return
            }
            // Verifica o estado da sala antes de avançar
            jogoRepository.obterEstadoSala(codigoSala)
                .addOnSuccessListener {
                    guardarResultadoEEnviarPontuacoes()
                }
                .addOnFailureListener { erro ->
                    Toast.makeText(this@JogoActivity, "Erro ao verificar estado da sala: ${erro.message}", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@JogoActivity, MainActivity::class.java)
                    adicionarDadosJogador(intent, nomeUtilizador.ifBlank { null }, nomeJogador)
                    startActivity(intent)
                    finish()
                }
            return
        }

        if (modoJogo == GameConstants.MODO_ELIMINATORIAS) {
            verificarFimEliminatoriasOuAvancar()
        } else {
            perguntaAtualIndex++
            if (admin) {
                // Se for admin, avança para a próxima pergunta
                jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
                mostrarRespostaAdmin()
            }
        }
    }

    private fun escutarFimEliminatorias() {
        if (modoJogo != GameConstants.MODO_ELIMINATORIAS || estadoSalaListener != null) return

        estadoSalaListener = jogoRepository.escutarEstadoSala(
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_TERMINADO) {
                    guardarResultadoEEnviarPontuacoes()
                }
            }
        )
    }

    private fun terminarEliminatoriasEEnviar() {
        if (navegacaoPontuacoesIniciada) return

        jogoRepository.atualizarEstadoSala(codigoSala, GameConstants.ESTADO_TERMINADO)
            .addOnCompleteListener {
                guardarResultadoEEnviarPontuacoes()
            }
    }

    private fun guardarResultadoEEnviarPontuacoes() {
        if (navegacaoPontuacoesIniciada) return
        navegacaoPontuacoesIniciada = true

        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        pararSom()
        removerListeners()

        if (!admin) {
            jogoRepository.guardarResultadoJogador(
                codigoSala,
                nomeJogador,
                totalPontos,
                totalPerguntascertas
            ).addOnCompleteListener {
                enviarPontuacaoActivity()
            }
        } else {
            enviarPontuacaoActivity()
        }
    }

    private fun abrirEsperaEliminadoActivity() {
        removerListeners()
        val intent = Intent(this, EsperaEliminadoActivity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
        intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
        intent.putExtra(IntentExtras.TOTAL_PONTOS, totalPontos)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
        intent.putExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, numeroPerguntasCertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, totalPerguntascertas)
        intent.putExtra(IntentExtras.RESPOSTAS_CERTAS, totalPerguntascertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS, perguntas.size)
        startActivity(intent)
        finish()
    }

    // Função para enviar os dados do jogo para a atividade de pontuações
    private fun enviarPontuacaoActivity() {
        val intent = Intent(this, PontuacoesActivity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
        intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
        intent.putExtra(IntentExtras.TOTAL_PONTOS, totalPontos)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
        intent.putExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, numeroPerguntasCertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, totalPerguntascertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS, perguntas.size)
        startActivity(intent)
        finish() // Fecha a atividade atual
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
