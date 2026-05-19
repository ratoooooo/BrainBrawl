package com.example.brainbrawl

import android.content.Context
import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.routes.UteisNavegacao.abrirSalaDeEsperaGrupo
import com.example.brainbrawl.repositories.CategoriaRepository
import com.example.brainbrawl.repositories.SalaRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala

object UteisSala {
    private val salaRepository = SalaRepository()
    private val categoriaRepository = CategoriaRepository()
    private val authService = AuthService()

    // Funçao utilizada para criar uma sala caótica e ir buscar todas as perguntas de todas as categorias
    fun criarSalaCaoticaEEntrar(
        context: Context,
        nomeUtilizador: String?,
        nomeJogador: String?,
        uid: String? = null,
        onError: (String) -> Unit = {}
    ) {
        val codigoSala = gerarCodigoSala()
        val jogadorAdmin = identidadeJogador(uid, nomeUtilizador, nomeJogador)
        if (jogadorAdmin.nomeDisplay.isBlank()) {
            onError("Nome de utilizador ou jogador não fornecido!")
            return
        }
        // Busca todas as perguntas de todas as categorias
        categoriaRepository.carregarTodasPerguntasOficiais().addOnSuccessListener { todasPerguntas ->
            val perguntasRandom = todasPerguntas.shuffled().take(8)
            val salaData = dadosSalaBase(jogadorAdmin, "Todas as categorias", GameConstants.MODO_CAOTICO, perguntasRandom) +
                mapOf(
                    FirebasePaths.JOGADORES to mapOf(
                        jogadorAdmin.chaveSala to jogadorAdmin.toFirebaseMap(isHostOnly = false)
                    )
                )
            salaRepository.criarSala(codigoSala, salaData).addOnSuccessListener {
                abrirSalaDeEsperaGrupo(
                    context,
                    codigoSala,
                    nomeUtilizador,
                    nomeJogador,
                    "Todas as categorias",
                    true,
                    GameConstants.MODO_CAOTICO,
                    jogadorAdmin.uid
                )
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
        uid: String? = null,
        onError: (String) -> Unit = {}
    ) {
        val jogadorAdmin = identidadeJogador(uid, nomeUtilizador, nomeJogador)
        if (jogadorAdmin.nomeDisplay.isBlank()) {
            onError("Nome de utilizador ou jogador não fornecido!")
            return
        }
        // Busca as perguntas da categoria específica
        categoriaRepository.carregarPerguntasCategoriaOficial(nomeCategoria).addOnSuccessListener { perguntas ->
            val perguntasRandom = perguntas.shuffled().limitarPerguntasParaModo(modoJogo)
            val salaData = dadosSalaBase(jogadorAdmin, nomeCategoria, modoJogo, perguntasRandom)
            salaRepository.criarSala(codigoSala, salaData).addOnSuccessListener {
                abrirSalaDeEsperaGrupo(
                    context,
                    codigoSala,
                    nomeUtilizador,
                    nomeJogador,
                    nomeCategoria,
                    admin,
                    modoJogo,
                    jogadorAdmin.uid
                )
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
        uid: String? = null,
        onError: (String) -> Unit = {}
    ) {
        val jogadorAdmin = identidadeJogador(uid, nomeUtilizador, null)
        categoriaRepository.carregarPerguntasCategoriaPersonalizada(jogadorAdmin.uid, nomeUtilizador, nomeCategoria)
            .addOnSuccessListener { perguntas ->
                if (perguntas.isEmpty()) {
                    onError("A categoria personalizada ainda não tem perguntas válidas.")
                    return@addOnSuccessListener
                }

                val salaData = dadosSalaBase(jogadorAdmin, nomeCategoria, modoJogo, perguntas.shuffled().limitarPerguntasParaModo(modoJogo)) +
                    mapOf(
                        "categoriaPersonalizada" to true,
                        "donoCategoria" to nomeUtilizador,
                        FirebasePaths.DONO_UID to jogadorAdmin.uid
                    )

                salaRepository.criarSala(codigoSala, salaData).addOnSuccessListener {
                    abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, null, nomeCategoria, admin, modoJogo, jogadorAdmin.uid)
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
        uid: String? = null,
        onError: (String) -> Unit = {}
    ) {
        val jogadorAdmin = identidadeJogador(uid, nomeUtilizador, nomeJogador)
        if (jogadorAdmin.nomeDisplay.isBlank()) {
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

            val salaData = dadosSalaBase(jogadorAdmin, nomeCategoria, modoJogo, perguntas.shuffled().limitarPerguntasParaModo(modoJogo)) +
                mapOf(
                    "categoriaPublica" to true,
                    "categoriaPublicaId" to categoriaPublicaId,
                    "criadorCategoriaPublica" to criador,
                    "criadorCategoriaPublicaId" to criadorId
                )

            salaRepository.criarSala(codigoSala, salaData).addOnSuccessListener {
                categoriaRepository.incrementarUsos(categoriaPublicaId)
                abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, nomeJogador, nomeCategoria, admin, modoJogo, jogadorAdmin.uid)
            }.addOnFailureListener { onError(it.message ?: "Erro desconhecido") }
        }.addOnFailureListener { onError(it.message ?: "Erro ao buscar categoria pública") }
    }

    private fun identidadeJogador(
        uid: String?,
        nomeUtilizador: String?,
        nomeJogador: String?
    ): JogadorSalaIdentidade {
        return JogadorSalaIdentidade.from(
            uid?.takeIf { it.isNotBlank() } ?: authService.utilizadorAtual()?.uid,
            nomeUtilizador,
            nomeJogador
        )
    }

    private fun dadosSalaBase(
        jogadorAdmin: JogadorSalaIdentidade,
        nomeCategoria: String,
        modoJogo: String,
        perguntas: List<Any>
    ): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            "horaCriacao" to System.currentTimeMillis(),
            FirebasePaths.ADMIN to jogadorAdmin.nomeDisplay,
            FirebasePaths.ESTADO to GameConstants.ESTADO_EM_ESPERA,
            FirebasePaths.MODO_JOGO to modoJogo,
            FirebasePaths.JOGADORES to mapOf<String, Any>(),
            "categoria" to nomeCategoria,
            FirebasePaths.NOME_CATEGORIA to nomeCategoria,
            FirebasePaths.PERGUNTAS to perguntas
        )
        if (jogadorAdmin.uid.isNotBlank()) {
            dados[FirebasePaths.ADMIN_ID] = jogadorAdmin.uid
            dados[FirebasePaths.ADMIN_UID] = jogadorAdmin.uid
        }
        return dados
    }

    private fun <T> List<T>.limitarPerguntasParaModo(modoJogo: String): List<T> {
        return if (modoJogo == GameConstants.MODO_ELIMINATORIAS) this else take(8)
    }
}
