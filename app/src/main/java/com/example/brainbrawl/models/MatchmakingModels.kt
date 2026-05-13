package com.example.brainbrawl.models

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.google.firebase.database.DataSnapshot

data class MatchmakingPlayer(
    val playerKey: String,
    val uid: String,
    val tipoJogador: String,
    val nomeUtilizador: String,
    val nomeJogador: String,
    val nomeDisplay: String,
    val avatar: String,
    val timestampEntrada: Long,
    val estado: String = GameConstants.ESTADO_AGUARDANDO
) {
    val isGuest: Boolean
        get() = tipoJogador == GameConstants.TIPO_JOGADOR_GUEST

    val podeGravarEstatisticas: Boolean
        get() = uid.isNotBlank() && !isGuest

    fun toFirebaseMap(estadoOverride: String = estado): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.PLAYER_KEY to playerKey,
            FirebasePaths.UID to uid,
            FirebasePaths.TIPO_JOGADOR to tipoJogador,
            FirebasePaths.NOME_DISPLAY to nomeDisplay,
            FirebasePaths.TIMESTAMP_ENTRADA to timestampEntrada,
            FirebasePaths.ESTADO to estadoOverride,
            FirebasePaths.IS_GUEST to isGuest
        )
        if (nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = nomeUtilizador
        if (nomeJogador.isNotBlank()) dados[FirebasePaths.NOME_JOGADOR] = nomeJogador
        if (avatar.isNotBlank()) dados[FirebasePaths.AVATAR] = avatar
        return dados
    }

    fun toSalaJogadorMap(): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.NOME to nomeDisplay,
            FirebasePaths.NOME_DISPLAY to nomeDisplay,
            FirebasePaths.PLAYER_KEY to playerKey,
            FirebasePaths.TIPO_JOGADOR to tipoJogador,
            FirebasePaths.IS_GUEST to isGuest,
            FirebasePaths.PONTUACAO to 0.0,
            FirebasePaths.TOTAL_RESPOSTAS_CERTAS to 0,
            FirebasePaths.ESTADO to GameConstants.ESTADO_ON,
            FirebasePaths.IS_HOST_ONLY to false
        )
        if (uid.isNotBlank()) dados[FirebasePaths.UID] = uid
        if (nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = nomeUtilizador
        if (nomeJogador.isNotBlank()) dados[FirebasePaths.NOME_JOGADOR] = nomeJogador
        if (avatar.isNotBlank()) dados[FirebasePaths.AVATAR] = avatar
        return dados
    }

    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): MatchmakingPlayer? {
            val key = snapshot.key.orEmpty()
            val playerKey = snapshot.child(FirebasePaths.PLAYER_KEY).texto().ifBlank { key }
            if (playerKey.isBlank()) return null
            val uid = snapshot.child(FirebasePaths.UID).texto()
            val tipo = snapshot.child(FirebasePaths.TIPO_JOGADOR).texto().ifBlank {
                if (uid.isBlank()) GameConstants.TIPO_JOGADOR_GUEST else GameConstants.TIPO_JOGADOR_AUTH
            }
            val nomeUtilizador = snapshot.child(FirebasePaths.NOME_UTILIZADOR).texto()
            val nomeJogador = snapshot.child(FirebasePaths.NOME_JOGADOR).texto()
            val nomeDisplay = snapshot.child(FirebasePaths.NOME_DISPLAY).texto()
                .ifBlank { nomeUtilizador }
                .ifBlank { nomeJogador }
                .ifBlank { uid }
                .ifBlank { playerKey }
            return MatchmakingPlayer(
                playerKey = playerKey,
                uid = uid,
                tipoJogador = tipo,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                nomeDisplay = nomeDisplay,
                avatar = snapshot.child(FirebasePaths.AVATAR).texto(),
                timestampEntrada = snapshot.child(FirebasePaths.TIMESTAMP_ENTRADA).longValue(),
                estado = snapshot.child(FirebasePaths.ESTADO).texto().ifBlank { GameConstants.ESTADO_AGUARDANDO }
            )
        }
    }
}

data class MatchmakingResult(
    val playerKey: String,
    val uid: String,
    val tipoJogador: String,
    val codigoSala: String,
    val modo: String,
    val nomeCategoria: String,
    val criadorKey: String,
    val criadorUid: String,
    val estado: String,
    val jogadores: List<MatchmakingPlayer>
) {
    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): MatchmakingResult? {
            if (!snapshot.exists()) return null
            val jogadores = snapshot.child(FirebasePaths.JOGADORES).children.mapNotNull {
                MatchmakingPlayer.fromSnapshot(it)
            }
            return MatchmakingResult(
                playerKey = snapshot.child(FirebasePaths.PLAYER_KEY).texto().ifBlank { snapshot.key.orEmpty() },
                uid = snapshot.child(FirebasePaths.UID).texto(),
                tipoJogador = snapshot.child(FirebasePaths.TIPO_JOGADOR).texto(),
                codigoSala = snapshot.child(FirebasePaths.CODIGO_SALA).texto(),
                modo = snapshot.child(FirebasePaths.MODO).texto(),
                nomeCategoria = snapshot.child(FirebasePaths.NOME_CATEGORIA).texto(),
                criadorKey = snapshot.child(FirebasePaths.CRIADOR_ID).texto(),
                criadorUid = snapshot.child(FirebasePaths.CRIADOR_UID).texto(),
                estado = snapshot.child(FirebasePaths.ESTADO).texto(),
                jogadores = jogadores
            ).takeIf { it.codigoSala.isNotBlank() && it.estado == GameConstants.ESTADO_ENCONTRADO }
        }
    }
}

private fun DataSnapshot.texto(): String = getValue(String::class.java).orEmpty()

private fun DataSnapshot.longValue(): Long {
    return getValue(Long::class.java)
        ?: getValue(Int::class.java)?.toLong()
        ?: getValue(Double::class.java)?.toLong()
        ?: 0L
}
