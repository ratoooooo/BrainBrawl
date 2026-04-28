package com.example.brainbrawl

import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.ScoreCompetitivoService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val scoreCompetitivoService = ScoreCompetitivoService()
    private val estatisticasService = EstatisticasService()

    @Test
    fun pontuacaoCompetitivaSemBonusMantemFormulaAtual() {
        val resultado = scoreCompetitivoService.calcularPontuacao(
            tempoRestante = 10.0,
            numeroPerguntasCertas = 1,
            bonus = 50
        )

        assertEquals(100, resultado.pontos)
        assertEquals(0, resultado.bonusAplicado)
    }

    @Test
    fun pontuacaoCompetitivaAplicaBonusDeDuasCertas() {
        val resultado = scoreCompetitivoService.calcularPontuacao(
            tempoRestante = 10.0,
            numeroPerguntasCertas = 2,
            bonus = 50
        )

        assertEquals(150, resultado.pontos)
        assertEquals(50, resultado.bonusAplicado)
    }

    @Test
    fun pontuacaoCompetitivaAplicaBonusDeTresCertas() {
        val resultado = scoreCompetitivoService.calcularPontuacao(
            tempoRestante = 10.0,
            numeroPerguntasCertas = 3,
            bonus = 50
        )

        assertEquals(175, resultado.pontos)
        assertEquals(75, resultado.bonusAplicado)
    }

    @Test
    fun pontuacaoCompetitivaAplicaBonusMaximoComQuatroOuMaisCertas() {
        val resultado = scoreCompetitivoService.calcularPontuacao(
            tempoRestante = 10.0,
            numeroPerguntasCertas = 4,
            bonus = 50
        )

        assertEquals(250, resultado.pontos)
        assertEquals(150, resultado.bonusAplicado)
    }

    @Test
    fun taxaAcertosMantemMediaPonderadaPorJogo() {
        val novaTaxa = estatisticasService.calcularTaxaAcertos(
            taxaAcertosAnterior = 50.0,
            totalJogosAnterior = 2,
            respostasCertas = 6,
            totalPerguntas = 8
        )

        assertEquals(58.333333333333336, novaTaxa, 0.0001)
    }

    @Test
    fun vencedor1x1EPrimeiroDoPodioOrdenado() {
        val resultados = listOf(
            EstatisticasService.ResultadoJogador(nome = "B", pontos = 120.0),
            EstatisticasService.ResultadoJogador(nome = "A", pontos = 150.0)
        )

        assertEquals(setOf("A"), estatisticasService.vencedores(resultados, EstatisticasService.Modo.UM_CONTRA_UM))
    }

    @Test
    fun empate2x2MantemVitoriaDaEquipaAParaEstatisticas() {
        val resultados = listOf(
            EstatisticasService.ResultadoJogador(nome = "A1", pontos = 100.0, equipa = "A"),
            EstatisticasService.ResultadoJogador(nome = "A2", pontos = 50.0, equipa = "A"),
            EstatisticasService.ResultadoJogador(nome = "B1", pontos = 80.0, equipa = "B"),
            EstatisticasService.ResultadoJogador(nome = "B2", pontos = 70.0, equipa = "B")
        )

        assertEquals(
            setOf("A1", "A2"),
            estatisticasService.vencedores(resultados, EstatisticasService.Modo.DOIS_CONTRA_DOIS)
        )
        assertEquals("Empate!", estatisticasService.textoVencedor2x2(totalA = 150.0, totalB = 150.0))
    }

    @Test
    fun validacaoBloqueiaEstatisticasJaAtualizadas() {
        val resultados = listOf(EstatisticasService.ResultadoJogador(nome = "A", pontos = 10.0))

        assertFalse(estatisticasService.deveAtualizarEstatisticas(true, resultados))
        assertTrue(estatisticasService.deveAtualizarEstatisticas(false, resultados))
    }
}
