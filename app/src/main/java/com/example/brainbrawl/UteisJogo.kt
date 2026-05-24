package com.example.brainbrawl

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import com.example.brainbrawl.config.GameConstants

object UteisJogo {

    // Função auxiliar para mudar a cor de um botão
    fun definirCorBotao(botao: android.widget.Button, cor: String) {
        botao.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(cor)
        )
    }

    // Função para atualizar a pontuação dos jogadores
    fun atualizarPontuacao(context: Context, tempoRestante: Double, numeroPerguntasCertas: Int, bonus: Int): Int {
        val tempoTotal = GameConstants.CLASSIC_QUESTION_TIME_SECONDS.toInt()
        val tempoUsado = (tempoTotal - tempoRestante).toInt()
        var pontuacao = (tempoTotal - tempoUsado) * 10

        // Bonus por sequência de respostas corretas
        if (numeroPerguntasCertas == 2) {
            pontuacao += bonus
            Toast.makeText(context, context.getString(R.string.bonus_sequencia_format, bonus), Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas == 3) {
            pontuacao += bonus + 25
            Toast.makeText(context, context.getString(R.string.bonus_sequencia_format, bonus + 25), Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas >= 4) {
            pontuacao += bonus + 100
            Toast.makeText(context, context.getString(R.string.bonus_sequencia_format, bonus + 100), Toast.LENGTH_SHORT).show()
        }
        return pontuacao
    }

    // Função utiçizada para tocar o somm (certo ou errado)
    fun tocarSom(context: Context, resourceId: Int) {
        val mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
        mediaPlayer?.start()
    }

}
