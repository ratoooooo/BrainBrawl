package com.example.brainbrawl.models

import com.example.brainbrawl.config.FirebasePaths

data class HistoricoJogo(
    val historicoId: String = "",
    val modo: String = "",
    val codigoSala: String = "",
    val nomeCategoria: String = "",
    val pontuacao: Double = 0.0,
    val recordeFoiBatido: Boolean = false,
    val respostasCertas: Int = 0,
    val totalPerguntas: Int = 0,
    val venceu: Boolean = false,
    val empate: Boolean = false,
    val equipa: String = "",
    val competitivo: Boolean = true,
    val dataHora: Long = 0L,
    val jogadoresDaPartida: List<String> = emptyList()
) {
    val resultadoTexto: String
        get() = when {
            empate -> "Empate"
            venceu -> "Vitória"
            else -> "Derrota"
        }

    fun toFirebaseMap(): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.MODO to modo,
            FirebasePaths.CODIGO_SALA to codigoSala,
            FirebasePaths.NOME_CATEGORIA to nomeCategoria,
            FirebasePaths.PONTUACAO to pontuacao,
            FirebasePaths.RECORDE_FOI_BATIDO to recordeFoiBatido,
            FirebasePaths.RESPOSTAS_CERTAS to respostasCertas,
            FirebasePaths.TOTAL_PERGUNTAS to totalPerguntas,
            FirebasePaths.VENCEU to venceu,
            FirebasePaths.EMPATE to empate,
            FirebasePaths.COMPETITIVO to competitivo,
            FirebasePaths.DATA_HORA to dataHora,
            FirebasePaths.JOGADORES to jogadoresDaPartida
        )
        if (equipa.isNotBlank()) dados[FirebasePaths.EQUIPA] = equipa
        return dados
    }
}
