package com.example.brainbrawl.utils

object CodigoSalaUtils {
    fun gerarCodigoSala(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    fun normalizarCodigo(codigo: String): String {
        return codigo.trim().uppercase()
    }

    fun codigoTemCaracteresValidos(codigo: String): Boolean {
        return normalizarCodigo(codigo).matches(Regex("^[A-Z0-9]+$"))
    }
}
