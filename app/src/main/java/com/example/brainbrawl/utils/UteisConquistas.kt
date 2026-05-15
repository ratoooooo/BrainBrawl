package com.example.brainbrawl.utils

import android.content.res.Resources
import androidx.annotation.DrawableRes

object UteisConquistas {
    data class BadgeMarco(
        val objetivo: Int,
        val drawableName: String
    )

    val partidasJogadasBadges = listOf(
        100 to "pj100",
        50 to "pj50",
        25 to "pj25",
        10 to "pj10"
    ).map { (objetivo, drawableName) -> BadgeMarco(objetivo, drawableName) }

    val vitoriasBadges = listOf(
        100 to "vt100",
        50 to "vt50",
        25 to "vt25",
        5 to "vt5"
    ).map { (objetivo, drawableName) -> BadgeMarco(objetivo, drawableName) }

    val xpBadges = listOf(
        1_000_000 to "xp1000000",
        500_000 to "xp500000",
        250_000 to "xp250000",
        100_000 to "xp100000",
        50_000 to "xp50000",
        25_000 to "xp25000",
        10_000 to "xp10000",
        5_000 to "xp5000",
        2_500 to "xp2500",
        1_000 to "xp1000",
        500 to "xp500",
        100 to "xp100"
    ).map { (objetivo, drawableName) -> BadgeMarco(objetivo, drawableName) }

    val respostasCertasBadges = listOf(
        1_000 to "rc1000",
        500 to "rc500",
        200 to "rc200",
        100 to "rc100",
        50 to "rc50"
    ).map { (objetivo, drawableName) -> BadgeMarco(objetivo, drawableName) }

    val creditosBadges = listOf(
        1_000 to "cr1000",
        500 to "cr500",
        250 to "cr250",
        100 to "cr100",
        50 to "cr50",
        25 to "cr25",
        10 to "cr10",
        5 to "cr5",
        1 to "cr1"
    ).map { (objetivo, drawableName) -> BadgeMarco(objetivo, drawableName) }

    val crBadges = creditosBadges

    fun obterBadgePartidasJogadas(resources: Resources, packageName: String, valor: Int): Int? {
        return obterBadge(valor, partidasJogadasBadges, resources, packageName)
    }

    fun obterBadgeVitorias(resources: Resources, packageName: String, valor: Int): Int? {
        return obterBadge(valor, vitoriasBadges, resources, packageName)
    }

    fun obterBadgeRespostasCertas(resources: Resources, packageName: String, valor: Int): Int? {
        return obterBadge(valor, respostasCertasBadges, resources, packageName)
    }

    fun obterBadgeXp(resources: Resources, packageName: String, valor: Int): Int? {
        return obterBadge(valor, xpBadges, resources, packageName)
    }

    fun obterBadgeCreditos(resources: Resources, packageName: String, valor: Int): Int? {
        return obterBadge(valor, creditosBadges, resources, packageName)
    }

    internal fun obterNomeBadgePartidasJogadas(valor: Int): String? {
        return obterNomeBadge(valor, partidasJogadasBadges)
    }

    internal fun obterNomeBadgeVitorias(valor: Int): String? {
        return obterNomeBadge(valor, vitoriasBadges)
    }

    internal fun obterNomeBadgeRespostasCertas(valor: Int): String? {
        return obterNomeBadge(valor, respostasCertasBadges)
    }

    internal fun obterNomeBadgeXp(valor: Int): String? {
        return obterNomeBadge(valor, xpBadges)
    }

    internal fun obterNomeBadgeCreditos(valor: Int): String? {
        return obterNomeBadge(valor, creditosBadges)
    }

    @DrawableRes
    private fun obterBadge(
        valor: Int,
        badges: List<BadgeMarco>,
        resources: Resources,
        packageName: String
    ): Int? {
        val drawableName = obterNomeBadge(valor, badges) ?: return null
        return resources.getIdentifier(drawableName, "drawable", packageName).takeIf { it != 0 }
    }

    private fun obterNomeBadge(valor: Int, badges: List<BadgeMarco>): String? {
        if (valor <= 0) return null
        return badges.firstOrNull { valor >= it.objetivo }?.drawableName
    }
}
