package com.example.brainbrawl

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMatchmakingBinding
import com.example.brainbrawl.models.MatchmakingPlayer
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
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
        Log.d(
            FLOW_TAG,
            "flow=${GameConstants.ORIGEM_MATCHMAKING} mode=$modoJogo room=<search> " +
                "activity=MatchmakingActivity event=open uid=${uid.maskedLogId()} playerKey=${uid.maskedLogId()} " +
                "category=$nomeCategoria"
        )

        binding.txtTituloMatchmaking.text = when (modoJogo) {
            GameConstants.MODO_2X2 -> getString(R.string.matchmaking_2x2_titulo)
            else -> getString(R.string.matchmaking_1x1_titulo)
        }
        configurarObservers()
        binding.btnBackHeader.setOnClickListener { viewModel.cancelar() }
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
        Log.d(
            FLOW_TAG,
            "flow=${GameConstants.ORIGEM_MATCHMAKING} mode=$modoJogo room=<search> " +
                "activity=MatchmakingActivity event=onDestroy navigating=$navegando changing=$isChangingConfigurations " +
                "uid=${uid.maskedLogId()} cleanupTriggered=false"
        )
        if (!isChangingConfigurations) {
            viewModel.removerListeners()
        }
        super.onDestroy()
    }

    override fun onStop() {
        Log.d(
            FLOW_TAG,
            "flow=${GameConstants.ORIGEM_MATCHMAKING} mode=$modoJogo room=<search> " +
                "activity=MatchmakingActivity event=onStop navigating=$navegando uid=${uid.maskedLogId()} cleanupTriggered=false"
        )
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
        binding.progressMatchmaking.visibility = if (estado.mostrarLoading) View.VISIBLE else View.GONE
        binding.txtEstadoMatchmaking.text = estado.estadoTexto.ifBlank {
            getString(R.string.matchmaking_estado_default)
        }
        val jogadoresVisiveis = estado.jogadores
            .distinctBy { it.playerKey }
            .take(estado.limite)
        binding.txtContadorMatchmaking.text = getString(
            R.string.matchmaking_contador_curto_format,
            jogadoresVisiveis.size,
            estado.limite
        )
        binding.txtTempoMatchmaking.text = formatarTempo(estado.tempoEsperaSegundos)
        renderizarJogadores(estado, jogadoresVisiveis)
        binding.btnCancelarMatchmaking.isEnabled = estado.podeCancelar && !navegando
        binding.btnCancelarMatchmaking.text = when {
            estado.status == MatchmakingStatus.CANCELLED ||
                estado.status == MatchmakingStatus.TIMEOUT ||
                estado.status == MatchmakingStatus.ERROR -> getString(R.string.voltar)
            estado.podeCancelar -> getString(R.string.cancelar_procura)
            else -> getString(R.string.matchmaking_cancelando)
        }
        binding.txtTituloMatchmaking.text = getString(
            if (estado.modo == GameConstants.MODO_2X2) {
                R.string.matchmaking_2x2_titulo
            } else {
                R.string.matchmaking_1x1_titulo
            }
        )
        binding.txtMatchmakingListaTitulo.text = getString(
            if (estado.modo == GameConstants.MODO_2X2) {
                R.string.matchmaking_equipa
            } else {
                R.string.matchmaking_jogadores_encontrados
            }
        )
        binding.txtMatchmakingNote.text = getString(
            if (estado.modo == GameConstants.MODO_2X2) {
                R.string.matchmaking_footer_2x2
            } else {
                R.string.matchmaking_footer_1x1
            }
        )
    }

    private fun renderizarJogadores(
        estado: MatchmakingUiState,
        jogadoresVisiveis: List<MatchmakingPlayer>
    ) {
        val slots = listOf(
            MatchmakingSlotBinding(
                row = binding.rowMatchPlayer1,
                avatar = binding.imgMatchAvatar1,
                nome = binding.txtMatchNome1,
                subtitulo = binding.txtMatchSub1,
                status = binding.txtMatchStatus1,
                dividerAfter = null
            ),
            MatchmakingSlotBinding(
                row = binding.rowMatchPlayer2,
                avatar = binding.imgMatchAvatar2,
                nome = binding.txtMatchNome2,
                subtitulo = binding.txtMatchSub2,
                status = binding.txtMatchStatus2,
                dividerAfter = binding.dividerMatch3
            ),
            MatchmakingSlotBinding(
                row = binding.rowMatchPlayer3,
                avatar = binding.imgMatchAvatar3,
                nome = binding.txtMatchNome3,
                subtitulo = binding.txtMatchSub3,
                status = binding.txtMatchStatus3,
                dividerAfter = binding.dividerMatch4
            ),
            MatchmakingSlotBinding(
                row = binding.rowMatchPlayer4,
                avatar = binding.imgMatchAvatar4,
                nome = binding.txtMatchNome4,
                subtitulo = binding.txtMatchSub4,
                status = binding.txtMatchStatus4,
                dividerAfter = null
            )
        )

        slots.forEachIndexed { index, slot ->
            val slotDentroDoModo = index < estado.limite
            slot.row.visibility = if (slotDentroDoModo) View.VISIBLE else View.GONE
            slot.dividerAfter?.visibility = if (slotDentroDoModo && index < estado.limite - 1) View.VISIBLE else View.GONE
            if (!slotDentroDoModo) return@forEachIndexed
            renderizarSlot(slot, jogadoresVisiveis.getOrNull(index), index, estado.modo)
        }
    }

    private fun renderizarSlot(
        slot: MatchmakingSlotBinding,
        jogador: MatchmakingPlayer?,
        index: Int,
        modo: String
    ) {
        if (jogador == null) {
            slot.avatar.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_game_type_icon, theme)
            slot.avatar.setPadding(dp(13), dp(13), dp(13), dp(13))
            slot.avatar.setImageResource(R.drawable.ic_person)
            ImageViewCompat.setImageTintList(
                slot.avatar,
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bb_text_secondary))
            )
            slot.nome.text = getString(R.string.a_procurar)
            slot.subtitulo.text = getString(R.string.aguardando_estado)
            slot.status.text = getString(R.string.matchmaking_waiting_dots)
            slot.status.setTextColor(ContextCompat.getColor(this, R.color.bb_luso_gold))
            return
        }

        slot.avatar.background = null
        slot.avatar.setPadding(0, 0, 0, 0)
        ImageViewCompat.setImageTintList(slot.avatar, null)
        slot.avatar.setImageResource(AvatarUtils.resolverAvatar(this, jogador.avatar))
        slot.nome.text = jogador.nomeDisplay
        slot.subtitulo.text = if (jogador.isJogadorAtual()) {
            getString(R.string.matchmaking_voce)
        } else {
            getString(R.string.matchmaking_encontrado)
        }
        slot.status.text = getString(R.string.matchmaking_ready_status)
        slot.status.setTextColor(ContextCompat.getColor(this, R.color.bb_luso_navy))
        Log.d(
            AVATAR_TAG,
            "bind search slot modo=$modo index=$index playerKey=${jogador.playerKey.maskedLogId()} " +
                "uid=${jogador.uid.maskedLogId()} username=${jogador.nomeDisplay} avatar=${jogador.avatar.ifBlank { "<empty>" }} " +
                "current=${jogador.isJogadorAtual()} source=matchmaking/${modo}/fila"
        )
    }

    private fun MatchmakingPlayer.isJogadorAtual(): Boolean {
        return playerKey == this@MatchmakingActivity.uid || uid == this@MatchmakingActivity.uid
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
        Log.d(
            FLOW_TAG,
            "flow=${GameConstants.ORIGEM_MATCHMAKING} mode=${dados.modo} room=${dados.codigoSala} " +
                "activity=MatchmakingActivity event=navigateWaitingRoom uid=${dados.uid.maskedLogId()} " +
                "playerKey=${dados.playerKey.maskedLogId()} cleanupTriggered=false"
        )
        val intent = android.content.Intent(this, destino)
        intent.putExtra(IntentExtras.CODIGO_SALA, dados.codigoSala)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, dados.nomeCategoria)
        intent.putExtra(IntentExtras.ORIGEM_SALA, GameConstants.ORIGEM_MATCHMAKING)
        adicionarDadosJogador(intent, dados)
        startActivity(intent)
        finish()
    }

    private fun voltarMain(dados: MatchmakingNavegacaoDados) {
        if (navegando) return
        navegando = true
        Log.d(
            FLOW_TAG,
            "flow=${GameConstants.ORIGEM_MATCHMAKING} mode=$modoJogo room=<search> " +
                "activity=MatchmakingActivity event=returnMain uid=${dados.uid.ifBlank { uid }.maskedLogId()}"
        )
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
        return "%02d:%02d".format(minutos, segundos)
    }

    private companion object {
        const val TAG = "Matchmaking"
        const val AVATAR_TAG = "MatchmakingAvatar"
        const val FLOW_TAG = "RoomFlow"
    }
}

private data class MatchmakingSlotBinding(
    val row: LinearLayout,
    val avatar: ImageView,
    val nome: TextView,
    val subtitulo: TextView,
    val status: TextView,
    val dividerAfter: View?
)

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
