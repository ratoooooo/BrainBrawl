package com.example.brainbrawl.routes

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.brainbrawl.AmigosActivity
import com.example.brainbrawl.MainActivity
import com.example.brainbrawl.MeuPerfilActivity
import com.example.brainbrawl.R
import com.example.brainbrawl.RankingActivity
import com.example.brainbrawl.config.IntentExtras

object BottomNavHelper {
    enum class Item {
        MAIN,
        RANKING,
        HISTORICO,
        AMIGOS,
        PERFIL
    }

    fun instalar(
        activity: AppCompatActivity,
        itemAtivo: Item,
        uid: String?,
        nomeUtilizador: String?,
        nomeJogador: String?,
        email: String? = null
    ) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        root.setPadding(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom + activity.dp(104))

        val nav = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(activity.dp(8), 0, activity.dp(8), 0)
            background = ContextCompat.getDrawable(activity, R.drawable.bg_main_bottom_nav)
            elevation = activity.dp(8).toFloat()
        }

        listOf(
            NavDestino(Item.MAIN, "Início", R.drawable.ic_home, MainActivity::class.java),
            NavDestino(Item.RANKING, "Ranking", R.drawable.ic_trophy, RankingActivity::class.java),
            NavDestino(Item.AMIGOS, "Amigos", R.drawable.ic_group, AmigosActivity::class.java, requerConta = true),
            NavDestino(Item.PERFIL, "Perfil", R.drawable.ic_person, MeuPerfilActivity::class.java, requerConta = true)
        ).forEach { destino ->
            nav.addView(criarItem(activity, destino, itemAtivo, uid, nomeUtilizador, nomeJogador, email))
        }

        content.addView(
            nav,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                activity.dp(82)
            ).apply {
                gravity = Gravity.BOTTOM
                marginStart = activity.dp(20)
                marginEnd = activity.dp(20)
                bottomMargin = activity.dp(18)
            }
        )
    }

    private fun criarItem(
        activity: AppCompatActivity,
        destino: NavDestino,
        itemAtivo: Item,
        uid: String?,
        nomeUtilizador: String?,
        nomeJogador: String?,
        email: String?
    ): LinearLayout {
        val ativo = destino.item == itemAtivo
        val cor = ContextCompat.getColor(activity, if (ativo) R.color.bb_accent else R.color.bb_text_secondary)
        val textoCor = ContextCompat.getColor(activity, if (ativo) R.color.bb_text_primary else R.color.bb_text_secondary)

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        val iconHost = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(activity.dp(42), activity.dp(42))
            if (ativo) background = ContextCompat.getDrawable(activity, R.drawable.bg_main_nav_selected)
        }
        iconHost.addView(
            ImageView(activity).apply {
                setImageResource(destino.icon)
                setColorFilter(cor)
            },
            FrameLayout.LayoutParams(activity.dp(24), activity.dp(24), Gravity.CENTER)
        )
        container.addView(iconHost)
        container.addView(TextView(activity).apply {
            text = destino.label
            setTextColor(textoCor)
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
        })

        container.setOnClickListener {
            if (ativo) return@setOnClickListener
            if (destino.requerConta && uid.isNullOrBlank() && nomeUtilizador.isNullOrBlank()) {
                Toast.makeText(activity, R.string.disponivel_sessao_iniciada, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            activity.startActivity(Intent(activity, destino.activityClass).apply {
                adicionarExtras(uid, nomeUtilizador, nomeJogador, email)
            })
        }
        return container
    }

    private fun Intent.adicionarExtras(uid: String?, nomeUtilizador: String?, nomeJogador: String?, email: String?) {
        uid?.takeIf { it.isNotBlank() }?.let { putExtra(IntentExtras.UID, it) }
        nomeUtilizador?.takeIf { it.isNotBlank() }?.let { putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.takeIf { it.isNotBlank() }?.let { putExtra(IntentExtras.NOME_JOGADOR, it) }
        email?.takeIf { it.isNotBlank() }?.let { putExtra(IntentExtras.EMAIL, it) }
    }

    private fun Activity.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class NavDestino(
        val item: Item,
        val label: String,
        @DrawableRes val icon: Int,
        val activityClass: Class<*>,
        val requerConta: Boolean = false
    )
}
