package com.example.brainbrawl.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UteisConquistasTest {
    @Test
    fun obterNomeBadgeXp_respeitaMarcosEExtremos() {
        assertNull(UteisConquistas.obterNomeBadgeXp(-1))
        assertNull(UteisConquistas.obterNomeBadgeXp(0))
        assertNull(UteisConquistas.obterNomeBadgeXp(99))
        assertEquals("xp100", UteisConquistas.obterNomeBadgeXp(100))
        assertEquals("xp2500", UteisConquistas.obterNomeBadgeXp(3_200))
        assertEquals("xp1000000", UteisConquistas.obterNomeBadgeXp(1_000_000))
        assertEquals("xp1000000", UteisConquistas.obterNomeBadgeXp(2_000_000))
    }

    @Test
    fun obterNomeBadgePartidasJogadas_devolveMaiorMarcoAtingido() {
        assertNull(UteisConquistas.obterNomeBadgePartidasJogadas(0))
        assertEquals("pj1", UteisConquistas.obterNomeBadgePartidasJogadas(1))
        assertEquals("pj10", UteisConquistas.obterNomeBadgePartidasJogadas(10))
        assertEquals("pj10", UteisConquistas.obterNomeBadgePartidasJogadas(49))
        assertEquals("pj50", UteisConquistas.obterNomeBadgePartidasJogadas(50))
        assertEquals("pj250", UteisConquistas.obterNomeBadgePartidasJogadas(250))
    }

    @Test
    fun obterNomeBadgeVitorias_devolveMaiorMarcoAtingido() {
        assertNull(UteisConquistas.obterNomeBadgeVitorias(0))
        assertEquals("vt1", UteisConquistas.obterNomeBadgeVitorias(1))
        assertEquals("vt10", UteisConquistas.obterNomeBadgeVitorias(49))
        assertEquals("vt50", UteisConquistas.obterNomeBadgeVitorias(50))
        assertEquals("vt100", UteisConquistas.obterNomeBadgeVitorias(101))
    }

    @Test
    fun obterNomeBadgeRespostasCertas_devolveMaiorMarcoAtingido() {
        assertNull(UteisConquistas.obterNomeBadgeRespostasCertas(0))
        assertEquals("rc1", UteisConquistas.obterNomeBadgeRespostasCertas(1))
        assertEquals("rc10", UteisConquistas.obterNomeBadgeRespostasCertas(49))
        assertEquals("rc50", UteisConquistas.obterNomeBadgeRespostasCertas(50))
        assertEquals("rc250", UteisConquistas.obterNomeBadgeRespostasCertas(499))
        assertEquals("rc500", UteisConquistas.obterNomeBadgeRespostasCertas(500))
        assertEquals("rc5000", UteisConquistas.obterNomeBadgeRespostasCertas(5_000))
    }

    @Test
    fun obterNomeBadgeCreditos_devolveMaiorMarcoAtingido() {
        assertNull(UteisConquistas.obterNomeBadgeCreditos(0))
        assertEquals("cr1", UteisConquistas.obterNomeBadgeCreditos(1))
        assertEquals("cr25", UteisConquistas.obterNomeBadgeCreditos(49))
        assertEquals("cr50", UteisConquistas.obterNomeBadgeCreditos(50))
        assertEquals("cr1000", UteisConquistas.obterNomeBadgeCreditos(2_000))
    }
}
