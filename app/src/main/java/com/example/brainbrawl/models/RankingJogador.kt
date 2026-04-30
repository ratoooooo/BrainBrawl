package com.example.brainbrawl.models

data class RankingJogador(
    val posicao: Int = 0,
    val chavePerfil: String = "",
    val uid: String = "",
    val nomeDisplay: String = "",
    val pontuacao: Double = 0.0,
    val totalJogos: Int = 0,
    val totalVitorias: Int = 0,
    val taxaAcertos: Double = 0.0
)
