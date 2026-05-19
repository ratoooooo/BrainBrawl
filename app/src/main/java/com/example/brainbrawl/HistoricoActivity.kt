package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityHistoricoBinding
import com.example.brainbrawl.routes.BottomNavHelper
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.HistoricoUiState
import com.example.brainbrawl.viewmodels.HistoricoViewModel

class HistoricoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityHistoricoBinding.inflate(layoutInflater) }
    private val viewModel by lazy { ViewModelProvider(this)[HistoricoViewModel::class.java] }
    private val adapter = HistoricoAdapter()
    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: ""
        val email = intent.getStringExtra(IntentExtras.EMAIL) ?: authService.utilizadorAtual()?.email ?: ""
        BottomNavHelper.instalar(this, BottomNavHelper.Item.HISTORICO, uid, nomeUtilizador, nomeJogador, email)
        binding.recyclerHistorico.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistorico.adapter = adapter
        binding.btnVoltarHistorico.setOnClickListener { finish() }
        viewModel.estado.observe(this) { mostrarEstado(it) }
        viewModel.carregarHistorico(uid)
    }

    private fun mostrarEstado(estado: HistoricoUiState) {
        binding.progressHistorico.visibility = if (estado.carregando) View.VISIBLE else View.GONE
        binding.txtMensagemHistorico.visibility = if (estado.mensagem.isBlank()) View.GONE else View.VISIBLE
        binding.txtMensagemHistorico.text = estado.mensagem
        adapter.submeterJogos(estado.jogos)
    }
}
