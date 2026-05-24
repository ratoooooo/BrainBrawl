package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.models.HistoricoJogo
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricoAdapter(
    private var jogos: List<HistoricoJogo> = emptyList()
) : RecyclerView.Adapter<HistoricoAdapter.HistoricoViewHolder>() {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
    private val scoreFormat = DecimalFormat("#,###")

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
        private val txtBadge: TextView = view.findViewById(R.id.txtBadgeHistorico)
        private val imgIcon: ImageView = view.findViewById(R.id.imgIconHistorico)

        fun bind(jogo: HistoricoJogo) {
            val context = itemView.context
            txtModo.text = jogo.modo.formatarModo(context)
            txtResultado.text = jogo.resultadoTexto
            val resultadoColor = ContextCompat.getColor(
                context,
                when {
                    jogo.empate -> R.color.bb_accent
                    jogo.venceu -> R.color.bb_success
                    else -> R.color.bb_danger
                }
            )
            val resultadoBg = if (jogo.venceu || jogo.empate) {
                R.drawable.bg_history_result_success
            } else {
                R.drawable.bg_history_result_danger
            }
            txtResultado.setTextColor(resultadoColor)
            txtResultado.setBackgroundResource(resultadoBg)
            txtPontuacao.setTextColor(resultadoColor)
            txtPontuacao.text = scoreFormat.format(jogo.pontuacao.toInt())
            val categoria = jogo.nomeCategoria.ifBlank { context.getString(R.string.sem_categoria) }
            txtCategoria.text = categoria
            txtData.text = if (jogo.dataHora > 0L) dateFormat.format(Date(jogo.dataHora)) else ""
            txtBadge.text = if (jogo.competitivo) {
                context.getString(R.string.competitivo)
            } else {
                context.getString(R.string.nao_competitivo)
            }
            txtBadge.setBackgroundResource(
                if (jogo.competitivo) R.drawable.bg_history_badge_competitive else R.drawable.bg_history_badge_casual
            )
            imgIcon.setImageResource(jogo.modo.iconHistorico())
            txtJogadores.text = jogo.jogadoresDaPartida.joinToString(", ")
                .ifBlank { jogo.codigoSala.ifBlank { context.getString(R.string.historico_sem_jogadores) } }
        }
    }

    private fun String.formatarModo(context: android.content.Context): String {
        val normalizado = lowercase(Locale.getDefault())
        return when {
            "1x1" in normalizado -> "Modo 1x1"
            "2x2" in normalizado -> "Modo 2x2"
            "explorar" in normalizado -> "Explorar"
            isNotBlank() -> replaceFirstChar { it.titlecase(Locale.getDefault()) }
            else -> context.getString(R.string.modo_generico)
        }
    }

    private fun String.iconHistorico(): Int {
        val normalizado = lowercase(Locale.getDefault())
        return when {
            "2x2" in normalizado -> R.drawable.ic_shield_clean
            "explorar" in normalizado || "solo" in normalizado -> R.drawable.ic_compass_simple
            else -> R.drawable.ic_sword
        }
    }
}
