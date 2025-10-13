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
    private val database = FirebaseDatabase.getInstance().reference

    // Listeners do Firebase para serem removidos quando a atividade é destruída
    private var perguntaIndexListener: ValueEventListener? = null
    private var adminAdvanceHandler: Runnable? = null

    // Função chamada quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Obter os dados passados da atividade anterior (Intent)
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""

        // Referência para a sala no Firebase
        val salaRef = database.child("salas").child(codigoSala)

        // Verifica se o utilizador atual é o administrador da sala
        salaRef.child("admin").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nomeAdmin = snapshot.getValue(String::class.java)
                admin = (nomeAdmin == nomeUtilizador) || (nomeAdmin == nomeJogador)

                // Mostra ou esconde a indicação "Admin"
                binding.txtAdmin.apply {
                    if (admin) {
                        text = "Admin"
                        visibility = android.view.View.VISIBLE
                    } else {
                        visibility = android.view.View.GONE
                    }
                }

                // Carrega o modo de jogo definido para a sala
                salaRef.child("modoJogo").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        modoJogo = snapshot.getValue(String::class.java) ?: "classico"
                        carregarPerguntas() // Inicia o carregamento das perguntas após obter o modo de jogo
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
        // Remove todos os callbacks do handler
        handler.removeCallbacksAndMessages(null)
        // Para qualquer som que esteja a tocar
        pararSom()
        // Remove os listeners do Firebase
        removerListeners()
    }

    // Função para remover os listeners do Firebase e evitar memory leaks
    private fun removerListeners() {
        perguntaIndexListener?.let {
            database.child("salas").child(codigoSala).child("perguntaAtualIndex").removeEventListener(it)
        }
        adminAdvanceHandler?.let {
            handler.removeCallbacks(it)
        }
    }

    // Função para carregar as perguntas do Firebase
    private fun carregarPerguntas() {
        database.child("salas").child(codigoSala).child("perguntas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    perguntas.clear()
                    var contador = 0
                    // Itera sobre as perguntas recebidas e adiciona-as à lista local
                    for (perguntaSnapshot in snapshot.children) {
                        // Limita o número de perguntas a 8
                        if (contador >= 8) break
                        val pergunta = perguntaSnapshot.getValue(Pergunta::class.java)
                        if (pergunta != null) perguntas.add(pergunta)
                        contador++
                    }
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
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@JogoActivity, "Erro ao carregar perguntas: ${error.message}", Toast.LENGTH_SHORT).show()
                    finalizarJogo()
                }
            })
    }

    // Função para os jogadores escutarem as mudanças no índice da pergunta
    private fun escutarIndicePergunta() {
        perguntaIndexListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val novoIndex = snapshot.getValue(Int::class.java) ?: 0
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
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("salas").child(codigoSala).child("perguntaAtualIndex")
            .addValueEventListener(perguntaIndexListener!!)

        // Garante que a primeira pergunta é mostrada corretamente
        database.child("salas").child(codigoSala).child("perguntaAtualIndex")
            .get().addOnSuccessListener { snap ->
                val idx = snap.getValue(Int::class.java) ?: 0
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
        database.child("salas").child(codigoSala).child("perguntaHoraInicio")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val horaInicio = snapshot.getValue(Long::class.java) ?: System.currentTimeMillis()
                    iniciarCronometroSincronizado(horaInicio)
                }
                override fun onCancelled(error: DatabaseError) {
                    iniciarCronometroSincronizado(System.currentTimeMillis())
                }
            })
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
        val salaRef = database.child("salas").child(codigoSala)
        val updates = mapOf(
            "perguntaAtualIndex" to perguntaAtualIndex,
            "perguntaHoraInicio" to System.currentTimeMillis()
        )
        salaRef.updateChildren(updates)

        // Limpa as respostas da pergunta anterior no Firebase
        database.child("salas").child(codigoSala).child("perguntaAtual").child("respostas").removeValue()

        // Inicia o cronómetro do lado do admin
        iniciarCronometroAdmin()
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
        database.child("salas").child(codigoSala).child("perguntaAtual")
            .child("respostas").child(nomeJogador).setValue(acertouUltimaPergunta)

    }

    // Função para iniciar o cronómetro sincronizado para os jogadores
    private fun iniciarCronometroSincronizado(horaInicio: Long) {
        tempoDecorrido = true
        progressBarAtivo = true
        val tempoTotal = if (modoJogo == "caotico") 10.0 else 20.0
        binding.pbTempo.max = tempoTotal.toInt()
        binding.pbTempo.progress = tempoTotal.toInt()
        var primeiraAtualizacao = true

        val runnable = object : Runnable {
            override fun run() {
                val tempoAtual = System.currentTimeMillis()
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
    private fun iniciarCronometroAdmin() {
        tempoDecorrido = true
        progressBarAtivo = true
        val tempoTotal = if (modoJogo == "caotico") 10.0 else 20.0
        binding.pbTempo.max = tempoTotal.toInt()
        binding.pbTempo.progress = tempoTotal.toInt()
        val horaInicio = System.currentTimeMillis()

        adminAdvanceHandler = object : Runnable {
            override fun run() {
                val tempoAtual = System.currentTimeMillis()
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
                            if (modoJogo == "eliminatorias") {
                                verificarFimEliminatoriasOuAvancar()
                            } else {
                                perguntaAtualIndex++
                                // Atualiza o índice da pergunta no Firebase para todos
                                val salaRef = database.child("salas").child(codigoSala)
                                val updates = mapOf(
                                    "perguntaAtualIndex" to perguntaAtualIndex,
                                    "perguntaHoraInicio" to System.currentTimeMillis()
                                )
                                salaRef.updateChildren(updates)
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

    // Função para verificar se o modo "Eliminatórias" deve terminar
    private fun verificarFimEliminatoriasOuAvancar() {
        if (!admin && !acertouUltimaPergunta) {
            eliminarJogador()
            return
        }
        database.child("salas").child(codigoSala).child("jogadores")
            .get()
            .addOnSuccessListener { snapshot ->
                // Conta quantos jogadores ainda estão na sala
                val jogadoresRestantes = snapshot.children
                    .mapNotNull { it.key }
                    .filter { nome ->
                        nome != nomeUtilizador && nome != nomeJogador
                    }
                if (jogadoresRestantes.size <= 1) {
                    // Se restar apenas um jogador, o jogo termina
                    Toast.makeText(this@JogoActivity, "Jogo terminado! Só resta um jogador.", Toast.LENGTH_LONG).show()
                    enviarPontuacaoActivity()
                } else {
                    // Caso contrário, avança para a próxima pergunta
                    perguntaAtualIndex++
                    val salaRef = database.child("salas").child(codigoSala)
                    val updates = mapOf(
                        "perguntaAtualIndex" to perguntaAtualIndex,
                        "perguntaHoraInicio" to System.currentTimeMillis()
                    )
                    salaRef.updateChildren(updates)
                    if (admin) mostrarRespostaAdmin()
                }
            }
            .addOnFailureListener {
                // Em caso de erro, volta para o menu principal
                Toast.makeText(this@JogoActivity, "Erro ao verificar jogadores.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@JogoActivity, MainActivity::class.java)
                intent.putExtra("nomeUtilizador", nomeUtilizador)
                intent.putExtra("nomeJogador", nomeJogador)
                startActivity(intent)
                finish()
            }
    }

    // Função para eliminar um jogador no modo "Eliminatórias"
    private fun eliminarJogador() {
        if (admin) return
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        pararSom()
        // Remove o jogador da sala no Firebase
        database.child("salas").child(codigoSala).child("jogadores")
            .child(nomeJogador).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Você foi eliminado!", Toast.LENGTH_LONG).show()
                // Envia de volta para o ecrã principal
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

    // Função para calcular e atualizar a pontuação do jogador
    private fun atualizarPontuacao() {
        // Calcula os pontos com base no tempo de resposta
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

        // Bonus por sequência de respostas corretas
        if (numeroPerguntasCertas == 2) {
            pontuacao += bonus
            Toast.makeText(this, "Bónus de sequência! +$bonus pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas == 3) {
            pontuacao += bonus + 25
            Toast.makeText(this, "Bónus de sequência! +${bonus + 25} pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas >= 4) {
            pontuacao += bonus + 50
            Toast.makeText(this, "Bónus de sequência! +${bonus + 50} pontos", Toast.LENGTH_SHORT).show()
        }
        totalPontos += pontuacao
    }

    // Função para finalizar o jogo
    private fun finalizarJogo() {
        tempoDecorrido = false
        progressBarAtivo = false
        handler.removeCallbacksAndMessages(null)
        pararSom()
        removerListeners()
        if (perguntaAtualIndex >= perguntas.size) {
            if (!admin) {
                // Guarda a pontuação final e o total de respostas certas no Firebase
                database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador)
                    .child("pontuacao").setValue(totalPontos)
                database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador)
                    .child("totalRespostasCertas").setValue(totalPerguntascertas)
            }
            // Verifica o estado da sala antes de avançar
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
            if (admin) {
                // Se for admin, avança para a próxima pergunta
                val salaRef = database.child("salas").child(codigoSala)
                val updates = mapOf(
                    "perguntaAtualIndex" to perguntaAtualIndex,
                    "perguntaHoraInicio" to System.currentTimeMillis()
                )
                salaRef.updateChildren(updates)
                mostrarRespostaAdmin()
            }
        }
    }

    // Função para enviar os dados do jogo para a atividade de pontuações
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