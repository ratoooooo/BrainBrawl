package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityEsperaEliminadoBinding
import com.example.brainbrawl.repositories.JogoRepository

class EsperaEliminadoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEsperaEliminadoBinding.inflate(layoutInflater)
    }
    private val jogoRepository = JogoRepository()

    private lateinit var codigoSala: String
    private lateinit var nomeJogador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeUtilizador: String
    private var modoJogo: String? = null
    private var totalPontos = 0.0
    private var numeroPerguntasCertas = 0
    private var totalPerguntascertas = 0
    private var totalPerguntas = 1
    private var estadoListener: JogoRepository.ListenerHandle? = null
    private var podioAberto = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: "Jogador"
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_ELIMINATORIAS
        totalPontos = intent.getDoubleExtra(IntentExtras.TOTAL_PONTOS, 0.0)
        numeroPerguntasCertas = intent.getIntExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, 0)
        totalPerguntascertas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, 0)
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 1)

        binding.txtCodigoSala.text = "Código da Sala: $codigoSala"
        escutarFimJogo()
    }

    override fun onDestroy() {
        super.onDestroy()
        jogoRepository.removerListener(estadoListener)
    }

    private fun escutarFimJogo() {
        if (codigoSala.isBlank()) {
            Toast.makeText(this, "Dados da sala inválidos.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        estadoListener = jogoRepository.escutarEstadoSala(
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_TERMINADO) {
                    abrirPodio()
                }
            },
            onErro = {
                Toast.makeText(this, "Erro ao aguardar fim do jogo.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun abrirPodio() {
        if (podioAberto) return
        podioAberto = true
        jogoRepository.removerListener(estadoListener)
        estadoListener = null

        val intent = Intent(this, PontuacoesActivity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
        intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
        intent.putExtra(IntentExtras.TOTAL_PONTOS, totalPontos)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
        intent.putExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, numeroPerguntasCertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, totalPerguntascertas)
        intent.putExtra(IntentExtras.RESPOSTAS_CERTAS, totalPerguntascertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS, totalPerguntas)
        startActivity(intent)
        finish()
    }
}
