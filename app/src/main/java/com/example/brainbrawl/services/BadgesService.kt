package com.example.brainbrawl.services

import com.example.brainbrawl.models.Badge
import com.example.brainbrawl.models.BadgeFamily
import com.example.brainbrawl.models.BadgeProgress

class BadgesService {

    fun calcularBadges(
        progress: BadgeProgress,
        badgesPersistidas: Set<String>,
        permitirDesbloqueioLocal: Boolean
    ): List<Badge> {
        return criarDefinicoes().map { definicao ->
            val progressoAtual = when (definicao.familia) {
                BadgeFamily.RC -> progress.totalRespostasCertas
                BadgeFamily.PJ -> progress.totalPartidasJogadas
                BadgeFamily.VT -> progress.totalVitorias
                BadgeFamily.XP -> progress.xpTotal
                BadgeFamily.CR -> progress.creditos
            }.coerceAtLeast(0)
            val atingiuObjetivo = progressoAtual >= definicao.objetivo
            definicao.toBadge(
                progressoAtual = progressoAtual,
                desbloqueada = definicao.id in badgesPersistidas || (permitirDesbloqueioLocal && atingiuObjetivo)
            )
        }
    }

    fun badgesParaGravar(badges: List<Badge>, badgesPersistidas: Set<String>): List<Badge> {
        return badges.filter { badge ->
            badge.desbloqueada && badge.progressoAtual >= badge.objetivo && badge.id !in badgesPersistidas
        }
    }

    private fun criarDefinicoes(): List<BadgeDefinition> {
        return buildList {
            THRESHOLDS.forEach { objetivo ->
                add(
                    BadgeDefinition(
                        familia = BadgeFamily.RC,
                        objetivo = objetivo,
                        nomeSingular = "1 resposta certa",
                        nomePlural = "$objetivo respostas certas",
                        descricaoSingular = "Acertaste 1 resposta.",
                        descricaoPlural = "Acertaste $objetivo respostas.",
                        condicao = "totalRespostasCertas",
                        drawablePrefix = "rc"
                    )
                )
                add(
                    BadgeDefinition(
                        familia = BadgeFamily.PJ,
                        objetivo = objetivo,
                        nomeSingular = "1 partida jogada",
                        nomePlural = "$objetivo partidas jogadas",
                        descricaoSingular = "Terminaste 1 partida.",
                        descricaoPlural = "Terminaste $objetivo partidas.",
                        condicao = "totalJogos",
                        drawablePrefix = "pj"
                    )
                )
                add(
                    BadgeDefinition(
                        familia = BadgeFamily.VT,
                        objetivo = objetivo,
                        nomeSingular = "1 vitória",
                        nomePlural = "$objetivo vitórias",
                        descricaoSingular = "Venceste 1 partida.",
                        descricaoPlural = "Venceste $objetivo partidas.",
                        condicao = "totalVitorias",
                        drawablePrefix = "vt"
                    )
                )
            }
            XP_THRESHOLDS.forEach { objetivo ->
                add(
                    BadgeDefinition(
                        familia = BadgeFamily.XP,
                        objetivo = objetivo,
                        nomeSingular = "1 XP",
                        nomePlural = "$objetivo XP",
                        descricaoSingular = "Ganhaste 1 ponto de experiência.",
                        descricaoPlural = "Ganhaste $objetivo pontos de experiência.",
                        condicao = "xpTotal",
                        drawablePrefix = "xp"
                    )
                )
            }
            CREDIT_THRESHOLDS.forEach { objetivo ->
                add(
                    BadgeDefinition(
                        familia = BadgeFamily.CR,
                        objetivo = objetivo,
                        nomeSingular = "1 crédito",
                        nomePlural = "$objetivo créditos",
                        descricaoSingular = "Somaste 1 crédito competitivo.",
                        descricaoPlural = "Somaste $objetivo créditos competitivos.",
                        condicao = "pontuacao",
                        drawablePrefix = "cr"
                    )
                )
            }
        }.sortedWith(compareBy<BadgeDefinition> { it.familia.ordinal }.thenBy { it.objetivo })
    }

    private data class BadgeDefinition(
        val familia: BadgeFamily,
        val objetivo: Int,
        val nomeSingular: String,
        val nomePlural: String,
        val descricaoSingular: String,
        val descricaoPlural: String,
        val condicao: String,
        val drawablePrefix: String
    ) {
        val id: String = "${familia.codigo}_$objetivo"

        fun toBadge(progressoAtual: Int, desbloqueada: Boolean): Badge {
            val singular = objetivo == 1
            return Badge(
                id = id,
                familia = familia,
                nome = if (singular) nomeSingular else nomePlural,
                descricao = if (singular) descricaoSingular else descricaoPlural,
                condicao = condicao,
                desbloqueada = desbloqueada,
                drawableName = "$drawablePrefix$objetivo",
                progressoAtual = progressoAtual,
                objetivo = objetivo
            )
        }
    }

    private companion object {
        val THRESHOLDS = listOf(1, 10, 50, 100, 250, 500, 1000, 2500, 5000)
        val XP_THRESHOLDS = listOf(100, 300, 600, 1000, 2000, 5000, 10000)
        val CREDIT_THRESHOLDS = listOf(1, 5, 10, 25, 50, 100, 250, 500, 1000)
    }
}
