package com.example.brainbrawl.utils

import com.example.brainbrawl.models.Pergunta

object UteisPerguntas {
    fun obterOpcoesAleatorias(pergunta: Pergunta): List<String> {
        val opcoes = pergunta.opcoes.toMutableList()
        opcoes.shuffle()
        return opcoes
    }
}
