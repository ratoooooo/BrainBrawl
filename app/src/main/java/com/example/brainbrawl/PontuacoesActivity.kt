package com.example.brainbrawl

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: "Jogador"
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        val nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        val totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 1)
        val modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: ""
        val tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        val isGuest = intent.getBooleanExtra(
            IntentExtras.IS_GUEST,
            tipoJogador == GameConstants.TIPO_JOGADOR_GUEST
        )

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
                tipoJogador = tipoJogador,
                isGuest = isGuest
            )
        )

        binding.btnVoltar.setOnClickListener {
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador, uid.ifBlank { null })
            finish()
        }
    }

    private fun configurarObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.layoutPodio.removeAllViews()
            if (state.mensagem.isNotBlank()) {
                mostrarMensagemPodio(state.mensagem)
            }
            val inflater = LayoutInflater.from(this)
            state.podio.forEach { item ->
                val view = inflater.inflate(R.layout.item_podio, binding.layoutPodio, false)
                val txtMedalha = view.findViewById<TextView>(R.id.txt_medalha)
                val txtNome = view.findViewById<TextView>(R.id.txt_nome_jogador)
                val txtPontos = view.findViewById<TextView>(R.id.txt_pontos)
                txtMedalha.text = item.medalha
                txtMedalha.setTextColor(item.corMedalha.toColorInt())
                txtNome.text = item.nome
                txtPontos.text = item.pontos
                binding.layoutPodio.addView(view)
            }
        }
    }

    private fun mostrarMensagemPodio(mensagem: String) {
        val textView = TextView(this)
        textView.text = mensagem
        textView.textSize = 16f
        textView.setTextColor(Color.BLACK)
        binding.layoutPodio.addView(textView)
    }
}
