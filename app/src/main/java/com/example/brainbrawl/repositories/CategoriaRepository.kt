package com.example.brainbrawl.repositories

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
        val opcoes: List<String>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "pergunta" to pergunta,
                "respostaCorreta" to respostaCorreta,
                "opcoes" to opcoes
            )
        }
    }

    data class CategoriaPersonalizada(
        val nome: String,
        val descricao: String,
        val categoriaPublicaId: String?,
        val estadoPublicacao: String?
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
        return categoriasOficiaisRef().child(nomeCategoria).child("perguntas").get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar perguntas.")
            task.result.toPerguntasValidasMap(minimoOpcoes = 4, exigirQuatroOpcoes = true)
        }
    }

    fun carregarTodasPerguntasOficiais(): Task<List<Map<String, Any>>> {
        return categoriasOficiaisRef().get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar categorias.")
            task.result.children.flatMap { categoriaSnapshot ->
                categoriaSnapshot.child("perguntas").toPerguntasValidasMap(minimoOpcoes = 4, exigirQuatroOpcoes = true)
            }
        }
    }

    fun carregarCategoriasPersonalizadas(nomeUtilizador: String): Task<List<CategoriaPersonalizada>> {
        return categoriasPersonalizadasRef(nomeUtilizador).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar categorias personalizadas.")
            task.result.children.mapNotNull { categoriaSnapshot ->
                val nome = categoriaSnapshot.key ?: return@mapNotNull null
                CategoriaPersonalizada(
                    nome = nome,
                    descricao = categoriaSnapshot.child("descricao").getValue(String::class.java).orEmpty(),
                    categoriaPublicaId = categoriaSnapshot.child("categoriaPublicaId").getValue(String::class.java),
                    estadoPublicacao = categoriaSnapshot.child("estadoPublicacao").getValue(String::class.java)
                )
            }.sortedBy { it.nome }
        }
    }

    fun carregarPerguntasCategoriaPersonalizada(
        nomeUtilizador: String,
        nomeCategoria: String,
        minimoOpcoes: Int = 2
    ): Task<List<Map<String, Any>>> {
        return perguntasPersonalizadasRef(nomeUtilizador, nomeCategoria).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar perguntas personalizadas.")
            task.result.toPerguntasValidasMap(minimoOpcoes = minimoOpcoes, exigirQuatroOpcoes = false)
        }
    }

    fun carregarPerguntasEditaveis(nomeUtilizador: String, nomeCategoria: String): Task<List<PerguntaCategoria>> {
        return perguntasPersonalizadasRef(nomeUtilizador, nomeCategoria).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar perguntas.")
            task.result.children.mapNotNull { perguntaSnapshot ->
                perguntaSnapshot.toPerguntaCategoria()
            }
        }
    }

    fun criarCategoriaPersonalizada(nomeUtilizador: String, nomeCategoria: String): Task<Void> {
        return categoriasPersonalizadasRef(nomeUtilizador).child(nomeCategoria).child("nome").setValue(nomeCategoria)
    }

    fun editarCategoria(nomeUtilizador: String, nomeCategoria: String, dados: Map<String, Any?>): Task<Void> {
        return categoriasPersonalizadasRef(nomeUtilizador).child(nomeCategoria).updateChildren(dados)
    }

    fun eliminarCategoria(nomeUtilizador: String, nomeCategoria: String): Task<Void> {
        return categoriasPersonalizadasRef(nomeUtilizador).child(nomeCategoria).removeValue()
    }

    fun guardarPerguntaPersonalizada(
        nomeUtilizador: String,
        nomeCategoria: String,
        perguntaId: String?,
        pergunta: PerguntaCategoria
    ): Task<Void> {
        val categoriaRef = categoriasPersonalizadasRef(nomeUtilizador).child(nomeCategoria)
        val perguntaKey = perguntaId ?: categoriaRef.child("perguntas").push().key
            ?: return failedTask("Erro ao gerar identificador da pergunta.")
        val updates = hashMapOf<String, Any>(
            "nome" to nomeCategoria,
            "perguntas/$perguntaKey" to pergunta.toMap()
        )
        return categoriaRef.updateChildren(updates)
    }

    fun eliminarPerguntaPersonalizada(
        nomeUtilizador: String,
        nomeCategoria: String,
        perguntaId: String
    ): Task<Void> {
        return perguntasPersonalizadasRef(nomeUtilizador, nomeCategoria).child(perguntaId).removeValue()
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
                perguntas = snapshot.child("perguntas").toPerguntasValidasMap(minimoOpcoes = 4, exigirQuatroOpcoes = true)
            )
        }
    }

    fun publicarCategoria(
        nomeUtilizador: String,
        nomeJogador: String?,
        nomeCategoria: String
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val categoriaRef = categoriasPersonalizadasRef(nomeUtilizador).child(nomeCategoria)
        categoriaRef.get().addOnSuccessListener { snapshot ->
            val perguntasValidas = snapshot.child("perguntas").toPerguntasValidasMap(
                minimoOpcoes = 4,
                exigirQuatroOpcoes = true
            )
            if (perguntasValidas.isEmpty()) {
                result.setException(IllegalStateException("A categoria precisa de perguntas válidas para ser pública."))
                return@addOnSuccessListener
            }

            val categoriaPublicaId = snapshot.child("categoriaPublicaId").getValue(String::class.java)
                ?: categoriaPublicaId(nomeUtilizador, nomeCategoria)
            val publicaRef = categoriasPublicasRef().child(categoriaPublicaId)
            publicaRef.get().addOnSuccessListener publicaListener@ { publicaSnapshot ->
                val criadorExistente = publicaSnapshot.child("criadorId").getValue(String::class.java)
                if (publicaSnapshot.exists() && criadorExistente != nomeUtilizador) {
                    result.setException(IllegalStateException("Só o criador pode atualizar esta categoria pública."))
                    return@publicaListener
                }

                val agora = System.currentTimeMillis()
                val dadosPublicos = hashMapOf<String, Any>(
                    "id" to categoriaPublicaId,
                    "nome" to nomeCategoria,
                    "descricao" to (snapshot.child("descricao").getValue(String::class.java) ?: ""),
                    "criador" to (nomeJogador ?: nomeUtilizador),
                    "criadorId" to nomeUtilizador,
                    "nomeUtilizador" to nomeUtilizador,
                    "perguntas" to perguntasValidas,
                    "usos" to (publicaSnapshot.child("usos").getValue(Int::class.java) ?: 0),
                    "ratingMedio" to (publicaSnapshot.child("ratingMedio").getValue(Double::class.java) ?: 0.0),
                    "totalAvaliacoes" to (publicaSnapshot.child("totalAvaliacoes").getValue(Int::class.java) ?: 0),
                    "dataCriacao" to (snapshot.child("dataCriacao").getValue(Long::class.java) ?: agora),
                    "dataPublicacao" to (publicaSnapshot.child("dataPublicacao").getValue(Long::class.java) ?: agora)
                )

                publicaRef.updateChildren(dadosPublicos).addOnSuccessListener {
                    categoriaRef.updateChildren(
                        mapOf(
                            "categoriaPublicaId" to categoriaPublicaId,
                            "estadoPublicacao" to "publica",
                            "dataPublicacao" to ServerValue.TIMESTAMP
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
        return result.task
    }

    fun removerCategoriaPublica(nomeUtilizador: String, nomeCategoria: String): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val categoriaRef = categoriasPersonalizadasRef(nomeUtilizador).child(nomeCategoria)
        val categoriaPublicaId = categoriaPublicaId(nomeUtilizador, nomeCategoria)
        val publicaRef = categoriasPublicasRef().child(categoriaPublicaId)
        publicaRef.get().addOnSuccessListener { snapshot ->
            val criadorId = snapshot.child("criadorId").getValue(String::class.java)
            if (snapshot.exists() && criadorId != nomeUtilizador) {
                result.setException(IllegalStateException("Só o criador pode remover esta categoria pública."))
                return@addOnSuccessListener
            }
            publicaRef.removeValue().addOnSuccessListener {
                categoriaRef.updateChildren(
                    mapOf(
                        "categoriaPublicaId" to null,
                        "estadoPublicacao" to "privada"
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
        return result.task
    }

    fun guardarCopiaCategoriaPublica(nomeUtilizador: String, categoriaPublicaId: String): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val publicaRef = categoriasPublicasRef().child(categoriaPublicaId)
        publicaRef.get().addOnSuccessListener { publicaSnapshot ->
            val categoria = publicaSnapshot.toCategoriaPublica()
            val perguntas = publicaSnapshot.child("perguntas").toPerguntasValidasMap(
                minimoOpcoes = 4,
                exigirQuatroOpcoes = true
            )
            if (categoria == null || perguntas.isEmpty()) {
                result.setException(IllegalStateException("Esta categoria não tem perguntas válidas para copiar."))
                return@addOnSuccessListener
            }

            val pessoaisRef = categoriasPersonalizadasRef(nomeUtilizador)
            pessoaisRef.get().addOnSuccessListener { pessoaisSnapshot ->
                val nomeCopia = nomeDisponivel(categoria.nome, pessoaisSnapshot)
                val copia = mapOf(
                    "nome" to nomeCopia,
                    "descricao" to publicaSnapshot.child("descricao").getValue(String::class.java).orEmpty(),
                    "origemCategoriaPublica" to categoria.id,
                    "criadorOriginal" to categoria.criador,
                    "criadorOriginalId" to categoria.criadorId,
                    "dataCriacao" to ServerValue.TIMESTAMP,
                    "perguntas" to perguntas
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
        categoriasPublicasRef().child(categoriaPublicaId).child("usos")
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
        nomeUtilizador: String,
        valor: Int
    ): Task<ResultadoAvaliacao> {
        val result = TaskCompletionSource<ResultadoAvaliacao>()
        categoriasPublicasRef().child(categoriaPublicaId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value == null) return Transaction.abort()
                val avaliacoes = currentData.child("avaliacoes")
                if (avaliacoes.child(nomeUtilizador).value != null) return Transaction.abort()

                val totalAtual = currentData.child("totalAvaliacoes").getValue(Int::class.java) ?: 0
                val mediaAtual = currentData.child("ratingMedio").getValue(Double::class.java) ?: 0.0
                val novoTotal = totalAtual + 1
                val novaMedia = ((mediaAtual * totalAtual) + valor) / novoTotal

                currentData.child("totalAvaliacoes").value = novoTotal
                currentData.child("ratingMedio").value = novaMedia
                avaliacoes.child(nomeUtilizador).value = mapOf(
                    "valor" to valor,
                    "data" to ServerValue.TIMESTAMP
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

    private fun categoriasOficiaisRef(): DatabaseReference {
        return database.child("categorias")
    }

    private fun categoriasPersonalizadasRef(nomeUtilizador: String): DatabaseReference {
        return database.child("jogadores").child(nomeUtilizador).child("categoriasPersonalizadas")
    }

    private fun perguntasPersonalizadasRef(nomeUtilizador: String, nomeCategoria: String): DatabaseReference {
        return categoriasPersonalizadasRef(nomeUtilizador).child(nomeCategoria).child("perguntas")
    }

    private fun categoriasPublicasRef(): DatabaseReference {
        return database.child("categoriasPublicas")
    }

    private fun DataSnapshot.toPerguntaCategoria(): PerguntaCategoria? {
        val pergunta = child("pergunta").getValue(String::class.java) ?: return null
        val respostaCorreta = child("respostaCorreta").getValue(String::class.java) ?: ""
        val opcoes = child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
        return PerguntaCategoria(
            id = key,
            pergunta = pergunta,
            respostaCorreta = respostaCorreta,
            opcoes = opcoes
        )
    }

    private fun DataSnapshot.toPerguntasValidasMap(
        minimoOpcoes: Int,
        exigirQuatroOpcoes: Boolean
    ): List<Map<String, Any>> {
        return children.mapNotNull { perguntaSnap ->
            val pergunta = perguntaSnap.child("pergunta").getValue(String::class.java)
            val respostaCorreta = perguntaSnap.child("respostaCorreta").getValue(String::class.java)
            val opcoes = perguntaSnap.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
            val opcoesValidas = if (exigirQuatroOpcoes) opcoes.size == 4 else opcoes.size >= minimoOpcoes
            if (!pergunta.isNullOrBlank() && !respostaCorreta.isNullOrBlank() && opcoesValidas) {
                mapOf(
                    "pergunta" to pergunta,
                    "respostaCorreta" to respostaCorreta,
                    "opcoes" to opcoes
                )
            } else {
                null
            }
        }
    }

    private fun DataSnapshot.toCategoriaPublica(): CategoriaPublica? {
        val id = child("id").getValue(String::class.java) ?: key ?: return null
        val nome = child("nome").getValue(String::class.java) ?: return null
        return CategoriaPublica(
            id = id,
            nome = nome,
            descricao = child("descricao").getValue(String::class.java).orEmpty(),
            criador = child("criador").getValue(String::class.java)
                ?: child("criadorId").getValue(String::class.java).orEmpty(),
            criadorId = child("criadorId").getValue(String::class.java)
                ?: child("nomeUtilizador").getValue(String::class.java).orEmpty(),
            totalPerguntas = child("perguntas").childrenCount.toInt(),
            usos = child("usos").getValue(Int::class.java) ?: 0,
            ratingMedio = child("ratingMedio").getValue(Double::class.java) ?: 0.0,
            totalAvaliacoes = child("totalAvaliacoes").getValue(Int::class.java) ?: 0
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

    private fun <T> failedTask(message: String): Task<T> {
        val result = TaskCompletionSource<T>()
        result.setException(IllegalStateException(message))
        return result.task
    }
}
