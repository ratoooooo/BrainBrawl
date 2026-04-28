package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt

object UteisDicas {
    fun mostrarDicas(context: Context, titulo: String, itens: List<Pair<String, String>>) {
        val padding = (20 * context.resources.displayMetrics.density).toInt()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding / 2)
            setBackgroundColor("#FFC400".toColorInt())
        }

        content.addView(TextView(context).apply {
            text = titulo
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
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
                    setTextColor("#111111".toColorInt())
                })
                addView(TextView(context).apply {
                    text = descricao
                    textSize = 14f
                    setTextColor("#333333".toColorInt())
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
