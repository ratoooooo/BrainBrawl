package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class JogadoresSalaAdapter(
    private val jogadores: List<String>,
    private val avatares: List<String>,
    private val estados: List<String>
) : RecyclerView.Adapter<JogadoresSalaAdapter.JogadorViewHolder>() {

    // ViewHolder para cada jogador na sala
    inner class JogadorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatarJogador: ImageView = view.findViewById(R.id.imgAvatarJogador)
        val txtNomeJogador: TextView = view.findViewById(R.id.txtNomeJogador)
        val viewEstadoJogador: View = view.findViewById(R.id.viewEstadoJogador)
    }

    // Cria o ViewHolder para cada jogador na sala
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JogadorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jogador_sala, parent, false)
        return JogadorViewHolder(view)
    }

    // Preenche os dados do jogador no ViewHolder
    override fun onBindViewHolder(holder: JogadorViewHolder, position: Int) {
        val jogador = jogadores[position]
        holder.txtNomeJogador.text = jogador

        // Avatar do jogador
        val avatarName = avatares.getOrNull(position) ?: "avatar_1_playstore"
        val context = holder.itemView.context
        val resId = context.resources.getIdentifier(avatarName, "drawable", context.packageName)
        holder.imgAvatarJogador.setImageResource(resId)

        // Estado (verde se "on", cinza se "off" ou outro)
        val estado = estados.getOrNull(position) ?: "off"
        val cor = if (estado == "on") 0xFF43A047.toInt() else 0xFFBDBDBD.toInt() // verde ou cinza
        holder.viewEstadoJogador.background.setTint(cor)

        holder.itemView.setOnClickListener {
            // Ação ao clicar no jogador (pode ser abrir perfil ou outra ação)
            Toast.makeText(context, "Clicou em $jogador", Toast.LENGTH_SHORT).show()
        }
    }

    // Retorna o número de jogadores na sala
    override fun getItemCount() = jogadores.size
}


