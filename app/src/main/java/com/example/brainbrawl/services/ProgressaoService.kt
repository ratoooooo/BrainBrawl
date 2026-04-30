package com.example.brainbrawl.services

class ProgressaoService {
    data class EstadoProgressao(
        val xpTotal: Int,
        val nivel: Int,
        val xpNoNivelAtual: Int,
        val xpNecessarioProximoNivel: Int
    )

    fun xpNecessarioParaProximoNivel(nivelAtual: Int): Int {
        val nivelSeguro = nivelAtual.coerceAtLeast(1)
        return 300 + ((nivelSeguro - 1) * 150)
    }

    fun calcularXpGanho(
        respostasCertas: Int,
        venceu: Boolean
    ): Int {
        val baseJogoTerminado = 50
        val bonusVitoria = if (venceu) 100 else 0
        val bonusRespostasCertas = respostasCertas.coerceAtLeast(0) * 10
        return baseJogoTerminado + bonusVitoria + bonusRespostasCertas
    }

    fun calcularEstadoProgressao(xpTotal: Int): EstadoProgressao {
        val xpSeguro = xpTotal.coerceAtLeast(0)
        var nivel = 1
        var xpRestante = xpSeguro
        var xpNecessario = xpNecessarioParaProximoNivel(nivel)

        while (xpRestante >= xpNecessario) {
            xpRestante -= xpNecessario
            nivel += 1
            xpNecessario = xpNecessarioParaProximoNivel(nivel)
        }

        return EstadoProgressao(
            xpTotal = xpSeguro,
            nivel = nivel,
            xpNoNivelAtual = xpRestante,
            xpNecessarioProximoNivel = xpNecessario
        )
    }
}
