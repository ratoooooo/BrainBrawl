package com.example.brainbrawl

import android.content.Context
import com.example.brainbrawl.UteisNavegacao.abrirSalaDeEsperaGrupo
import com.example.brainbrawl.repositories.CategoriaRepository
import com.example.brainbrawl.repositories.SalaRepository

object UteisSala {
    private val salaRepository = SalaRepository()
    private val categoriaRepository = CategoriaRepository()

    // Função utilizada para gerar um código de sala
    fun gerarCodigoSala(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    // Funçao utilizada para criar uma sala caótica e ir buscar todas as perguntas de todas as categorias
    fun criarSalaCaoticaEEntrar(context: Context, nomeUtilizador: String?, nomeJogador: String?, onError: (String) -> Unit = {}) {
        val codigoSala = gerarCodigoSala()
        // Determina o nome do admin e do jogador principal
        val nomeAdmin = nomeUtilizador ?: nomeJogador
        if (nomeAdmin == null) {
            onError("Nome de utilizador ou jogador não fornecido!")
            return
        }
        // Busca todas as perguntas de todas as categorias
        categoriaRepository.carregarTodasPerguntasOficiais().addOnSuccessListener { todasPerguntas ->
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
            salaRepository.criarSala(codigoSala, salaData).addOnSuccessListener {
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
        val nomeAdmin = nomeUtilizador ?: nomeJogador
        if (nomeAdmin == null) {
            onError("Nome de utilizador ou jogador não fornecido!")
            return
        }
        // Busca as perguntas da categoria específica
        categoriaRepository.carregarPerguntasCategoriaOficial(nomeCategoria).addOnSuccessListener { perguntas ->
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
            salaRepository.criarSala(codigoSala, salaData).addOnSuccessListener {
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
        categoriaRepository.carregarPerguntasCategoriaPersonalizada(nomeUtilizador, nomeCategoria)
            .addOnSuccessListener { perguntas ->
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

                salaRepository.criarSala(codigoSala, salaData).addOnSuccessListener {
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
        val nomeAdmin = nomeUtilizador ?: nomeJogador
        if (nomeAdmin.isNullOrBlank()) {
            onError("Nome de utilizador ou jogador não fornecido!")
            return
        }

        categoriaRepository.carregarCategoriaPublica(categoriaPublicaId).addOnSuccessListener { detalhe ->
            if (detalhe == null) {
                onError("Categoria pública não encontrada.")
                return@addOnSuccessListener
            }

            val nomeCategoria = detalhe.categoria.nome
            val criador = detalhe.categoria.criador
            val criadorId = detalhe.categoria.criadorId
            val perguntas = detalhe.perguntas

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

            salaRepository.criarSala(codigoSala, salaData).addOnSuccessListener {
                categoriaRepository.incrementarUsos(categoriaPublicaId)
                abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, nomeJogador, nomeCategoria, admin, modoJogo)
            }.addOnFailureListener { onError(it.message ?: "Erro desconhecido") }
        }.addOnFailureListener { onError(it.message ?: "Erro ao buscar categoria pública") }
    }
}
