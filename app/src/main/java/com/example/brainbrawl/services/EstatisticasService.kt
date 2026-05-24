package com.example.brainbrawl.services

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants

class EstatisticasService {
    private val progressaoService = ProgressaoService()

    enum class Modo {
        SOLO,
        GRUPO,
        UM_CONTRA_UM,
        DOIS_CONTRA_DOIS
    }

    data class ResultadoJogador(
        val nome: String,
        val pontos: Double,
        val respostasCertas: Int = 0,
        val equipa: String? = null,
        val uid: String = "",
        val chave: String = "",
        val nomeUtilizador: String = "",
        val nomeJogador: String = "",
        val avatar: String = "",
        /** Estado na sala (ex.: terminado vs eliminado) para ordenação do pódio em eliminatórias grupo. */
        val estadoPartida: String = ""
    ) {
        val identificadorEstatisticas: String
            get() = uid.ifBlank { chave.ifBlank { nomeUtilizador.ifBlank { nomeJogador.ifBlank { nome } } } }

        val chavesCompatibilidade: List<String>
            get() = listOf(identificadorEstatisticas, uid, chave, nomeUtilizador, nomeJogador, nome)
                .filter { it.isNotBlank() }
                .distinct()

        fun corresponde(identificador: String): Boolean {
            return identificador.isNotBlank() && identificador in chavesCompatibilidade
        }
    }

    data class EstatisticasAtuais(
        val pontuacao: Double,
        val recordePontuacao: Double,
        val taxaAcertos: Double,
        val totalJogos: Int,
        val totalVitorias: Int,
        val totalRespostasCertas: Int,
        val totalVitoriasModo1x1: Int,
        val totalVitoriasModo2x2: Int,
        val totalVitoriasModoSolo: Int,
        val xpTotal: Int,
        val totalPontosSomados: Double
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

    /**
     * Eliminatórias em grupo: vencedor/sobrevivente (terminado) primeiro, depois pontuação e respostas certas.
     */
    fun ordenarPodioGrupoEliminatorias(resultados: List<ResultadoJogador>): List<ResultadoJogador> {
        return resultados.sortedWith(
            compareByDescending<ResultadoJogador> { it.estadoPartida == GameConstants.ESTADO_TERMINADO }
                .thenByDescending { it.pontos }
                .thenByDescending { it.respostasCertas }
        )
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
            Modo.UM_CONTRA_UM -> setOfNotNull(ordenarPodio(resultados).firstOrNull()?.identificadorEstatisticas)

            Modo.GRUPO -> setOfNotNull(resultados.firstOrNull()?.identificadorEstatisticas)

            Modo.DOIS_CONTRA_DOIS -> {
                val totalA = resultados.filter { it.equipa == GameConstants.EQUIPA_A }.sumOf { it.pontos }
                val totalB = resultados.filter { it.equipa == GameConstants.EQUIPA_B }.sumOf { it.pontos }
                if (totalA == totalB) return emptySet()
                val equipaVencedora = if (totalA > totalB) GameConstants.EQUIPA_A else GameConstants.EQUIPA_B
                resultados.filter { it.equipa == equipaVencedora }
                    .map { it.identificadorEstatisticas }
                    .toSet()
            }
        }
    }

    fun deveAtualizarEstatisticas(jaAtualizadas: Boolean, resultados: List<ResultadoJogador>): Boolean {
        return !jaAtualizadas && resultados.any { it.identificadorEstatisticas.isNotBlank() }
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
        val contaVitoriaCompetitiva = venceu && modo != Modo.SOLO
        val novoTotalVitorias = estatisticasAtuais.totalVitorias + if (contaVitoriaCompetitiva) 1 else 0
        val novaTaxa = calcularTaxaAcertos(
            taxaAcertosAnterior = estatisticasAtuais.taxaAcertos,
            totalJogosAnterior = estatisticasAtuais.totalJogos,
            respostasCertas = resultado.respostasCertas,
            totalPerguntas = totalPerguntas
        )

        val novoRecorde = maxOf(estatisticasAtuais.recordePontuacao, resultado.pontos)

        val novoTotalPontosSomados = estatisticasAtuais.totalPontosSomados + resultado.pontos
        val updates = mutableMapOf<String, Any>(
            FirebasePaths.PONTUACAO to novoTotalPontosSomados,
            FirebasePaths.TOTAL_PONTOS_SOMADOS to novoTotalPontosSomados,
            FirebasePaths.RECORDE_PONTUACAO to novoRecorde,
            FirebasePaths.TOTAL_JOGOS to novoTotalJogos,
            FirebasePaths.TOTAL_VITORIAS to novoTotalVitorias,
            FirebasePaths.TOTAL_RESPOSTAS_CERTAS to (estatisticasAtuais.totalRespostasCertas + resultado.respostasCertas),
            FirebasePaths.TAXA_ACERTOS to novaTaxa
        )

        val xpGanho = progressaoService.calcularXpGanho(
            respostasCertas = resultado.respostasCertas,
            venceu = venceu
        )
        val novoXpTotal = estatisticasAtuais.xpTotal + xpGanho
        val estadoProgressao = progressaoService.calcularEstadoProgressao(novoXpTotal)

        updates[FirebasePaths.XP_TOTAL] = estadoProgressao.xpTotal
        updates[FirebasePaths.NIVEL] = estadoProgressao.nivel
        updates[FirebasePaths.XP_NO_NIVEL_ATUAL] = estadoProgressao.xpNoNivelAtual
        updates[FirebasePaths.XP_NECESSARIO_PROXIMO_NIVEL] = estadoProgressao.xpNecessarioProximoNivel

        when (modo) {
            Modo.SOLO -> Unit

            Modo.GRUPO -> {
                updates[FirebasePaths.TOTAL_VITORIAS_MODO_SOLO] =
                    estatisticasAtuais.totalVitoriasModoSolo + if (venceu) 1 else 0
            }

            Modo.UM_CONTRA_UM -> {
                updates[FirebasePaths.TOTAL_VITORIAS_MODO_1X1] =
                    estatisticasAtuais.totalVitoriasModo1x1 + if (venceu) 1 else 0
            }

            Modo.DOIS_CONTRA_DOIS -> {
                updates[FirebasePaths.TOTAL_VITORIAS_MODO_2X2] =
                    estatisticasAtuais.totalVitoriasModo2x2 + if (venceu) 1 else 0
            }
        }

        return updates
    }
}
