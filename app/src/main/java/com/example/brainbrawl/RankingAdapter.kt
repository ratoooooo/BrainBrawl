package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.models.RankingJogador

class RankingAdapter(
    private val jogadores: MutableList<RankingJogador> = mutableListOf()
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    inner class RankingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtPosicao: TextView = view.findViewById(R.id.txtPosicaoRanking)
        val txtNome: TextView = view.findViewById(R.id.txtNomeRanking)
        val txtPontuacao: TextView = view.findViewById(R.id.txtPontuacaoRanking)
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
        holder.txtPosicao.text = "#${jogador.posicao}"
        holder.txtNome.text = jogador.nomeDisplay
        holder.txtPontuacao.text = jogador.pontuacao.formatarNumero()
        holder.txtTotalJogos.text = "Jogos: ${jogador.totalJogos}"
        holder.txtTotalVitorias.text = "Vitórias: ${jogador.totalVitorias}"
        holder.txtTaxaAcertos.text = "Acertos: ${jogador.taxaAcertos.formatarPercentagem()}%"
    }

    override fun getItemCount(): Int = jogadores.size

    fun atualizar(novosJogadores: List<RankingJogador>) {
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
}
