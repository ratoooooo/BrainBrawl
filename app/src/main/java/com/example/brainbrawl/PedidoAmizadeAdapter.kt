package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PedidoAmizadeAdapter(
    private val pedidos: List<String>,
    private val onAceitarClick: (String) -> Unit
) : RecyclerView.Adapter<PedidoAmizadeAdapter.PedidoViewHolder>() {

    inner class PedidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomePedido: TextView = view.findViewById(R.id.txtNomePedido)
        val btnAceitar: Button = view.findViewById(R.id.btnAceitarPedido)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido_amizade, parent, false)
        return PedidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val nomeOutro = pedidos[position]
        holder.txtNomePedido.text = nomeOutro
        holder.btnAceitar.setOnClickListener { onAceitarClick(nomeOutro) }
    }

    override fun getItemCount() = pedidos.size
}