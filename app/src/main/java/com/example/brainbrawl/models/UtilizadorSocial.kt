package com.example.brainbrawl.models

data class UtilizadorSocial(
    val uid: String = "",
    val nomeUtilizador: String = "",
    val chavePerfil: String = "",
    val chaveOrigem: String = ""
) {
    val chavePrimaria: String
        get() = uid.ifBlank { chavePerfil.ifBlank { nomeUtilizador.ifBlank { chaveOrigem } } }

    val chaveConvite: String
        get() = uid.ifBlank { chavePerfil.ifBlank { nomeUtilizador.ifBlank { chaveOrigem } } }

    val chaveDonoSocial: String
        get() = chavePerfil.ifBlank { uid.ifBlank { nomeUtilizador.ifBlank { chaveOrigem } } }

    val nomeDisplay: String
        get() = nomeUtilizador.ifBlank { chaveOrigem.ifBlank { chavePerfil.ifBlank { uid } } }

    val chavesCompatibilidade: List<String>
        get() = listOf(chavePrimaria, chavePerfil, nomeUtilizador, chaveOrigem)
            .filter { it.isNotBlank() }
            .distinct()

    val chavesDonoSocial: List<String>
        get() = listOf(chaveDonoSocial, uid, chavePerfil, nomeUtilizador, chaveOrigem)
            .filter { it.isNotBlank() }
            .distinct()

    val chaveDedupe: String
        get() = uid.ifBlank { nomeUtilizador.ifBlank { chavePrimaria } }

    fun corresponde(outro: UtilizadorSocial): Boolean {
        return chaveDedupe.isNotBlank() && chaveDedupe == outro.chaveDedupe ||
            chavesCompatibilidade.any { it in outro.chavesCompatibilidade }
    }

    fun corresponde(uidAtual: String, nomeAtual: String): Boolean {
        return (uidAtual.isNotBlank() && uidAtual in chavesCompatibilidade) ||
            (nomeAtual.isNotBlank() && nomeAtual in chavesCompatibilidade)
    }
}
