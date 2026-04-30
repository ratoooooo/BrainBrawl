package com.example.brainbrawl

import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.ProgressaoService
import com.example.brainbrawl.services.ScoreCompetitivoService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val scoreCompetitivoService = ScoreCompetitivoService()
    private val estatisticasService = EstatisticasService()
    private val progressaoService = ProgressaoService()

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
    fun vencedorUsaUidQuandoResultadoTemIdentidadeAutenticada() {
        val resultados = listOf(
            EstatisticasService.ResultadoJogador(nome = "Ana", pontos = 150.0, uid = "uid-ana", nomeUtilizador = "Ana"),
            EstatisticasService.ResultadoJogador(nome = "Beto", pontos = 120.0, uid = "uid-beto", nomeUtilizador = "Beto")
        )

        assertEquals(
            setOf("uid-ana"),
            estatisticasService.vencedores(resultados, EstatisticasService.Modo.UM_CONTRA_UM)
        )
        assertTrue(resultados.first().corresponde("Ana"))
        assertTrue(resultados.first().corresponde("uid-ana"))
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

    @Test
    fun progressaoNovoJogadorComecaNoNivelUm() {
        val estado = progressaoService.calcularEstadoProgressao(0)

        assertEquals(1, estado.nivel)
        assertEquals(0, estado.xpNoNivelAtual)
        assertEquals(300, estado.xpNecessarioProximoNivel)
    }

    @Test
    fun progressaoPassar300XpSobeParaNivelDois() {
        val estado = progressaoService.calcularEstadoProgressao(300)

        assertEquals(2, estado.nivel)
        assertEquals(0, estado.xpNoNivelAtual)
        assertEquals(450, estado.xpNecessarioProximoNivel)
    }

    @Test
    fun progressaoPassar750XpTotalFicaNivelTresComProgressoCorreto() {
        val estado = progressaoService.calcularEstadoProgressao(750)

        assertEquals(3, estado.nivel)
        assertEquals(0, estado.xpNoNivelAtual)
        assertEquals(600, estado.xpNecessarioProximoNivel)
    }

    @Test
    fun progressaoSuportaMultiplosNiveisDeUmaVez() {
        val estado = progressaoService.calcularEstadoProgressao(3000)

        assertTrue(estado.nivel > 1)
        assertTrue(estado.xpNoNivelAtual >= 0)
        assertTrue(estado.xpNoNivelAtual < estado.xpNecessarioProximoNivel)
    }

    @Test
    fun atualizacaoEstatisticasSomaPontuacaoAoTotalExistente() {
        val updates = estatisticasService.calcularAtualizacao(
            estatisticasAtuais = EstatisticasService.EstatisticasAtuais(
                pontuacao = 1000.0,
                taxaAcertos = 50.0,
                totalJogos = 10,
                totalVitorias = 4,
                totalRespostasCertas = 40,
                totalVitoriasModo1x1 = 1,
                totalVitoriasModo2x2 = 1,
                totalVitoriasModoSolo = 2,
                xpTotal = 0
            ),
            resultado = EstatisticasService.ResultadoJogador(
                nome = "Jogador",
                pontos = 2100.0,
                respostasCertas = 8
            ),
            modo = EstatisticasService.Modo.SOLO,
            venceu = true,
            totalPerguntas = 8
        )

        assertEquals(3100.0, updates["pontuacao"] as Double, 0.0001)
    }
}
