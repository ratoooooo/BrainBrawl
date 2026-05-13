package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

class CategoriaRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class PerguntaCategoria(
        val id: String? = null,
        val pergunta: String,
        val respostaCorreta: String,
        val opcoes: List<String>,
        val imagem: String = "",
        val dificuldade: String? = null
    ) {
        fun toMap(): Map<String, Any> {
            val dados = linkedMapOf<String, Any>(
                FirebasePaths.PERGUNTA to pergunta,
                FirebasePaths.RESPOSTA_CORRETA to respostaCorreta,
                FirebasePaths.OPCOES to opcoes
            )
            if (imagem.isNotBlank()) dados[FirebasePaths.IMAGEM] = imagem
            if (!dificuldade.isNullOrBlank()) dados[FirebasePaths.DIFICULDADE] = dificuldade
            return dados
        }
    }

    data class CategoriaPersonalizada(
        val nome: String,
        val descricao: String,
        val categoriaPublicaId: String?,
        val estadoPublicacao: String?,
        val chaveDono: String = "",
        val uid: String = "",
        val nomeUtilizador: String = ""
    )

    data class CategoriaPublica(
        val id: String,
        val nome: String,
        val descricao: String,
        val criador: String,
        val criadorId: String,
        val totalPerguntas: Int,
        val usos: Int,
        val ratingMedio: Double,
        val totalAvaliacoes: Int
    ) {
        fun descricaoCurta(): String {
            val texto = descricao.ifBlank { "Sem descrição." }
            return if (texto.length <= 90) texto else texto.take(87).trimEnd() + "..."
        }

        fun ratingTexto(): String {
            return if (totalAvaliacoes == 0) {
                "sem avaliações"
            } else {
                "rating %.1f (%d)".format(ratingMedio, totalAvaliacoes)
            }
        }
    }

    data class CategoriaPublicaDetalhe(
        val categoria: CategoriaPublica,
        val perguntas: List<Map<String, Any>>
    )

    private data class DonoCategoria(
        val uid: String,
        val nomeUtilizador: String,
        val nomeDisplay: String
    ) {
        val chavePrincipal: String
            get() = uid.ifBlank { nomeUtilizador }

        val chavesCompatibilidade: List<String>
            get() = listOf(uid, nomeUtilizador, nomeDisplay)
                .filter { it.isNotBlank() }
                .distinct()

        fun podeGerir(valor: String): Boolean {
            return valor.isNotBlank() && valor in chavesCompatibilidade
        }
    }

    enum class ResultadoAvaliacao {
        GUARDADA,
        JA_AVALIADA
    }

    data class ListenerHandle internal constructor(
        private val reference: DatabaseReference,
        private val listener: ValueEventListener
    ) {
        internal fun remover() {
            reference.removeEventListener(listener)
        }
    }

    fun carregarCategoriasOficiais(): Task<List<String>> {
        return categoriasOficiaisRef().get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar categorias.")
            task.result.children.mapNotNull { it.key }.sorted()
        }
    }

    fun carregarPerguntasCategoriaOficial(nomeCategoria: String): Task<List<Map<String, Any>>> {
        return categoriasOficiaisRef().child(nomeCategoria).child(FirebasePaths.PERGUNTAS).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar perguntas.")
            task.result.toPerguntasValidasMap(minimoOpcoes = 4, exigirQuatroOpcoes = true)
        }
    }

    fun carregarTodasPerguntasOficiais(): Task<List<Map<String, Any>>> {
        return categoriasOficiaisRef().get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar categorias.")
            task.result.children.flatMap { categoriaSnapshot ->
                categoriaSnapshot.child(FirebasePaths.PERGUNTAS).toPerguntasValidasMap(minimoOpcoes = 4, exigirQuatroOpcoes = true)
            }
        }
    }

    fun carregarCategoriasPersonalizadas(uid: String, nomeUtilizador: String): Task<List<CategoriaPersonalizada>> {
        val result = TaskCompletionSource<List<CategoriaPersonalizada>>()
        val dono = donoCategoria(uid, nomeUtilizador, nomeUtilizador)
        if (dono.chavePrincipal.isBlank()) {
            result.setResult(emptyList())
            return result.task
        }

        carregarCategoriasPersonalizadasDasChaves(dono.chavesCompatibilidade, 0, mutableMapOf(), result)
        return result.task
    }

    fun carregarPerguntasCategoriaPersonalizada(
        uid: String,
        nomeUtilizador: String,
        nomeCategoria: String,
        minimoOpcoes: Int = 2
    ): Task<List<Map<String, Any>>> {
        return resolverCategoriaPersonalizadaRef(donoCategoria(uid, nomeUtilizador, nomeUtilizador), nomeCategoria).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar categoria personalizada.")
            task.result.child(FirebasePaths.PERGUNTAS).get().continueWith { perguntasTask ->
                if (!perguntasTask.isSuccessful) throw perguntasTask.exception ?: IllegalStateException("Erro ao carregar perguntas personalizadas.")
                perguntasTask.result.toPerguntasValidasMap(minimoOpcoes = minimoOpcoes, exigirQuatroOpcoes = false)
            }
        }
    }

    fun carregarPerguntasEditaveis(uid: String, nomeUtilizador: String, nomeCategoria: String): Task<List<PerguntaCategoria>> {
        return resolverCategoriaPersonalizadaRef(donoCategoria(uid, nomeUtilizador, nomeUtilizador), nomeCategoria).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar categoria personalizada.")
            task.result.child(FirebasePaths.PERGUNTAS).get().continueWith { perguntasTask ->
                if (!perguntasTask.isSuccessful) throw perguntasTask.exception ?: IllegalStateException("Erro ao carregar perguntas.")
                perguntasTask.result.children.mapNotNull { perguntaSnapshot ->
                    perguntaSnapshot.toPerguntaCategoria()
                }
            }
        }
    }

    fun criarCategoriaPersonalizada(uid: String, nomeUtilizador: String, nomeCategoria: String): Task<Void> {
        val dono = donoCategoria(uid, nomeUtilizador, nomeUtilizador)
        if (dono.chavePrincipal.isBlank()) return failedTask("Inicia sessão para criar categorias personalizadas.")
        return categoriasPersonalizadasRef(dono.chavePrincipal).child(nomeCategoria).updateChildren(dadosCategoriaPersonalizada(dono, nomeCategoria))
    }

    fun editarCategoria(uid: String, nomeUtilizador: String, nomeCategoria: String, dados: Map<String, Any?>): Task<Void> {
        return resolverCategoriaPersonalizadaRef(donoCategoria(uid, nomeUtilizador, nomeUtilizador), nomeCategoria).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar categoria personalizada.")
            task.result.updateChildren(dados)
        }
    }

    fun eliminarCategoria(uid: String, nomeUtilizador: String, nomeCategoria: String): Task<Void> {
        return resolverCategoriaPersonalizadaRef(donoCategoria(uid, nomeUtilizador, nomeUtilizador), nomeCategoria).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar categoria personalizada.")
            task.result.removeValue()
        }
    }

    fun guardarPerguntaPersonalizada(
        uid: String,
        nomeUtilizador: String,
        nomeCategoria: String,
        perguntaId: String?,
        pergunta: PerguntaCategoria
    ): Task<Void> {
        val dono = donoCategoria(uid, nomeUtilizador, nomeUtilizador)
        return resolverCategoriaPersonalizadaRef(dono, nomeCategoria).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar categoria personalizada.")
            val categoriaRef = task.result
            val perguntaKey = perguntaId ?: categoriaRef.child(FirebasePaths.PERGUNTAS).push().key
                ?: throw IllegalStateException("Erro ao gerar identificador da pergunta.")
            val updates = HashMap<String, Any>()
            updates.putAll(dadosCategoriaPersonalizada(dono, nomeCategoria))
            updates["${FirebasePaths.PERGUNTAS}/$perguntaKey"] = pergunta.toMap()
            categoriaRef.updateChildren(updates)
        }
    }

    fun eliminarPerguntaPersonalizada(
        uid: String,
        nomeUtilizador: String,
        nomeCategoria: String,
        perguntaId: String
    ): Task<Void> {
        return resolverCategoriaPersonalizadaRef(donoCategoria(uid, nomeUtilizador, nomeUtilizador), nomeCategoria).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar categoria personalizada.")
            task.result.child(FirebasePaths.PERGUNTAS).child(perguntaId).removeValue()
        }
    }

    fun carregarCategoriasPublicas(): Task<List<CategoriaPublica>> {
        return categoriasPublicasRef().get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar categorias públicas.")
            task.result.toCategoriasPublicasOrdenadas()
        }
    }

    fun escutarCategoriasPublicas(
        onCategoriasAlteradas: (List<CategoriaPublica>) -> Unit,
        onErro: () -> Unit
    ): ListenerHandle {
        val reference = categoriasPublicasRef()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onCategoriasAlteradas(snapshot.toCategoriasPublicasOrdenadas())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun carregarCategoriaPublica(categoriaPublicaId: String): Task<CategoriaPublicaDetalhe?> {
        return categoriasPublicasRef().child(categoriaPublicaId).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar categoria pública.")
            val snapshot = task.result
            val categoria = snapshot.toCategoriaPublica() ?: return@continueWith null
            CategoriaPublicaDetalhe(
                categoria = categoria,
                perguntas = snapshot.child(FirebasePaths.PERGUNTAS).toPerguntasValidasMap(minimoOpcoes = 4, exigirQuatroOpcoes = true)
            )
        }
    }

    fun publicarCategoria(
        uid: String,
        nomeUtilizador: String,
        nomeJogador: String?,
        nomeCategoria: String
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val dono = donoCategoria(uid, nomeUtilizador, nomeJogador ?: nomeUtilizador)
        if (dono.chavePrincipal.isBlank()) {
            result.setException(IllegalStateException("Inicia sessão para publicar categorias."))
            return result.task
        }

        resolverCategoriaPersonalizadaRef(dono, nomeCategoria).addOnSuccessListener { categoriaRef ->
            categoriaRef.get().addOnSuccessListener categoriaListener@ { snapshot ->
                val perguntasValidas = snapshot.child(FirebasePaths.PERGUNTAS).toPerguntasValidasMap(
                    minimoOpcoes = 4,
                    exigirQuatroOpcoes = true
                )
                if (perguntasValidas.isEmpty()) {
                    result.setException(IllegalStateException("A categoria precisa de perguntas válidas para ser pública."))
                    return@categoriaListener
                }

                val idsPossiveis = idsCategoriaPublica(dono, nomeCategoria, snapshot.child(FirebasePaths.CATEGORIA_PUBLICA_ID).getValue(String::class.java))
                selecionarCategoriaPublicaId(idsPossiveis)
                    .addOnSuccessListener { categoriaPublicaId ->
                        val publicaRef = categoriasPublicasRef().child(categoriaPublicaId)
                        publicaRef.get().addOnSuccessListener publicaListener@ { publicaSnapshot ->
                            val criadorExistente = publicaSnapshot.criadorCompatibilidade()
                            if (publicaSnapshot.exists() && !dono.podeGerir(criadorExistente)) {
                                result.setException(IllegalStateException("Só o criador pode atualizar esta categoria pública."))
                                return@publicaListener
                            }

                            val agora = System.currentTimeMillis()
                            val dadosPublicos = hashMapOf<String, Any>(
                                FirebasePaths.ID to categoriaPublicaId,
                                FirebasePaths.NOME to nomeCategoria,
                                FirebasePaths.DESCRICAO to (snapshot.child(FirebasePaths.DESCRICAO).getValue(String::class.java) ?: ""),
                                FirebasePaths.CRIADOR to dono.nomeDisplay,
                                FirebasePaths.CRIADOR_ID to dono.chavePrincipal,
                                FirebasePaths.CRIADOR_UID to dono.uid,
                                FirebasePaths.NOME_UTILIZADOR to nomeUtilizador,
                                FirebasePaths.NOME_DISPLAY to dono.nomeDisplay,
                                FirebasePaths.PERGUNTAS to perguntasValidas,
                                FirebasePaths.USOS to (publicaSnapshot.child(FirebasePaths.USOS).getValue(Int::class.java) ?: 0),
                                FirebasePaths.RATING_MEDIO to (publicaSnapshot.child(FirebasePaths.RATING_MEDIO).getValue(Double::class.java) ?: 0.0),
                                FirebasePaths.TOTAL_AVALIACOES to (publicaSnapshot.child(FirebasePaths.TOTAL_AVALIACOES).getValue(Int::class.java) ?: 0),
                                FirebasePaths.DATA_CRIACAO to (snapshot.child(FirebasePaths.DATA_CRIACAO).getValue(Long::class.java) ?: agora),
                                FirebasePaths.DATA_PUBLICACAO to (publicaSnapshot.child(FirebasePaths.DATA_PUBLICACAO).getValue(Long::class.java) ?: agora)
                            )

                            publicaRef.updateChildren(dadosPublicos).addOnSuccessListener {
                                categoriaRef.updateChildren(
                                    mapOf(
                                        FirebasePaths.CATEGORIA_PUBLICA_ID to categoriaPublicaId,
                                        FirebasePaths.ESTADO_PUBLICACAO to GameConstants.ESTADO_PUBLICA,
                                        FirebasePaths.DATA_PUBLICACAO to ServerValue.TIMESTAMP
                                    )
                                ).addOnSuccessListener {
                                    result.setResult(null)
                                }.addOnFailureListener { error ->
                                    result.setException(error)
                                }
                            }.addOnFailureListener { error ->
                                result.setException(error)
                            }
                        }.addOnFailureListener { error ->
                            result.setException(error)
                        }
                    }.addOnFailureListener { error ->
                        result.setException(error)
                    }
            }.addOnFailureListener { error ->
                result.setException(error)
            }
        }.addOnFailureListener { error ->
            result.setException(error)
        }
        return result.task
    }

    fun removerCategoriaPublica(uid: String, nomeUtilizador: String, nomeCategoria: String): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val dono = donoCategoria(uid, nomeUtilizador, nomeUtilizador)
        resolverCategoriaPersonalizadaRef(dono, nomeCategoria).addOnSuccessListener { categoriaRef ->
            categoriaRef.get().addOnSuccessListener { categoriaSnapshot ->
                val idsPossiveis = idsCategoriaPublica(dono, nomeCategoria, categoriaSnapshot.child(FirebasePaths.CATEGORIA_PUBLICA_ID).getValue(String::class.java))
                removerCategoriaPublicaPorIds(idsPossiveis, 0, dono, categoriaRef, result)
            }.addOnFailureListener { error ->
                result.setException(error)
            }
        }.addOnFailureListener { error ->
            result.setException(error)
        }
        return result.task
    }

    fun guardarCopiaCategoriaPublica(uid: String, nomeUtilizador: String, categoriaPublicaId: String): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val dono = donoCategoria(uid, nomeUtilizador, nomeUtilizador)
        if (dono.chavePrincipal.isBlank()) {
            result.setException(IllegalStateException("Inicia sessão para guardar categorias."))
            return result.task
        }

        val publicaRef = categoriasPublicasRef().child(categoriaPublicaId)
        publicaRef.get().addOnSuccessListener { publicaSnapshot ->
            val categoria = publicaSnapshot.toCategoriaPublica()
            val perguntas = publicaSnapshot.child(FirebasePaths.PERGUNTAS).toPerguntasValidasMap(
                minimoOpcoes = 4,
                exigirQuatroOpcoes = true
            )
            if (categoria == null || perguntas.isEmpty()) {
                result.setException(IllegalStateException("Esta categoria não tem perguntas válidas para copiar."))
                return@addOnSuccessListener
            }

            val pessoaisRef = categoriasPersonalizadasRef(dono.chavePrincipal)
            pessoaisRef.get().addOnSuccessListener { pessoaisSnapshot ->
                val nomeCopia = nomeDisponivel(categoria.nome, pessoaisSnapshot)
                val copia = mapOf(
                    FirebasePaths.NOME to nomeCopia,
                    FirebasePaths.DESCRICAO to publicaSnapshot.child(FirebasePaths.DESCRICAO).getValue(String::class.java).orEmpty(),
                    FirebasePaths.ORIGEM_CATEGORIA_PUBLICA to categoria.id,
                    FirebasePaths.CRIADOR_ORIGINAL to categoria.criador,
                    FirebasePaths.CRIADOR_ORIGINAL_ID to categoria.criadorId,
                    FirebasePaths.DONO_UID to dono.uid,
                    FirebasePaths.NOME_UTILIZADOR to dono.nomeUtilizador,
                    FirebasePaths.DATA_CRIACAO to ServerValue.TIMESTAMP,
                    FirebasePaths.PERGUNTAS to perguntas
                )
                pessoaisRef.child(nomeCopia).setValue(copia).addOnSuccessListener {
                    result.setResult(null)
                }.addOnFailureListener { error ->
                    result.setException(error)
                }
            }.addOnFailureListener { error ->
                result.setException(error)
            }
        }.addOnFailureListener { error ->
            result.setException(error)
        }
        return result.task
    }

    fun incrementarUsos(categoriaPublicaId: String): Task<Void> {
        val result = TaskCompletionSource<Void>()
        categoriasPublicasRef().child(categoriaPublicaId).child(FirebasePaths.USOS)
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val usosAtuais = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = usosAtuais + 1
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        result.setException(error.toException())
                    } else {
                        result.setResult(null)
                    }
                }
            })
        return result.task
    }

    fun avaliarCategoria(
        categoriaPublicaId: String,
        uid: String,
        nomeUtilizador: String,
        valor: Int
    ): Task<ResultadoAvaliacao> {
        val result = TaskCompletionSource<ResultadoAvaliacao>()
        val dono = donoCategoria(uid, nomeUtilizador, nomeUtilizador)
        if (dono.chavePrincipal.isBlank()) {
            result.setResult(ResultadoAvaliacao.JA_AVALIADA)
            return result.task
        }

        categoriasPublicasRef().child(categoriaPublicaId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value == null) return Transaction.abort()
                val avaliacoes = currentData.child(FirebasePaths.AVALIACOES)
                if (dono.chavesCompatibilidade.any { avaliacoes.child(it).value != null }) return Transaction.abort()

                val totalAtual = currentData.child(FirebasePaths.TOTAL_AVALIACOES).getValue(Int::class.java) ?: 0
                val mediaAtual = currentData.child(FirebasePaths.RATING_MEDIO).getValue(Double::class.java) ?: 0.0
                val novoTotal = totalAtual + 1
                val novaMedia = ((mediaAtual * totalAtual) + valor) / novoTotal

                currentData.child(FirebasePaths.TOTAL_AVALIACOES).value = novoTotal
                currentData.child(FirebasePaths.RATING_MEDIO).value = novaMedia
                avaliacoes.child(dono.chavePrincipal).value = mapOf(
                    FirebasePaths.VALOR to valor,
                    FirebasePaths.UID to dono.uid,
                    FirebasePaths.NOME_UTILIZADOR to dono.nomeUtilizador,
                    FirebasePaths.DATA to ServerValue.TIMESTAMP
                )
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                when {
                    error != null -> result.setException(error.toException())
                    committed -> result.setResult(ResultadoAvaliacao.GUARDADA)
                    else -> result.setResult(ResultadoAvaliacao.JA_AVALIADA)
                }
            }
        })
        return result.task
    }

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    private fun carregarCategoriasPersonalizadasDasChaves(
        chaves: List<String>,
        index: Int,
        acumuladas: MutableMap<String, CategoriaPersonalizada>,
        result: TaskCompletionSource<List<CategoriaPersonalizada>>
    ) {
        if (index >= chaves.size) {
            result.setResult(acumuladas.values.sortedBy { it.nome })
            return
        }

        val chaveDono = chaves[index]
        categoriasPersonalizadasRef(chaveDono).get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { categoriaSnapshot ->
                    val nome = categoriaSnapshot.key ?: return@forEach
                    if (!acumuladas.containsKey(nome)) {
                        acumuladas[nome] = categoriaSnapshot.toCategoriaPersonalizada(chaveDono, nome)
                    }
                }
                carregarCategoriasPersonalizadasDasChaves(chaves, index + 1, acumuladas, result)
            }
            .addOnFailureListener { error ->
                result.setException(error)
            }
    }

    private fun resolverCategoriaPersonalizadaRef(dono: DonoCategoria, nomeCategoria: String): Task<DatabaseReference> {
        val result = TaskCompletionSource<DatabaseReference>()
        if (dono.chavePrincipal.isBlank()) {
            result.setException(IllegalStateException("Inicia sessão para gerir categorias personalizadas."))
            return result.task
        }

        procurarCategoriaPersonalizadaRef(
            chaves = dono.chavesCompatibilidade,
            index = 0,
            nomeCategoria = nomeCategoria,
            fallback = categoriasPersonalizadasRef(dono.chavePrincipal).child(nomeCategoria),
            result = result
        )
        return result.task
    }

    private fun procurarCategoriaPersonalizadaRef(
        chaves: List<String>,
        index: Int,
        nomeCategoria: String,
        fallback: DatabaseReference,
        result: TaskCompletionSource<DatabaseReference>
    ) {
        if (index >= chaves.size) {
            result.setResult(fallback)
            return
        }

        val reference = categoriasPersonalizadasRef(chaves[index]).child(nomeCategoria)
        reference.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    result.setResult(reference)
                } else {
                    procurarCategoriaPersonalizadaRef(chaves, index + 1, nomeCategoria, fallback, result)
                }
            }
            .addOnFailureListener { error ->
                result.setException(error)
            }
    }

    private fun removerCategoriaPublicaPorIds(
        ids: List<String>,
        index: Int,
        dono: DonoCategoria,
        categoriaRef: DatabaseReference,
        result: TaskCompletionSource<Void>
    ) {
        if (index >= ids.size) {
            categoriaRef.updateChildren(
                mapOf(
                    FirebasePaths.CATEGORIA_PUBLICA_ID to null,
                    FirebasePaths.ESTADO_PUBLICACAO to GameConstants.ESTADO_PRIVADA
                )
            ).addOnSuccessListener {
                result.setResult(null)
            }.addOnFailureListener { error ->
                result.setException(error)
            }
            return
        }

        val publicaRef = categoriasPublicasRef().child(ids[index])
        publicaRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                removerCategoriaPublicaPorIds(ids, index + 1, dono, categoriaRef, result)
                return@addOnSuccessListener
            }

            val criadorId = snapshot.criadorCompatibilidade()
            if (!dono.podeGerir(criadorId)) {
                result.setException(IllegalStateException("Só o criador pode remover esta categoria pública."))
                return@addOnSuccessListener
            }

            publicaRef.removeValue().addOnSuccessListener {
                categoriaRef.updateChildren(
                    mapOf(
                        FirebasePaths.CATEGORIA_PUBLICA_ID to null,
                        FirebasePaths.ESTADO_PUBLICACAO to GameConstants.ESTADO_PRIVADA
                    )
                ).addOnSuccessListener {
                    result.setResult(null)
                }.addOnFailureListener { error ->
                    result.setException(error)
                }
            }.addOnFailureListener { error ->
                result.setException(error)
            }
        }.addOnFailureListener { error ->
            result.setException(error)
        }
    }

    private fun selecionarCategoriaPublicaId(ids: List<String>): Task<String> {
        val result = TaskCompletionSource<String>()
        procurarCategoriaPublicaExistente(ids, 0, result)
        return result.task
    }

    private fun procurarCategoriaPublicaExistente(
        ids: List<String>,
        index: Int,
        result: TaskCompletionSource<String>
    ) {
        if (ids.isEmpty()) {
            result.setException(IllegalStateException("Erro ao gerar categoria pública."))
            return
        }
        if (index >= ids.size) {
            result.setResult(ids.first())
            return
        }

        categoriasPublicasRef().child(ids[index]).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    result.setResult(ids[index])
                } else {
                    procurarCategoriaPublicaExistente(ids, index + 1, result)
                }
            }
            .addOnFailureListener { error ->
                result.setException(error)
            }
    }

    private fun donoCategoria(uid: String?, nomeUtilizador: String?, nomeDisplay: String?): DonoCategoria {
        return DonoCategoria(
            uid = uid.orEmpty(),
            nomeUtilizador = nomeUtilizador.orEmpty(),
            nomeDisplay = nomeDisplay.orEmpty().ifBlank { nomeUtilizador.orEmpty() }.ifBlank { uid.orEmpty() }
        )
    }

    private fun dadosCategoriaPersonalizada(dono: DonoCategoria, nomeCategoria: String): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.NOME to nomeCategoria,
            FirebasePaths.NOME_UTILIZADOR to dono.nomeUtilizador
        )
        if (dono.uid.isNotBlank()) dados[FirebasePaths.DONO_UID] = dono.uid
        return dados
    }

    private fun idsCategoriaPublica(
        dono: DonoCategoria,
        nomeCategoria: String,
        idExistente: String?
    ): List<String> {
        return (listOf(idExistente) + dono.chavesCompatibilidade.map { categoriaPublicaId(it, nomeCategoria) })
            .filter { !it.isNullOrBlank() }
            .map { it.orEmpty() }
            .distinct()
    }

    private fun categoriasOficiaisRef(): DatabaseReference {
        return database.child(FirebasePaths.CATEGORIAS)
    }

    private fun categoriasPersonalizadasRef(chaveDono: String): DatabaseReference {
        return database.child(FirebasePaths.JOGADORES).child(chaveDono).child(FirebasePaths.CATEGORIAS_PERSONALIZADAS)
    }

    private fun categoriasPublicasRef(): DatabaseReference {
        return database.child(FirebasePaths.CATEGORIAS_PUBLICAS)
    }

    private fun DataSnapshot.toPerguntaCategoria(): PerguntaCategoria? {
        val pergunta = child(FirebasePaths.PERGUNTA).getValue(String::class.java) ?: return null
        val respostaCorreta = child(FirebasePaths.RESPOSTA_CORRETA).getValue(String::class.java) ?: ""
        val opcoes = child(FirebasePaths.OPCOES).children.mapNotNull { it.getValue(String::class.java) }
        return PerguntaCategoria(
            id = key,
            pergunta = pergunta,
            respostaCorreta = respostaCorreta,
            opcoes = opcoes,
            imagem = child(FirebasePaths.IMAGEM).getValue(String::class.java).orEmpty(),
            dificuldade = child(FirebasePaths.DIFICULDADE).getValue(String::class.java)
        )
    }

    private fun DataSnapshot.toCategoriaPersonalizada(chaveDono: String, nomeCategoria: String): CategoriaPersonalizada {
        return CategoriaPersonalizada(
            nome = nomeCategoria,
            descricao = child(FirebasePaths.DESCRICAO).getValue(String::class.java).orEmpty(),
            categoriaPublicaId = child(FirebasePaths.CATEGORIA_PUBLICA_ID).getValue(String::class.java),
            estadoPublicacao = child(FirebasePaths.ESTADO_PUBLICACAO).getValue(String::class.java),
            chaveDono = chaveDono,
            uid = child(FirebasePaths.DONO_UID).getValue(String::class.java)
                ?: child(FirebasePaths.UID).getValue(String::class.java).orEmpty(),
            nomeUtilizador = child(FirebasePaths.NOME_UTILIZADOR).getValue(String::class.java).orEmpty()
        )
    }

    private fun DataSnapshot.toPerguntasValidasMap(
        minimoOpcoes: Int,
        exigirQuatroOpcoes: Boolean
    ): List<Map<String, Any>> {
        return children.mapNotNull { perguntaSnap ->
            val pergunta = perguntaSnap.child(FirebasePaths.PERGUNTA).getValue(String::class.java)
            val respostaCorreta = perguntaSnap.child(FirebasePaths.RESPOSTA_CORRETA).getValue(String::class.java)
            val opcoes = perguntaSnap.child(FirebasePaths.OPCOES).children.mapNotNull { it.getValue(String::class.java) }
            val opcoesValidas = if (exigirQuatroOpcoes) opcoes.size == 4 else opcoes.size >= minimoOpcoes
            if (!pergunta.isNullOrBlank() && !respostaCorreta.isNullOrBlank() && opcoesValidas) {
                val dados = linkedMapOf<String, Any>(
                    FirebasePaths.PERGUNTA to pergunta,
                    FirebasePaths.RESPOSTA_CORRETA to respostaCorreta,
                    FirebasePaths.OPCOES to opcoes
                )
                perguntaSnap.child(FirebasePaths.IMAGEM).getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { dados[FirebasePaths.IMAGEM] = it }
                perguntaSnap.child(FirebasePaths.DIFICULDADE).getValue(String::class.java)
                    ?.takeIf { it in DIFICULDADES_VALIDAS }
                    ?.let { dados[FirebasePaths.DIFICULDADE] = it }
                dados
            } else {
                null
            }
        }
    }

    private fun DataSnapshot.toCategoriaPublica(): CategoriaPublica? {
        val id = child(FirebasePaths.ID).getValue(String::class.java) ?: key ?: return null
        val nome = child(FirebasePaths.NOME).getValue(String::class.java) ?: return null
        val criadorUid = child(FirebasePaths.CRIADOR_UID).getValue(String::class.java).orEmpty()
        val criadorLegado = child(FirebasePaths.CRIADOR_ID).getValue(String::class.java)
            ?: child(FirebasePaths.NOME_UTILIZADOR).getValue(String::class.java).orEmpty()
        return CategoriaPublica(
            id = id,
            nome = nome,
            descricao = child(FirebasePaths.DESCRICAO).getValue(String::class.java).orEmpty(),
            criador = child(FirebasePaths.CRIADOR).getValue(String::class.java)
                ?: child(FirebasePaths.NOME_DISPLAY).getValue(String::class.java)
                ?: child(FirebasePaths.CRIADOR_ID).getValue(String::class.java).orEmpty(),
            criadorId = criadorUid.ifBlank { criadorLegado },
            totalPerguntas = child(FirebasePaths.PERGUNTAS).childrenCount.toInt(),
            usos = child(FirebasePaths.USOS).getValue(Int::class.java) ?: 0,
            ratingMedio = child(FirebasePaths.RATING_MEDIO).getValue(Double::class.java) ?: 0.0,
            totalAvaliacoes = child(FirebasePaths.TOTAL_AVALIACOES).getValue(Int::class.java) ?: 0
        )
    }

    private fun DataSnapshot.toCategoriasPublicasOrdenadas(): List<CategoriaPublica> {
        return children.mapNotNull { it.toCategoriaPublica() }
            .sortedWith(compareByDescending<CategoriaPublica> { it.usos }.thenBy { it.nome.lowercase() })
    }

    private fun nomeDisponivel(nomeBase: String, categoriasPessoais: DataSnapshot): String {
        if (!categoriasPessoais.hasChild(nomeBase)) return nomeBase
        var indice = 2
        while (categoriasPessoais.hasChild("$nomeBase ($indice)")) {
            indice++
        }
        return "$nomeBase ($indice)"
    }

    private fun categoriaPublicaId(nomeUtilizador: String, categoria: String): String {
        val bruto = "${nomeUtilizador}_${categoria}".lowercase()
        return bruto.replace(Regex("[.#$\\[\\]/]"), "_").replace(Regex("\\s+"), "_")
    }

    private fun DataSnapshot.criadorCompatibilidade(): String {
        return child(FirebasePaths.CRIADOR_UID).getValue(String::class.java)
            ?: child(FirebasePaths.CRIADOR_ID).getValue(String::class.java)
            ?: child(FirebasePaths.NOME_UTILIZADOR).getValue(String::class.java)
            ?: child(FirebasePaths.CRIADOR).getValue(String::class.java)
            ?: child(FirebasePaths.NOME_DISPLAY).getValue(String::class.java)
            ?: ""
    }

    private fun <T> failedTask(message: String): Task<T> {
        val result = TaskCompletionSource<T>()
        result.setException(IllegalStateException(message))
        return result.task
    }

    private companion object {
        val DIFICULDADES_VALIDAS = setOf("facil", "media", "dificil")
    }
}
