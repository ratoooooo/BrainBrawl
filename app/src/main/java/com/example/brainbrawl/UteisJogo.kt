package com.example.brainbrawl

import Pergunta
import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast

object UteisJogo {

    // Função auxiliar para mudar a cor de um botão
    fun definirCorBotao(botao: android.widget.Button, cor: String) {
        botao.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(cor)
        )
    }

    // Função que devolve a lista de opções embaralhada
    fun obterOpcoesAleatorias(pergunta: Pergunta): List<String> {
        val opcoes = pergunta.opcoes.toMutableList()
        opcoes.shuffle()
        return opcoes
    }

    // Função para atualizar a pontuação dos jogadores
    fun atualizarPontuacao(context: Context, tempoRestante: Double, numeroPerguntasCertas: Int, bonus: Int): Int {
        val tempoUsado = (15 - tempoRestante).toInt()
        var pontuacao = (15 - tempoUsado) * 10

        // Bonus por sequência de respostas corretas
        if (numeroPerguntasCertas == 2) {
            pontuacao += bonus
            Toast.makeText(context, "Bónus de sequência! +$bonus pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas == 3) {
            pontuacao += bonus + 25
            Toast.makeText(context, "Bónus de sequência! +${bonus + 25} pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas >= 4) {
            pontuacao += bonus + 100
            Toast.makeText(context, "Bónus de sequência! +${bonus + 100} pontos", Toast.LENGTH_SHORT).show()
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
