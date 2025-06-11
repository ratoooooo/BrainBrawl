package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Convidar1x1AmigoAdapter(
    private val amigos: List<String>,
    private val onDesafiarClick: (String) -> Unit
) : RecyclerView.Adapter<Convidar1x1AmigoAdapter.ConvidarAmigoViewHolder>() {

    // ViewHolder para cada amigo na lista de amigos
    inner class ConvidarAmigoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomeAmigo: TextView = view.findViewById(R.id.txtNomeAmigo)
        val btnDesafiar: Button = view.findViewById(R.id.btnDesafiar)
    }

    // Cria o ViewHolder para cada amigo na lista de amigos
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConvidarAmigoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_convidar_amigo, parent, false)
        return ConvidarAmigoViewHolder(view)
    }

    // Define o nome do amigo e o que acontece ao clicar no botão de desafiar
    override fun onBindViewHolder(holder: ConvidarAmigoViewHolder, position: Int) {
        val amigo = amigos[position]
        holder.txtNomeAmigo.text = amigo
        holder.btnDesafiar.setOnClickListener { onDesafiarClick(amigo) }
    }

    // Retorna o número de amigos na lista
    override fun getItemCount() = amigos.size
}