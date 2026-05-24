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
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera1x1Binding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.viewmodels.Sala1x1Event
import com.example.brainbrawl.viewmodels.SalaJogadorUiState
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
            FLOW_TAG,
            "mode=1x1 room=$codigoSala onCreate uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()} " +
                "categoryIntent=${nomeCategoria.ifBlank { "<empty>" }} type=$tipoJogador path=sala_1x1/$codigoSala"
        )
        Log.d(
            FLOW_TAG,
            "mode=1x1 room=$codigoSala intentExtras=${
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

        // Listener para o clique no botão de iniciar jogo
        binding.btnIniciarJogo.setOnClickListener {
            inicioJogoEmCurso = !salaMatchmakingAtual
            Log.d(
                HOST_REMOVAL_TAG,
                "mode=1x1 room=$codigoSala startPressed uid=${uid.maskedLogId()} " +
                    "playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()} " +
                    "matchmaking=$salaMatchmakingAtual cleanupBlocked=$inicioJogoEmCurso method=acaoPrincipal"
            )
            binding.btnIniciarJogo.isEnabled = false
            viewModel.acaoPrincipal(codigoSala)
        }

        binding.btnSairSala.setOnClickListener {
            Log.d(
                HOST_REMOVAL_TAG,
                "mode=1x1 room=$codigoSala leaveButton uid=${uid.maskedLogId()} " +
                    "startInProgress=$inicioJogoEmCurso navigating=$aNavegarParaJogo"
            )
            binding.btnSairSala.isEnabled = false
            sairDaSala()
        }
    }

    override fun onPause() {
        Log.d(
            HOST_REMOVAL_TAG,
            "mode=1x1 room=$codigoSala onPause cleanupTriggered=false " +
                "startInProgress=$inicioJogoEmCurso navigating=$aNavegarParaJogo"
        )
        super.onPause()
    }

    override fun onStop() {
        Log.d(
            HOST_REMOVAL_TAG,
            "mode=1x1 room=$codigoSala onStop cleanupTriggered=false " +
                "startInProgress=$inicioJogoEmCurso navigating=$aNavegarParaJogo"
        )
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(
            HOST_REMOVAL_TAG,
            "mode=1x1 room=$codigoSala onDestroy cleanupTriggered=false uid=${uid.maskedLogId()} " +
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

    private fun atualizarEstadoSala(estado: SalaCompetitivaUiState) {
        salaMatchmakingAtual = estado.matchmaking
        origemSalaAtual = estado.origemSala
        chaveJogadorAtual = estado.chaveJogadorAtual.ifBlank { chaveJogadorAtual }
        if (estado.avatarJogadorAtual.isNotBlank()) {
            avatar = estado.avatarJogadorAtual
        }
        if (estado.nomeCategoria.isNotBlank()) {
            nomeCategoria = estado.nomeCategoria
        }
        Log.d(
            FLOW_TAG,
            "mode=1x1 room=$codigoSala state uiKey=${chaveJogadorAtual.maskedLogId()} " +
                "admin=${estado.admin} canStart=${estado.podeIniciar} " +
                "roomType=${estado.origemSala.ifBlank { "<empty>" }} matchmaking=${estado.matchmaking} " +
                "category=$nomeCategoria players=${estado.jogadoresDetalhe.map { it.chave.maskedLogId() }}"
        )
        binding.txtListaJogadores.text = if (estado.jogadores.isEmpty()) {
            getString(R.string.aguardando_jogadores)
        } else {
            estado.jogadores.joinToString(separator = "\n")
        }
        atualizarCabecalho(estado)
        atualizarSlotsJogadores(estado)
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
            binding.txtCodigoSala.text = codigoSala
            binding.btnCopiarCodigoSala.visibility = View.VISIBLE
        } else {
            binding.layoutCodigoSala.visibility = View.GONE
            binding.btnCopiarCodigoSala.visibility = View.GONE
        }
    }

    private fun atualizarCabecalho(estado: SalaCompetitivaUiState) {
        if (estado.matchmaking) {
            binding.txtTituloSala.text = getString(R.string.matchmaking_1x1_header)
            binding.txtStatusTitulo.text = getString(R.string.sala_status_matchmaking_1x1)
            binding.txtStatusDescricao.text = getString(R.string.sala_status_matchmaking_1x1_desc)
            binding.txtFooterInfo.text = getString(R.string.o_jogo_comeca_ambos_prontos)
        } else {
            binding.txtTituloSala.text = getString(R.string.sala_de_espera_1x1)
            binding.txtStatusTitulo.text = getString(R.string.sala_status_aguardar_adversario)
            binding.txtStatusDescricao.text = getString(R.string.sala_status_aguardar_adversario_desc)
            binding.txtFooterInfo.text = getString(R.string.o_jogo_comeca_ambos_prontos)
        }
    }

    private fun atualizarSlotsJogadores(estado: SalaCompetitivaUiState) {
        val primeiro = estado.jogadores.getOrNull(0)?.toJogadorSala()
        val segundo = estado.jogadores.getOrNull(1)?.toJogadorSala()
        val primeiroDetalhe = estado.jogadoresDetalhe.getOrNull(0)
        val segundoDetalhe = estado.jogadoresDetalhe.getOrNull(1)
        binding.txtJogador1Nome.text = primeiroDetalhe?.nomeDisplay
            ?: primeiro?.nome?.ifBlank { nomeJogador.ifBlank { nomeUtilizador } }
            ?: nomeJogador.ifBlank { nomeUtilizador }
        binding.txtJogador1Estado.text = primeiroDetalhe?.estadoPronto()
            ?: primeiro?.estado ?: if (estado.jogadorPronto) {
            getString(R.string.pronto_estado)
        } else {
            getString(R.string.aguardando_estado)
        }
        binding.txtJogador2Nome.text = segundoDetalhe?.nomeDisplay
            ?: segundo?.nome ?: getString(R.string.aguardando_adversario_curto)
        binding.txtJogador2Estado.text = segundoDetalhe?.estadoPronto()
            ?: segundo?.estado ?: getString(R.string.aguardando_estado)
        bindingSlotAvatar(binding.imgJogador1Avatar, primeiroDetalhe, slot = "current", fallbackAvatar = avatar)
        bindingSlotAvatar(binding.imgJogador2Avatar, segundoDetalhe, slot = "opponent")
    }

    private fun SalaJogadorUiState.estadoPronto(): String {
        return if (pronto) getString(R.string.pronto_estado) else getString(R.string.aguardando_estado)
    }

    private fun bindingSlotAvatar(
        imageView: ImageView,
        jogador: SalaJogadorUiState?,
        slot: String,
        fallbackAvatar: String = ""
    ) {
        if (jogador == null) {
            imageView.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_game_type_icon, theme)
            imageView.setPadding(dp(16), dp(16), dp(16), dp(16))
            imageView.setImageResource(R.drawable.ic_person)
            ImageViewCompat.setImageTintList(
                imageView,
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bb_luso_navy))
            )
            Log.d(
                AVATAR_TAG,
                "bind room1x1 codigo=$codigoSala slot=$slot placeholder=true source=sala_1x1/$codigoSala/jogadores"
            )
            return
        }

        val avatarEfetivo = jogador.avatar.ifBlank { fallbackAvatar }
        imageView.background = null
        imageView.setPadding(0, 0, 0, 0)
        ImageViewCompat.setImageTintList(imageView, null)
        imageView.setImageResource(AvatarUtils.resolverAvatar(this, avatarEfetivo))
        Log.d(
            AVATAR_TAG,
            "bind room1x1 codigo=$codigoSala slot=$slot chave=${jogador.chave.maskedLogId()} " +
                "playerKey=${jogador.playerKey.maskedLogId()} uid=${jogador.uid.maskedLogId()} " +
                "username=${jogador.nomeDisplay} avatar=${avatarEfetivo.ifBlank { "<empty>" }} " +
                "source=sala_1x1/$codigoSala/jogadores/${jogador.chave}/avatar"
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun String.toJogadorSala(): JogadorSalaSlot {
        val partes = split("·", limit = 2).map { it.trim() }
        return JogadorSalaSlot(
            nome = partes.getOrNull(0).orEmpty(),
            estado = partes.getOrNull(1) ?: getString(R.string.aguardando_estado)
        )
    }

    private fun tratarEvento(evento: Sala1x1Event) {
        when (evento) {
            Sala1x1Event.JogoIniciado -> {
                aNavegarParaJogo = true
                Log.d(
                    HOST_REMOVAL_TAG,
                        "mode=1x1 room=$codigoSala navigateGame uid=${uid.maskedLogId()} " +
                        "playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()} " +
                        "category=$nomeCategoria cleanupSkipped=true origin=${origemSalaAtual.ifBlank { GameConstants.ORIGEM_CONVITE }}"
                )
                val intent = Intent(this@SalaDeEspera1x1Activity, Jogo1x1Activity::class.java)
                intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
                uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
                intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_1X1)
                intent.putExtra(
                    IntentExtras.ORIGEM_SALA,
                    origemSalaAtual.ifBlank { if (salaMatchmakingAtual) GameConstants.ORIGEM_MATCHMAKING else GameConstants.ORIGEM_CONVITE }
                )
                adicionarExtrasJogador(intent)
                startActivity(intent)
                finish()
            }
            Sala1x1Event.SalaEncerrada -> {
                inicioJogoEmCurso = false
                Log.w(
                    HOST_REMOVAL_TAG,
                    "mode=1x1 room=$codigoSala navigateMainReason=room_closed uid=${uid.maskedLogId()} " +
                        "resolvedKey=${chaveJogadorAtual.maskedLogId()}"
                )
                Toast.makeText(this@SalaDeEspera1x1Activity, R.string.sala_encerrada, Toast.LENGTH_SHORT).show()
                abrirMainActivity(this@SalaDeEspera1x1Activity, nomeUtilizador, nomeJogador, uid.ifBlank { null })
                finish()
            }
            Sala1x1Event.AguardarAdversario ->
                Toast.makeText(this, R.string.aguardar_adversario, Toast.LENGTH_SHORT).show()
            Sala1x1Event.JogadoresNaoProntos ->
                Toast.makeText(this@SalaDeEspera1x1Activity, R.string.ambos_jogadores_na_sala, Toast.LENGTH_SHORT).show()
            Sala1x1Event.OponenteSaiu -> {
                val estavaAIniciar = inicioJogoEmCurso
                inicioJogoEmCurso = false
                val mensagem = if (salaMatchmakingAtual) {
                    R.string.sala_espera_jogador_saiu
                } else {
                    R.string.sala_espera_jogador_saiu_convite
                }
                Log.w(
                    FLOW_TAG,
                    "mode=1x1 room=$codigoSala showPlayerLeftMessage " +
                        "roomType=${origemSalaAtual.ifBlank { if (salaMatchmakingAtual) GameConstants.ORIGEM_MATCHMAKING else GameConstants.ORIGEM_CONVITE }} " +
                        "matchmaking=$salaMatchmakingAtual startInProgress=$estavaAIniciar " +
                        "uid=${uid.maskedLogId()} playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()}"
                )
                Toast.makeText(this@SalaDeEspera1x1Activity, mensagem, Toast.LENGTH_LONG).show()
            }
            Sala1x1Event.EntradaBloqueada -> {
                inicioJogoEmCurso = false
                Log.w(
                    HOST_REMOVAL_TAG,
                    "mode=1x1 room=$codigoSala navigateMainReason=entry_blocked uid=${uid.maskedLogId()} " +
                        "playerKey=${playerKey.maskedLogId()} resolvedKey=${chaveJogadorAtual.maskedLogId()}"
                )
                Toast.makeText(this@SalaDeEspera1x1Activity, R.string.sala_1x1_cheia, Toast.LENGTH_SHORT).show()
                abrirMainActivity(this@SalaDeEspera1x1Activity, nomeUtilizador, nomeJogador, uid.ifBlank { null })
                finish()
            }
        }
        if (evento == Sala1x1Event.AguardarAdversario ||
            evento == Sala1x1Event.JogadoresNaoProntos ||
            evento == Sala1x1Event.OponenteSaiu
        ) {
            inicioJogoEmCurso = false
            binding.btnIniciarJogo.isEnabled = viewModel.estado.value?.podeIniciar == true
        }
    }

    private fun sairDaSala() {
        if (saidaJaProcessada || aNavegarParaJogo || inicioJogoEmCurso) {
            Log.d(
                HOST_REMOVAL_TAG,
                "mode=1x1 room=$codigoSala sairDaSala skipped uid=${uid.maskedLogId()} " +
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
            "mode=1x1 room=$codigoSala sairDaSala cleanupTriggered=true reason=explicit_leave " +
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

private data class JogadorSalaSlot(
    val nome: String,
    val estado: String
)

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
