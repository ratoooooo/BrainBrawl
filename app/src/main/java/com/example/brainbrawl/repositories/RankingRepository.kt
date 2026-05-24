package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.models.RankingJogador
import com.example.brainbrawl.models.RankingTipo
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class RankingRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {

    fun carregarRankingGlobal(limite: Int = LIMITE_PADRAO): Task<List<RankingJogador>> {
        return carregarRankingPorTipo(RankingTipo.GLOBAL, limite)
    }

    fun carregarRankingPorTipo(
        tipo: RankingTipo,
        limite: Int = LIMITE_PADRAO
    ): Task<List<RankingJogador>> {
        val result = TaskCompletionSource<List<RankingJogador>>()

        jogadoresRef()
            .get()
            .addOnSuccessListener { snapshot ->
                val jogadores = snapshot.rankingOrdenado(tipo)
                    .take(limite.coerceAtLeast(1))

                result.setResult(jogadores)
            }
            .addOnFailureListener { exception ->
                result.setException(exception)
            }

        return result.task
    }

    fun obterPosicaoGlobal(
        uid: String,
        nomeUtilizador: String
    ): Task<Int?> {
        val result = TaskCompletionSource<Int?>()
        jogadoresRef()
            .get()
            .addOnSuccessListener { snapshot ->
                val uidNormalizado = uid.trim()
                val nomeNormalizado = nomeUtilizador.lowercase(Locale.ROOT).trim()
                val posicao = snapshot.rankingOrdenado(RankingTipo.GLOBAL)
                    .firstOrNull { jogador ->
                        (uidNormalizado.isNotBlank() &&
                            (jogador.uid == uidNormalizado || jogador.chavePerfil == uidNormalizado)) ||
                            (nomeNormalizado.isNotBlank() &&
                                jogador.nomeDisplay.lowercase(Locale.ROOT).trim() == nomeNormalizado)
                    }
                    ?.posicao
                result.setResult(posicao)
            }
            .addOnFailureListener { exception ->
                result.setException(exception)
            }
        return result.task
    }

    private fun jogadoresRef(): DatabaseReference {
        return database.child(FirebasePaths.JOGADORES)
    }

    private fun DataSnapshot.rankingOrdenado(tipo: RankingTipo): List<RankingJogador> {
        return children
            .mapNotNull { it.toRankingJogador() }
            .deduplicarPerfis(tipo)
            .sortedWith(
                compareByDescending<RankingJogador> { tipo.valorOrdenacao(it) }
                    .thenBy { it.nomeDisplay.lowercase(Locale.ROOT) }
            )
            .mapIndexed { index, jogador -> jogador.copy(posicao = index + 1) }
    }

    private fun DataSnapshot.toRankingJogador(): RankingJogador? {
        if (!exists()) return null
        if (child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true) return null

        val chavePerfil = key.orEmpty()
        val uid = child(FirebasePaths.UID).getValue(String::class.java).orEmpty()
        val temPerfilPersistente = uid.isNotBlank() ||
            child(FirebasePaths.NOME_UTILIZADOR).exists() ||
            child(FirebasePaths.EMAIL).exists() ||
            child(FirebasePaths.PASSWORD).exists()

        if (!temPerfilPersistente) return null

        val nomeDisplay = child(FirebasePaths.NOME_UTILIZADOR).texto()
            .ifBlank { child(FirebasePaths.NOME_DISPLAY).texto() }
            .ifBlank { child(FirebasePaths.NOME).texto() }
            .ifBlank { chavePerfil.takeUnless { uid.isNotBlank() && it == uid }.orEmpty() }

        if (nomeDisplay.isBlank()) return null
        val pontuacaoLegada = child(FirebasePaths.PONTUACAO).doubleValue()
        val totalPontosSomados = child(FirebasePaths.TOTAL_PONTOS_SOMADOS).doubleValue()
            .takeIf { it > 0.0 }
            ?: pontuacaoLegada
        return RankingJogador(
            chavePerfil = chavePerfil,
            uid = uid,
            nomeDisplay = nomeDisplay,
            avatar = child(FirebasePaths.AVATAR).texto(),
            pontuacao = pontuacaoLegada,
            totalPontosSomados = totalPontosSomados,
            recordePontuacao = child(FirebasePaths.RECORDE_PONTUACAO).doubleValue(),
            totalJogos = child(FirebasePaths.TOTAL_JOGOS).intValue(),
            totalVitorias = child(FirebasePaths.TOTAL_VITORIAS).intValue(),
            taxaAcertos = child(FirebasePaths.TAXA_ACERTOS).doubleValue(),
            totalVitoriasModoSolo = child(FirebasePaths.TOTAL_VITORIAS_MODO_SOLO).intValue(),
            totalVitoriasModo1x1 = child(FirebasePaths.TOTAL_VITORIAS_MODO_1X1).intValue(),
            totalVitoriasModo2x2 = child(FirebasePaths.TOTAL_VITORIAS_MODO_2X2).intValue(),
            nivel = child(FirebasePaths.NIVEL).intValue().coerceAtLeast(1)
        )
    }

    private fun List<RankingJogador>.deduplicarPerfis(tipo: RankingTipo): List<RankingJogador> {
        val porIdentidade = linkedMapOf<String, RankingJogador>()

        forEach { jogador ->
            val identidade = jogador.nomeDisplay.lowercase(Locale.ROOT).trim()
                .ifBlank { jogador.uid.ifBlank { jogador.chavePerfil } }
            val existente = porIdentidade[identidade]

            if (existente == null || jogador.deveSubstituir(existente, tipo)) {
                porIdentidade[identidade] = jogador
            }
        }

        return porIdentidade.values.toList()
    }

    private fun RankingJogador.deveSubstituir(outro: RankingJogador, tipo: RankingTipo): Boolean {
        if (uid.isNotBlank() && outro.uid.isBlank()) return true
        if (uid.isBlank() && outro.uid.isNotBlank()) return false
        return tipo.valorOrdenacao(this) > tipo.valorOrdenacao(outro)
    }

    private fun DataSnapshot.texto(): String {
        return getValue(String::class.java).orEmpty()
    }

    private fun DataSnapshot.intValue(): Int {
        return (value as? Number)?.toInt()
            ?: getValue(String::class.java)?.toIntOrNull()
            ?: 0
    }

    private fun DataSnapshot.doubleValue(): Double {
        return (value as? Number)?.toDouble()
            ?: getValue(String::class.java)?.replace(',', '.')?.toDoubleOrNull()
            ?: 0.0
    }

    private companion object {
        const val LIMITE_PADRAO = 10
    }
}
