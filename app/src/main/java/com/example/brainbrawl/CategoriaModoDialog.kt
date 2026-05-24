package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

object CategoriaModoDialog {
    data class Opcao(
        val titulo: String,
        val subtitulo: String,
        val iconeRes: Int,
        val onClick: () -> Unit
    )

    fun mostrar(context: Context, opcoes: List<Opcao>) {
        val dialog = AlertDialog.Builder(context).create()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 22), dp(context, 22), dp(context, 22), dp(context, 18))
            background = ContextCompat.getDrawable(context, R.drawable.bg_main_hero_card)
        }

        val fechar = TextView(context).apply {
            text = context.getString(R.string.fechar_simbolo)
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.bb_luso_gold))
            background = ContextCompat.getDrawable(context, R.drawable.bg_choose_back_button)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(dp(context, 42), dp(context, 42)).apply {
                gravity = Gravity.END
            }
        }
        container.addView(fechar)

        container.addView(TextView(context).apply {
            text = context.getString(R.string.escolher_modo)
            textSize = 27f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.bb_luso_navy))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        container.addView(TextView(context).apply {
            text = context.getString(R.string.como_jogar_categoria)
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.bb_text_secondary))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(context, 10)
                bottomMargin = dp(context, 12)
            }
        })

        container.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(context, 126), dp(context, 16)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(context, 14)
            }
            addView(TextView(context).apply {
                background = ColorDrawable(ContextCompat.getColor(context, R.color.bb_luso_border))
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            })
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_accent_diamond)
                layoutParams = LinearLayout.LayoutParams(dp(context, 12), dp(context, 12)).apply {
                    marginStart = dp(context, 10)
                    marginEnd = dp(context, 10)
                }
            })
            addView(TextView(context).apply {
                background = ColorDrawable(ContextCompat.getColor(context, R.color.bb_luso_border))
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            })
        })

        opcoes.forEach { opcao ->
            container.addView(criarOpcao(context, opcao, dialog))
        }

        dialog.setView(container)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun criarOpcao(context: Context, opcao: Opcao, dialog: AlertDialog): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_ranking_card)
            setPadding(dp(context, 14), dp(context, 12), dp(context, 12), dp(context, 12))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dialog.dismiss()
                opcao.onClick()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(context, 8)
            }

            addView(FrameLayout(context).apply {
                background = ContextCompat.getDrawable(context, R.drawable.bg_game_type_icon)
                layoutParams = LinearLayout.LayoutParams(dp(context, 54), dp(context, 54))
                addView(ImageView(context).apply {
                    setImageResource(opcao.iconeRes)
                    setColorFilter(ContextCompat.getColor(context, R.color.bb_luso_navy))
                }, FrameLayout.LayoutParams(dp(context, 30), dp(context, 30), Gravity.CENTER))
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(context, 14)
                }
                addView(TextView(context).apply {
                    text = opcao.titulo
                    textSize = 19f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.bb_luso_navy))
                })
                addView(TextView(context).apply {
                    text = opcao.subtitulo
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.bb_text_secondary))
                    setPadding(0, dp(context, 3), 0, 0)
                })
            })

            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_chevron_right)
                setColorFilter(ContextCompat.getColor(context, R.color.bb_luso_navy))
                layoutParams = LinearLayout.LayoutParams(dp(context, 24), dp(context, 24))
            })
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
