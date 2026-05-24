package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacaoBinding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.PontuacoesInput
import com.example.brainbrawl.viewmodels.PontuacoesViewModel

class PontuacoesActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPontuacaoBinding.inflate(layoutInflater)
    }
    private val authService = AuthService()
    private val viewModel by lazy {
        ViewModelProvider(this)[PontuacoesViewModel::class.java]
    }

    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeJogador: String
    private var uid: String = ""
    private var totalPerguntas: Int = 1
    private var totalRespostasCertas: Int = 0
    private var podioRenderer: PodioUiRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: "Jogador"
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        val nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 1)
        val modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: ""
        val totalPontos = intent.getDoubleExtra(IntentExtras.TOTAL_PONTOS, 0.0)
        totalRespostasCertas = intent.getIntExtra(
            IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY,
            intent.getIntExtra(IntentExtras.RESPOSTAS_CERTAS, 0)
        )
        val modoSolo = intent.getBooleanExtra(IntentExtras.MODO_SOLO, false) || codigoSala.isBlank()
        val partidaId = intent.getStringExtra(IntentExtras.PARTIDA_ID).orEmpty()
        val categoriaCompetitiva = intent.getBooleanExtra(IntentExtras.CATEGORIA_COMPETITIVA, true)
        val isAdmin = intent.getBooleanExtra(IntentExtras.ADMIN, false)
        val tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        val isGuest = intent.getBooleanExtra(
            IntentExtras.IS_GUEST,
            tipoJogador == GameConstants.TIPO_JOGADOR_GUEST
        )
        val avatar = intent.getStringExtra(IntentExtras.AVATAR).orEmpty()

        podioRenderer = PodioUiRenderer(binding.root)
        configurarObservers()
        viewModel.iniciar(
            PontuacoesInput(
                codigoSala = codigoSala,
                uid = uid,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                nomeCategoria = nomeCategoria,
                totalPerguntas = totalPerguntas,
                modoJogo = modoJogo,
                totalPontos = totalPontos,
                totalRespostasCertas = totalRespostasCertas,
                modoSolo = modoSolo,
                partidaId = partidaId,
                categoriaCompetitiva = categoriaCompetitiva,
                tipoJogador = tipoJogador,
                isGuest = isGuest,
                isHostOnly = isAdmin,
                avatar = avatar
            )
        )

        binding.btnVoltar.setOnClickListener {
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador, uid.ifBlank { null })
            finish()
        }
    }

    private fun configurarObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.txtEstadoPodio.text = state.mensagem
            binding.txtEstadoPodio.visibility = if (state.mensagem.isBlank()) View.GONE else View.VISIBLE
            podioRenderer?.render(
                jogadores = state.podio.mapIndexed { index, item ->
                    PodioPlayerUi(
                        position = index + 1,
                        nome = item.nome,
                        pontos = item.pontos,
                        avatar = item.avatar
                    )
                },
                stats = criarStats()
            )
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
}
