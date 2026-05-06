package com.example.brainbrawl

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMatchmakingBinding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService

class MatchmakingActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMatchmakingBinding.inflate(layoutInflater) }
    private val authService = AuthService()

    private var uid: String = ""
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var modoJogo: String = GameConstants.MODO_1X1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_1X1

        binding.txtTituloMatchmaking.text = when (modoJogo) {
            GameConstants.MODO_2X2 -> "2x2 Aleatório"
            else -> "1x1 Aleatório"
        }
        binding.txtEstadoMatchmaking.text = "À procura de jogadores..."
        binding.btnCancelarMatchmaking.setOnClickListener { voltarMain() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                voltarMain()
            }
        })
    }

    private fun voltarMain() {
        abrirMainActivity(this, nomeUtilizador, nomeJogador, uid.ifBlank { null })
        finish()
    }
}
