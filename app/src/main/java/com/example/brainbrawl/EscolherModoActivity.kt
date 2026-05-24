package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityEscolherModoBinding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala

class EscolherModoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEscolherModoBinding.inflate(layoutInflater) }
    private val authService = AuthService()

    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid

        binding.btnModoClassico.setOnClickListener {
            abrirTipoModo(GameConstants.MODO_CLASSICO)
        }

        binding.btnModoCaotico.setOnClickListener {
            abrirTipoModo(GameConstants.MODO_CAOTICO)
        }

        binding.btnModoEliminatorias.setOnClickListener {
            abrirTipoModo(GameConstants.MODO_ELIMINATORIAS)
        }

        binding.btnBackHeader.setOnClickListener { voltarAoInicio() }
        binding.btnVoltar.setOnClickListener { voltarAoInicio() }
    }

    private fun abrirTipoModo(modo: String) {
        val intent = Intent(this, TipoModoClassico::class.java)
        intent.putExtra(IntentExtras.MODO_JOGO, modo)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.ADMIN, true)
        intent.putExtra(IntentExtras.CODIGO_SALA, gerarCodigoSala())
        startActivity(intent)
    }

    private fun voltarAoInicio() {
        val intent = Intent(this, MainActivity::class.java)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.ADMIN, false)
        startActivity(intent)
        finish()
    }
}
