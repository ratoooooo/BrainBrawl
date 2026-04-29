package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class JogadorRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class EstatisticasJogador(
        val pontuacao: Double,
        val taxaAcertos: Double,
        val totalJogos: Int,
        val totalVitorias: Int,
        val totalRespostasCertas: Int,
        val totalVitoriasModo1x1: Int,
        val totalVitoriasModo2x2: Int,
        val totalVitoriasModoSolo: Int
    )

    data class PerfilJogador(
        val nome: String,
        val password: String,
        val avatar: String,
        val estado: String,
        val estatisticas: EstatisticasJogador
    )

    fun obterPerfil(nomeJogador: String): Task<PerfilJogador?> {
        return jogadorRef(nomeJogador).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao obter perfil.")
            task.result.toPerfilJogador(nomeJogador)
        }
    }

    fun verificarJogadorExiste(nomeJogador: String): Task<Boolean> {
        return jogadorRef(nomeJogador).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao verificar jogador.")
            task.result.exists()
        }
    }

    fun criarJogador(nomeJogador: String, passwordHash: String, avatar: String): Task<Void> {
        val jogadorData = mapOf(
            FirebasePaths.PASSWORD to passwordHash,
            FirebasePaths.AVATAR to avatar,
            FirebasePaths.PONTUACAO to 0.0,
            FirebasePaths.TOTAL_JOGOS to 0,
            FirebasePaths.TOTAL_VITORIAS to 0,
            FirebasePaths.TOTAL_RESPOSTAS_CERTAS to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_2X2 to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_1X1 to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_SOLO to 0
        )
        return jogadorRef(nomeJogador).setValue(jogadorData)
    }

    fun obterAvatar(nomeJogador: String): Task<String> {
        return jogadorRef(nomeJogador).child(FirebasePaths.AVATAR).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao obter avatar.")
            task.result.getValue(String::class.java) ?: AVATAR_PADRAO
        }
    }

    fun atualizarEstado(nomeJogador: String, estado: String): Task<Void> {
        return jogadorRef(nomeJogador).child(FirebasePaths.ESTADO).setValue(estado)
    }

    fun marcarOnline(nomeJogador: String): Task<Void> {
        return atualizarEstado(nomeJogador, GameConstants.ESTADO_ON)
    }

    fun marcarOffline(nomeJogador: String): Task<Void> {
        return atualizarEstado(nomeJogador, GameConstants.ESTADO_OFF)
    }

    private fun jogadorRef(nomeJogador: String): DatabaseReference {
        return database.child(FirebasePaths.JOGADORES).child(nomeJogador)
    }

    private fun DataSnapshot.toPerfilJogador(nomeJogador: String): PerfilJogador? {
        if (!exists()) return null

        return PerfilJogador(
            nome = nomeJogador,
            password = child(FirebasePaths.PASSWORD).getValue(String::class.java).orEmpty(),
            avatar = child(FirebasePaths.AVATAR).getValue(String::class.java) ?: AVATAR_PADRAO,
            estado = child(FirebasePaths.ESTADO).getValue(String::class.java) ?: GameConstants.ESTADO_OFF,
            estatisticas = EstatisticasJogador(
                pontuacao = child(FirebasePaths.PONTUACAO).doubleValue(),
                taxaAcertos = child(FirebasePaths.TAXA_ACERTOS).doubleValue(),
                totalJogos = child(FirebasePaths.TOTAL_JOGOS).intValue(),
                totalVitorias = child(FirebasePaths.TOTAL_VITORIAS).intValue(),
                totalRespostasCertas = child(FirebasePaths.TOTAL_RESPOSTAS_CERTAS).intValue(),
                totalVitoriasModo1x1 = child(FirebasePaths.TOTAL_VITORIAS_MODO_1X1).intValue(),
                totalVitoriasModo2x2 = child(FirebasePaths.TOTAL_VITORIAS_MODO_2X2).intValue(),
                totalVitoriasModoSolo = child(FirebasePaths.TOTAL_VITORIAS_MODO_SOLO).intValue()
            )
        )
    }

    private fun DataSnapshot.intValue(): Int {
        return getValue(Int::class.java)
            ?: getValue(Long::class.java)?.toInt()
            ?: getValue(Double::class.java)?.toInt()
            ?: 0
    }

    private fun DataSnapshot.doubleValue(): Double {
        return getValue(Double::class.java)
            ?: getValue(Long::class.java)?.toDouble()
            ?: getValue(Int::class.java)?.toDouble()
            ?: 0.0
    }

    private companion object {
        const val AVATAR_PADRAO = "avatar_1_playstore"
    }
}
