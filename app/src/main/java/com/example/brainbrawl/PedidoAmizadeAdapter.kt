package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.models.PedidoAmizade

class PedidoAmizadeAdapter(
    private val pedidos: List<PedidoAmizade>,
    private val onAceitarClick: (PedidoAmizade) -> Unit
) : RecyclerView.Adapter<PedidoAmizadeAdapter.PedidoViewHolder>() {

    // ViewHolder para cada item do RecyclerView
    inner class PedidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomePedido: TextView = view.findViewById(R.id.txtNomePedido)
        val btnAceitar: Button = view.findViewById(R.id.btnAceitarPedido)
    }

    // Cria uma nova ViewHolder quando o RecyclerView precisa de uma nova View
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido_amizade, parent, false)
        return PedidoViewHolder(view)
    }

    // Liga os dados do pedido à ViewHolder
    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidos[position]
        holder.txtNomePedido.text = pedido.utilizador.nomeDisplay
        holder.btnAceitar.setOnClickListener { onAceitarClick(pedido) }
    }

    override fun getItemCount() = pedidos.size
}
