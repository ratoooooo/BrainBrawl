package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: "Jogador"
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        modoJogo = intent.getStringExtra("modoJogo") ?: "eliminatorias"
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        numeroPerguntasCertas = intent.getIntExtra("numeroPerguntasCertas", 0)
        totalPerguntascertas = intent.getIntExtra("totalPerguntascertas", 0)
        totalPerguntas = intent.getIntExtra("totalPerguntas", 1)

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
                if (estado == "terminado") {
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
        intent.putExtra("codigoSala", codigoSala)
        intent.putExtra("nomeJogador", nomeJogador)
        intent.putExtra("totalPontos", totalPontos)
        intent.putExtra("nomeCategoria", nomeCategoria)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("numeroPerguntasCertas", numeroPerguntasCertas)
        intent.putExtra("totalPerguntascertas", totalPerguntascertas)
        intent.putExtra("respostasCertas", totalPerguntascertas)
        intent.putExtra("totalPerguntas", totalPerguntas)
        startActivity(intent)
        finish()
    }
}
