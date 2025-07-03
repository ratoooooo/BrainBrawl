package com.example.brainbrawl

import java.security.MessageDigest

object UteisValidacao {
    // Função utilizada para validar os campos
    fun validarCampos(nome: String, password: String? = null): String? {
        if (nome.isEmpty() || (password != null && password.isEmpty())) {
            return "Preencha todos os campos"
        }

        if (nome.length < 3 || nome.length > 20) {
            return "O nome deve ter entre 3 e 20 caracteres"
        }

        if (!nome.matches(Regex("^[\\p{L}0-9_]+$"))) {
            return "O nome só pode conter letras, números e underscores"
        }

        if (password != null) {
            if (password.length < 8 || password.length > 20) {
                return "A senha deve ter entre 8 e 20 caracteres"
            }
        }

        return null
    }

    // Encriptar senha usando SHA-256
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

}