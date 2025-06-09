package com.example.brainbrawl

import android.content.Context
import android.content.Intent
import java.security.MessageDigest
import kotlin.random.Random

object Uteis {

    fun validarCampos(nome: String, senha: String? = null): String? {
        if (nome.isEmpty() || (senha != null && senha.isEmpty())) {
            return "Preencha todos os campos"
        }

        if (nome.length < 3 || nome.length > 20) {
            return "O nome deve ter entre 3 e 20 caracteres"
        }

        if (!nome.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return "O nome só pode conter letras, números e underscores"
        }

        if (senha != null) {
            if (senha.length < 8 || senha.length > 20) {
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

// Função para abrir a MainActivity com os parâmetros necessários
    fun abrirMainActivity(context: Context, nomeCategoria: String?, modoJogo: String, nomeUtilizador: String?) {
        // Criar intent para MainActivity
        val intent = Intent(context, MainActivity::class.java)
        //Guardar o código da sala
        intent.putExtra("codigoSala", gerarCodigoSala())
        if (nomeCategoria != null)
        {
            intent.putExtra("nomeCategoria", nomeCategoria)
        }
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("modoJogo", modoJogo)
        context.startActivity(intent)
    }

    //Gerar código da sala
    fun gerarCodigoSala(): String {
        return Random.nextInt(1000, 9999).toString()
    }

    // Função para abrir activity de categoria
     fun abrirEscolherCategoriaActivity(context: Context, modoJogo: String, nomeUtilizador: String?) {
        val intent = Intent(context, EscolherCategoriaActivity::class.java)
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        context.startActivity(intent)
    }

}