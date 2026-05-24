package com.example.brainbrawl

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.utils.AvatarUtils

data class PodioStatsUi(
    val rodadas: String,
    val precisao: String,
    val acertos: String
)

class PodioUiRenderer(private val root: View) {
    private val continuationAdapter = PodioContinuationAdapter()
    private val recycler: RecyclerView? = root.findViewById(R.id.recyclerPodioContinuacao)

    init {
        recycler?.layoutManager = LinearLayoutManager(root.context)
        recycler?.adapter = continuationAdapter
    }

    fun render(jogadores: List<PodioPlayerUi>, stats: PodioStatsUi?) {
        bindSlot(R.id.layoutPodioPrimeiro, R.id.imgAvatarPodioPrimeiro, R.id.txtNomePodioPrimeiro, R.id.txtPontosPodioPrimeiro, jogadores.getOrNull(0))
        bindSlot(R.id.layoutPodioSegundo, R.id.imgAvatarPodioSegundo, R.id.txtNomePodioSegundo, R.id.txtPontosPodioSegundo, jogadores.getOrNull(1))
        bindSlot(R.id.layoutPodioTerceiro, R.id.imgAvatarPodioTerceiro, R.id.txtNomePodioTerceiro, R.id.txtPontosPodioTerceiro, jogadores.getOrNull(2))
        renderContinuation(jogadores.drop(3))
        renderStats(stats)
    }

    private fun bindSlot(
        slotId: Int,
        avatarId: Int,
        nomeId: Int,
        pontosId: Int,
        jogador: PodioPlayerUi?
    ) {
        val slot = root.findViewById<View>(slotId) ?: return
        if (jogador == null || jogador.nome.isBlank()) {
            slot.visibility = View.INVISIBLE
            return
        }

        slot.visibility = View.VISIBLE
        root.findViewById<ImageView>(avatarId)?.setImageResource(
            AvatarUtils.resolverAvatar(root.context, jogador.avatar)
        )
        root.findViewById<TextView>(nomeId)?.text = jogador.nome
        root.findViewById<TextView>(pontosId)?.text = jogador.pontos
    }

    private fun renderContinuation(jogadores: List<PodioPlayerUi>) {
        val container = root.findViewById<View>(R.id.layoutRankingContinuacao) ?: return
        val empty = root.findViewById<TextView>(R.id.txtPodioContinuacaoEstado)
        if (jogadores.isEmpty()) {
            container.visibility = View.GONE
            continuationAdapter.submeterJogadores(emptyList())
            empty?.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        empty?.visibility = View.GONE
        continuationAdapter.submeterJogadores(jogadores)
    }

    private fun renderStats(stats: PodioStatsUi?) {
        val container = root.findViewById<View>(R.id.layoutPodioStats) ?: return
        if (stats == null) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.txtPodioStatRodadas)?.text = stats.rodadas
        root.findViewById<TextView>(R.id.txtPodioStatPrecisao)?.text = stats.precisao
        root.findViewById<TextView>(R.id.txtPodioStatAcertos)?.text = stats.acertos
    }
}
