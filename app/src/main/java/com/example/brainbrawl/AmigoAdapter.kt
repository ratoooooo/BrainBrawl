package com.example.brainbrawl

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.models.UtilizadorSocial
import com.example.brainbrawl.utils.AvatarUtils

class AmigoAdapter(
    private val amigos: List<UtilizadorSocial>,
    private val avatares: List<String>,
    private val estados: List<String>,
    private val nomeUtilizador: String,
    private val uidUtilizador: String,
) : RecyclerView.Adapter<AmigoAdapter.AmigoViewHolder>() {

    // ViewHolder para cada amigo da lista de amigos
    inner class AmigoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatarAmigo: ImageView = view.findViewById(R.id.imgAvatarAmigo)
        val txtNomeAmigo: TextView = view.findViewById(R.id.txtNomeAmigo)
        val txtEstadoAmigo: TextView = view.findViewById(R.id.txtEstadoAmigo)
        val viewEstadoAmigo: View = view.findViewById(R.id.viewEstadoAmigo)
    }

    // Cria o ViewHolder para cada amigo da lista de amigos
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmigoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_amigo, parent, false)
        return AmigoViewHolder(view)
    }

    // Liga os dados do amigo ao ViewHolder
    override fun onBindViewHolder(holder: AmigoViewHolder, position: Int) {
        val amigo = amigos[position]
        holder.txtNomeAmigo.text = amigo.nomeDisplay

        // Define o avatar do amigo (ou avatar default)
        val avatarName = avatares.getOrNull(position) ?: "avatar_1_playstore"
        val context = holder.itemView.context
        holder.imgAvatarAmigo.setImageResource(AvatarUtils.resolverAvatar(context, avatarName))

        // Estado (verde se "on", cinza se "off" ou outro)
        val estado = estados.getOrNull(position) ?: "off"
        val online = estado == "on"
        holder.viewEstadoAmigo.background = ContextCompat.getDrawable(
            context,
            if (online) R.drawable.bg_status_online else R.drawable.bg_status_offline
        )
        holder.txtEstadoAmigo.text = context.getString(if (online) R.string.online_status else R.string.offline_status)
        holder.txtEstadoAmigo.setTextColor(
            ContextCompat.getColor(context, if (online) R.color.bb_success else R.color.bb_text_secondary)
        )

        // Se o amigo for o próprio utilizador, abre o perfil pessoal
        if (amigo.corresponde(uidUtilizador, nomeUtilizador)) {
            holder.itemView.setOnClickListener {
                val intent = Intent(context, MeuPerfilActivity::class.java)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                uidUtilizador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                context.startActivity(intent)
            }
        } else {
            // Caso contrário, abre o perfil do amigo
            holder.itemView.setOnClickListener {
                val intent = Intent(context, PerfilAmigoActivity::class.java)
                intent.putExtra(IntentExtras.NOME_AMIGO, amigo.nomeDisplay)
                amigo.uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID_AMIGO, it) }
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                uidUtilizador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                context.startActivity(intent)
            }
        }
    }

    // Retorna o número de amigos na lista
    override fun getItemCount() = amigos.size
}
