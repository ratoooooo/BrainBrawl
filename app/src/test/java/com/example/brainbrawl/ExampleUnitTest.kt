package com.example.brainbrawl

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.utils.AvatarUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val estatisticasService = EstatisticasService()

    @Test
    fun estatisticasAtuaisIncluiRecordePontuacao() {
        val atuais = EstatisticasService.EstatisticasAtuais(
            pontuacao = 1000.0,
            recordePontuacao = 250.0,
            taxaAcertos = 50.0,
            totalJogos = 2,
            totalVitorias = 1,
            totalRespostasCertas = 8,
            totalVitoriasModo1x1 = 1,
            totalVitoriasModo2x2 = 0,
            totalVitoriasModoSolo = 0,
            xpTotal = 120,
            totalPontosSomados = 1000.0
        )
        val resultado = EstatisticasService.ResultadoJogador(
            nome = "Jogador",
            pontos = 300.0,
            respostasCertas = 4
        )

        val updates = estatisticasService.calcularAtualizacao(
            estatisticasAtuais = atuais,
            resultado = resultado,
            modo = EstatisticasService.Modo.SOLO,
            venceu = true,
            totalPerguntas = 5
        )

        assertEquals(1300.0, updates[FirebasePaths.PONTUACAO])
        assertEquals(1300.0, updates[FirebasePaths.TOTAL_PONTOS_SOMADOS])
        assertEquals(300.0, updates[FirebasePaths.RECORDE_PONTUACAO])
        assertEquals(1, updates[FirebasePaths.TOTAL_VITORIAS])
        assertEquals(null, updates[FirebasePaths.TOTAL_VITORIAS_MODO_SOLO])
    }

    @Test
    fun recordePontuacaoNaoDesceQuandoSoloFazMenosPontos() {
        val atuais = EstatisticasService.EstatisticasAtuais(
            pontuacao = 1535.0,
            recordePontuacao = 1535.0,
            taxaAcertos = 70.0,
            totalJogos = 2,
            totalVitorias = 0,
            totalRespostasCertas = 10,
            totalVitoriasModo1x1 = 0,
            totalVitoriasModo2x2 = 0,
            totalVitoriasModoSolo = 0,
            xpTotal = 200,
            totalPontosSomados = 1535.0
        )
        val resultado = EstatisticasService.ResultadoJogador(
            nome = "Jogador",
            pontos = 900.0,
            respostasCertas = 3
        )

        val updates = estatisticasService.calcularAtualizacao(
            estatisticasAtuais = atuais,
            resultado = resultado,
            modo = EstatisticasService.Modo.SOLO,
            venceu = true,
            totalPerguntas = 5
        )

        assertEquals(2435.0, updates[FirebasePaths.TOTAL_PONTOS_SOMADOS])
        assertEquals(1535.0, updates[FirebasePaths.RECORDE_PONTUACAO])
        assertEquals(0, updates[FirebasePaths.TOTAL_VITORIAS])
        assertEquals(null, updates[FirebasePaths.TOTAL_VITORIAS_MODO_SOLO])
    }

    @Test
    fun vitoriaGrupoUsaContadorLegadoDeGrupo() {
        val atuais = EstatisticasService.EstatisticasAtuais(
            pontuacao = 500.0,
            recordePontuacao = 500.0,
            taxaAcertos = 60.0,
            totalJogos = 1,
            totalVitorias = 0,
            totalRespostasCertas = 5,
            totalVitoriasModo1x1 = 0,
            totalVitoriasModo2x2 = 0,
            totalVitoriasModoSolo = 2,
            xpTotal = 100,
            totalPontosSomados = 500.0
        )
        val resultado = EstatisticasService.ResultadoJogador(
            nome = "Jogador",
            pontos = 800.0,
            respostasCertas = 5
        )

        val updates = estatisticasService.calcularAtualizacao(
            estatisticasAtuais = atuais,
            resultado = resultado,
            modo = EstatisticasService.Modo.GRUPO,
            venceu = true,
            totalPerguntas = 5
        )

        assertEquals(1, updates[FirebasePaths.TOTAL_VITORIAS])
        assertEquals(3, updates[FirebasePaths.TOTAL_VITORIAS_MODO_SOLO])
    }

    @Test
    fun vencedorGrupoRespeitaOrdemFinalRecebida() {
        val resultados = listOf(
            EstatisticasService.ResultadoJogador(
                nome = "Sobrevivente",
                pontos = 100.0,
                uid = "survivor"
            ),
            EstatisticasService.ResultadoJogador(
                nome = "MaisPontosEliminado",
                pontos = 500.0,
                uid = "eliminated"
            )
        )

        assertEquals(
            setOf("survivor"),
            estatisticasService.vencedores(resultados, EstatisticasService.Modo.GRUPO)
        )
    }

    @Test
    fun empate2x2NaoAtribuiVitoriaParaEstatisticas() {
        val resultados = listOf(
            EstatisticasService.ResultadoJogador(
                nome = "A1",
                pontos = 100.0,
                equipa = GameConstants.EQUIPA_A,
                uid = "a1"
            ),
            EstatisticasService.ResultadoJogador(
                nome = "A2",
                pontos = 50.0,
                equipa = GameConstants.EQUIPA_A,
                uid = "a2"
            ),
            EstatisticasService.ResultadoJogador(
                nome = "B1",
                pontos = 75.0,
                equipa = GameConstants.EQUIPA_B,
                uid = "b1"
            ),
            EstatisticasService.ResultadoJogador(
                nome = "B2",
                pontos = 75.0,
                equipa = GameConstants.EQUIPA_B,
                uid = "b2"
            )
        )

        assertEquals("Empate!", estatisticasService.textoVencedor2x2(150.0, 150.0))
        assertTrue(
            estatisticasService.vencedores(
                resultados,
                EstatisticasService.Modo.DOIS_CONTRA_DOIS
            ).isEmpty()
        )
    }

    @Test
    fun avatarPorIndexMantemContratoFirebase() {
        assertEquals("avatar_1_playstore", AvatarUtils.nomeAvatarPorIndex(0))
        assertEquals("avatar_8_playstore", AvatarUtils.nomeAvatarPorIndex(7))
        assertEquals("avatar_12_playstore", AvatarUtils.nomeAvatarPorIndex(11))
    }
}
