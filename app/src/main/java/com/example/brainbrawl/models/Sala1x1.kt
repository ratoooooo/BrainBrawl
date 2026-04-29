package com.example.brainbrawl.models

data class Sala1x1(
    val admin: String = "",
    val estado: String = "",
    val nomeCategoria: String = "",
    val jogadores: Map<String, Boolean> = emptyMap(),
    val prontos: Map<String, Boolean> = emptyMap(),
    val perguntas: List<Pergunta> = emptyList(),
    val pontuacoes: Map<String, Double> = emptyMap()
)
