package com.example.brainbrawl.services

import com.example.brainbrawl.models.BadgeProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesServiceTest {

    private val service = BadgesService()

    @Test
    fun calcularBadges_criaTresFamiliasComNoveBadgesCada() {
        val badges = service.calcularBadges(
            progress = BadgeProgress(
                totalRespostasCertas = 0,
                totalPartidasJogadas = 0,
                totalVitorias = 0
            ),
            badgesPersistidas = emptySet(),
            permitirDesbloqueioLocal = true
        )

        assertEquals(27, badges.size)
        assertEquals(9, badges.count { it.familia.codigo == "RC" })
        assertEquals(9, badges.count { it.familia.codigo == "PJ" })
        assertEquals(9, badges.count { it.familia.codigo == "VT" })
    }

    @Test
    fun calcularBadges_desbloqueiaPorThresholdsDeStats() {
        val badges = service.calcularBadges(
            progress = BadgeProgress(
                totalRespostasCertas = 10,
                totalPartidasJogadas = 50,
                totalVitorias = 0
            ),
            badgesPersistidas = emptySet(),
            permitirDesbloqueioLocal = true
        )

        assertTrue(badges.first { it.id == "RC_10" }.desbloqueada)
        assertFalse(badges.first { it.id == "RC_50" }.desbloqueada)
        assertTrue(badges.first { it.id == "PJ_50" }.desbloqueada)
        assertFalse(badges.first { it.id == "VT_1" }.desbloqueada)
    }

    @Test
    fun calcularBadges_semDesbloqueioLocalMantemBloqueadasExcetoPersistidas() {
        val badges = service.calcularBadges(
            progress = BadgeProgress(
                totalRespostasCertas = 5000,
                totalPartidasJogadas = 5000,
                totalVitorias = 5000
            ),
            badgesPersistidas = setOf("RC_10"),
            permitirDesbloqueioLocal = false
        )

        assertTrue(badges.first { it.id == "RC_10" }.desbloqueada)
        assertFalse(badges.first { it.id == "PJ_10" }.desbloqueada)
        assertFalse(badges.first { it.id == "VT_10" }.desbloqueada)
    }

    @Test
    fun badgesParaGravar_devolveApenasDesbloqueadasNovas() {
        val badges = service.calcularBadges(
            progress = BadgeProgress(
                totalRespostasCertas = 10,
                totalPartidasJogadas = 1,
                totalVitorias = 1
            ),
            badgesPersistidas = setOf("RC_1"),
            permitirDesbloqueioLocal = true
        )

        val novas = service.badgesParaGravar(badges, badgesPersistidas = setOf("RC_1"))

        assertEquals(setOf("RC_10", "PJ_1", "VT_1"), novas.map { it.id }.toSet())
    }
}
