package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.models.RankingJogador
import com.example.brainbrawl.models.RankingTipo

class RankingAdapter(
    private val jogadores: MutableList<RankingJogador> = mutableListOf()
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {
    private var rankingTipo: RankingTipo = RankingTipo.GLOBAL

    inner class RankingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtPosicao: TextView = view.findViewById(R.id.txtPosicaoRanking)
        val txtNome: TextView = view.findViewById(R.id.txtNomeRanking)
        val txtPontuacao: TextView = view.findViewById(R.id.txtPontuacaoRanking)
        val txtValorLabel: TextView = view.findViewById(R.id.txtValorLabelRanking)
        val txtTotalJogos: TextView = view.findViewById(R.id.txtTotalJogosRanking)
        val txtTotalVitorias: TextView = view.findViewById(R.id.txtTotalVitoriasRanking)
        val txtTaxaAcertos: TextView = view.findViewById(R.id.txtTaxaAcertosRanking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking_jogador, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val jogador = jogadores[position]
        val context = holder.itemView.context
        holder.txtPosicao.text = context.getString(R.string.posicao_ranking_format, jogador.posicao)
        holder.txtNome.text = context.getString(R.string.nome_nivel_format, jogador.nomeDisplay, jogador.nivel)
        holder.txtValorLabel.text = rankingTipo.valorLabel(context)
        holder.txtPontuacao.text = rankingTipo.valorOrdenacao(jogador).formatarNumero()
        holder.txtTotalJogos.text = context.getString(R.string.jogos_curto_format, jogador.totalJogos)
        holder.txtTotalVitorias.text = context.getString(R.string.vitorias_curto_format, jogador.totalVitorias)
        holder.txtTaxaAcertos.text = context.getString(R.string.acertos_curto_format, jogador.taxaAcertos.formatarPercentagem())
    }

    override fun getItemCount(): Int = jogadores.size

    fun atualizar(tipo: RankingTipo, novosJogadores: List<RankingJogador>) {
        rankingTipo = tipo
        jogadores.clear()
        jogadores.addAll(novosJogadores)
        notifyDataSetChanged()
    }

    private fun Double.formatarNumero(): String {
        return if (this % 1.0 == 0.0) {
            toInt().toString()
        } else {
            String.format("%.1f", this)
        }
    }

    private fun Double.formatarPercentagem(): String {
        return if (this % 1.0 == 0.0) {
            toInt().toString()
        } else {
            String.format("%.1f", this)
        }
    }

    private fun RankingTipo.valorLabel(context: android.content.Context): String {
        return when (this) {
            RankingTipo.GLOBAL -> context.getString(R.string.pontos_totais)
            RankingTipo.RECORDE -> context.getString(R.string.melhor_jogo)
            RankingTipo.SOLO -> context.getString(R.string.vitorias_solo)
            RankingTipo.MODO_1X1 -> context.getString(R.string.vitorias_1x1)
            RankingTipo.MODO_2X2 -> context.getString(R.string.vitorias_2x2)
        }
    }
}
