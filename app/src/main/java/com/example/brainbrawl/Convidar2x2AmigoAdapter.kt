package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Convidar2x2AmigoAdapter(
    private val amigos: List<String>
) : RecyclerView.Adapter<Convidar2x2AmigoAdapter.AmigoViewHolder>() {

    // Conjunto para armazenar os amigos selecionados
    private val selecionados = mutableSetOf<String>()

    // ViewHolder para cad a amigo na lista de amigos
    inner class AmigoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomeAmigo: TextView = view.findViewById(R.id.txtNomeAmigo)
        val checkBox: CheckBox = view.findViewById(R.id.checkBoxSelecionarAmigo)
    }

    // Cria a ViewHolder para cada amigo na lista de amigos
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmigoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lista_multi_jogadores, parent, false)
        return AmigoViewHolder(view)
    }

    // Vincula os dados do amigo à ViewHolder
    override fun onBindViewHolder(holder: AmigoViewHolder, position: Int) {
        val amigo = amigos[position]
        holder.txtNomeAmigo.text = amigo
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selecionados.contains(amigo)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (selecionados.size < 3) {
                    selecionados.add(amigo)
                } else {
                    holder.checkBox.isChecked = false
                }
            } else {
                selecionados.remove(amigo)
            }
        }
    }

    // Retorna o número de amigos na lista
    override fun getItemCount() = amigos.size

    // Retorna a lista de amigos selecionados
    fun getSelecionados(): List<String> = selecionados.toList()
}