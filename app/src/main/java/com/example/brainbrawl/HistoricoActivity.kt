package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityHistoricoBinding
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.routes.BottomNavHelper
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.HistoricoUiState
import com.example.brainbrawl.viewmodels.HistoricoViewModel

class HistoricoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityHistoricoBinding.inflate(layoutInflater) }
    private val viewModel by lazy { ViewModelProvider(this)[HistoricoViewModel::class.java] }
    private val adapter = HistoricoAdapter()
    private val authService = AuthService()
    private var jogos: List<HistoricoJogo> = emptyList()
    private var mensagemAtual: String = ""
    private var carregando = false
    private var filtroAtual = HistoricoFiltro.TODOS

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
        configurarFiltros()
        viewModel.estado.observe(this) { mostrarEstado(it) }
        viewModel.carregarHistorico(uid)
    }

    private fun mostrarEstado(estado: HistoricoUiState) {
        carregando = estado.carregando
        jogos = estado.jogos
        mensagemAtual = estado.mensagem
        aplicarFiltro()
    }

    private fun configurarFiltros() {
        binding.btnFiltroTodos.setOnClickListener { selecionarFiltro(HistoricoFiltro.TODOS) }
        binding.btnFiltroOficial.setOnClickListener { selecionarFiltro(HistoricoFiltro.OFICIAL) }
        binding.btnFiltroPersonalizadas.setOnClickListener { selecionarFiltro(HistoricoFiltro.PERSONALIZADAS) }
        atualizarFiltroVisual()
    }

    private fun selecionarFiltro(filtro: HistoricoFiltro) {
        filtroAtual = filtro
        aplicarFiltro()
    }

    private fun aplicarFiltro() {
        binding.progressHistorico.visibility = if (carregando) View.VISIBLE else View.GONE
        val filtrados = when (filtroAtual) {
            HistoricoFiltro.TODOS -> jogos
            HistoricoFiltro.OFICIAL -> jogos.filter { it.competitivo }
            HistoricoFiltro.PERSONALIZADAS -> jogos.filter { !it.competitivo }
        }
        adapter.submeterJogos(filtrados)
        atualizarFiltroVisual()

        val mensagem = when {
            carregando -> ""
            mensagemAtual.isNotBlank() && jogos.isEmpty() -> mensagemAtual
            filtrados.isEmpty() && jogos.isNotEmpty() -> getString(R.string.historico_sem_resultados_filtro)
            else -> ""
        }
        binding.txtMensagemHistorico.visibility = if (mensagem.isBlank()) View.GONE else View.VISIBLE
        binding.txtMensagemHistorico.text = mensagem
    }

    private fun atualizarFiltroVisual() {
        listOf(
            binding.btnFiltroTodos to HistoricoFiltro.TODOS,
            binding.btnFiltroOficial to HistoricoFiltro.OFICIAL,
            binding.btnFiltroPersonalizadas to HistoricoFiltro.PERSONALIZADAS
        ).forEach { (view, filtro) ->
            val selecionado = filtro == filtroAtual
            view.atualizarFiltroSelecionado(selecionado)
        }
    }

    private fun TextView.atualizarFiltroSelecionado(selecionado: Boolean) {
        setBackgroundResource(if (selecionado) R.drawable.bg_luso_segment_selected else R.drawable.bg_luso_segment_unselected)
        setTextColor(
            ContextCompat.getColor(
                this@HistoricoActivity,
                if (selecionado) R.color.bb_primary_text else R.color.bb_luso_navy
            )
        )
    }
}

private enum class HistoricoFiltro {
    TODOS,
    OFICIAL,
    PERSONALIZADAS
}
