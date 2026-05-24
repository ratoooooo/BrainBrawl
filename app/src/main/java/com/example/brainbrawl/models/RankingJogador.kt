package com.example.brainbrawl.models

data class RankingJogador(
    val posicao: Int = 0,
    val chavePerfil: String = "",
    val uid: String = "",
    val nomeDisplay: String = "",
    val avatar: String = "",
    val pontuacao: Double = 0.0,
    val totalPontosSomados: Double = 0.0,
    val recordePontuacao: Double = 0.0,
    val totalJogos: Int = 0,
    val totalVitorias: Int = 0,
    val taxaAcertos: Double = 0.0,
    val totalVitoriasModoSolo: Int = 0,
    val totalVitoriasModo1x1: Int = 0,
    val totalVitoriasModo2x2: Int = 0,
    val nivel: Int = 1
)
