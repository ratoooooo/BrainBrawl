package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera2x2Binding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.Sala2x2Event
import com.example.brainbrawl.viewmodels.Sala2x2UiState
import com.example.brainbrawl.viewmodels.Sala2x2ViewModel

class SalaDeEspera2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySalaDeEspera2x2Binding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[Sala2x2ViewModel::class.java]
    }
    private val authService = AuthService()
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var codigoSala: String
    private var uid: String = ""
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private var categoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        categoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)
            ?: intent.getStringExtra(IntentExtras.CATEGORIA_LEGACY)
            ?: getString(R.string.categoria5)

        // Mostrar o código da sala
        binding.txtCodigoSala.text = "Código da sala: $codigoSala"

        configurarObservers()
        viewModel.iniciar(codigoSala, uid, nomeUtilizador, nomeJogador)
        viewModel.observarJogadores(codigoSala)
        viewModel.observarEstadoSala(codigoSala)
        viewModel.observarSalaApagada(codigoSala)

        // Configurar botão de Iniciar Jogo
        binding.btnIniciarJogo.setOnClickListener {
            viewModel.iniciarJogo(codigoSala)
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

    private fun atualizarEstadoSala(estado: Sala2x2UiState) {
        // Atualiza os TextViews com os nomes dos jogadores
        binding.txtJogadorA1.text = estado.equipaA.getOrNull(0) ?: "Aguardando..."
        binding.txtJogadorA2.text = estado.equipaA.getOrNull(1) ?: "Aguardando..."
        binding.txtJogadorB1.text = estado.equipaB.getOrNull(0) ?: "Aguardando..."
        binding.txtJogadorB2.text = estado.equipaB.getOrNull(1) ?: "Aguardando..."
        binding.btnIniciarJogo.isEnabled = estado.podeIniciar
    }

    private fun tratarEvento(evento: Sala2x2Event) {
        when (evento) {
            Sala2x2Event.JogoIniciado -> {
                val intent = Intent(this@SalaDeEspera2x2Activity, Jogo2x2Activity::class.java)
                codigoSala.let { intent.putExtra(IntentExtras.CODIGO_SALA, it) }
                uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                nomeUtilizador.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
                nomeJogador.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
                categoria?.let {
                    intent.putExtra(IntentExtras.NOME_CATEGORIA, it)
                    intent.putExtra(IntentExtras.CATEGORIA_LEGACY, it)
                }
                startActivity(intent)
                finish()
            }
            Sala2x2Event.SalaEncerrada -> {
                abrirMainActivity(this@SalaDeEspera2x2Activity, nomeUtilizador, nomeJogador, uid.ifBlank { null })
                finish()
            }
        }
    }

    private fun sairDaSala() {
        viewModel.sairDaSala(codigoSala)
        abrirMainActivity(this, nomeUtilizador, nomeJogador, uid.ifBlank { null })
        finish()
    }
}
