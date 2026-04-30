package com.example.brainbrawl.models

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants

data class JogadorSalaIdentidade(
    val uid: String = "",
    val nomeUtilizador: String = "",
    val nomeJogador: String = ""
) {
    val chaveSala: String
        get() = uid.ifBlank { nomeJogador.ifBlank { nomeUtilizador } }

    val nomeDisplay: String
        get() = nomeUtilizador.ifBlank { nomeJogador.ifBlank { uid } }

    val chavesCompatibilidade: List<String>
        get() = listOf(chaveSala, uid, nomeUtilizador, nomeJogador, nomeDisplay)
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
        if (uid.isNotBlank()) dados[FirebasePaths.UID] = uid
        if (nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = nomeUtilizador
        if (nomeJogador.isNotBlank()) dados[FirebasePaths.NOME_JOGADOR] = nomeJogador
        if (!avatar.isNullOrBlank()) dados[FirebasePaths.AVATAR] = avatar
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

        fun fromUtilizadorSocial(utilizador: UtilizadorSocial): JogadorSalaIdentidade {
            return JogadorSalaIdentidade(
                uid = utilizador.uid,
                nomeUtilizador = utilizador.nomeUtilizador,
                nomeJogador = ""
            )
        }
    }
}
