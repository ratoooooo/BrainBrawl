package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.Pergunta
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

class JogoCompetitivoRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    enum class ModoCompetitivo(val node: String) {
        UM_CONTRA_UM(FirebasePaths.SALA_1X1),
        DOIS_CONTRA_DOIS(FirebasePaths.SALA_2X2)
    }

    data class ListenerHandle internal constructor(
        private val removerListener: () -> Unit
    ) {
        internal fun remover() {
            removerListener()
        }
    }

    fun adicionarJogador(
        modo: ModoCompetitivo,
        codigoSala: String,
        nomeUtilizador: String
    ): Task<Void> {
        return salaRef(modo, codigoSala).child(FirebasePaths.JOGADORES).child(nomeUtilizador).setValue(true)
    }

    fun marcarPronto1x1(
        codigoSala: String,
        nomeUtilizador: String,
        pronto: Boolean = true
    ): Task<Void> {
        return salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .child(FirebasePaths.PRONTOS)
            .child(nomeUtilizador)
            .setValue(pronto)
    }

    fun obterAdmin(modo: ModoCompetitivo, codigoSala: String): Task<String?> {
        return salaRef(modo, codigoSala).child(FirebasePaths.ADMIN).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar admin.")
            task.result.getValue(String::class.java)
        }
    }

    fun escutarJogadores(
        modo: ModoCompetitivo,
        codigoSala: String,
        onJogadoresAlterados: (List<String>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(modo, codigoSala).child(FirebasePaths.JOGADORES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onJogadoresAlterados(snapshot.children.mapNotNull { it.key })
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun escutarEstadoSala(
        modo: ModoCompetitivo,
        codigoSala: String,
        onEstadoAlterado: (String?) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(modo, codigoSala).child(FirebasePaths.ESTADO)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onEstadoAlterado(snapshot.getValue(String::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun escutarSalaApagada(
        modo: ModoCompetitivo,
        codigoSala: String,
        onSalaExisteAlterada: (Boolean) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(modo, codigoSala)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onSalaExisteAlterada(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun obterProntos1x1(codigoSala: String): Task<List<String>> {
        return salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .child(FirebasePaths.PRONTOS)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao verificar jogadores prontos.")
                task.result.children.mapNotNull { it.key }
            }
    }

    fun atualizarEstadoSala(
        modo: ModoCompetitivo,
        codigoSala: String,
        estado: String
    ): Task<Void> {
        return salaRef(modo, codigoSala).child(FirebasePaths.ESTADO).setValue(estado)
    }

    fun apagarSala(modo: ModoCompetitivo, codigoSala: String): Task<Void> {
        return salaRef(modo, codigoSala).removeValue()
    }

    fun removerJogador1x1(codigoSala: String, nomeUtilizador: String): Task<Void> {
        return salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala).updateChildren(
            hashMapOf<String, Any?>(
                "${FirebasePaths.JOGADORES}/$nomeUtilizador" to null,
                "${FirebasePaths.PRONTOS}/$nomeUtilizador" to null
            )
        )
    }

    fun removerJogador2x2(codigoSala: String, nomeUtilizador: String): Task<Void> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(
            hashMapOf<String, Any?>(
                "${FirebasePaths.JOGADORES}/$nomeUtilizador" to null,
                "${FirebasePaths.EQUIPA_A}/$nomeUtilizador" to null,
                "${FirebasePaths.EQUIPA_B}/$nomeUtilizador" to null
            )
        )
    }

    fun guardarEquipas2x2(
        codigoSala: String,
        equipaA: List<String>,
        equipaB: List<String>
    ): Task<Void> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(
            mapOf(
                FirebasePaths.EQUIPA_A to equipaA.associateWith { true },
                FirebasePaths.EQUIPA_B to equipaB.associateWith { true },
                FirebasePaths.PONTUACAO_A to 0,
                FirebasePaths.PONTUACAO_B to 0
            )
        )
    }

    fun carregarNomeCategoria(
        modo: ModoCompetitivo,
        codigoSala: String,
        categoriaPadrao: String
    ): Task<String> {
        return salaRef(modo, codigoSala).child(FirebasePaths.NOME_CATEGORIA).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao ler categoria.")
            task.result.getValue(String::class.java) ?: categoriaPadrao
        }
    }

    fun identificarEquipa2x2(codigoSala: String, nomeUtilizador: String): Task<String> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar equipa.")
            val snapshot = task.result
            val equipaA = snapshot.child(FirebasePaths.EQUIPA_A).children.mapNotNull { it.key }
            val equipaB = snapshot.child(FirebasePaths.EQUIPA_B).children.mapNotNull { it.key }
            when {
                equipaA.contains(nomeUtilizador) -> GameConstants.EQUIPA_A
                equipaB.contains(nomeUtilizador) -> GameConstants.EQUIPA_B
                else -> ""
            }
        }
    }

    fun carregarOuCriarPerguntas(
        modo: ModoCompetitivo,
        codigoSala: String,
        categoria: String,
        categoriaTodas: String
    ): Task<List<Pergunta>> {
        val result = TaskCompletionSource<List<Pergunta>>()
        val perguntasRef = salaRef(modo, codigoSala).child(FirebasePaths.PERGUNTAS)

        perguntasRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                result.setResult(snapshot.toPerguntas())
                return@addOnSuccessListener
            }

            buscarPerguntasAleatorias(categoria, categoriaTodas)
                .addOnSuccessListener { perguntasAleatorias ->
                    guardarPerguntasSeAusentes(perguntasRef, perguntasAleatorias)
                        .addOnSuccessListener { perguntasGuardadas ->
                            result.setResult(perguntasGuardadas)
                        }
                        .addOnFailureListener { error ->
                            result.setException(error)
                        }
                }
                .addOnFailureListener { error ->
                    result.setException(error)
                }
        }.addOnFailureListener { error ->
            result.setException(error)
        }

        return result.task
    }

    fun escutarOffsetServidor(
        onOffsetAlterado: (Long) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = FirebaseDatabase.getInstance().getReference(FirebasePaths.SERVER_TIME_OFFSET)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onOffsetAlterado(snapshot.getValue(Long::class.java) ?: 0L)
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun sincronizarInicioPergunta(
        modo: ModoCompetitivo,
        codigoSala: String,
        perguntaAtualIndex: Int,
        horaFallback: Long
    ): Task<Long> {
        val result = TaskCompletionSource<Long>()
        val inicioRef = salaRef(modo, codigoSala)
            .child(FirebasePaths.PERGUNTA_INICIOS)
            .child(perguntaAtualIndex.toString())

        inicioRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value == null) {
                    currentData.value = ServerValue.TIMESTAMP
                }
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    result.setResult(horaFallback)
                    return
                }

                inicioRef.get().addOnSuccessListener { inicioSnapshot ->
                    val inicio = inicioSnapshot.getValue(Long::class.java) ?: horaFallback
                    salaRef(modo, codigoSala).child(FirebasePaths.PERGUNTA_HORA_INICIO).setValue(inicio)
                    result.setResult(inicio)
                }.addOnFailureListener {
                    result.setResult(horaFallback)
                }
            }
        })

        return result.task
    }

    fun guardarPontuacao1x1(
        codigoSala: String,
        nomeUtilizador: String,
        totalPontos: Double
    ): Task<Void> {
        return salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .child(FirebasePaths.PONTUACOES)
            .child(nomeUtilizador)
            .setValue(totalPontos)
    }

    fun escutarPodio1x1(
        codigoSala: String,
        totalJogadoresEsperados: Long = 2,
        onPodioCompleto: () -> Unit,
        onAguardar: () -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala).child(FirebasePaths.PONTUACOES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount >= totalJogadoresEsperados) {
                    reference.removeEventListener(this)
                    onPodioCompleto()
                } else {
                    onAguardar()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun guardarResposta2x2(
        codigoSala: String,
        nomeUtilizador: String,
        perguntaAtualIndex: Int,
        resposta: String
    ): Task<Void> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .child(FirebasePaths.RESPOSTAS)
            .child(nomeUtilizador)
            .child(perguntaAtualIndex.toString())
            .setValue(resposta)
    }

    fun guardarResultado2x2(
        codigoSala: String,
        equipa: String,
        nomeUtilizador: String,
        totalPontos: Double,
        totalPerguntasCertas: Int
    ): Task<Void> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(
            mapOf(
                "${FirebasePaths.PONTUACOES}_$equipa/$nomeUtilizador" to totalPontos,
                "${FirebasePaths.TOTAL_PERGUNTAS_CERTAS}_$equipa/$nomeUtilizador" to totalPerguntasCertas
            )
        )
    }

    fun escutarPodio2x2(
        codigoSala: String,
        jogadoresPorEquipa: Long = 2,
        onPodioCompleto: () -> Unit,
        onAguardar: () -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val pontuacoesARef = salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).child(FirebasePaths.PONTUACOES_A)
        val pontuacoesBRef = salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).child(FirebasePaths.PONTUACOES_B)

        fun remover(listener: ValueEventListener) {
            pontuacoesARef.removeEventListener(listener)
            pontuacoesBRef.removeEventListener(listener)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pontuacoesARef.get().addOnSuccessListener { snapA ->
                    pontuacoesBRef.get().addOnSuccessListener { snapB ->
                        if (snapA.childrenCount >= jogadoresPorEquipa && snapB.childrenCount >= jogadoresPorEquipa) {
                            remover(this)
                            onPodioCompleto()
                        } else {
                            onAguardar()
                        }
                    }.addOnFailureListener {
                        onErro()
                    }
                }.addOnFailureListener {
                    onErro()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }

        pontuacoesARef.addValueEventListener(listener)
        pontuacoesBRef.addValueEventListener(listener)
        return ListenerHandle { remover(listener) }
    }

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    private fun buscarPerguntasAleatorias(categoria: String, categoriaTodas: String): Task<List<Pergunta>> {
        val categoriasRef = database.child(FirebasePaths.CATEGORIAS)
        val task = if (categoria == categoriaTodas || categoria.isEmpty()) {
            categoriasRef.get().continueWith { taskSnapshot ->
                if (!taskSnapshot.isSuccessful) throw taskSnapshot.exception ?: IllegalStateException("Erro ao buscar perguntas.")
                taskSnapshot.result.children
                    .flatMap { categoriaSnapshot -> categoriaSnapshot.child(FirebasePaths.PERGUNTAS).toPerguntas() }
                    .shuffled()
                    .take(8)
            }
        } else {
            categoriasRef.child(categoria).child(FirebasePaths.PERGUNTAS).get().continueWith { taskSnapshot ->
                if (!taskSnapshot.isSuccessful) throw taskSnapshot.exception ?: IllegalStateException("Erro ao buscar perguntas.")
                taskSnapshot.result.toPerguntas().shuffled().take(8)
            }
        }
        return task
    }

    private fun guardarPerguntasSeAusentes(
        perguntasRef: DatabaseReference,
        perguntasAleatorias: List<Pergunta>
    ): Task<List<Pergunta>> {
        val result = TaskCompletionSource<List<Pergunta>>()
        perguntasRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value == null) {
                    currentData.value = perguntasAleatorias
                    return Transaction.success(currentData)
                }
                return Transaction.abort()
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    result.setException(error.toException())
                    return
                }

                perguntasRef.get().addOnSuccessListener { snapshot ->
                    result.setResult(snapshot.toPerguntas())
                }.addOnFailureListener { exception ->
                    result.setException(exception)
                }
            }
        })
        return result.task
    }

    private fun salaRef(modo: ModoCompetitivo, codigoSala: String): DatabaseReference {
        return database.child(modo.node).child(codigoSala)
    }

    private fun DataSnapshot.toPerguntas(): List<Pergunta> {
        return children.mapNotNull { perguntaSnapshot ->
            val perguntaCompleta = perguntaSnapshot.getValue(Pergunta::class.java)
            if (perguntaCompleta != null && perguntaCompleta.opcoes.size == 4) {
                return@mapNotNull perguntaCompleta
            }

            val pergunta = perguntaSnapshot.child(FirebasePaths.PERGUNTA).getValue(String::class.java)
            val respostaCorreta = perguntaSnapshot.child(FirebasePaths.RESPOSTA_CORRETA).getValue(String::class.java)
            val opcoes = perguntaSnapshot.child(FirebasePaths.OPCOES).children.mapNotNull { it.getValue(String::class.java) }
            if (pergunta != null && respostaCorreta != null && opcoes.size == 4) {
                Pergunta(pergunta, respostaCorreta, opcoes)
            } else {
                null
            }
        }
    }
}
