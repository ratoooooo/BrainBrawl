package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.Pergunta
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class JogoRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class SalaInfo(
        val admin: Boolean,
        val modoJogo: String
    )

    data class JogadorEliminatorias(
        val nome: String,
        val estado: String,
        val isHostOnly: Boolean
    )

    data class ListenerHandle internal constructor(
        private val reference: DatabaseReference,
        private val listener: ValueEventListener
    ) {
        internal fun remover() {
            reference.removeEventListener(listener)
        }
    }

    fun obterInfoSala(
        codigoSala: String,
        nomeUtilizador: String,
        nomeJogador: String
    ): Task<SalaInfo> {
        val salaRef = salaRef(codigoSala)
        return salaRef.child(FirebasePaths.ADMIN).get().continueWithTask { adminTask ->
            if (!adminTask.isSuccessful) {
                throw adminTask.exception ?: IllegalStateException("Erro ao identificar admin.")
            }
            val nomeAdmin = adminTask.result.getValue(String::class.java).orEmpty()
            val isAdmin = nomeAdmin == nomeUtilizador || nomeAdmin == nomeJogador
            salaRef.child(FirebasePaths.MODO_JOGO).get().continueWith { modoTask ->
                if (!modoTask.isSuccessful) {
                    throw modoTask.exception ?: IllegalStateException("Erro ao carregar modo de jogo.")
                }
                SalaInfo(
                    admin = isAdmin,
                    modoJogo = modoTask.result.getValue(String::class.java) ?: GameConstants.MODO_CLASSICO
                )
            }
        }
    }

    fun carregarPerguntas(codigoSala: String): Task<List<Pergunta>> {
        return salaRef(codigoSala).child(FirebasePaths.PERGUNTAS).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar perguntas.")
            task.result.children
                .take(8)
                .mapNotNull { it.getValue(Pergunta::class.java) }
        }
    }

    fun escutarIndicePergunta(
        codigoSala: String,
        onIndiceAlterado: (Int) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(codigoSala).child(FirebasePaths.PERGUNTA_ATUAL_INDEX)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onIndiceAlterado(snapshot.intValue())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun obterIndicePergunta(codigoSala: String): Task<Int> {
        return salaRef(codigoSala).child(FirebasePaths.PERGUNTA_ATUAL_INDEX).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao obter pergunta atual.")
            task.result.intValue()
        }
    }

    fun atualizarPerguntaAtual(codigoSala: String, perguntaAtualIndex: Int): Task<Void> {
        return salaRef(codigoSala).updateChildren(
            mapOf<String, Any>(
                FirebasePaths.PERGUNTA_ATUAL_INDEX to perguntaAtualIndex,
                FirebasePaths.PERGUNTA_HORA_INICIO to ServerValue.TIMESTAMP
            )
        )
    }

    fun limparRespostasPergunta(codigoSala: String): Task<Void> {
        return salaRef(codigoSala).child(FirebasePaths.PERGUNTA_ATUAL).child(FirebasePaths.RESPOSTAS).removeValue()
    }

    fun obterHoraInicioPergunta(codigoSala: String): Task<Long?> {
        return salaRef(codigoSala).child(FirebasePaths.PERGUNTA_HORA_INICIO).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao obter hora da pergunta.")
            task.result.getValue(Long::class.java)
        }
    }

    fun registarResposta(codigoSala: String, nomeJogador: String, acertou: Boolean): Task<Void> {
        return salaRef(codigoSala).child(FirebasePaths.PERGUNTA_ATUAL)
            .child(FirebasePaths.RESPOSTAS)
            .child(nomeJogador)
            .setValue(acertou)
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
        return ListenerHandle(reference, listener)
    }

    fun obterJogadores(codigoSala: String): Task<List<String>> {
        return salaRef(codigoSala).child(FirebasePaths.JOGADORES).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao obter jogadores.")
            task.result.children.mapNotNull { it.key }
        }
    }

    fun obterJogadoresEliminatorias(codigoSala: String): Task<List<JogadorEliminatorias>> {
        return salaRef(codigoSala).child(FirebasePaths.JOGADORES).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao obter jogadores.")
            task.result.children.mapNotNull { jogadorSnapshot ->
                val nome = jogadorSnapshot.key ?: return@mapNotNull null
                JogadorEliminatorias(
                    nome = nome,
                    estado = jogadorSnapshot.child(FirebasePaths.ESTADO).getValue(String::class.java).orEmpty(),
                    isHostOnly = jogadorSnapshot.child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true
                )
            }
        }
    }

    fun removerJogador(codigoSala: String, nomeJogador: String): Task<Void> {
        return salaRef(codigoSala).child(FirebasePaths.JOGADORES).child(nomeJogador).removeValue()
    }

    fun marcarJogadorEliminado(
        codigoSala: String,
        nomeJogador: String,
        totalPontos: Double,
        totalRespostasCertas: Int
    ): Task<Void> {
        return salaRef(codigoSala).child(FirebasePaths.JOGADORES).child(nomeJogador).updateChildren(
            mapOf(
                FirebasePaths.ESTADO to GameConstants.ESTADO_ELIMINADO,
                FirebasePaths.PONTUACAO to totalPontos,
                FirebasePaths.TOTAL_RESPOSTAS_CERTAS to totalRespostasCertas
            )
        )
    }

    fun guardarResultadoJogador(
        codigoSala: String,
        nomeJogador: String,
        totalPontos: Double,
        totalRespostasCertas: Int
    ): Task<Void> {
        return salaRef(codigoSala).child(FirebasePaths.JOGADORES).child(nomeJogador).updateChildren(
            mapOf(
                FirebasePaths.PONTUACAO to totalPontos,
                FirebasePaths.TOTAL_RESPOSTAS_CERTAS to totalRespostasCertas
            )
        )
    }

    fun obterEstadoSala(codigoSala: String): Task<String?> {
        return salaRef(codigoSala).child(FirebasePaths.ESTADO).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao verificar estado da sala.")
            task.result.getValue(String::class.java)
        }
    }

    fun escutarEstadoSala(
        codigoSala: String,
        onEstadoAlterado: (String?) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(codigoSala).child(FirebasePaths.ESTADO)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onEstadoAlterado(snapshot.getValue(String::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun atualizarEstadoSala(codigoSala: String, estado: String): Task<Void> {
        return salaRef(codigoSala).child(FirebasePaths.ESTADO).setValue(estado)
    }

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    private fun salaRef(codigoSala: String): DatabaseReference {
        return database.child(FirebasePaths.SALAS).child(codigoSala)
    }

    private fun DataSnapshot.intValue(): Int {
        return getValue(Int::class.java)
            ?: getValue(Long::class.java)?.toInt()
            ?: getValue(Double::class.java)?.toInt()
            ?: 0
    }
}
