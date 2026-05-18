package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.models.Convite

// Adapter para a lista de convites recebidos
class ConviteAdapter(
    private val convites: List<Convite>,
    private val onAceitarClick: (Convite) -> Unit,
    private val onRecusarClick: (Convite) -> Unit
) : RecyclerView.Adapter<ConviteAdapter.ConviteViewHolder>() {

    // ViewHolder para cada convite na lista de convites
    inner class ConviteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomeConvite: TextView = view.findViewById(R.id.txtNomeConvite)
        val btnAceitarConvite: Button = view.findViewById(R.id.btnAceitarConvite)
        val btnRecusarConvite: Button = view.findViewById(R.id.btnRecusarConvite)
    }

    // Cria o ViewHolder para cada convite na lista de convites
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConviteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_convite, parent, false)
        return ConviteViewHolder(view)
    }

    // Define o nome do convite e o que acontece ao clicar no botão de aceitar convite
    override fun onBindViewHolder(holder: ConviteViewHolder, position: Int) {
        val convite = convites[position]
        holder.txtNomeConvite.text = holder.itemView.context.getString(R.string.convite_de_format, convite.nomeAmigo)
        holder.btnAceitarConvite.isEnabled = true
        holder.btnRecusarConvite.isEnabled = true
        holder.btnAceitarConvite.setOnClickListener {
            holder.btnAceitarConvite.isEnabled = false
            holder.btnRecusarConvite.isEnabled = false
            onAceitarClick(convite)
        }
        holder.btnRecusarConvite.setOnClickListener {
            holder.btnAceitarConvite.isEnabled = false
            holder.btnRecusarConvite.isEnabled = false
            onRecusarClick(convite)
        }
    }

    // Retorna o número de convites na lista
    override fun getItemCount() = convites.size
}
