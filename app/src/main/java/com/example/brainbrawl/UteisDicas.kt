package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

object UteisDicas {
    fun mostrarDicas(context: Context, titulo: String, itens: List<Pair<String, String>>) {
        val padding = (20 * context.resources.displayMetrics.density).toInt()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding / 2)
            setBackgroundColor(ContextCompat.getColor(context, R.color.bb_bg_start))
        }

        content.addView(TextView(context).apply {
            text = titulo
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.bb_text_primary))
            setPadding(0, 0, 0, padding / 2)
        })

        itens.forEach { (nome, descricao) ->
            content.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding / 2, padding, padding / 2)
                background = context.getDrawable(R.drawable.bg_dica_card)
                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, padding / 2)
                layoutParams = params

                addView(TextView(context).apply {
                    text = nome
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, R.color.bb_text_primary))
                })
                addView(TextView(context).apply {
                    text = descricao
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.bb_text_secondary))
                    setPadding(0, 4, 0, 0)
                })
            })
        }

        AlertDialog.Builder(context)
            .setView(content)
            .setPositiveButton("OK", null)
            .show()
    }
}
