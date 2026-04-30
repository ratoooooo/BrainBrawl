package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
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
        val totalVitoriasModoSolo: Int,
        val xpTotal: Int,
        val nivel: Int,
        val xpNoNivelAtual: Int,
        val xpNecessarioProximoNivel: Int
    )

    data class PerfilJogador(
        val uid: String,
        val nome: String,
        val nomeUtilizador: String,
        val email: String,
        val password: String,
        val avatar: String,
        val estado: String,
        val estatisticas: EstatisticasJogador
    )

    fun obterPerfil(identificador: String): Task<PerfilJogador?> {
        val result = TaskCompletionSource<PerfilJogador?>()
        procurarJogador(
            identificador = identificador,
            onSuccess = { snapshot ->
                result.setResult(snapshot?.toPerfilJogador())
            },
            onFailure = { exception ->
                result.setException(exception)
            }
        )
        return result.task
    }

    fun verificarJogadorExiste(nomeJogador: String): Task<Boolean> {
        val result = TaskCompletionSource<Boolean>()
        procurarJogador(
            identificador = nomeJogador,
            onSuccess = { snapshot ->
                result.setResult(snapshot != null)
            },
            onFailure = { exception ->
                result.setException(exception)
            }
        )
        return result.task
    }

    fun criarJogador(nomeJogador: String, passwordHash: String, avatar: String): Task<Void> {
        val jogadorData = mapOf(
            FirebasePaths.NOME_UTILIZADOR to nomeJogador,
            FirebasePaths.PASSWORD to passwordHash,
            FirebasePaths.AVATAR to avatar,
            FirebasePaths.ESTADO to GameConstants.ESTADO_ON,
            FirebasePaths.PONTUACAO to 0.0,
            FirebasePaths.TAXA_ACERTOS to 0.0,
            FirebasePaths.TOTAL_JOGOS to 0,
            FirebasePaths.TOTAL_VITORIAS to 0,
            FirebasePaths.TOTAL_RESPOSTAS_CERTAS to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_2X2 to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_1X1 to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_SOLO to 0,
            FirebasePaths.XP_TOTAL to 0,
            FirebasePaths.NIVEL to 1,
            FirebasePaths.XP_NO_NIVEL_ATUAL to 0,
            FirebasePaths.XP_NECESSARIO_PROXIMO_NIVEL to 300
        )
        return jogadorRef(nomeJogador).setValue(jogadorData)
    }

    fun criarPerfilAutenticado(
        uid: String,
        nomeUtilizador: String,
        email: String,
        avatar: String
    ): Task<Void> {
        val jogadorData = mapOf(
            FirebasePaths.UID to uid,
            FirebasePaths.NOME_UTILIZADOR to nomeUtilizador,
            FirebasePaths.EMAIL to email,
            FirebasePaths.AVATAR to avatar,
            FirebasePaths.ESTADO to GameConstants.ESTADO_ON,
            FirebasePaths.PONTUACAO to 0.0,
            FirebasePaths.TAXA_ACERTOS to 0.0,
            FirebasePaths.TOTAL_JOGOS to 0,
            FirebasePaths.TOTAL_RESPOSTAS_CERTAS to 0,
            FirebasePaths.TOTAL_VITORIAS to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_1X1 to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_2X2 to 0,
            FirebasePaths.TOTAL_VITORIAS_MODO_SOLO to 0,
            FirebasePaths.XP_TOTAL to 0,
            FirebasePaths.NIVEL to 1,
            FirebasePaths.XP_NO_NIVEL_ATUAL to 0,
            FirebasePaths.XP_NECESSARIO_PROXIMO_NIVEL to 300
        )
        return jogadorRef(uid).setValue(jogadorData)
    }

    fun obterAvatar(identificador: String): Task<String> {
        val result = TaskCompletionSource<String>()
        procurarJogador(
            identificador = identificador,
            onSuccess = { snapshot ->
                result.setResult(snapshot?.child(FirebasePaths.AVATAR)?.getValue(String::class.java) ?: AVATAR_PADRAO)
            },
            onFailure = { exception ->
                result.setException(exception)
            }
        )
        return result.task
    }

    fun atualizarEstado(identificador: String, estado: String): Task<Void> {
        val result = TaskCompletionSource<Void>()
        procurarJogador(
            identificador = identificador,
            onSuccess = { snapshot ->
                val key = snapshot?.key
                if (key == null) {
                    result.setResult(null)
                } else {
                    jogadorRef(key).child(FirebasePaths.ESTADO).setValue(estado)
                        .addOnSuccessListener {
                            result.setResult(null)
                        }
                        .addOnFailureListener { exception ->
                            result.setException(exception)
                        }
                }
            },
            onFailure = { exception ->
                result.setException(exception)
            }
        )
        return result.task
    }

    fun marcarOnline(identificador: String): Task<Void> {
        return atualizarEstado(identificador, GameConstants.ESTADO_ON)
    }

    fun marcarOffline(identificador: String): Task<Void> {
        return atualizarEstado(identificador, GameConstants.ESTADO_OFF)
    }

    private fun procurarJogador(
        identificador: String,
        onSuccess: (DataSnapshot?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (identificador.isBlank()) {
            onSuccess(null)
            return
        }

        jogadorRef(identificador).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onSuccess(snapshot)
                    return@addOnSuccessListener
                }

                jogadoresRef()
                    .orderByChild(FirebasePaths.NOME_UTILIZADOR)
                    .equalTo(identificador)
                    .limitToFirst(1)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        onSuccess(querySnapshot.children.firstOrNull())
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    private fun jogadoresRef(): DatabaseReference {
        return database.child(FirebasePaths.JOGADORES)
    }

    private fun jogadorRef(identificador: String): DatabaseReference {
        return jogadoresRef().child(identificador)
    }

    private fun DataSnapshot.toPerfilJogador(): PerfilJogador? {
        if (!exists()) return null

        val key = key.orEmpty()
        val uid = child(FirebasePaths.UID).getValue(String::class.java) ?: key
        val nomeUtilizador = child(FirebasePaths.NOME_UTILIZADOR).getValue(String::class.java)
            ?: child(FirebasePaths.NOME).getValue(String::class.java)
            ?: key

        return PerfilJogador(
            uid = uid,
            nome = nomeUtilizador,
            nomeUtilizador = nomeUtilizador,
            email = child(FirebasePaths.EMAIL).getValue(String::class.java).orEmpty(),
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
                totalVitoriasModoSolo = child(FirebasePaths.TOTAL_VITORIAS_MODO_SOLO).intValue(),
                xpTotal = child(FirebasePaths.XP_TOTAL).intValue(),
                nivel = child(FirebasePaths.NIVEL).intValue().coerceAtLeast(1),
                xpNoNivelAtual = child(FirebasePaths.XP_NO_NIVEL_ATUAL).intValue(),
                xpNecessarioProximoNivel = child(FirebasePaths.XP_NECESSARIO_PROXIMO_NIVEL).intValue().takeIf { it > 0 } ?: 300
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
