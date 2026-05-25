package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacaoMultiBinding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.viewmodels.Pontuacao2x2EquipaUi
import com.example.brainbrawl.viewmodels.Pontuacao2x2Event
import com.example.brainbrawl.viewmodels.Pontuacao2x2Input
import com.example.brainbrawl.viewmodels.Pontuacao2x2ResultadoUi
import com.example.brainbrawl.viewmodels.Pontuacao2x2UiState
import com.example.brainbrawl.viewmodels.Pontuacao2x2ViewModel

class Pontuacao2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPontuacaoMultiBinding.inflate(layoutInflater)
    }
    private val authService = AuthService()
    private val viewModel by lazy {
        ViewModelProvider(this)[Pontuacao2x2ViewModel::class.java]
    }

    private lateinit var codigoSala: String
    private var uid: String = ""
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private var nomeCategoria: String = ""
    private var equipa: String = ""
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var avatar: String = ""
    private var origemSala: String = ""
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
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: "Todas as categorias"
        equipa = intent.getStringExtra(IntentExtras.EQUIPA).orEmpty()
        totalRespostasCertas = intent.getIntExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, 0)
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 8)
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        avatar = intent.getStringExtra(IntentExtras.AVATAR) ?: ""
        origemSala = intent.getStringExtra(IntentExtras.ORIGEM_SALA).orEmpty()
        val categoriaCompetitiva = intent.getBooleanExtra(IntentExtras.CATEGORIA_COMPETITIVA, true)
        isGuest = intent.getBooleanExtra(IntentExtras.IS_GUEST, false) ||
            tipoJogador == GameConstants.TIPO_JOGADOR_GUEST ||
            uid.isBlank()

        configurarObservers()
        viewModel.iniciar(
            Pontuacao2x2Input(
                codigoSala = codigoSala,
                uid = uid,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                nomeCategoria = nomeCategoria,
                equipa = equipa,
                totalRespostasCertas = totalRespostasCertas,
                totalPerguntas = totalPerguntas,
                playerKey = playerKey,
                tipoJogador = tipoJogador,
                isGuest = isGuest,
                categoriaCompetitiva = categoriaCompetitiva
            )
        )

        binding.btnVoltar.setOnClickListener {
            voltarAoInicio()
        }
        binding.btnJogarNovamente2x2.setOnClickListener {
            binding.btnJogarNovamente2x2.isEnabled = false
            viewModel.pedirDesforra()
        }
    }

    private fun configurarObservers() {
        viewModel.uiState.observe(this) { state ->
            renderTeamPodium(state)
        }

        viewModel.evento.observe(this) { evento ->
            when (evento) {
                is Pontuacao2x2Event.MostrarMensagem -> {
                    Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
                    mostrarEstadoDesforra(evento.mensagem)
                }
                is Pontuacao2x2Event.AbrirNovaSalaDesforra -> abrirSalaDesforra2x2(evento.codigoSala)
                null -> Unit
            }
            if (evento != null) viewModel.consumirEvento()
        }
    }

    private fun renderTeamPodium(state: Pontuacao2x2UiState) {
        val primeiraEquipa = state.equipas.getOrNull(0) ?: Pontuacao2x2EquipaUi(equipa = GameConstants.EQUIPA_A)
        val segundaEquipa = state.equipas.getOrNull(1) ?: Pontuacao2x2EquipaUi(equipa = GameConstants.EQUIPA_B)
        val resultadoFinal = primeiraEquipa.resultado != Pontuacao2x2ResultadoUi.AGUARDANDO ||
            segundaEquipa.resultado != Pontuacao2x2ResultadoUi.AGUARDANDO

        binding.txtResultado2x2.text = if (resultadoFinal) {
            getString(R.string.podio_batalha_concluida)
        } else {
            getString(R.string.podio_aguardando_resultado)
        }
        binding.txtEstadoPontuacao.visibility = if (resultadoFinal) View.GONE else View.VISIBLE
        binding.txtEstadoPontuacao.text = getString(R.string.podio_aguardando_resultado)

        bindTeam(
            equipa = primeiraEquipa,
            avatar1 = binding.imgAvatarWinner1,
            avatar2 = binding.imgAvatarWinner2,
            nomeEquipa = binding.txtNomeEquipaWinner,
            nomesJogadores = binding.txtNomesEquipaWinner,
            pontos = binding.txtPontosEquipaWinner,
            resultado = binding.txtResultadoEquipaWinner
        )
        bindTeam(
            equipa = segundaEquipa,
            avatar1 = binding.imgAvatarSecond1,
            avatar2 = binding.imgAvatarSecond2,
            nomeEquipa = binding.txtNomeEquipaSecond,
            nomesJogadores = binding.txtNomesEquipaSecond,
            pontos = binding.txtPontosEquipaSecond,
            resultado = binding.txtResultadoEquipaSecond
        )
        bindStats(criarStats())
    }

    private fun bindTeam(
        equipa: Pontuacao2x2EquipaUi,
        avatar1: ImageView,
        avatar2: ImageView,
        nomeEquipa: TextView,
        nomesJogadores: TextView,
        pontos: TextView,
        resultado: TextView
    ) {
        val jogadores = equipa.jogadores
        val jogador1 = jogadores.getOrNull(0)
        val jogador2 = jogadores.getOrNull(1)

        avatar1.setImageResource(AvatarUtils.resolverAvatar(this, jogador1?.avatar.orEmpty()))
        avatar2.setImageResource(AvatarUtils.resolverAvatar(this, jogador2?.avatar.orEmpty()))
        avatar1.alpha = if (jogador1 == null) 0.42f else 1f
        avatar2.alpha = if (jogador2 == null) 0.42f else 1f

        nomeEquipa.text = textoNomeEquipa(equipa.equipa)
        nomesJogadores.text = nomesDaEquipa(equipa)
        pontos.text = formatarPontos(equipa.pontos)
        resultado.text = textoResultado(equipa.resultado)
        resultado.setTextColor(getColor(corResultado(equipa.resultado)))
    }

    private fun textoNomeEquipa(equipa: String): String {
        return when (equipa) {
            GameConstants.EQUIPA_A -> getString(R.string.equipa_lusa)
            GameConstants.EQUIPA_B -> getString(R.string.os_descobridores)
            else -> getString(R.string.equipa_lusa)
        }
    }

    private fun nomesDaEquipa(equipa: Pontuacao2x2EquipaUi): String {
        return equipa.jogadores
            .mapNotNull { jogador -> jogador.nome.takeIf { it.isNotBlank() } }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "  +  ")
            ?: getString(R.string.aguardando_jogador_curto)
    }

    private fun textoResultado(resultado: Pontuacao2x2ResultadoUi): String {
        return when (resultado) {
            Pontuacao2x2ResultadoUi.VITORIA -> getString(R.string.podio_vitoria)
            Pontuacao2x2ResultadoUi.DERROTA -> getString(R.string.podio_derrota)
            Pontuacao2x2ResultadoUi.EMPATE -> getString(R.string.podio_empate)
            Pontuacao2x2ResultadoUi.AGUARDANDO -> getString(R.string.podio_aguardando_resultado)
        }
    }

    private fun corResultado(resultado: Pontuacao2x2ResultadoUi): Int {
        return when (resultado) {
            Pontuacao2x2ResultadoUi.VITORIA -> R.color.bb_success
            Pontuacao2x2ResultadoUi.DERROTA -> R.color.bb_danger
            Pontuacao2x2ResultadoUi.EMPATE -> R.color.bb_luso_gold
            Pontuacao2x2ResultadoUi.AGUARDANDO -> R.color.bb_text_secondary
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

    private fun bindStats(stats: PodioStatsUi) {
        binding.txtPodioStatPerguntas.text = stats.rodadas
        binding.txtPodioStatPrecisao.text = stats.precisao
        binding.txtPodioStatAcertos.text = stats.acertos
    }

    private fun formatarPontos(valor: String): String {
        val numero = valor.toIntOrNull() ?: return valor.ifBlank { "0" }
        return "%,d".format(numero).replace(',', '.')
    }

    private fun voltarAoInicio() {
        abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador.ifBlank { null }, uid.ifBlank { null })
        finish()
    }

    private fun abrirSalaDesforra2x2(novaSala: String) {
        if (navegouParaDesforra) return
        navegouParaDesforra = true
        val intent = Intent(this, SalaDeEspera2x2Activity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, novaSala)
        uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_2X2)
        intent.putExtra(IntentExtras.ORIGEM_SALA, origemSala.ifBlank { GameConstants.ORIGEM_CONVITE })
        equipa.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.EQUIPA, it) }
        playerKey.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.PLAYER_KEY, it) }
        tipoJogador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.TIPO_JOGADOR, it) }
        avatar.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.AVATAR, it) }
        intent.putExtra(IntentExtras.IS_GUEST, isGuest)
        startActivity(intent)
        finish()
    }

    private fun mostrarEstadoDesforra(mensagem: String) {
        binding.txtEstadoPontuacao.visibility = if (mensagem.isBlank()) View.GONE else View.VISIBLE
        binding.txtEstadoPontuacao.text = mensagem
        if (mensagem.startsWith("Erro") || mensagem.startsWith("Não foi possível")) {
            binding.btnJogarNovamente2x2.isEnabled = true
        }
    }
}
