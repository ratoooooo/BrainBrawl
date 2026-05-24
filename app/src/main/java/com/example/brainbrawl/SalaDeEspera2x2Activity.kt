package com.example.brainbrawl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera2x2Binding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.viewmodels.Sala2x2Event
import com.example.brainbrawl.viewmodels.Sala2x2UiState
import com.example.brainbrawl.viewmodels.Sala2x2ViewModel
import com.example.brainbrawl.viewmodels.SalaJogadorUiState

class SalaDeEspera2x2Activity : AppCompatActivity() {

    private val binding by lazy {
        ActivitySalaDeEspera2x2Binding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[Sala2x2ViewModel::class.java]
    }

    private val authService = AuthService()

    private lateinit var codigoSala: String
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var nomeJogador: String = ""
    private var categoria: String? = null
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var avatar: String = ""
    private var aNavegarParaJogo = false
    private var inicioJogoEmCurso = false
    private var saidaJaProcessada = false
    private var salaMatchmakingAtual = false
    private var origemSalaAtual = ""
    private var chaveJogadorAtual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this) {
            sairDaSala()
        }

        codigoSala = CodigoSalaUtils.normalizarCodigo(intent.getStringExtra(IntentExtras.CODIGO_SALA).orEmpty())
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
                    ?: ""

        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
            ?: nomeUtilizador

        categoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)
            ?: intent.getStringExtra(IntentExtras.CATEGORIA_LEGACY)
                    ?: getString(R.string.categoria5)
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        avatar = intent.getStringExtra(IntentExtras.AVATAR) ?: ""
        Log.d(
            FLOW_TAG,
            "mode=2x2 room=$codigoSala onCreate uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()} " +
                "categoryIntent=${categoria.orEmpty().ifBlank { "<empty>" }} type=$tipoJogador path=sala_2x2/$codigoSala"
        )
        Log.d(
            FLOW_TAG,
            "mode=2x2 room=$codigoSala intentExtras=${
                intent.extras?.keySet()?.associateWith { key -> intent.extras?.getString(key).orEmpty() } ?: emptyMap()
            }"
        )

        binding.txtCodigoSala.text = getString(R.string.a_carregar_sala)
        binding.btnCopiarCodigoSala.visibility = View.GONE
        binding.btnCopiarCodigoSala.setOnClickListener {
            copiarCodigoSala()
        }
        binding.btnBackHeader.setOnClickListener {
            sairDaSala()
        }

        configurarObservers()

        viewModel.carregarExposicaoCodigo(codigoSala)
        viewModel.iniciar(
            codigoSala,
            uid,
            nomeUtilizador,
            nomeJogador,
            playerKey,
            tipoJogador,
            avatar,
            getString(R.string.categoria5)
        )
        viewModel.observarJogadores(codigoSala)
        viewModel.observarEstadoSala(codigoSala)
        viewModel.observarSalaApagada(codigoSala)

        binding.btnIniciarJogo.setOnClickListener {
            inicioJogoEmCurso = !salaMatchmakingAtual
            Log.d(
                HOST_REMOVAL_TAG,
                "mode=2x2 room=$codigoSala startPressed uid=${uid.maskedLogId()} " +
                    "playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()} " +
                    "matchmaking=$salaMatchmakingAtual cleanupBlocked=$inicioJogoEmCurso method=iniciarJogo"
            )
            binding.btnIniciarJogo.isEnabled = false
            viewModel.iniciarJogo(codigoSala)
        }

        binding.btnSairSala.setOnClickListener {
            Log.d(
                HOST_REMOVAL_TAG,
                "mode=2x2 room=$codigoSala leaveButton uid=${uid.maskedLogId()} " +
                    "startInProgress=$inicioJogoEmCurso navigating=$aNavegarParaJogo"
            )
            binding.btnSairSala.isEnabled = false
            sairDaSala()
        }
    }

    override fun onPause() {
        Log.d(
            HOST_REMOVAL_TAG,
            "mode=2x2 room=$codigoSala onPause cleanupTriggered=false " +
                "startInProgress=$inicioJogoEmCurso navigating=$aNavegarParaJogo"
        )
        super.onPause()
    }

    override fun onStop() {
        Log.d(
            HOST_REMOVAL_TAG,
            "mode=2x2 room=$codigoSala onStop cleanupTriggered=false " +
                "startInProgress=$inicioJogoEmCurso navigating=$aNavegarParaJogo"
        )
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(
            HOST_REMOVAL_TAG,
            "mode=2x2 room=$codigoSala onDestroy cleanupTriggered=false uid=${uid.maskedLogId()} " +
                "playerKey=${playerKey.maskedLogId()} startInProgress=$inicioJogoEmCurso " +
                "navigating=$aNavegarParaJogo leaveProcessed=$saidaJaProcessada"
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

    private fun atualizarEstadoSala(estado: Sala2x2UiState) {
        salaMatchmakingAtual = estado.matchmaking
        origemSalaAtual = estado.origemSala
        chaveJogadorAtual = estado.chaveJogadorAtual.ifBlank { chaveJogadorAtual }
        if (estado.avatarJogadorAtual.isNotBlank()) {
            avatar = estado.avatarJogadorAtual
        }
        if (estado.nomeCategoria.isNotBlank()) {
            categoria = estado.nomeCategoria
        }
        Log.d(
            FLOW_TAG,
            "mode=2x2 room=$codigoSala state uiKey=${chaveJogadorAtual.maskedLogId()} " +
                "canStart=${estado.podeIniciar} roomType=${estado.origemSala.ifBlank { "<empty>" }} " +
                "matchmaking=${estado.matchmaking} category=${categoria.orEmpty()} " +
                "teamA=${estado.equipaADetalhe.map { it.chave.maskedLogId() }} " +
                "teamB=${estado.equipaBDetalhe.map { it.chave.maskedLogId() }}"
        )
        atualizarCabecalho(estado)
        val jogadorA1 = estado.equipaADetalhe.getOrNull(0)
        val jogadorA2 = estado.equipaADetalhe.getOrNull(1)
        val jogadorB1 = estado.equipaBDetalhe.getOrNull(0)
        val jogadorB2 = estado.equipaBDetalhe.getOrNull(1)
        binding.txtJogadorA1.text = jogadorA1?.nomeComEstado() ?: estado.equipaA.getOrNull(0) ?: getString(R.string.aguardando_jogador_curto)
        binding.txtJogadorA2.text = jogadorA2?.nomeComEstado() ?: estado.equipaA.getOrNull(1) ?: getString(R.string.aguardando_jogador_curto)
        binding.txtJogadorB1.text = jogadorB1?.nomeComEstado() ?: estado.equipaB.getOrNull(0) ?: getString(R.string.aguardando_jogador_curto)
        binding.txtJogadorB2.text = jogadorB2?.nomeComEstado() ?: estado.equipaB.getOrNull(1) ?: getString(R.string.aguardando_jogador_curto)
        bindingSlotAvatar(binding.imgJogadorA1Avatar, jogadorA1, slot = "teamA_1")
        bindingSlotAvatar(binding.imgJogadorA2Avatar, jogadorA2, slot = "teamA_2")
        bindingSlotAvatar(binding.imgJogadorB1Avatar, jogadorB1, slot = "teamB_1")
        bindingSlotAvatar(binding.imgJogadorB2Avatar, jogadorB2, slot = "teamB_2")
        atualizarCodigoSala(estado)
        binding.btnIniciarJogo.isEnabled = estado.podeIniciar
        binding.btnIniciarJogo.text = when {
            estado.matchmaking && estado.jogadorPronto -> getString(R.string.pronto_confirmado)
            estado.matchmaking -> getString(R.string.pronto)
            else -> getString(R.string.iniciar_jogo)
        }
    }

    private fun SalaJogadorUiState.nomeComEstado(): String {
        return if (pronto) {
            "$nomeDisplay · ${getString(R.string.pronto_estado)}"
        } else {
            "$nomeDisplay · ${getString(R.string.aguardando_estado)}"
        }
    }

    private fun bindingSlotAvatar(
        imageView: ImageView,
        jogador: SalaJogadorUiState?,
        slot: String
    ) {
        if (jogador == null) {
            imageView.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_game_type_icon, theme)
            imageView.setPadding(dp(10), dp(10), dp(10), dp(10))
            imageView.setImageResource(R.drawable.ic_person)
            ImageViewCompat.setImageTintList(
                imageView,
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bb_luso_navy))
            )
            Log.d(
                AVATAR_TAG,
                "bind room2x2 codigo=$codigoSala slot=$slot placeholder=true source=sala_2x2/$codigoSala/jogadores"
            )
            return
        }

        imageView.background = null
        imageView.setPadding(0, 0, 0, 0)
        ImageViewCompat.setImageTintList(imageView, null)
        imageView.setImageResource(AvatarUtils.resolverAvatar(this, jogador.avatar))
        Log.d(
            AVATAR_TAG,
            "bind room2x2 codigo=$codigoSala slot=$slot chave=${jogador.chave.maskedLogId()} " +
                "playerKey=${jogador.playerKey.maskedLogId()} uid=${jogador.uid.maskedLogId()} " +
                "username=${jogador.nomeDisplay} avatar=${jogador.avatar.ifBlank { "<empty>" }} " +
                "source=sala_2x2/$codigoSala/jogadores/${jogador.chave}/avatar"
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun atualizarCodigoSala(estado: Sala2x2UiState) {
        if (estado.codigoSalaVisivel) {
            binding.layoutCodigoSala.visibility = View.VISIBLE
            binding.txtCodigoSala.text = codigoSala
            binding.btnCopiarCodigoSala.visibility = View.VISIBLE
        } else {
            binding.layoutCodigoSala.visibility = View.GONE
            binding.btnCopiarCodigoSala.visibility = View.GONE
        }
    }

    private fun atualizarCabecalho(estado: Sala2x2UiState) {
        if (estado.matchmaking) {
            binding.txtTituloSala.text = getString(R.string.matchmaking_2x2_header)
            binding.txtStatusTitulo.text = getString(R.string.sala_status_2x2_matchmaking)
            binding.txtStatusDescricao.text = getString(R.string.sala_status_2x2_matchmaking_desc)
            binding.txtFooterInfo.text = getString(R.string.o_jogo_comeca_quatro_prontos)
        } else {
            binding.txtTituloSala.text = getString(R.string.sala_de_espera_2x2)
            binding.txtStatusTitulo.text = getString(R.string.sala_status_2x2_invite)
            binding.txtStatusDescricao.text = getString(R.string.sala_status_2x2_invite_desc)
            binding.txtFooterInfo.text = getString(R.string.o_jogo_comeca_todos_prontos)
        }
    }

    private fun tratarEvento(evento: Sala2x2Event) {
        when (evento) {
            Sala2x2Event.JogoIniciado -> {
                aNavegarParaJogo = true
                Log.d(
                    HOST_REMOVAL_TAG,
                        "mode=2x2 room=$codigoSala navigateGame uid=${uid.maskedLogId()} " +
                        "playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()} " +
                        "category=${categoria.orEmpty()} cleanupSkipped=true origin=${origemSalaAtual.ifBlank { GameConstants.ORIGEM_CONVITE }}"
                )
                val intent = Intent(this@SalaDeEspera2x2Activity, Jogo2x2Activity::class.java)

                intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)

                uid.takeIf { it.isNotBlank() }?.let {
                    intent.putExtra(IntentExtras.UID, it)
                }

                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_2X2)
                intent.putExtra(
                    IntentExtras.ORIGEM_SALA,
                    origemSalaAtual.ifBlank { if (salaMatchmakingAtual) GameConstants.ORIGEM_MATCHMAKING else GameConstants.ORIGEM_CONVITE }
                )
                adicionarExtrasJogador(intent)

                categoria?.let {
                    intent.putExtra(IntentExtras.NOME_CATEGORIA, it)
                    intent.putExtra(IntentExtras.CATEGORIA_LEGACY, it)
                }

                startActivity(intent)
                finish()
            }

            Sala2x2Event.SalaEncerrada -> {
                inicioJogoEmCurso = false
                Log.w(
                    HOST_REMOVAL_TAG,
                    "mode=2x2 room=$codigoSala navigateMainReason=room_closed uid=${uid.maskedLogId()} " +
                        "resolvedKey=${chaveJogadorAtual.maskedLogId()}"
                )
                abrirMainActivity(
                    this@SalaDeEspera2x2Activity,
                    nomeUtilizador,
                    nomeJogador,
                    uid.ifBlank { null }
                )
                finish()
            }

            Sala2x2Event.ErroIniciarJogo -> {
                inicioJogoEmCurso = false
                Toast.makeText(this, R.string.erro_iniciar_jogo_2x2, Toast.LENGTH_SHORT).show()
                binding.btnIniciarJogo.isEnabled = viewModel.estado.value?.podeIniciar == true
            }

            Sala2x2Event.JogadoresNaoProntos -> {
                inicioJogoEmCurso = false
                Toast.makeText(this, R.string.sala_2x2_nem_todos_prontos, Toast.LENGTH_SHORT).show()
                binding.btnIniciarJogo.isEnabled = viewModel.estado.value?.podeIniciar == true
            }

            Sala2x2Event.OponenteSaiu -> {
                val estavaAIniciar = inicioJogoEmCurso
                inicioJogoEmCurso = false
                val mensagem = if (salaMatchmakingAtual) {
                    R.string.sala_espera_jogador_saiu_2x2
                } else {
                    R.string.sala_espera_jogador_saiu_2x2_convite
                }
                Log.w(
                    FLOW_TAG,
                    "mode=2x2 room=$codigoSala showPlayerLeftMessage " +
                        "roomType=${origemSalaAtual.ifBlank { if (salaMatchmakingAtual) GameConstants.ORIGEM_MATCHMAKING else GameConstants.ORIGEM_CONVITE }} " +
                        "matchmaking=$salaMatchmakingAtual startInProgress=$estavaAIniciar " +
                        "uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()}"
                )
                Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
            }

            Sala2x2Event.EntradaBloqueada -> {
                inicioJogoEmCurso = false
                Log.w(
                    HOST_REMOVAL_TAG,
                    "mode=2x2 room=$codigoSala navigateMainReason=entry_blocked uid=${uid.maskedLogId()} " +
                        "playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()}"
                )
                Toast.makeText(this, R.string.sala_2x2_cheia, Toast.LENGTH_SHORT).show()
                abrirMainActivity(
                    this@SalaDeEspera2x2Activity,
                    nomeUtilizador,
                    nomeJogador,
                    uid.ifBlank { null }
                )
                finish()
            }
        }
    }

    private fun sairDaSala() {
        if (saidaJaProcessada || aNavegarParaJogo || inicioJogoEmCurso) {
            Log.d(
                HOST_REMOVAL_TAG,
                "mode=2x2 room=$codigoSala sairDaSala skipped uid=${uid.maskedLogId()} " +
                    "reason=${when {
                        aNavegarParaJogo -> "navigating_to_game"
                        inicioJogoEmCurso -> "start_in_progress"
                        else -> "already_processed"
                    }} cleanupTriggered=false"
            )
            return
        }
        saidaJaProcessada = true
        Log.d(
            HOST_REMOVAL_TAG,
            "mode=2x2 room=$codigoSala sairDaSala cleanupTriggered=true reason=explicit_leave " +
                "uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()}"
        )
        viewModel.sairDaSala(codigoSala, "explicit_leave")
        abrirMainActivity(this, nomeUtilizador, nomeJogador, uid.ifBlank { null })
        finish()
    }

    private fun adicionarExtrasJogador(intent: Intent) {
        val chave = chaveJogadorAtual.ifBlank { playerKey }
        chave.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.PLAYER_KEY, it) }
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
        const val START_TAG = "INVITE_START_ROOT_CAUSE"
        const val HOST_REMOVAL_TAG = "HOST_REMOVAL_DEBUG"
        const val AVATAR_TAG = "WAITING_ROOM_AVATAR_DEBUG"
        const val FLOW_TAG = "FLOW_SEPARATION_DEBUG"
    }
}

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
