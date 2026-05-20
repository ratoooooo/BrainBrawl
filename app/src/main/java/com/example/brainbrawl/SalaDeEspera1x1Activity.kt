package com.example.brainbrawl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera1x1Binding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils
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
    private val authService = AuthService()

    // Variáveis para a lógica da sala
    private lateinit var codigoSala: String
    private var uid: String = ""
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private lateinit var nomeCategoria: String
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var avatar: String = ""
    private var aNavegarParaJogo = false
    private var saidaJaProcessada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this) {
            sairDaSala()
        }

        // Guardar dados passados pelo intent
        codigoSala = CodigoSalaUtils.normalizarCodigo(intent.getStringExtra(IntentExtras.CODIGO_SALA).orEmpty())
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: getString(R.string.categoria5)
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        avatar = intent.getStringExtra(IntentExtras.AVATAR) ?: ""
        Log.d(
            TAG,
            "SalaDeEspera1x1 onCreate: codigo=$codigoSala uid=$uid playerKey=$playerKey tipo=$tipoJogador"
        )

        binding.txtCodigoSala.text = getString(R.string.a_carregar_sala)
        binding.btnCopiarCodigoSala.visibility = View.GONE
        binding.btnCopiarCodigoSala.setOnClickListener {
            copiarCodigoSala()
        }

        configurarObservers()
        viewModel.carregarExposicaoCodigo(codigoSala)
        viewModel.iniciar(codigoSala, uid, nomeUtilizador, nomeJogador, playerKey, tipoJogador, avatar)
        viewModel.observarJogadores(codigoSala)
        viewModel.observarEstadoSala(codigoSala)
        viewModel.observarSalaApagada(codigoSala)

        // Listener para o clique no botão de iniciar jogo
        binding.btnIniciarJogo.setOnClickListener {
            binding.btnIniciarJogo.isEnabled = false
            viewModel.acaoPrincipal(codigoSala)
        }

        binding.btnSairSala.setOnClickListener {
            binding.btnSairSala.isEnabled = false
            sairDaSala()
        }
    }

    override fun onDestroy() {
        Log.d(
            TAG,
            "SalaDeEspera1x1 onDestroy: codigo=$codigoSala uid=$uid playerKey=$playerKey " +
                "aNavegarParaJogo=$aNavegarParaJogo saidaJaProcessada=$saidaJaProcessada"
        )
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
            getString(R.string.aguardando_jogadores)
        } else {
            estado.jogadores.joinToString(separator = "\n")
        }
        atualizarCodigoSala(estado)
        binding.btnIniciarJogo.isEnabled = estado.podeIniciar
        binding.btnIniciarJogo.text = when {
            estado.matchmaking && estado.jogadorPronto -> getString(R.string.pronto_confirmado)
            estado.matchmaking -> getString(R.string.pronto)
            else -> getString(R.string.iniciar_jogo)
        }
    }

    private fun atualizarCodigoSala(estado: SalaCompetitivaUiState) {
        if (estado.codigoSalaVisivel) {
            binding.layoutCodigoSala.visibility = View.VISIBLE
            binding.txtCodigoSala.text = getString(R.string.codigo_sala_format, codigoSala)
            binding.btnCopiarCodigoSala.visibility = View.VISIBLE
        } else {
            binding.layoutCodigoSala.visibility = View.GONE
            binding.btnCopiarCodigoSala.visibility = View.GONE
        }
    }

    private fun tratarEvento(evento: Sala1x1Event) {
        when (evento) {
            Sala1x1Event.JogoIniciado -> {
                aNavegarParaJogo = true
                Log.d(TAG, "SalaDeEspera1x1 navegar jogo: codigo=$codigoSala uid=$uid playerKey=$playerKey")
                val intent = Intent(this@SalaDeEspera1x1Activity, Jogo1x1Activity::class.java)
                intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
                uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
                adicionarExtrasMatchmaking(intent)
                startActivity(intent)
                finish()
            }
            Sala1x1Event.SalaEncerrada -> {
                Toast.makeText(this@SalaDeEspera1x1Activity, R.string.sala_encerrada, Toast.LENGTH_SHORT).show()
                abrirMainActivity(this@SalaDeEspera1x1Activity, nomeUtilizador, nomeJogador, uid.ifBlank { null })
                finish()
            }
            Sala1x1Event.AguardarAdversario ->
                Toast.makeText(this, R.string.aguardar_adversario, Toast.LENGTH_SHORT).show()
            Sala1x1Event.JogadoresNaoProntos ->
                Toast.makeText(this@SalaDeEspera1x1Activity, R.string.ambos_jogadores_na_sala, Toast.LENGTH_SHORT).show()
            Sala1x1Event.OponenteSaiu ->
                Toast.makeText(this@SalaDeEspera1x1Activity, R.string.sala_espera_jogador_saiu, Toast.LENGTH_LONG).show()
            Sala1x1Event.EntradaBloqueada -> {
                Toast.makeText(this@SalaDeEspera1x1Activity, R.string.sala_1x1_cheia, Toast.LENGTH_SHORT).show()
                abrirMainActivity(this@SalaDeEspera1x1Activity, nomeUtilizador, nomeJogador, uid.ifBlank { null })
                finish()
            }
        }
        if (evento == Sala1x1Event.AguardarAdversario ||
            evento == Sala1x1Event.JogadoresNaoProntos ||
            evento == Sala1x1Event.OponenteSaiu
        ) {
            binding.btnIniciarJogo.isEnabled = viewModel.estado.value?.podeIniciar == true
        }
    }

    private fun sairDaSala() {
        if (saidaJaProcessada || aNavegarParaJogo) return
        saidaJaProcessada = true
        Log.d(TAG, "SalaDeEspera1x1 sairDaSala: codigo=$codigoSala uid=$uid playerKey=$playerKey cleanupIntentional=true")
        viewModel.sairDaSala(codigoSala)
        abrirMainActivity(this, nomeUtilizador, nomeJogador, uid.ifBlank { null })
        finish()
    }

    private fun adicionarExtrasMatchmaking(intent: Intent) {
        playerKey.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.PLAYER_KEY, it) }
        tipoJogador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.TIPO_JOGADOR, it) }
        avatar.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.AVATAR, it) }
        intent.putExtra(IntentExtras.IS_GUEST, tipoJogador == com.example.brainbrawl.config.GameConstants.TIPO_JOGADOR_GUEST)
    }

    private fun copiarCodigoSala() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clipboard_codigo_sala), codigoSala))
        Toast.makeText(this, R.string.codigo_copiado, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "MATCHMAKING_DEBUG"
    }
}
