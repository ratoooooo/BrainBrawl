package com.example.brainbrawl.models

data class Pergunta(
    val pergunta: String = "",
    val respostaCorreta: String = "",
    val opcoes: List<String> = emptyList(),
    val imagem: String? = ""
)
