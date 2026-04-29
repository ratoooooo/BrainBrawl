package com.example.brainbrawl.utils

object CodigoSalaUtils {
    fun gerarCodigoSala(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
