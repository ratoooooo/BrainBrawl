package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.models.HistoricoJogo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricoAdapter(
    private var jogos: List<HistoricoJogo> = emptyList()
) : RecyclerView.Adapter<HistoricoAdapter.HistoricoViewHolder>() {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun submeterJogos(novosJogos: List<HistoricoJogo>) {
        jogos = novosJogos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoricoViewHolder {
        return HistoricoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_historico_jogo, parent, false))
    }

    override fun onBindViewHolder(holder: HistoricoViewHolder, position: Int) {
        holder.bind(jogos[position])
    }

    override fun getItemCount(): Int = jogos.size

    inner class HistoricoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtModo: TextView = view.findViewById(R.id.txtModoHistorico)
        private val txtResultado: TextView = view.findViewById(R.id.txtResultadoHistorico)
        private val txtPontuacao: TextView = view.findViewById(R.id.txtPontuacaoHistorico)
        private val txtCategoria: TextView = view.findViewById(R.id.txtCategoriaHistorico)
        private val txtData: TextView = view.findViewById(R.id.txtDataHistorico)
        private val txtJogadores: TextView = view.findViewById(R.id.txtJogadoresHistorico)

        fun bind(jogo: HistoricoJogo) {
            val context = itemView.context
            txtModo.text = jogo.modo.ifBlank { context.getString(R.string.modo_generico) }
            txtResultado.text = jogo.resultadoTexto
            txtPontuacao.text = context.getString(R.string.pontos_curto_format, jogo.pontuacao.toInt())
            txtCategoria.text = jogo.nomeCategoria.ifBlank { context.getString(R.string.sem_categoria) }
            txtData.text = if (jogo.dataHora > 0L) dateFormat.format(Date(jogo.dataHora)) else ""
            txtJogadores.text = jogo.jogadoresDaPartida.joinToString(", ")
                .ifBlank { jogo.codigoSala.ifBlank { context.getString(R.string.historico_sem_jogadores) } }
        }
    }
}
