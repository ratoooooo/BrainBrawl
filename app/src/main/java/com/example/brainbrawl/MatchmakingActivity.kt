package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMatchmakingBinding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.MatchmakingEvent
import com.example.brainbrawl.viewmodels.MatchmakingNavegacaoDados
import com.example.brainbrawl.viewmodels.MatchmakingStatus
import com.example.brainbrawl.viewmodels.MatchmakingUiState
import com.example.brainbrawl.viewmodels.MatchmakingViewModel

class MatchmakingActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMatchmakingBinding.inflate(layoutInflater) }
    private val viewModel by lazy {
        ViewModelProvider(this)[MatchmakingViewModel::class.java]
    }
    private val authService = AuthService()

    private var uid: String = ""
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var modoJogo: String = GameConstants.MODO_1X1
    private var nomeCategoria: String = ""
    private var navegando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_1X1
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: getString(R.string.categoria5)

        binding.txtTituloMatchmaking.text = when (modoJogo) {
            GameConstants.MODO_2X2 -> getString(R.string.matchmaking_2x2_titulo)
            else -> getString(R.string.matchmaking_1x1_titulo)
        }
        configurarObservers()
        binding.btnCancelarMatchmaking.setOnClickListener { viewModel.cancelar() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.cancelar()
            }
        })

        viewModel.iniciar(
            uid = uid,
            nomeUtilizador = nomeUtilizador,
            nomeJogador = nomeJogador,
            modoJogo = modoJogo,
            nomeCategoria = nomeCategoria
        )
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            viewModel.removerListeners()
        }
        super.onDestroy()
    }

    override fun onStop() {
        if (!isChangingConfigurations && !navegando && !isFinishing) {
            viewModel.cancelarPorBackground()
        }
        super.onStop()
    }

    private fun configurarObservers() {
        viewModel.estado.observe(this) { estado ->
            renderizarEstado(estado)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun renderizarEstado(estado: MatchmakingUiState) {
        val labelModo = if (estado.modo == GameConstants.MODO_2X2) "2x2" else "1x1"
        binding.progressMatchmaking.visibility = if (estado.mostrarLoading) View.VISIBLE else View.GONE
        binding.txtEstadoMatchmaking.text = estado.estadoTexto.ifBlank { getString(R.string.matchmaking_estado_default) }
        binding.txtContadorMatchmaking.text = getString(
            R.string.matchmaking_contador_format,
            estado.jogadores.size,
            estado.limite
        )
        binding.txtTempoMatchmaking.text = getString(
            R.string.matchmaking_tempo_format,
            formatarTempo(estado.tempoEsperaSegundos)
        )
        binding.txtJogadoresMatchmaking.text = if (estado.jogadores.isEmpty()) {
            getString(R.string.matchmaking_aguardando)
        } else {
            estado.jogadores.joinToString(separator = "\n") { jogador ->
                val tipo = if (jogador.isGuest) {
                    getString(R.string.matchmaking_tipo_convidado)
                } else {
                    getString(R.string.matchmaking_tipo_conta)
                }
                getString(R.string.matchmaking_jogador_linha, jogador.nomeDisplay, tipo)
            }
        }
        binding.btnCancelarMatchmaking.isEnabled = estado.podeCancelar && !navegando
        binding.btnCancelarMatchmaking.text = when {
            estado.status == MatchmakingStatus.CANCELLED ||
                estado.status == MatchmakingStatus.TIMEOUT ||
                estado.status == MatchmakingStatus.ERROR -> getString(R.string.voltar)
            estado.podeCancelar -> getString(R.string.cancelar)
            else -> getString(R.string.matchmaking_cancelando)
        }
        binding.txtTituloMatchmaking.text = getString(R.string.matchmaking_titulo_modo_format, labelModo)
    }

    private fun tratarEvento(evento: MatchmakingEvent) {
        when (evento) {
            is MatchmakingEvent.AbrirSala1x1 -> abrirSala(SalaDeEspera1x1Activity::class.java, evento.dados)
            is MatchmakingEvent.AbrirSala2x2 -> abrirSala(SalaDeEspera2x2Activity::class.java, evento.dados)
            is MatchmakingEvent.VoltarMain -> voltarMain(evento.dados)
            is MatchmakingEvent.MostrarMensagem -> Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirSala(destino: Class<*>, dados: MatchmakingNavegacaoDados) {
        if (navegando) return
        navegando = true
        val intent = android.content.Intent(this, destino)
        intent.putExtra(IntentExtras.CODIGO_SALA, dados.codigoSala)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, dados.nomeCategoria)
        adicionarDadosJogador(intent, dados)
        startActivity(intent)
        finish()
    }

    private fun voltarMain(dados: MatchmakingNavegacaoDados) {
        if (navegando) return
        navegando = true
        abrirMainActivity(
            this,
            dados.nomeUtilizador.ifBlank { nomeUtilizador },
            dados.nomeJogador.ifBlank { nomeJogador },
            dados.uid.ifBlank { uid }.ifBlank { null }
        )
        finish()
    }

    private fun adicionarDadosJogador(intent: android.content.Intent, dados: MatchmakingNavegacaoDados) {
        dados.uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
        dados.nomeUtilizador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        dados.nomeJogador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        dados.playerKey.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.PLAYER_KEY, it) }
        dados.tipoJogador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.TIPO_JOGADOR, it) }
        dados.avatar.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.AVATAR, it) }
        intent.putExtra(IntentExtras.IS_GUEST, dados.tipoJogador == GameConstants.TIPO_JOGADOR_GUEST)
    }

    private fun formatarTempo(segundosTotais: Int): String {
        val minutos = segundosTotais / 60
        val segundos = segundosTotais % 60
        return "%d:%02d".format(minutos, segundos)
    }
}
