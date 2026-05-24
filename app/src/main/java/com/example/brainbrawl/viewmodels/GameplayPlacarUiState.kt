package com.example.brainbrawl.viewmodels

data class JogadorCompetitivoUi(
    val chave: String,
    val nome: String,
    val avatar: String,
    val pontuacao: Double,
    val atual: Boolean = false
)

data class Jogo1x1PlacarUiState(
    val jogadores: List<JogadorCompetitivoUi>
)

data class EquipaCompetitivaUi(
    val nome: String,
    val jogadores: List<JogadorCompetitivoUi>,
    val pontuacao: Double
)

data class Jogo2x2PlacarUiState(
    val equipaA: EquipaCompetitivaUi,
    val equipaB: EquipaCompetitivaUi
)
