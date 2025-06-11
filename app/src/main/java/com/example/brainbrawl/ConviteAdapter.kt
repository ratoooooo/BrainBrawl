package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter para a lista de convites recebidos
class ConviteAdapter(
    private val convites: List<Convite1x1>,
    private val onAceitarClick: (Convite1x1) -> Unit
) : RecyclerView.Adapter<ConviteAdapter.ConviteViewHolder>() {

    // ViewHolder para cada convite na lista de convites
    inner class ConviteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomeConvite: TextView = view.findViewById(R.id.txtNomeConvite)
        val btnAceitarConvite: Button = view.findViewById(R.id.btnAceitarConvite)
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
        holder.txtNomeConvite.text = "Convite de: ${convite.nomeAmigo}"
        holder.btnAceitarConvite.setOnClickListener { onAceitarClick(convite) }
    }

    // Retorna o número de convites na lista
    override fun getItemCount() = convites.size
}