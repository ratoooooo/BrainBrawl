package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.databinding.ActivityRankingBinding
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

        viewModel.estado.observe(this) { estado ->
            atualizarEstado(estado)
        }
        viewModel.carregarRanking()
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
            RankingUiState.Loading -> Unit
            RankingUiState.Empty -> {
                binding.txtEstadoRanking.text = "Ainda não há jogadores no ranking."
            }
            RankingUiState.Error -> {
                binding.txtEstadoRanking.text = "Não foi possível carregar o ranking."
            }
            is RankingUiState.Content -> {
                rankingAdapter.atualizar(estado.jogadores)
            }
        }
    }
}
