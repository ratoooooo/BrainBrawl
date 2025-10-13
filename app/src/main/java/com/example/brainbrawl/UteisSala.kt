package com.example.brainbrawl

import android.content.Context
import com.example.brainbrawl.UteisNavegacao.abrirSalaDeEsperaGrupo
import com.google.firebase.database.FirebaseDatabase

object UteisSala {
    // Função utilizada para gerar um código de sala
    fun gerarCodigoSala(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    // Funçao utilizada para criar uma sala caótica e ir buscar todas as perguntas de todas as categorias
    fun criarSalaCaoticaEEntrar(context: Context, nomeUtilizador: String?, nomeJogador: String?, onError: (String) -> Unit = {}) {
        val database = FirebaseDatabase.getInstance().reference
        val codigoSala = gerarCodigoSala()
        // Determina o nome do admin e do jogador principal
        val nomeAdmin = nomeUtilizador ?: nomeJogador
        if (nomeAdmin == null) {
            onError("Nome de utilizador ou jogador não fornecido!")
            return
        }
        // Busca todas as perguntas de todas as categorias
        database.child("categorias").get().addOnSuccessListener { snapshot ->
            val todasPerguntas = mutableListOf<Map<String, Any>>()
            snapshot.children.forEach { catSnap ->
                val perguntasSnap = catSnap.child("perguntas")
                perguntasSnap.children.forEach { perguntaSnap ->
                    val pergunta = perguntaSnap.child("pergunta").getValue(String::class.java)
                    val respostaCorreta = perguntaSnap.child("respostaCorreta").getValue(String::class.java)
                    val opcoes = perguntaSnap.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
                    if (pergunta != null && respostaCorreta != null && opcoes.size == 4) {
                        todasPerguntas.add(mapOf("pergunta" to pergunta, "respostaCorreta" to respostaCorreta, "opcoes" to opcoes))
                    }
                }
            }
            val perguntasRandom = todasPerguntas.shuffled().take(8)
            val salaData = mapOf(
                "horaCriacao" to System.currentTimeMillis(),
                "admin" to nomeAdmin,
                "estado" to "em_espera",
                "modoJogo" to "caotico",
                "jogadores" to mapOf<String, Any>(
                    nomeAdmin to mapOf("nome" to nomeAdmin, "pontuacao" to 0.0)
                ),
                "categoria" to "Todas as categorias",
                "perguntas" to perguntasRandom
            )
            database.child("salas").child(codigoSala).setValue(salaData).addOnSuccessListener {
                abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, nomeJogador, "Todas as categorias", true, "caotico")
            }.addOnFailureListener { onError(it.message ?: "Erro desconhecido") }
        }.addOnFailureListener { onError(it.message ?: "Erro ao buscar categorias") }
    }

    // Função utilizada para criar a sala com as perguntas de uma categoria específica
    fun criarSalaComCategoriaEEntrar(
        context: Context,
        codigoSala: String,
        nomeUtilizador: String?,
        nomeJogador: String?,
        nomeCategoria: String,
        admin: Boolean,
        modoJogo: String,
        onError: (String) -> Unit = {}
    ) {
        val database = FirebaseDatabase.getInstance().reference
        val nomeAdmin = nomeUtilizador ?: nomeJogador
        if (nomeAdmin == null) {
            onError("Nome de utilizador ou jogador não fornecido!")
            return
        }
        // Busca as perguntas da categoria específica
        database.child("categorias").child(nomeCategoria).child("perguntas").get().addOnSuccessListener { snapshot ->
            val perguntas = mutableListOf<Map<String, Any>>()
            snapshot.children.forEach { perguntaSnap ->
                val pergunta = perguntaSnap.child("pergunta").getValue(String::class.java)
                val respostaCorreta = perguntaSnap.child("respostaCorreta").getValue(String::class.java)
                val opcoes = perguntaSnap.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
                if (pergunta != null && respostaCorreta != null && opcoes.size == 4) {
                    perguntas.add(mapOf("pergunta" to pergunta, "respostaCorreta" to respostaCorreta, "opcoes" to opcoes))
                }
            }
            val perguntasRandom = perguntas.shuffled().take(8)
            val salaData = mapOf(
                "horaCriacao" to System.currentTimeMillis(),
                "admin" to nomeAdmin,
                "estado" to "em_espera",
                "modoJogo" to modoJogo,
                "jogadores" to mapOf<String, Any>(),
                "categoria" to nomeCategoria,
                "perguntas" to perguntasRandom
            )
            database.child("salas").child(codigoSala).setValue(salaData).addOnSuccessListener {
                abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, nomeJogador, nomeCategoria, admin, modoJogo)
            }.addOnFailureListener { onError(it.message ?: "Erro desconhecido") }
        }.addOnFailureListener { onError(it.message ?: "Erro ao buscar perguntas") }
    }
}