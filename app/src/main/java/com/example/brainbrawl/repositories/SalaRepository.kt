package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class JogadorSala(
        val nome: String,
        val isHostOnly: Boolean
    )

    data class ResultadoProcuraSala(
        val existe: Boolean,
        val jogadorJaExiste: Boolean
    )

    data class ListenerHandle internal constructor(
        private val reference: DatabaseReference,
        private val listener: ValueEventListener
    ) {
        internal fun remover() {
            reference.removeEventListener(listener)
        }
    }

    fun criarSala(codigoSala: String, dadosSala: Map<String, Any>): Task<Void> {
        return salaRef(codigoSala).setValue(dadosSala)
    }

    fun procurarSalaPorCodigo(codigoSala: String): Task<Boolean> {
        return salaRef(codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao procurar sala.")
            task.result.exists()
        }
    }

    fun procurarSalaPorCodigo(codigoSala: String, nomeJogador: String): Task<ResultadoProcuraSala> {
        return salaRef(codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao procurar sala.")
            val snapshot = task.result
            ResultadoProcuraSala(
                existe = snapshot.exists(),
                jogadorJaExiste = snapshot.child(FirebasePaths.JOGADORES).hasChild(nomeJogador)
            )
        }
    }

    fun adicionarJogadorASala(
        codigoSala: String,
        nomeJogador: String,
        dadosJogador: Map<String, Any>
    ): Task<Void> {
        return jogadorRef(codigoSala, nomeJogador).setValue(dadosJogador)
    }

    fun garantirJogadorNaSala(
        codigoSala: String,
        nomeJogador: String,
        admin: Boolean
    ): Task<Void> {
        val jogadorRef = jogadorRef(codigoSala, nomeJogador)
        return jogadorRef.get().continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao procurar jogador.")

            if (task.result.exists()) {
                jogadorRef.updateChildren(
                    mapOf(
                        FirebasePaths.ESTADO to GameConstants.ESTADO_ON,
                        FirebasePaths.IS_HOST_ONLY to admin
                    )
                )
            } else {
                jogadorRef.setValue(
                    mapOf(
                        FirebasePaths.NOME to nomeJogador,
                        FirebasePaths.PONTUACAO to 0.0,
                        FirebasePaths.TOTAL_RESPOSTAS_CERTAS to 0,
                        FirebasePaths.ESTADO to GameConstants.ESTADO_ON,
                        FirebasePaths.IS_HOST_ONLY to admin
                    )
                )
            }
        }
    }

    fun removerJogadorDaSala(codigoSala: String, nomeJogador: String): Task<Void> {
        return jogadorRef(codigoSala, nomeJogador).removeValue()
    }

    fun apagarSala(codigoSala: String): Task<Void> {
        return salaRef(codigoSala).removeValue()
    }

    fun atualizarEstadoSala(codigoSala: String, estado: String): Task<Void> {
        return salaRef(codigoSala).child(FirebasePaths.ESTADO).setValue(estado)
    }

    fun obterJogadoresDaSala(codigoSala: String): Task<List<JogadorSala>> {
        return jogadoresRef(codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao obter jogadores.")
            task.result.toJogadoresSala()
        }
    }

    fun escutarJogadoresDaSala(
        codigoSala: String,
        onJogadoresAlterados: (List<JogadorSala>) -> Unit,
        onErro: () -> Unit
    ): ListenerHandle {
        val reference = jogadoresRef(codigoSala)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onJogadoresAlterados(snapshot.toJogadoresSala())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun escutarEstadoDaSala(
        codigoSala: String,
        onEstadoAlterado: (String?) -> Unit,
        onErro: () -> Unit
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

    fun escutarSalaApagada(
        codigoSala: String,
        onSalaExisteAlterada: (Boolean) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(codigoSala)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onSalaExisteAlterada(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    private fun salaRef(codigoSala: String): DatabaseReference {
        return database.child(FirebasePaths.SALAS).child(codigoSala)
    }

    private fun jogadoresRef(codigoSala: String): DatabaseReference {
        return salaRef(codigoSala).child(FirebasePaths.JOGADORES)
    }

    private fun jogadorRef(codigoSala: String, nomeJogador: String): DatabaseReference {
        return jogadoresRef(codigoSala).child(nomeJogador)
    }

    private fun DataSnapshot.toJogadoresSala(): List<JogadorSala> {
        return children.mapNotNull { jogadorSnapshot ->
            val nome = jogadorSnapshot.key ?: return@mapNotNull null
            JogadorSala(
                nome = nome,
                isHostOnly = jogadorSnapshot.child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true
            )
        }
    }
}
