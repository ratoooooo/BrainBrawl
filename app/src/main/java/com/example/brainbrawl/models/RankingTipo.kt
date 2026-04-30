package com.example.brainbrawl.models

import com.example.brainbrawl.config.FirebasePaths

enum class RankingTipo(
    val firebaseField: String,
    val titulo: String,
    val valorLabel: String
) {
    GLOBAL(
        firebaseField = FirebasePaths.PONTUACAO,
        titulo = "Ranking Global",
        valorLabel = "Pontos"
    ),
    SOLO(
        firebaseField = FirebasePaths.TOTAL_VITORIAS_MODO_SOLO,
        titulo = "Ranking Solo",
        valorLabel = "Vitórias Solo"
    ),
    MODO_1X1(
        firebaseField = FirebasePaths.TOTAL_VITORIAS_MODO_1X1,
        titulo = "Ranking 1x1",
        valorLabel = "Vitórias 1x1"
    ),
    MODO_2X2(
        firebaseField = FirebasePaths.TOTAL_VITORIAS_MODO_2X2,
        titulo = "Ranking 2x2",
        valorLabel = "Vitórias 2x2"
    );

    fun valorOrdenacao(jogador: RankingJogador): Double {
        return when (this) {
            GLOBAL -> jogador.pontuacao
            SOLO -> jogador.totalVitoriasModoSolo.toDouble()
            MODO_1X1 -> jogador.totalVitoriasModo1x1.toDouble()
            MODO_2X2 -> jogador.totalVitoriasModo2x2.toDouble()
        }
    }
}
