package com.example.brainbrawl.models

data class Jogador(
    val nome: String = "",
    val password: String = "",
    val avatar: String = "",
    val estado: String = "",
    val pontuacao: Double = 0.0,
    val taxaAcertos: Double = 0.0,
    val totalJogos: Int = 0,
    val totalVitorias: Int = 0,
    val totalRespostasCertas: Int = 0,
    val totalVitoriasModo1x1: Int = 0,
    val totalVitoriasModo2x2: Int = 0,
    val totalVitoriasModoSolo: Int = 0,
    val isHostOnly: Boolean = false
)
