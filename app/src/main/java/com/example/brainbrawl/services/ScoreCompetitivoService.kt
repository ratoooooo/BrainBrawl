package com.example.brainbrawl.services

class ScoreCompetitivoService {
    data class ResultadoPontuacao(
        val pontos: Int,
        val bonusAplicado: Int
    )

    fun calcularPontuacao(
        tempoRestante: Double,
        numeroPerguntasCertas: Int,
        bonus: Int
    ): ResultadoPontuacao {
        val tempoUsado = (15 - tempoRestante).toInt()
        val bonusAplicado = when {
            numeroPerguntasCertas == 2 -> bonus
            numeroPerguntasCertas == 3 -> bonus + 25
            numeroPerguntasCertas >= 4 -> bonus + 100
            else -> 0
        }

        return ResultadoPontuacao(
            pontos = ((15 - tempoUsado) * 10) + bonusAplicado,
            bonusAplicado = bonusAplicado
        )
    }
}
