package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
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

    data class JogadorCompetitivo(
        val chave: String,
        val nomeDisplay: String,
        val uid: String,
        val nomeUtilizador: String,
        val nomeJogador: String
    )

    data class EquipaJogador(
        val equipa: String,
        val chaveJogador: String,
        val nomeDisplay: String
    )

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
        jogador: JogadorSalaIdentidade
    ): Task<JogadorCompetitivo> {
        val result = TaskCompletionSource<JogadorCompetitivo>()
        val jogadoresRef = salaRef(modo, codigoSala).child(FirebasePaths.JOGADORES)
        jogadoresRef.get()
            .addOnSuccessListener { snapshot ->
                val chave = snapshot.encontrarChaveJogador(jogador) ?: jogador.chaveSala
                jogadoresRef.child(chave).setValue(jogador.toFirebaseMap(isHostOnly = false))
                    .addOnSuccessListener {
                        result.setResult(
                            JogadorCompetitivo(
                                chave = chave,
                                nomeDisplay = jogador.nomeDisplay,
                                uid = jogador.uid,
                                nomeUtilizador = jogador.nomeUtilizador,
                                nomeJogador = jogador.nomeJogador
                            )
                        )
                    }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun marcarPronto1x1(
        codigoSala: String,
        chaveJogador: String,
        pronto: Boolean = true
    ): Task<Void> {
        return salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .child(FirebasePaths.PRONTOS)
            .child(chaveJogador)
            .setValue(pronto)
    }

    fun obterChavesAdmin(modo: ModoCompetitivo, codigoSala: String): Task<List<String>> {
        return salaRef(modo, codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar admin.")
            listOf(
                task.result.child(FirebasePaths.ADMIN_UID).texto(),
                task.result.child(FirebasePaths.ADMIN_ID).texto(),
                task.result.child(FirebasePaths.ADMIN).texto()
            ).filter { it.isNotBlank() }.distinct()
        }
    }

    fun escutarJogadores(
        modo: ModoCompetitivo,
        codigoSala: String,
        onJogadoresAlterados: (List<JogadorCompetitivo>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(modo, codigoSala).child(FirebasePaths.JOGADORES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onJogadoresAlterados(snapshot.toJogadoresCompetitivos())
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

    fun resolverJogador(
        modo: ModoCompetitivo,
        codigoSala: String,
        jogador: JogadorSalaIdentidade
    ): Task<JogadorCompetitivo> {
        return salaRef(modo, codigoSala).child(FirebasePaths.JOGADORES).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar jogador.")
            val jogadorSnapshot = task.result.encontrarJogador(jogador)
            val chave = jogadorSnapshot?.key ?: jogador.chaveSala
            JogadorCompetitivo(
                chave = chave,
                nomeDisplay = jogadorSnapshot?.nomeDisplay()?.ifBlank { jogador.nomeDisplay } ?: jogador.nomeDisplay,
                uid = jogadorSnapshot?.child(FirebasePaths.UID)?.texto()?.ifBlank { jogador.uid } ?: jogador.uid,
                nomeUtilizador = jogadorSnapshot?.child(FirebasePaths.NOME_UTILIZADOR)?.texto()
                    ?.ifBlank { jogador.nomeUtilizador } ?: jogador.nomeUtilizador,
                nomeJogador = jogadorSnapshot?.child(FirebasePaths.NOME_JOGADOR)?.texto()
                    ?.ifBlank { jogador.nomeJogador } ?: jogador.nomeJogador
            )
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

    fun removerJogador1x1(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        chaveJogador: String
    ): Task<Void> {
        val chaves = (jogador.chavesCompatibilidade + chaveJogador).filter { it.isNotBlank() }.distinct()
        return salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala).updateChildren(
            chaves.flatMap { chave ->
                listOf(
                    "${FirebasePaths.JOGADORES}/$chave" to null,
                    "${FirebasePaths.PRONTOS}/$chave" to null
                )
            }.toMap()
        )
    }

    fun removerJogador2x2(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        chaveJogador: String
    ): Task<Void> {
        val chaves = (jogador.chavesCompatibilidade + chaveJogador).filter { it.isNotBlank() }.distinct()
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(
            chaves.flatMap { chave ->
                listOf(
                    "${FirebasePaths.JOGADORES}/$chave" to null,
                    "${FirebasePaths.EQUIPA_A}/$chave" to null,
                    "${FirebasePaths.EQUIPA_B}/$chave" to null
                )
            }.toMap()
        )
    }

    fun guardarEquipas2x2(
        codigoSala: String,
        equipaA: List<JogadorCompetitivo>,
        equipaB: List<JogadorCompetitivo>
    ): Task<Void> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(
            mapOf(
                FirebasePaths.EQUIPA_A to equipaA.associate { it.chave to it.toFirebaseMap() },
                FirebasePaths.EQUIPA_B to equipaB.associate { it.chave to it.toFirebaseMap() },
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

    fun identificarEquipa2x2(codigoSala: String, jogador: JogadorSalaIdentidade): Task<EquipaJogador> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar equipa.")
            val snapshot = task.result
            val jogadorA = snapshot.child(FirebasePaths.EQUIPA_A).encontrarJogador(jogador)
            val jogadorB = snapshot.child(FirebasePaths.EQUIPA_B).encontrarJogador(jogador)
            when {
                jogadorA != null -> EquipaJogador(
                    equipa = GameConstants.EQUIPA_A,
                    chaveJogador = jogadorA.key ?: jogador.chaveSala,
                    nomeDisplay = jogadorA.nomeDisplay().ifBlank { jogador.nomeDisplay }
                )
                jogadorB != null -> EquipaJogador(
                    equipa = GameConstants.EQUIPA_B,
                    chaveJogador = jogadorB.key ?: jogador.chaveSala,
                    nomeDisplay = jogadorB.nomeDisplay().ifBlank { jogador.nomeDisplay }
                )
                else -> EquipaJogador(
                    equipa = "",
                    chaveJogador = snapshot.child(FirebasePaths.JOGADORES).encontrarChaveJogador(jogador) ?: jogador.chaveSala,
                    nomeDisplay = jogador.nomeDisplay
                )
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
        chaveJogador: String,
        totalPontos: Double
    ): Task<Void> {
        return salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .child(FirebasePaths.PONTUACOES)
            .child(chaveJogador)
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
        chaveJogador: String,
        perguntaAtualIndex: Int,
        resposta: String
    ): Task<Void> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .child(FirebasePaths.RESPOSTAS)
            .child(chaveJogador)
            .child(perguntaAtualIndex.toString())
            .setValue(resposta)
    }

    fun guardarResultado2x2(
        codigoSala: String,
        equipa: String,
        chaveJogador: String,
        totalPontos: Double,
        totalPerguntasCertas: Int
    ): Task<Void> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(
            mapOf(
                "${FirebasePaths.PONTUACOES}_$equipa/$chaveJogador" to totalPontos,
                "${FirebasePaths.TOTAL_PERGUNTAS_CERTAS}_$equipa/$chaveJogador" to totalPerguntasCertas
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

    private fun DataSnapshot.encontrarChaveJogador(jogador: JogadorSalaIdentidade): String? {
        return encontrarJogador(jogador)?.key
    }

    private fun DataSnapshot.encontrarJogador(jogador: JogadorSalaIdentidade): DataSnapshot? {
        return children.firstOrNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key.orEmpty()
            chave in jogador.chavesCompatibilidade ||
                jogadorSnapshot.child(FirebasePaths.UID).texto() in jogador.chavesCompatibilidade ||
                jogadorSnapshot.child(FirebasePaths.NOME_UTILIZADOR).texto() in jogador.chavesCompatibilidade ||
                jogadorSnapshot.child(FirebasePaths.NOME_JOGADOR).texto() in jogador.chavesCompatibilidade ||
                jogadorSnapshot.nomeDisplay() in jogador.chavesCompatibilidade
        }
    }

    private fun DataSnapshot.toJogadoresCompetitivos(): List<JogadorCompetitivo> {
        return children.mapNotNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key ?: return@mapNotNull null
            JogadorCompetitivo(
                chave = chave,
                nomeDisplay = jogadorSnapshot.nomeDisplay().ifBlank { chave },
                uid = jogadorSnapshot.child(FirebasePaths.UID).texto(),
                nomeUtilizador = jogadorSnapshot.child(FirebasePaths.NOME_UTILIZADOR).texto(),
                nomeJogador = jogadorSnapshot.child(FirebasePaths.NOME_JOGADOR).texto()
            )
        }
    }

    private fun JogadorCompetitivo.toFirebaseMap(): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.NOME to nomeDisplay,
            FirebasePaths.NOME_DISPLAY to nomeDisplay
        )
        if (uid.isNotBlank()) dados[FirebasePaths.UID] = uid
        if (nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = nomeUtilizador
        if (nomeJogador.isNotBlank()) dados[FirebasePaths.NOME_JOGADOR] = nomeJogador
        return dados
    }

    private fun DataSnapshot.nomeDisplay(): String {
        return child(FirebasePaths.NOME_DISPLAY).texto()
            .ifBlank { child(FirebasePaths.NOME_UTILIZADOR).texto() }
            .ifBlank { child(FirebasePaths.NOME_JOGADOR).texto() }
            .ifBlank { child(FirebasePaths.NOME).texto() }
    }

    private fun DataSnapshot.texto(): String {
        return getValue(String::class.java).orEmpty()
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
