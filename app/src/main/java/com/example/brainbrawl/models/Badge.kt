package com.example.brainbrawl.models

data class Badge(
    val id: String,
    val familia: BadgeFamily,
    val nome: String,
    val descricao: String,
    val condicao: String,
    val desbloqueada: Boolean,
    val drawableName: String,
    val drawableResId: Int? = null,
    val progressoAtual: Int,
    val objetivo: Int
)

data class BadgeProgress(
    val totalRespostasCertas: Int,
    val totalPartidasJogadas: Int,
    val totalVitorias: Int,
    val xpTotal: Int = 0,
    val creditos: Int = 0
)
