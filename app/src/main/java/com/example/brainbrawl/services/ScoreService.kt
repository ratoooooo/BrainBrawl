package com.example.brainbrawl.services

import com.example.brainbrawl.config.GameConstants

class ScoreService {
    data class ResultadoPontuacao(
        val pontos: Int,
        val bonusAplicado: Int
    )

    fun calcularPontuacao(
        modoJogo: String?,
        tempoRestante: Double,
        numeroPerguntasCertas: Int,
        bonus: Int
    ): ResultadoPontuacao {
        val tempoTotal = when (modoJogo) {
            GameConstants.MODO_CAOTICO -> GameConstants.CHAOTIC_QUESTION_TIME_SECONDS
            GameConstants.MODO_ELIMINATORIAS -> GameConstants.ELIMINATION_QUESTION_TIME_SECONDS
            else -> GameConstants.CLASSIC_QUESTION_TIME_SECONDS
        }
        val tempoUsado = (tempoTotal - tempoRestante).toInt()

        var pontuacao = if (modoJogo == GameConstants.MODO_CAOTICO) {
            (tempoTotal.toInt() - tempoUsado) * 30
        } else {
            (tempoTotal.toInt() - tempoUsado) * 10
        }

        val bonusAplicado = when {
            numeroPerguntasCertas == 2 -> bonus
            numeroPerguntasCertas == 3 -> bonus + 25
            numeroPerguntasCertas >= 4 -> bonus + 50
            else -> 0
        }
        pontuacao += bonusAplicado

        return ResultadoPontuacao(
            pontos = pontuacao,
            bonusAplicado = bonusAplicado
        )
    }
}
