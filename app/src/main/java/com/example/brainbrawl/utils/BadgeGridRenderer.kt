package com.example.brainbrawl.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.TextView
import com.example.brainbrawl.R
import com.example.brainbrawl.databinding.ItemBadgeBinding
import com.example.brainbrawl.models.Badge
import com.example.brainbrawl.models.BadgeFamily

object BadgeGridRenderer {
    fun render(
        context: Context,
        inflater: LayoutInflater,
        grid: GridLayout,
        badges: List<Badge>
    ) {
        val colunas = if (context.resources.configuration.screenWidthDp >= 360) 3 else 2
        grid.columnCount = colunas
        grid.removeAllViews()

        val badgesPorFamilia = badges.groupBy { it.familia }
        listOf(BadgeFamily.PJ, BadgeFamily.VT, BadgeFamily.RC).forEach { familia ->
            val badgesFamilia = badgesPorFamilia[familia].orEmpty()
            if (badgesFamilia.isEmpty()) return@forEach

            grid.addView(criarTituloFamilia(context, familia), criarParamsTitulo(colunas, context.dp(4)))
            badgesFamilia.forEach { badge ->
                val itemBinding = ItemBadgeBinding.inflate(inflater, grid, false)
                configurarItem(context, itemBinding, badge)
                grid.addView(itemBinding.root, criarParamsItem(context.dp(4)))
            }
        }
    }

    private fun configurarItem(context: Context, itemBinding: ItemBadgeBinding, badge: Badge) {
        itemBinding.imgBadge.setImageResource(resolverBadgeDrawable(context, badge))
        itemBinding.imgBadge.imageAlpha = if (badge.desbloqueada) 255 else 90
        itemBinding.imgBadge.scaleX = if (badge.desbloqueada) 1f else 0.92f
        itemBinding.imgBadge.scaleY = if (badge.desbloqueada) 1f else 0.92f
        itemBinding.imgBadge.contentDescription = badge.descricao
        itemBinding.txtBadgeNome.text = badge.nome
        itemBinding.txtBadgeEstado.text = if (badge.desbloqueada) {
            context.getString(R.string.conquista_desbloqueada)
        } else {
            context.getString(R.string.conquista_bloqueada)
        }
        itemBinding.txtBadgeProgresso.text = context.getString(
            R.string.badge_progresso_format,
            badge.progressoAtual,
            badge.objetivo
        )
        itemBinding.root.alpha = if (badge.desbloqueada) 1f else 0.68f
    }

    private fun criarTituloFamilia(context: Context, familia: BadgeFamily): TextView {
        return TextView(context).apply {
            text = when (familia) {
                BadgeFamily.RC -> context.getString(R.string.respostas_certas)
                BadgeFamily.PJ -> context.getString(R.string.partidas_jogadas)
                BadgeFamily.VT -> context.getString(R.string.vit_rias)
            }
            gravity = Gravity.START
            setTextColor(context.getColor(R.color.bb_text_primary))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
    }

    private fun criarParamsTitulo(colunas: Int, margem: Int): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = GridLayout.LayoutParams.MATCH_PARENT
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(0, colunas)
            setMargins(margem, margem * 3, margem, margem)
        }
    }

    private fun criarParamsItem(margem: Int): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(margem, margem, margem, margem)
            setGravity(Gravity.FILL_HORIZONTAL)
        }
    }

    private fun resolverBadgeDrawable(context: Context, badge: Badge): Int {
        val badgeRes = context.resources.getIdentifier(badge.drawableName, "drawable", context.packageName)
        if (badgeRes != 0) return badgeRes

        val fallbackName = if (badge.desbloqueada) BADGE_DEFAULT else BADGE_LOCKED
        val fallbackRes = context.resources.getIdentifier(fallbackName, "drawable", context.packageName)
        if (fallbackRes != 0) return fallbackRes

        val defaultRes = context.resources.getIdentifier(BADGE_DEFAULT, "drawable", context.packageName)
        if (defaultRes != 0) return defaultRes

        return if (badge.desbloqueada) R.drawable.ic_trophy else R.drawable.ic_lock
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private const val BADGE_DEFAULT = "badge_default"
    private const val BADGE_LOCKED = "badge_locked"
}
