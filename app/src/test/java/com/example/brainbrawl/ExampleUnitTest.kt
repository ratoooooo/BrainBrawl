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
            xpTotal = 120
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
        assertEquals(300.0, updates[FirebasePaths.RECORDE_PONTUACAO])
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
