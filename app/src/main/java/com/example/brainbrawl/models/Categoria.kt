package com.example.brainbrawl.models

data class Categoria(
    val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val criador: String = "",
    val criadorId: String = "",
    val nomeUtilizador: String = "",
    val perguntas: List<Pergunta> = emptyList(),
    val categoriaPublicaId: String = "",
    val estadoPublicacao: String = "",
    val origemCategoriaPublica: String = "",
    val usos: Int = 0,
    val ratingMedio: Double = 0.0,
    val totalAvaliacoes: Int = 0
)
