package com.example.brainbrawl.services

import com.example.brainbrawl.config.GameConstants

class GameService {
    fun tempoTotal(modoJogo: String?): Double {
        return when (modoJogo) {
            GameConstants.MODO_CAOTICO -> GameConstants.CHAOTIC_QUESTION_TIME_SECONDS
            GameConstants.MODO_ELIMINATORIAS -> GameConstants.ELIMINATION_QUESTION_TIME_SECONDS
            else -> GameConstants.CLASSIC_QUESTION_TIME_SECONDS
        }
    }

    fun jogadoresRestantesEliminatorias(
        jogadores: List<String>,
        nomeUtilizador: String,
        nomeJogador: String
    ): List<String> {
        return jogadores.filter { nome ->
            nome != nomeUtilizador && nome != nomeJogador
        }
    }

    fun deveTerminarEliminatorias(jogadoresRestantes: List<String>): Boolean {
        return jogadoresRestantes.size <= 1
    }
}
