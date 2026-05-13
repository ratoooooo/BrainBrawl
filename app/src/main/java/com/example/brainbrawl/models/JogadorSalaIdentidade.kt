package com.example.brainbrawl.models

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants

data class JogadorSalaIdentidade(
    val uid: String = "",
    val nomeUtilizador: String = "",
    val nomeJogador: String = "",
    val playerKey: String = "",
    val tipoJogador: String = "",
    val avatar: String = ""
) {
    val chaveSala: String
        get() = uid.ifBlank { playerKey.ifBlank { nomeJogador.ifBlank { nomeUtilizador } } }

    val nomeDisplay: String
        get() = nomeUtilizador.ifBlank { nomeJogador.ifBlank { uid } }

    val chavesCompatibilidade: List<String>
        get() = listOf(chaveSala, uid, playerKey, nomeUtilizador, nomeJogador, nomeDisplay)
            .filter { it.isNotBlank() }
            .distinct()

    fun toFirebaseMap(isHostOnly: Boolean, avatar: String? = null): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.NOME to nomeDisplay,
            FirebasePaths.NOME_DISPLAY to nomeDisplay,
            FirebasePaths.PONTUACAO to 0.0,
            FirebasePaths.TOTAL_RESPOSTAS_CERTAS to 0,
            FirebasePaths.ESTADO to GameConstants.ESTADO_ON,
            FirebasePaths.IS_HOST_ONLY to isHostOnly
        )
        val chaveEfetiva = playerKey.ifBlank { chaveSala }
        if (chaveEfetiva.isNotBlank()) dados[FirebasePaths.PLAYER_KEY] = chaveEfetiva
        if (uid.isNotBlank()) dados[FirebasePaths.UID] = uid
        if (nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = nomeUtilizador
        if (nomeJogador.isNotBlank()) dados[FirebasePaths.NOME_JOGADOR] = nomeJogador
        val avatarEfetivo = avatar?.takeIf { it.isNotBlank() } ?: this.avatar.takeIf { it.isNotBlank() }
        if (!avatarEfetivo.isNullOrBlank()) dados[FirebasePaths.AVATAR] = avatarEfetivo
        val tipoEfetivo = tipoJogador.ifBlank {
            if (uid.isBlank()) GameConstants.TIPO_JOGADOR_GUEST else GameConstants.TIPO_JOGADOR_AUTH
        }
        dados[FirebasePaths.TIPO_JOGADOR] = tipoEfetivo
        dados[FirebasePaths.IS_GUEST] = tipoEfetivo == GameConstants.TIPO_JOGADOR_GUEST
        return dados
    }

    fun corresponde(chaveOuNome: String): Boolean {
        return chaveOuNome.isNotBlank() && chaveOuNome in chavesCompatibilidade
    }

    companion object {
        fun from(uid: String?, nomeUtilizador: String?, nomeJogador: String?): JogadorSalaIdentidade {
            return JogadorSalaIdentidade(
                uid = uid.orEmpty(),
                nomeUtilizador = nomeUtilizador.orEmpty(),
                nomeJogador = nomeJogador.orEmpty()
            )
        }

        fun from(
            uid: String?,
            nomeUtilizador: String?,
            nomeJogador: String?,
            playerKey: String?,
            tipoJogador: String?,
            avatar: String?
        ): JogadorSalaIdentidade {
            return JogadorSalaIdentidade(
                uid = uid.orEmpty(),
                nomeUtilizador = nomeUtilizador.orEmpty(),
                nomeJogador = nomeJogador.orEmpty(),
                playerKey = playerKey.orEmpty(),
                tipoJogador = tipoJogador.orEmpty(),
                avatar = avatar.orEmpty()
            )
        }

        fun fromUtilizadorSocial(utilizador: UtilizadorSocial): JogadorSalaIdentidade {
            return JogadorSalaIdentidade(
                uid = utilizador.uid,
                nomeUtilizador = utilizador.nomeUtilizador,
                nomeJogador = ""
            )
        }
    }
}
