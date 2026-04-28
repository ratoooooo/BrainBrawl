package com.example.brainbrawl.services

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
        val tempoUsado = if (modoJogo == "caotico") {
            (10 - tempoRestante).toInt()
        } else {
            (20 - tempoRestante).toInt()
        }

        var pontuacao = if (modoJogo == "caotico") {
            (10 - tempoUsado) * 30
        } else {
            (20 - tempoUsado) * 10
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
