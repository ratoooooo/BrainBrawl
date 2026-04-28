package com.example.brainbrawl.services

class GameService {
    fun tempoTotal(modoJogo: String?): Double {
        return if (modoJogo == "caotico") 10.0 else 20.0
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
