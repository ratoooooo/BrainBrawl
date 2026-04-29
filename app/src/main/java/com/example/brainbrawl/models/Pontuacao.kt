package com.example.brainbrawl.models

data class Pontuacao(
    val nome: String = "",
    val pontos: Double = 0.0,
    val respostasCertas: Int = 0,
    val equipa: String? = null
)
