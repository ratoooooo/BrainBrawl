package com.example.brainbrawl.services

class EstatisticasService {
    enum class Modo {
        SOLO,
        UM_CONTRA_UM,
        DOIS_CONTRA_DOIS
    }

    data class ResultadoJogador(
        val nome: String,
        val pontos: Double,
        val respostasCertas: Int = 0,
        val equipa: String? = null
    )

    data class EstatisticasAtuais(
        val pontuacao: Double,
        val taxaAcertos: Double,
        val totalJogos: Int,
        val totalVitorias: Int,
        val totalRespostasCertas: Int,
        val totalVitoriasModo1x1: Int,
        val totalVitoriasModo2x2: Int,
        val totalVitoriasModoSolo: Int
    )

    data class Podio2x2(
        val equipaA: List<ResultadoJogador>,
        val equipaB: List<ResultadoJogador>,
        val totalA: Double,
        val totalB: Double,
        val podio: List<ResultadoJogador>
    )

    fun ordenarPodio(resultados: List<ResultadoJogador>): List<ResultadoJogador> {
        return resultados.sortedByDescending { it.pontos }
    }

    fun ordenarPodio2x2(
        equipaA: List<ResultadoJogador>,
        equipaB: List<ResultadoJogador>
    ): Podio2x2 {
        val equipaAOrdenada = equipaA.sortedByDescending { it.pontos }
        val equipaBOrdenada = equipaB.sortedByDescending { it.pontos }
        val totalA = equipaAOrdenada.sumOf { it.pontos }
        val totalB = equipaBOrdenada.sumOf { it.pontos }
        val podio = if (totalA >= totalB) {
            equipaAOrdenada + equipaBOrdenada
        } else {
            equipaBOrdenada + equipaAOrdenada
        }

        return Podio2x2(
            equipaA = equipaAOrdenada,
            equipaB = equipaBOrdenada,
            totalA = totalA,
            totalB = totalB,
            podio = podio
        )
    }

    fun textoVencedor2x2(totalA: Double, totalB: Double): String {
        return when {
            totalA > totalB -> "Vitória da Equipa A!"
            totalB > totalA -> "Vitória da Equipa B!"
            else -> "Empate!"
        }
    }

    fun vencedores(resultados: List<ResultadoJogador>, modo: Modo): Set<String> {
        if (resultados.isEmpty()) return emptySet()

        return when (modo) {
            Modo.SOLO,
            Modo.UM_CONTRA_UM -> setOfNotNull(ordenarPodio(resultados).firstOrNull()?.nome)
            Modo.DOIS_CONTRA_DOIS -> {
                val totalA = resultados.filter { it.equipa == "A" }.sumOf { it.pontos }
                val totalB = resultados.filter { it.equipa == "B" }.sumOf { it.pontos }
                val equipaVencedora = if (totalA >= totalB) "A" else "B"
                resultados.filter { it.equipa == equipaVencedora }.map { it.nome }.toSet()
            }
        }
    }

    fun deveAtualizarEstatisticas(jaAtualizadas: Boolean, resultados: List<ResultadoJogador>): Boolean {
        return !jaAtualizadas && resultados.any { it.nome.isNotBlank() }
    }

    fun calcularTaxaAcertos(
        taxaAcertosAnterior: Double,
        totalJogosAnterior: Int,
        respostasCertas: Int,
        totalPerguntas: Int
    ): Double {
        val novoTotalJogos = totalJogosAnterior + 1
        val percentagemEsteJogo = if (totalPerguntas > 0) {
            respostasCertas.toDouble() / totalPerguntas * 100
        } else {
            0.0
        }

        return if (totalJogosAnterior == 0) {
            percentagemEsteJogo
        } else {
            ((taxaAcertosAnterior * totalJogosAnterior) + percentagemEsteJogo) / novoTotalJogos
        }
    }

    fun calcularAtualizacao(
        estatisticasAtuais: EstatisticasAtuais,
        resultado: ResultadoJogador,
        modo: Modo,
        venceu: Boolean,
        totalPerguntas: Int
    ): Map<String, Any> {
        val novoTotalJogos = estatisticasAtuais.totalJogos + 1
        val novoTotalVitorias = estatisticasAtuais.totalVitorias + if (venceu) 1 else 0
        val novaTaxa = calcularTaxaAcertos(
            taxaAcertosAnterior = estatisticasAtuais.taxaAcertos,
            totalJogosAnterior = estatisticasAtuais.totalJogos,
            respostasCertas = resultado.respostasCertas,
            totalPerguntas = totalPerguntas
        )

        val updates = mutableMapOf<String, Any>(
            "pontuacao" to maxOf(resultado.pontos, estatisticasAtuais.pontuacao),
            "totalJogos" to novoTotalJogos,
            "totalVitorias" to novoTotalVitorias,
            "totalRespostasCertas" to (estatisticasAtuais.totalRespostasCertas + resultado.respostasCertas),
            "taxaAcertos" to novaTaxa
        )

        when (modo) {
            Modo.SOLO -> {
                updates["totalVitoriasModoSolo"] =
                    estatisticasAtuais.totalVitoriasModoSolo + if (venceu) 1 else 0
            }
            Modo.UM_CONTRA_UM -> {
                updates["totalVitoriasModo1x1"] =
                    estatisticasAtuais.totalVitoriasModo1x1 + if (venceu) 1 else 0
            }
            Modo.DOIS_CONTRA_DOIS -> {
                updates["totalVitoriasModo2x2"] =
                    estatisticasAtuais.totalVitoriasModo2x2 + if (venceu) 1 else 0
            }
        }

        return updates
    }
}
