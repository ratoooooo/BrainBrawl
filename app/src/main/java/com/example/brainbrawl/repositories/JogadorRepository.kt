package com.example.brainbrawl.repositories

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

    fun obterAvatar(nomeJogador: String): Task<String> {
        return jogadorRef(nomeJogador).child("avatar").get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao obter avatar.")
            task.result.getValue(String::class.java) ?: AVATAR_PADRAO
        }
    }

    fun atualizarEstado(nomeJogador: String, estado: String): Task<Void> {
        return jogadorRef(nomeJogador).child("estado").setValue(estado)
    }

    fun marcarOnline(nomeJogador: String): Task<Void> {
        return atualizarEstado(nomeJogador, "on")
    }

    fun marcarOffline(nomeJogador: String): Task<Void> {
        return atualizarEstado(nomeJogador, "off")
    }

    private fun jogadorRef(nomeJogador: String): DatabaseReference {
        return database.child("jogadores").child(nomeJogador)
    }

    private fun DataSnapshot.toPerfilJogador(nomeJogador: String): PerfilJogador? {
        if (!exists()) return null

        return PerfilJogador(
            nome = nomeJogador,
            password = child("password").getValue(String::class.java).orEmpty(),
            avatar = child("avatar").getValue(String::class.java) ?: AVATAR_PADRAO,
            estado = child("estado").getValue(String::class.java) ?: "off",
            estatisticas = EstatisticasJogador(
                pontuacao = child("pontuacao").doubleValue(),
                taxaAcertos = child("taxaAcertos").doubleValue(),
                totalJogos = child("totalJogos").intValue(),
                totalVitorias = child("totalVitorias").intValue(),
                totalRespostasCertas = child("totalRespostasCertas").intValue(),
                totalVitoriasModo1x1 = child("totalVitoriasModo1x1").intValue(),
                totalVitoriasModo2x2 = child("totalVitoriasModo2x2").intValue(),
                totalVitoriasModoSolo = child("totalVitoriasModoSolo").intValue()
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
