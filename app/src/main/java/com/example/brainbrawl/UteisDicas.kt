package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat

object UteisDicas {
    fun mostrarDicas(context: Context, titulo: String, itens: List<Pair<String, String>>) {
        val padding = context.dp(20)
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
            includeFontPadding = false
        })

        itens.forEach { (nome, descricao) ->
            content.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
                background = context.getDrawable(R.drawable.bg_dica_card)
                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, padding / 2)
                layoutParams = params

                addView(criarMarcador(context))
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                    addView(TextView(context).apply {
                        text = nome
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                        maxLines = 2
                        setTextColor(ContextCompat.getColor(context, R.color.bb_text_primary))
                        includeFontPadding = false
                    })
                    addView(TextView(context).apply {
                        text = descricao
                        textSize = 13f
                        maxLines = 4
                        setTextColor(ContextCompat.getColor(context, R.color.bb_text_secondary))
                        setPadding(0, context.dp(4), 0, 0)
                    })
                })
            })
        }

        val scrollView = ScrollView(context).apply {
            addView(content)
        }

        AlertDialog.Builder(context)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun criarMarcador(context: Context): TextView {
        return TextView(context).apply {
            text = "i"
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.bb_text_primary))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, R.color.bb_accent))
            }
            layoutParams = LinearLayout.LayoutParams(context.dp(28), context.dp(28)).apply {
                marginEnd = context.dp(12)
            }
            includeFontPadding = false
        }
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
