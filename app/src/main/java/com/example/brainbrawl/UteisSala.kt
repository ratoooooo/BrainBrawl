package com.example.brainbrawl

import android.content.Context
import com.example.brainbrawl.UteisNavegacao.abrirSalaDeEsperaGrupo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

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
                    nomeAdmin to mapOf("nome" to nomeAdmin, "pontuacao" to 0.0, "isHostOnly" to true)
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

    fun criarSalaPersonalizadaEEntrar(
        context: Context,
        codigoSala: String,
        nomeUtilizador: String,
        nomeCategoria: String,
        admin: Boolean,
        modoJogo: String,
        onError: (String) -> Unit = {}
    ) {
        val database = FirebaseDatabase.getInstance().reference
        database.child("jogadores").child(nomeUtilizador)
            .child("categoriasPersonalizadas").child(nomeCategoria).child("perguntas")
            .get().addOnSuccessListener { snapshot ->
                val perguntas = mutableListOf<Map<String, Any>>()
                snapshot.children.forEach { perguntaSnap ->
                    val pergunta = perguntaSnap.child("pergunta").getValue(String::class.java)
                    val respostaCorreta = perguntaSnap.child("respostaCorreta").getValue(String::class.java)
                    val opcoes = perguntaSnap.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
                    if (!pergunta.isNullOrBlank() && !respostaCorreta.isNullOrBlank() && opcoes.size >= 2) {
                        perguntas.add(mapOf("pergunta" to pergunta, "respostaCorreta" to respostaCorreta, "opcoes" to opcoes))
                    }
                }

                if (perguntas.isEmpty()) {
                    onError("A categoria personalizada ainda não tem perguntas válidas.")
                    return@addOnSuccessListener
                }

                val salaData = mapOf(
                    "horaCriacao" to System.currentTimeMillis(),
                    "admin" to nomeUtilizador,
                    "estado" to "em_espera",
                    "modoJogo" to modoJogo,
                    "jogadores" to mapOf<String, Any>(),
                    "categoria" to nomeCategoria,
                    "nomeCategoria" to nomeCategoria,
                    "categoriaPersonalizada" to true,
                    "donoCategoria" to nomeUtilizador,
                    "perguntas" to perguntas.shuffled().take(8)
                )

                database.child("salas").child(codigoSala).setValue(salaData).addOnSuccessListener {
                    abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, null, nomeCategoria, admin, modoJogo)
                }.addOnFailureListener { onError(it.message ?: "Erro desconhecido") }
            }.addOnFailureListener { onError(it.message ?: "Erro ao buscar perguntas personalizadas") }
    }

    fun criarSalaCategoriaPublicaEEntrar(
        context: Context,
        codigoSala: String,
        nomeUtilizador: String?,
        nomeJogador: String?,
        categoriaPublicaId: String,
        admin: Boolean,
        modoJogo: String = "classico",
        onError: (String) -> Unit = {}
    ) {
        val database = FirebaseDatabase.getInstance().reference
        val nomeAdmin = nomeUtilizador ?: nomeJogador
        if (nomeAdmin.isNullOrBlank()) {
            onError("Nome de utilizador ou jogador não fornecido!")
            return
        }

        val categoriaRef = database.child("categoriasPublicas").child(categoriaPublicaId)
        categoriaRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                onError("Categoria pública não encontrada.")
                return@addOnSuccessListener
            }

            val nomeCategoria = snapshot.child("nome").getValue(String::class.java) ?: "Categoria pública"
            val criador = snapshot.child("criador").getValue(String::class.java)
                ?: snapshot.child("criadorId").getValue(String::class.java)
                ?: ""
            val criadorId = snapshot.child("criadorId").getValue(String::class.java)
                ?: snapshot.child("nomeUtilizador").getValue(String::class.java)
                ?: ""
            val perguntas = mutableListOf<Map<String, Any>>()
            snapshot.child("perguntas").children.forEach { perguntaSnap ->
                val pergunta = perguntaSnap.child("pergunta").getValue(String::class.java)
                val respostaCorreta = perguntaSnap.child("respostaCorreta").getValue(String::class.java)
                val opcoes = perguntaSnap.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
                if (!pergunta.isNullOrBlank() && !respostaCorreta.isNullOrBlank() && opcoes.size == 4) {
                    perguntas.add(
                        mapOf(
                            "pergunta" to pergunta,
                            "respostaCorreta" to respostaCorreta,
                            "opcoes" to opcoes
                        )
                    )
                }
            }

            if (perguntas.isEmpty()) {
                onError("Esta categoria pública não tem perguntas válidas.")
                return@addOnSuccessListener
            }

            val salaData = mapOf(
                "horaCriacao" to System.currentTimeMillis(),
                "admin" to nomeAdmin,
                "estado" to "em_espera",
                "modoJogo" to modoJogo,
                "jogadores" to mapOf<String, Any>(),
                "categoria" to nomeCategoria,
                "nomeCategoria" to nomeCategoria,
                "categoriaPublica" to true,
                "categoriaPublicaId" to categoriaPublicaId,
                "criadorCategoriaPublica" to criador,
                "criadorCategoriaPublicaId" to criadorId,
                "perguntas" to perguntas.shuffled().take(8)
            )

            database.child("salas").child(codigoSala).setValue(salaData).addOnSuccessListener {
                categoriaRef.child("usos").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val usosAtuais = currentData.getValue(Int::class.java) ?: 0
                        currentData.value = usosAtuais + 1
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) = Unit
                })
                abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, nomeJogador, nomeCategoria, admin, modoJogo)
            }.addOnFailureListener { onError(it.message ?: "Erro desconhecido") }
        }.addOnFailureListener { onError(it.message ?: "Erro ao buscar categoria pública") }
    }
}
