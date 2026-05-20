package com.example.brainbrawl

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacaoMultiBinding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.Pontuacao2x2Event
import com.example.brainbrawl.viewmodels.Pontuacao2x2Input
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        val nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: "Todas as categorias"
        val equipa = intent.getStringExtra(IntentExtras.EQUIPA)
        val totalRespostasCertas = intent.getIntExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, 0)
        val totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 8)
        val playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        val tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        val categoriaCompetitiva = intent.getBooleanExtra(IntentExtras.CATEGORIA_COMPETITIVA, true)
        val isGuest = intent.getBooleanExtra(IntentExtras.IS_GUEST, false) ||
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
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador.ifBlank { null }, uid.ifBlank { null })
            finish()
        }
    }

    private fun configurarObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.txtNomeJogador1.text = state.podio.getOrNull(0)?.nome.orEmpty()
            binding.txtPontos1.text = state.podio.getOrNull(0)?.pontos.orEmpty()
            binding.txtNomeJogador2.text = state.podio.getOrNull(1)?.nome.orEmpty()
            binding.txtPontos2.text = state.podio.getOrNull(1)?.pontos.orEmpty()
            binding.txtNomeJogador3.text = state.podio.getOrNull(2)?.nome.orEmpty()
            binding.txtPontos3.text = state.podio.getOrNull(2)?.pontos.orEmpty()
            binding.txtNomeJogador4.text = state.podio.getOrNull(3)?.nome.orEmpty()
            binding.txtPontos4.text = state.podio.getOrNull(3)?.pontos.orEmpty()
            binding.txtResultado2x2.text = state.resultado
            binding.txtEstadoPontuacao.text = state.estado
        }

        viewModel.evento.observe(this) { evento ->
            when (evento) {
                is Pontuacao2x2Event.MostrarMensagem -> Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
                null -> Unit
            }
            if (evento != null) viewModel.consumirEvento()
        }
    }
}
