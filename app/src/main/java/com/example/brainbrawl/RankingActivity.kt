package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.databinding.ActivityRankingBinding
import com.example.brainbrawl.models.RankingTipo
import com.example.brainbrawl.viewmodels.RankingUiState
import com.example.brainbrawl.viewmodels.RankingViewModel

class RankingActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRankingBinding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[RankingViewModel::class.java]
    }
    private val rankingAdapter = RankingAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.recyclerRanking.layoutManager = LinearLayoutManager(this)
        binding.recyclerRanking.adapter = rankingAdapter

        binding.btnVoltar.setOnClickListener {
            finish()
        }
        binding.btnRankingGlobal.setOnClickListener { viewModel.carregarRanking(RankingTipo.GLOBAL) }
        binding.btnRankingSolo.setOnClickListener { viewModel.carregarRanking(RankingTipo.SOLO) }
        binding.btnRanking1x1.setOnClickListener { viewModel.carregarRanking(RankingTipo.MODO_1X1) }
        binding.btnRanking2x2.setOnClickListener { viewModel.carregarRanking(RankingTipo.MODO_2X2) }

        viewModel.estado.observe(this) { estado ->
            atualizarEstado(estado)
        }
        viewModel.carregarRanking(RankingTipo.GLOBAL)
    }

    private fun atualizarEstado(estado: RankingUiState) {
        binding.progressRanking.visibility = if (estado is RankingUiState.Loading) View.VISIBLE else View.GONE
        binding.recyclerRanking.visibility = if (estado is RankingUiState.Content) View.VISIBLE else View.GONE
        binding.txtEstadoRanking.visibility = if (estado is RankingUiState.Empty || estado is RankingUiState.Error) {
            View.VISIBLE
        } else {
            View.GONE
        }

        when (estado) {
            is RankingUiState.Loading -> {
                atualizarTipoSelecionado(estado.tipo)
                binding.txtTituloRanking.text = estado.tipo.titulo
            }
            is RankingUiState.Empty -> {
                atualizarTipoSelecionado(estado.tipo)
                binding.txtTituloRanking.text = estado.tipo.titulo
                binding.txtEstadoRanking.text = "Ainda não há jogadores no ranking."
            }
            is RankingUiState.Error -> {
                atualizarTipoSelecionado(estado.tipo)
                binding.txtTituloRanking.text = estado.tipo.titulo
                binding.txtEstadoRanking.text = "Não foi possível carregar o ranking."
            }
            is RankingUiState.Content -> {
                atualizarTipoSelecionado(estado.tipo)
                binding.txtTituloRanking.text = estado.tipo.titulo
                rankingAdapter.atualizar(estado.tipo, estado.jogadores)
            }
        }
    }

    private fun atualizarTipoSelecionado(tipo: RankingTipo) {
        binding.btnRankingGlobal.alpha = if (tipo == RankingTipo.GLOBAL) 1.0f else 0.6f
        binding.btnRankingSolo.alpha = if (tipo == RankingTipo.SOLO) 1.0f else 0.6f
        binding.btnRanking1x1.alpha = if (tipo == RankingTipo.MODO_1X1) 1.0f else 0.6f
        binding.btnRanking2x2.alpha = if (tipo == RankingTipo.MODO_2X2) 1.0f else 0.6f
    }
}
