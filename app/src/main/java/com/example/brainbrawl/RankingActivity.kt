package com.example.brainbrawl

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityRankingBinding
import com.example.brainbrawl.models.RankingTipo
import com.example.brainbrawl.routes.BottomNavHelper
import com.example.brainbrawl.services.AuthService
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
    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: ""
        val email = intent.getStringExtra(IntentExtras.EMAIL) ?: authService.utilizadorAtual()?.email ?: ""
        BottomNavHelper.instalar(this, BottomNavHelper.Item.RANKING, uid, nomeUtilizador, nomeJogador, email)

        binding.recyclerRanking.layoutManager = LinearLayoutManager(this)
        binding.recyclerRanking.adapter = rankingAdapter

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.btnRankingRecorde.setOnClickListener {
            viewModel.carregarRanking(RankingTipo.RECORDE)
        }

        binding.btnRankingSolo.setOnClickListener {
            viewModel.carregarRanking(RankingTipo.GRUPO)
        }

        binding.btnRanking1x1.setOnClickListener {
            viewModel.carregarRanking(RankingTipo.MODO_1X1)
        }

        binding.btnRanking2x2.setOnClickListener {
            viewModel.carregarRanking(RankingTipo.MODO_2X2)
        }

        viewModel.estado.observe(this) { estado ->
            atualizarEstado(estado)
        }

        viewModel.carregarRanking(RankingTipo.GRUPO)
    }

    private fun atualizarEstado(estado: RankingUiState) {
        binding.progressRanking.visibility = if (estado is RankingUiState.Loading) View.VISIBLE else View.GONE
        binding.recyclerRanking.visibility = if (estado is RankingUiState.Content) View.VISIBLE else View.GONE
        binding.txtEstadoRanking.visibility =
            if (estado is RankingUiState.Empty || estado is RankingUiState.Error) {
                View.VISIBLE
            } else {
                View.GONE
            }

        when (estado) {
            is RankingUiState.Loading -> {
                atualizarTipoSelecionado(estado.tipo)
            }

            is RankingUiState.Empty -> {
                atualizarTipoSelecionado(estado.tipo)
                binding.txtEstadoRanking.text = getString(R.string.ainda_sem_ranking)
            }

            is RankingUiState.Error -> {
                atualizarTipoSelecionado(estado.tipo)
                binding.txtEstadoRanking.text = getString(R.string.erro_carregar_ranking)
            }

            is RankingUiState.Content -> {
                atualizarTipoSelecionado(estado.tipo)
                rankingAdapter.atualizar(estado.tipo, estado.jogadores)
            }
        }
    }

    private fun atualizarTipoSelecionado(tipo: RankingTipo) {
        listOf(
            binding.btnRankingSolo to RankingTipo.GRUPO,
            binding.btnRanking1x1 to RankingTipo.MODO_1X1,
            binding.btnRanking2x2 to RankingTipo.MODO_2X2,
            binding.btnRankingRecorde to RankingTipo.RECORDE
        ).forEach { (tab, tabTipo) ->
            val selecionado = tipo == tabTipo
            tab.background = ContextCompat.getDrawable(
                this,
                if (selecionado) R.drawable.bg_ranking_tab_selected else R.drawable.bg_ranking_tab_unselected
            )
            tab.setTextColor(
                ContextCompat.getColor(this, if (selecionado) R.color.bb_primary_text else R.color.bb_luso_navy)
            )
            TextViewCompat.setCompoundDrawableTintList(tab, ContextCompat.getColorStateList(
                this,
                if (selecionado) R.color.bb_luso_gold else R.color.bb_luso_navy
            ))
        }
    }
}
