package com.example.brainbrawl.models

data class SalaGrupo(
    val admin: String = "",
    val estado: String = "",
    val modoJogo: String = "",
    val nomeCategoria: String = "",
    val jogadores: Map<String, Jogador> = emptyMap(),
    val perguntas: List<Pergunta> = emptyList(),
    val perguntaAtualIndex: Int = 0
)
