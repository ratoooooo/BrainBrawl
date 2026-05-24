package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.utils.AvatarUtils

data class PodioPlayerUi(
    val position: Int,
    val nome: String,
    val pontos: String,
    val avatar: String = ""
)

class PodioContinuationAdapter(
    private var jogadores: List<PodioPlayerUi> = emptyList()
) : RecyclerView.Adapter<PodioContinuationAdapter.PodioContinuationViewHolder>() {

    fun submeterJogadores(novosJogadores: List<PodioPlayerUi>) {
        jogadores = novosJogadores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodioContinuationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_podio_continuacao, parent, false)
        return PodioContinuationViewHolder(view)
    }

    override fun onBindViewHolder(holder: PodioContinuationViewHolder, position: Int) {
        holder.bind(jogadores[position])
    }

    override fun getItemCount(): Int = jogadores.size

    class PodioContinuationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtPosicao: TextView = view.findViewById(R.id.txtPosicaoPodioContinua)
        private val imgAvatar: ImageView = view.findViewById(R.id.imgAvatarPodioContinua)
        private val txtNome: TextView = view.findViewById(R.id.txtNomePodioContinua)
        private val txtPontos: TextView = view.findViewById(R.id.txtPontosPodioContinua)

        fun bind(jogador: PodioPlayerUi) {
            val context = itemView.context
            txtPosicao.text = context.getString(R.string.posicao_ranking_format, jogador.position)
            txtNome.text = jogador.nome.ifBlank { context.getString(R.string.nome_jogador) }
            txtPontos.text = jogador.pontos.ifBlank { context.getString(R.string.pontos_curto_format, 0) }
            imgAvatar.setImageResource(AvatarUtils.resolverAvatar(context, jogador.avatar))
        }
    }
}
