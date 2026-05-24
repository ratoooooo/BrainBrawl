package com.example.brainbrawl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.models.UtilizadorSocial
import com.example.brainbrawl.utils.AvatarUtils

class Convidar2x2AmigoAdapter(
    private val amigos: List<UtilizadorSocial>,
    private val onSelecaoAlterada: (Int) -> Unit = {}
) : RecyclerView.Adapter<Convidar2x2AmigoAdapter.AmigoViewHolder>() {

    // Conjunto para armazenar os amigos selecionados
    private val selecionados = mutableSetOf<String>()

    // ViewHolder para cad a amigo na lista de amigos
    inner class AmigoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomeAmigo: TextView = view.findViewById(R.id.txtNomeAmigo)
        val txtEstadoAmigo: TextView = view.findViewById(R.id.txtEstadoAmigo)
        val imgAvatarAmigo: ImageView = view.findViewById(R.id.imgAvatarAmigo)
        val dotEstadoAmigo: View = view.findViewById(R.id.dotEstadoAmigo)
        val imgCheckSelecionado: ImageView = view.findViewById(R.id.imgCheckSelecionado)
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
        val context = holder.itemView.context
        val chaveAmigo = amigo.chavePrimaria
        val podeConvidar = amigo.online
        val estaSelecionado = selecionados.contains(chaveAmigo)
        holder.txtNomeAmigo.text = amigo.nomeDisplay
        holder.txtEstadoAmigo.text = context.getString(if (podeConvidar) R.string.online_status else R.string.offline_status)
        holder.txtEstadoAmigo.setTextColor(ContextCompat.getColor(context, if (podeConvidar) R.color.bb_success else R.color.bb_text_secondary))
        holder.dotEstadoAmigo.background = ContextCompat.getDrawable(context, if (podeConvidar) R.drawable.bg_status_online else R.drawable.bg_status_offline)
        holder.imgAvatarAmigo.setImageResource(AvatarUtils.resolverAvatar(context, amigo.avatar))
        holder.imgCheckSelecionado.visibility = if (estaSelecionado) View.VISIBLE else View.GONE
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = estaSelecionado
        holder.checkBox.isEnabled = podeConvidar
        holder.itemView.alpha = if (podeConvidar) 1f else 0.45f
        holder.itemView.isEnabled = podeConvidar
        holder.itemView.setOnClickListener {
            if (podeConvidar) alternarSelecao(chaveAmigo)
        }

        holder.checkBox.setOnClickListener {
            if (podeConvidar) alternarSelecao(chaveAmigo)
        }
    }

    // Retorna o número de amigos na lista
    override fun getItemCount() = amigos.size

    // Retorna a lista de amigos selecionados
    fun getSelecionados(): List<UtilizadorSocial> = amigos.filter { it.chavePrimaria in selecionados }

    private fun alternarSelecao(chaveAmigo: String) {
        if (chaveAmigo in selecionados) {
            selecionados.remove(chaveAmigo)
        } else if (selecionados.size < 3) {
            selecionados.add(chaveAmigo)
        }
        onSelecaoAlterada(selecionados.size)
        notifyDataSetChanged()
    }
}
