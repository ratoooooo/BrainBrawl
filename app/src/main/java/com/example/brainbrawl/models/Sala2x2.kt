package com.example.brainbrawl.models

data class Sala2x2(
    val admin: String = "",
    val estado: String = "",
    val nomeCategoria: String = "",
    val jogadores: Map<String, Boolean> = emptyMap(),
    val equipaA: Map<String, Boolean> = emptyMap(),
    val equipaB: Map<String, Boolean> = emptyMap(),
    val perguntas: List<Pergunta> = emptyList(),
    val pontuacoes_A: Map<String, Double> = emptyMap(),
    val pontuacoes_B: Map<String, Double> = emptyMap(),
    val totalPerguntasCertas_A: Map<String, Int> = emptyMap(),
    val totalPerguntasCertas_B: Map<String, Int> = emptyMap()
)
