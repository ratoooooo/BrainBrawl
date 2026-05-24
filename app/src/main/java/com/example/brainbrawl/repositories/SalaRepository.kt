package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class JogadorSala(
        val chave: String,
        val nome: String,
        val uid: String,
        val nomeUtilizador: String,
        val nomeJogador: String,
        val avatar: String,
        val estado: String,
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

    fun procurarSalaPorCodigo(codigoSala: String, jogador: JogadorSalaIdentidade): Task<ResultadoProcuraSala> {
        return salaRef(codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao procurar sala.")
            val snapshot = task.result
            ResultadoProcuraSala(
                existe = snapshot.exists(),
                jogadorJaExiste = snapshot.child(FirebasePaths.JOGADORES).temJogador(jogador)
            )
        }
    }

    fun adicionarJogadorASala(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        dadosJogador: Map<String, Any>
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        jogadoresRef(codigoSala).get()
            .addOnSuccessListener { jogadoresSnapshot ->
                val chaveExistente = jogadoresSnapshot.encontrarChaveJogador(jogador)
                jogadorRef(codigoSala, chaveExistente ?: jogador.chaveSala)
                    .setValue(dadosJogador)
                    .addOnSuccessListener { result.setResult(null) }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    @Suppress("UNUSED_PARAMETER")
    fun garantirJogadorNaSala(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        adminHint: Boolean
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()

        salaRef(codigoSala).get()
            .addOnSuccessListener { salaSnapshot ->
                val adminNome = salaSnapshot.child(FirebasePaths.ADMIN).getValue(String::class.java).orEmpty()

                val jogadoresSnapshot = salaSnapshot.child(FirebasePaths.JOGADORES)
                val chave = jogadoresSnapshot.encontrarChaveJogador(jogador) ?: jogador.chaveSala
                val jogadorRef = jogadorRef(codigoSala, chave)

                val isPlaceholderAdmin = chave == GameConstants.JOGADOR_ADMIN && adminNome == GameConstants.JOGADOR_ADMIN
                val isHostOnly = isPlaceholderAdmin || adminHint

                val dados = jogador.toFirebaseMap(isHostOnly = isHostOnly)

                if (jogadoresSnapshot.hasChild(chave)) {
                    jogadorRef.updateChildren(
                        dados + mapOf(
                            FirebasePaths.ESTADO to GameConstants.ESTADO_ON,
                            FirebasePaths.IS_HOST_ONLY to isHostOnly
                        )
                    ).addOnSuccessListener {
                        result.setResult(null)
                    }.addOnFailureListener {
                        result.setException(it)
                    }
                } else {
                    jogadorRef.setValue(dados)
                        .addOnSuccessListener { result.setResult(null) }
                        .addOnFailureListener { result.setException(it) }
                }
            }
            .addOnFailureListener { result.setException(it) }

        return result.task
    }

    fun removerJogadorDaSala(codigoSala: String, jogador: JogadorSalaIdentidade): Task<Void> {
        val updates = jogador.chavesCompatibilidade.associate { chave ->
            "${FirebasePaths.JOGADORES}/$chave" to null
        }
        return salaRef(codigoSala).updateChildren(updates)
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

    private fun jogadorRef(codigoSala: String, chaveJogador: String): DatabaseReference {
        return jogadoresRef(codigoSala).child(chaveJogador)
    }

    private fun DataSnapshot.temJogador(jogador: JogadorSalaIdentidade): Boolean {
        return encontrarChaveJogador(jogador) != null
    }

    private fun DataSnapshot.encontrarChaveJogador(jogador: JogadorSalaIdentidade): String? {
        return children.firstOrNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key.orEmpty()
            chave in jogador.chavesCompatibilidade ||
                jogadorSnapshot.valorTexto(FirebasePaths.UID) in jogador.chavesCompatibilidade ||
                jogadorSnapshot.valorTexto(FirebasePaths.NOME_UTILIZADOR) in jogador.chavesCompatibilidade ||
                jogadorSnapshot.valorTexto(FirebasePaths.NOME_JOGADOR) in jogador.chavesCompatibilidade ||
                jogadorSnapshot.nomeDisplay() in jogador.chavesCompatibilidade
        }?.key
    }

    private fun DataSnapshot.toJogadoresSala(): List<JogadorSala> {
        return children.mapNotNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key ?: return@mapNotNull null
            JogadorSala(
                chave = chave,
                nome = jogadorSnapshot.nomeDisplay().ifBlank { chave },
                uid = jogadorSnapshot.valorTexto(FirebasePaths.UID),
                nomeUtilizador = jogadorSnapshot.valorTexto(FirebasePaths.NOME_UTILIZADOR),
                nomeJogador = jogadorSnapshot.valorTexto(FirebasePaths.NOME_JOGADOR),
                avatar = jogadorSnapshot.valorTexto(FirebasePaths.AVATAR),
                estado = jogadorSnapshot.valorTexto(FirebasePaths.ESTADO).ifBlank { GameConstants.ESTADO_ON },
                isHostOnly = jogadorSnapshot.child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true
            )
        }
    }

    private fun DataSnapshot.nomeDisplay(): String {
        return valorTexto(FirebasePaths.NOME_DISPLAY)
            .ifBlank { valorTexto(FirebasePaths.NOME_UTILIZADOR) }
            .ifBlank { valorTexto(FirebasePaths.NOME_JOGADOR) }
            .ifBlank { valorTexto(FirebasePaths.NOME) }
    }

    private fun DataSnapshot.valorTexto(campo: String): String {
        return child(campo).getValue(String::class.java).orEmpty()
    }
}
