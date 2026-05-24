package com.example.brainbrawl.services

import com.example.brainbrawl.config.GameConstants

class ScoreCompetitivoService {
    data class ResultadoPontuacao(
        val pontos: Int,
        val bonusAplicado: Int
    )

    fun calcularPontuacao(
        tempoRestante: Double,
        numeroPerguntasCertas: Int,
        bonus: Int,
        tempoTotalPergunta: Double = GameConstants.COMPETITIVE_DEFAULT_QUESTION_TIME_SECONDS
    ): ResultadoPontuacao {
        val tempoUsado = (tempoTotalPergunta - tempoRestante).toInt()
        val bonusAplicado = when {
            numeroPerguntasCertas == 2 -> bonus
            numeroPerguntasCertas == 3 -> bonus + 25
            numeroPerguntasCertas >= 4 -> bonus + 100
            else -> 0
        }

        return ResultadoPontuacao(
            pontos = ((tempoTotalPergunta - tempoUsado).toInt() * 10) + bonusAplicado,
            bonusAplicado = bonusAplicado
        )
    }
}
