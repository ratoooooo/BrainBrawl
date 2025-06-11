package com.example.brainbrawl

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AmigoAdapter(
    private val amigos: List<String>,
    private val nomeUtilizador: String
) : RecyclerView.Adapter<AmigoAdapter.AmigoViewHolder>() {

    // ViewHolder para cada amigo da lista de amigos
    inner class AmigoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomeAmigo: TextView = view.findViewById(R.id.txtNomeAmigo)
    }

    // Cria o ViewHolder para cada amigop da lista de amigos
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmigoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_amigo, parent, false)
        return AmigoViewHolder(view)
    }

    // Abrir o perfil do amigo ao clicar no amigo
    override fun onBindViewHolder(holder: AmigoViewHolder, position: Int) {
        // Obtém o amigo na posição atual e define o nome no TextView
        val amigo = amigos[position]
        holder.txtNomeAmigo.text = amigo
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PerfilAmigoActivity::class.java)
            intent.putExtra("nomeAmigo", amigo)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            holder.itemView.context.startActivity(intent)
        }
    }

    // Retorna o número de amigos na lista
    override fun getItemCount() = amigos.size
}