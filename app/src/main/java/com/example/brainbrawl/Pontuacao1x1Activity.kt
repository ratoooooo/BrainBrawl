package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacao1x1Binding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.Pontuacao1x1Event
import com.example.brainbrawl.viewmodels.Pontuacao1x1Input
import com.example.brainbrawl.viewmodels.Pontuacao1x1ViewModel

class Pontuacao1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPontuacao1x1Binding.inflate(layoutInflater)
    }
    private val authService = AuthService()
    private val viewModel by lazy {
        ViewModelProvider(this)[Pontuacao1x1ViewModel::class.java]
    }

    private lateinit var codigoSala: String
    private var uid: String = ""
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private var nomeCategoria: String = ""
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var avatar: String = ""
    private var isGuest: Boolean = false
    private var navegouParaDesforra = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        val totalRespostasCertas = intent.getIntExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, 0)
        val totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 8)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        avatar = intent.getStringExtra(IntentExtras.AVATAR) ?: ""
        isGuest = intent.getBooleanExtra(IntentExtras.IS_GUEST, false) ||
            tipoJogador == GameConstants.TIPO_JOGADOR_GUEST ||
            uid.isBlank()

        configurarObservers()
        viewModel.iniciarPontuacao(
            Pontuacao1x1Input(
                codigoSala = codigoSala,
                uid = uid,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                totalRespostasCertas = totalRespostasCertas,
                totalPerguntas = totalPerguntas,
                nomeCategoria = nomeCategoria,
                playerKey = playerKey,
                tipoJogador = tipoJogador,
                avatar = avatar,
                isGuest = isGuest
            )
        )

        binding.btnVoltar.setOnClickListener {
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador.ifBlank { null }, uid.ifBlank { null })
            finish()
        }

        binding.btnDesforra.setOnClickListener {
            viewModel.pedirDesforra()
        }
    }

    private fun configurarObservers() {
        viewModel.pontuacaoUiState.observe(this) { state ->
            val primeiro = state.podio.getOrNull(0)
            val segundo = state.podio.getOrNull(1)
            binding.txtNomeJogador1.text = primeiro?.nome.orEmpty()
            binding.txtPontos1.text = primeiro?.pontos.orEmpty()
            binding.txtNomeJogador2.text = segundo?.nome.orEmpty()
            binding.txtPontos2.text = segundo?.pontos.orEmpty()
        }

        viewModel.estadoDesforra.observe(this) { estado ->
            binding.txtEstadoDesforra.text = estado.mensagem
            binding.txtEstadoDesforra.visibility = if (estado.mensagem.isBlank()) View.GONE else View.VISIBLE
            binding.btnDesforra.isEnabled = !estado.desforraPedida
        }

        viewModel.evento.observe(this) { evento ->
            when (evento) {
                is Pontuacao1x1Event.AbrirNovaSalaDesforra -> abrirSalaDesforra(evento.codigoSala)
                is Pontuacao1x1Event.MostrarMensagem -> Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
                null -> Unit
            }
            if (evento != null) viewModel.consumirEvento()
        }
    }

    private fun abrirSalaDesforra(novaSala: String) {
        if (navegouParaDesforra) return
        navegouParaDesforra = true
        val intent = Intent(this, SalaDeEspera1x1Activity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, novaSala)
        uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        adicionarExtrasMatchmaking(intent)
        startActivity(intent)
        finish()
    }

    private fun adicionarExtrasMatchmaking(intent: Intent) {
        playerKey.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.PLAYER_KEY, it) }
        tipoJogador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.TIPO_JOGADOR, it) }
        avatar.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.AVATAR, it) }
        intent.putExtra(IntentExtras.IS_GUEST, isGuest)
    }
}
