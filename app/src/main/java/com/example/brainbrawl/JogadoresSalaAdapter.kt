package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.utils.AvatarUtils

class JogadoresSalaAdapter(
    private val jogadores: List<String>,
    private val avatares: List<String>,
    private val estados: List<String>
) : RecyclerView.Adapter<JogadoresSalaAdapter.JogadorViewHolder>() {

    // ViewHolder para cada jogador na lista de jogadores
    inner class JogadorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatarJogador: ImageView = view.findViewById(R.id.imgAvatarJogador)
        val txtNomeJogador: TextView = view.findViewById(R.id.txtNomeJogador)
        val viewEstadoJogador: View = view.findViewById(R.id.viewEstadoJogador)
    }

    // Cria o ViewHolder para cada jogador na lista de jogadores
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JogadorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jogador_sala, parent, false)
        return JogadorViewHolder(view)
    }

    // Define o nome do jogador, o avatar e o estado visual
    override fun onBindViewHolder(holder: JogadorViewHolder, position: Int) {
        val context = holder.itemView.context

        // Guardar oa posição do jogador carregado
        val jogador = jogadores[position]
        holder.txtNomeJogador.text = jogador

        // Avatar do jogador
        val avatarName = avatares.getOrNull(position) ?: "avatar_1_playstore"
        holder.imgAvatarJogador.setImageResource(AvatarUtils.resolverAvatar(context, avatarName))

        // Estado visual
        val estado = estados.getOrNull(position) ?: "off"
        val cor = if (estado == "on") 0xFF43A047.toInt() else 0xFFBDBDBD.toInt()
        holder.viewEstadoJogador.background.setTint(cor)

        holder.itemView.setOnClickListener {
            Toast.makeText(context, "Clicou em $jogador", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = jogadores.size
}
