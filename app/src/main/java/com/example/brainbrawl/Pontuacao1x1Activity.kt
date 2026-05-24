package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacao1x1Binding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.viewmodels.Pontuacao1x1Event
import com.example.brainbrawl.viewmodels.Pontuacao1x1Input
import com.example.brainbrawl.viewmodels.Pontuacao1x1ViewModel
import com.example.brainbrawl.viewmodels.PontuacaoJogadorUi

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
    private var totalPerguntas: Int = 8
    private var totalRespostasCertas: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        totalRespostasCertas = intent.getIntExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, 0)
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 8)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        avatar = intent.getStringExtra(IntentExtras.AVATAR) ?: ""
        val categoriaCompetitiva = intent.getBooleanExtra(IntentExtras.CATEGORIA_COMPETITIVA, true)
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
                isGuest = isGuest,
                categoriaCompetitiva = categoriaCompetitiva
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
            renderDuelPodium(state.podio)
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

    private fun criarStats(): PodioStatsUi {
        val total = totalPerguntas.coerceAtLeast(1)
        val precisao = (totalRespostasCertas * 100 / total).coerceIn(0, 100)
        return PodioStatsUi(
            rodadas = total.toString(),
            precisao = "$precisao%",
            acertos = totalRespostasCertas.toString()
        )
    }

    private fun renderDuelPodium(podio: List<PontuacaoJogadorUi>) {
        val vencedor = podio.getOrNull(0) ?: PontuacaoJogadorUi()
        val segundo = podio.getOrNull(1) ?: PontuacaoJogadorUi(
            nome = getString(R.string.aguardando_adversario_curto)
        )
        val pontosVencedor = vencedor.pontos.toIntOrNull()
        val pontosSegundo = segundo.pontos.toIntOrNull()
        val completo = pontosVencedor != null && pontosSegundo != null
        val empate = completo && pontosVencedor == pontosSegundo

        bindDuelPlayer(
            jogador = vencedor,
            avatarView = binding.imgAvatarPrimeiroDuel,
            nomeView = binding.txtNomePrimeiroDuel,
            pontosView = binding.txtPontosPrimeiroDuel,
            resultadoView = binding.txtResultadoPrimeiroDuel,
            resultado = when {
                vencedor.nome.isBlank() -> ""
                !completo -> getString(R.string.podio_aguardando_resultado)
                empate -> getString(R.string.podio_empate)
                else -> getString(R.string.podio_vitoria)
            },
            resultadoColor = when {
                !completo -> R.color.bb_text_secondary
                empate -> R.color.bb_luso_gold
                else -> R.color.bb_success
            }
        )
        bindDuelPlayer(
            jogador = segundo,
            avatarView = binding.imgAvatarSegundoDuel,
            nomeView = binding.txtNomeSegundoDuel,
            pontosView = binding.txtPontosSegundoDuel,
            resultadoView = binding.txtResultadoSegundoDuel,
            resultado = when {
                segundo.pontos.isBlank() -> getString(R.string.podio_aguardando_resultado)
                empate -> getString(R.string.podio_empate)
                else -> getString(R.string.podio_derrota)
            },
            resultadoColor = when {
                segundo.pontos.isBlank() -> R.color.bb_text_secondary
                empate -> R.color.bb_luso_gold
                else -> R.color.bb_danger
            }
        )
        bindStats(criarStats())
    }

    private fun bindDuelPlayer(
        jogador: PontuacaoJogadorUi,
        avatarView: ImageView,
        nomeView: TextView,
        pontosView: TextView,
        resultadoView: TextView,
        resultado: String,
        resultadoColor: Int
    ) {
        avatarView.setImageResource(AvatarUtils.resolverAvatar(this, jogador.avatar))
        nomeView.text = jogador.nome.ifBlank { getString(R.string.aguardando_jogador_curto) }
        pontosView.text = formatarPontos(jogador.pontos)
        resultadoView.text = resultado
        resultadoView.setTextColor(getColor(resultadoColor))
    }

    private fun bindStats(stats: PodioStatsUi) {
        binding.txtPodioStatPerguntas.text = stats.rodadas
        binding.txtPodioStatPrecisao.text = stats.precisao
        binding.txtPodioStatAcertos.text = stats.acertos
    }

    private fun formatarPontos(valor: String): String {
        val numero = valor.toIntOrNull() ?: return valor.ifBlank { "0" }
        return "%,d".format(numero).replace(',', '.')
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
        intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_1X1)
        intent.putExtra(IntentExtras.ORIGEM_SALA, GameConstants.ORIGEM_CONVITE)
        adicionarExtrasMatchmaking(intent)
        Log.d(
            REMATCH_DEBUG_TAG,
            "1x1 rematch navigate oldRoom=$codigoSala newRoom=$novaSala category=$nomeCategoria " +
                "target=SalaDeEspera1x1Activity playerKey=$playerKey"
        )
        startActivity(intent)
        finish()
    }

    private fun adicionarExtrasMatchmaking(intent: Intent) {
        playerKey.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.PLAYER_KEY, it) }
        tipoJogador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.TIPO_JOGADOR, it) }
        avatar.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.AVATAR, it) }
        intent.putExtra(IntentExtras.IS_GUEST, isGuest)
    }

    private companion object {
        const val REMATCH_DEBUG_TAG = "REMATCH_FLOW_DEBUG"
    }
}
