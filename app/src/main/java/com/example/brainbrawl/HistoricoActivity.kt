package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityHistoricoBinding
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
        binding.recyclerHistorico.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistorico.adapter = adapter
        binding.btnVoltarHistorico.setOnClickListener { finish() }
        viewModel.estado.observe(this) { mostrarEstado(it) }
        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        viewModel.carregarHistorico(uid)
    }

    private fun mostrarEstado(estado: HistoricoUiState) {
        binding.progressHistorico.visibility = if (estado.carregando) View.VISIBLE else View.GONE
        binding.txtMensagemHistorico.visibility = if (estado.mensagem.isBlank()) View.GONE else View.VISIBLE
        binding.txtMensagemHistorico.text = estado.mensagem
        adapter.submeterJogos(estado.jogos)
    }
}
