package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera1x1Binding
import com.example.brainbrawl.viewmodels.Sala1x1Event
import com.example.brainbrawl.viewmodels.Sala1x1ViewModel
import com.example.brainbrawl.viewmodels.SalaCompetitivaUiState

class SalaDeEspera1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySalaDeEspera1x1Binding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[Sala1x1ViewModel::class.java]
    }

    // Variáveis para a lógica da sala
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private lateinit var nomeCategoria: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados pelo intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: getString(R.string.categoria5)

        // Define o texto do código da sala usando o binding
        binding.txtCodigoSala.text = "Código da Sala: $codigoSala"

        configurarObservers()
        viewModel.iniciar(codigoSala, nomeUtilizador)
        viewModel.observarJogadores(codigoSala)
        viewModel.observarEstadoSala(codigoSala)
        viewModel.observarSalaApagada(codigoSala)

        // Listener para o clique no botão de iniciar jogo
        binding.btnIniciarJogo.setOnClickListener {
            viewModel.verificarProntosEAvancar(codigoSala)
        }

        binding.btnSairSala.setOnClickListener {
            sairDaSala()
        }
    }

    override fun onDestroy() {
        viewModel.removerListeners()
        super.onDestroy()
    }

    private fun configurarObservers() {
        viewModel.estado.observe(this) { estado ->
            atualizarEstadoSala(estado)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun atualizarEstadoSala(estado: SalaCompetitivaUiState) {
        binding.txtListaJogadores.text = if (estado.jogadores.isEmpty()) {
            "Aguardando jogadores..."
        } else {
            estado.jogadores.joinToString(separator = "\n")
        }
        binding.btnIniciarJogo.isEnabled = estado.podeIniciar
    }

    private fun tratarEvento(evento: Sala1x1Event) {
        when (evento) {
            Sala1x1Event.JogoIniciado -> {
                val intent = Intent(this@SalaDeEspera1x1Activity, Jogo1x1Activity::class.java)
                intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
                startActivity(intent)
                finish()
            }
            Sala1x1Event.SalaEncerrada -> {
                Toast.makeText(this@SalaDeEspera1x1Activity, "A sala foi encerrada.", Toast.LENGTH_SHORT).show()
                abrirMainActivity(this@SalaDeEspera1x1Activity, nomeUtilizador, nomeJogador)
                finish()
            }
            Sala1x1Event.AguardarAdversario ->
                Toast.makeText(this, "Ainda a aguardar o adversário!", Toast.LENGTH_SHORT).show()
            Sala1x1Event.JogadoresNaoProntos ->
                Toast.makeText(this@SalaDeEspera1x1Activity, "Ambos os jogadores têm de estar na sala!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sairDaSala() {
        viewModel.sairDaSala(codigoSala)
        abrirMainActivity(this, nomeUtilizador, nomeJogador)
        finish()
    }
}
