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

class Convidar1x1AmigoAdapter(
    private val amigos: List<UtilizadorSocial>,
    private val onSelecionar: (UtilizadorSocial) -> Unit
) : RecyclerView.Adapter<Convidar1x1AmigoAdapter.ConvidarAmigoViewHolder>() {
    private var selecionado: String? = null

    inner class ConvidarAmigoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNomeAmigo: TextView = view.findViewById(R.id.txtNomeAmigo)
        val txtEstadoAmigo: TextView = view.findViewById(R.id.txtEstadoAmigo)
        val imgAvatarAmigo: ImageView = view.findViewById(R.id.imgAvatarAmigo)
        val dotEstadoAmigo: View = view.findViewById(R.id.dotEstadoAmigo)
        val imgCheckSelecionado: ImageView = view.findViewById(R.id.imgCheckSelecionado)
        val checkBox: CheckBox = view.findViewById(R.id.checkBoxSelecionarAmigo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConvidarAmigoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lista_multi_jogadores, parent, false)
        return ConvidarAmigoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConvidarAmigoViewHolder, position: Int) {
        val amigo = amigos[position]
        val context = holder.itemView.context
        val estaSelecionado = selecionado == amigo.chavePrimaria
        val podeConvidar = amigo.online
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
            if (podeConvidar) selecionar(amigo)
        }
        holder.checkBox.setOnClickListener {
            if (podeConvidar) selecionar(amigo)
        }
    }

    override fun getItemCount() = amigos.size

    fun selecionar(amigo: UtilizadorSocial) {
        selecionado = amigo.chavePrimaria
        onSelecionar(amigo)
        notifyDataSetChanged()
    }
}
