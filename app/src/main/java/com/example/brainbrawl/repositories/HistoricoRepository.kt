package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.HistoricoJogo
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class HistoricoRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference,
    private val pontuacaoRepository: PontuacaoRepository = PontuacaoRepository(database)
) {
    fun guardarHistoricoUmaVez(uid: String, historico: HistoricoJogo): Task<Boolean> {
        val result = TaskCompletionSource<Boolean>()
        if (uid.isBlank() || historico.historicoId.isBlank()) {
            result.setResult(false)
            return result.task
        }

        limparHistoricoAntigo(uid)
            .addOnCompleteListener {
                resolverHistoricoCompetitivo(historico)
                    .addOnSuccessListener { competitivo ->
                        escreverComRecorde(uid, historico.copy(competitivo = competitivo), result)
                    }
                    .addOnFailureListener {
                        escreverComRecorde(uid, historico, result)
                    }
            }
        return result.task
    }

    private fun escreverComRecorde(
        uid: String,
        historico: HistoricoJogo,
        result: TaskCompletionSource<Boolean>
    ) {
        if (!historico.competitivo) {
            escrever(uid, historico.copy(
                recordeFoiBatido = false,
                dataHora = historico.dataHora.takeIf { it > 0L } ?: System.currentTimeMillis()
            ), result)
            return
        }

        pontuacaoRepository.obterRecordePontuacaoJogador(uid)
            .addOnSuccessListener { recorde ->
                escrever(uid, historico.copy(
                    recordeFoiBatido = historico.pontuacao > recorde,
                    dataHora = historico.dataHora.takeIf { it > 0L } ?: System.currentTimeMillis()
                ), result)
            }
            .addOnFailureListener {
                escrever(uid, historico.copy(
                    dataHora = historico.dataHora.takeIf { it > 0L } ?: System.currentTimeMillis()
                ), result)
            }
    }

    private fun resolverHistoricoCompetitivo(historico: HistoricoJogo): Task<Boolean> {
        val tipoSala = when (historico.modo) {
            GameConstants.MODO_1X1 -> PontuacaoRepository.TipoSala.UM_CONTRA_UM
            GameConstants.MODO_2X2 -> PontuacaoRepository.TipoSala.DOIS_CONTRA_DOIS
            GameConstants.MODO_CLASSICO,
            GameConstants.MODO_CAOTICO,
            GameConstants.MODO_ELIMINATORIAS,
            "grupo" -> PontuacaoRepository.TipoSala.GRUPO
            else -> null
        }
        return if (tipoSala != null && historico.codigoSala.isNotBlank()) {
            pontuacaoRepository.salaCompetitiva(tipoSala, historico.codigoSala)
                .continueWith { task ->
                    historico.competitivo && if (task.isSuccessful) (task.result ?: true) else true
                }
        } else {
            val result = TaskCompletionSource<Boolean>()
            result.setResult(historico.competitivo)
            result.task
        }
    }

    fun limparHistoricoAntigo(uid: String): Task<Void> {
        val result = TaskCompletionSource<Void>()
        if (uid.isBlank()) {
            result.setResult(null)
            return result.task
        }

        val limite = System.currentTimeMillis() - GameConstants.HISTORICO_RETENCAO_DIAS * MILLIS_POR_DIA
        historicoRef(uid).get()
            .addOnSuccessListener { snapshot ->
                val updates = mutableMapOf<String, Any?>()
                snapshot.children.forEach { child ->
                    val dataHora = child.child(FirebasePaths.DATA_HORA).longValue()
                    if (dataHora > 0L && dataHora < limite) {
                        child.key?.let { updates[it] = null }
                    }
                }

                if (updates.isEmpty()) {
                    result.setResult(null)
                } else {
                    historicoRef(uid).updateChildren(updates)
                        .addOnSuccessListener { result.setResult(null) }
                        .addOnFailureListener { result.setException(it) }
                }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun carregarUltimosJogos(uid: String, limite: Int = LIMITE_HISTORICO): Task<List<HistoricoJogo>> {
        val result = TaskCompletionSource<List<HistoricoJogo>>()
        if (uid.isBlank()) {
            result.setResult(emptyList())
            return result.task
        }
        limparHistoricoAntigo(uid)
            .addOnCompleteListener {
                historicoRef(uid).orderByChild(FirebasePaths.DATA_HORA).limitToLast(limite).get()
                    .addOnSuccessListener { snapshot ->
                        result.setResult(snapshot.children.mapNotNull { it.toHistoricoJogo() }.sortedByDescending { it.dataHora })
                    }
                    .addOnFailureListener { result.setException(it) }
            }
        return result.task
    }

    private fun escrever(uid: String, historico: HistoricoJogo, result: TaskCompletionSource<Boolean>) {
        historicoRef(uid).child(historico.historicoId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value != null) return Transaction.abort()
                currentData.value = historico.toFirebaseMap()
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    result.setException(error.toException())
                } else {
                    if (committed) limitarHistorico(uid)
                    result.setResult(committed)
                }
            }
        })
    }

    private fun limitarHistorico(uid: String) {
        historicoRef(uid).orderByChild(FirebasePaths.DATA_HORA).get().addOnSuccessListener { snapshot ->
            val todos = snapshot.children.toList()
            if (todos.size <= LIMITE_HISTORICO) return@addOnSuccessListener
            val updates = mutableMapOf<String, Any?>()
            todos.sortedBy { it.child(FirebasePaths.DATA_HORA).longValue() }
                .take(todos.size - LIMITE_HISTORICO)
                .forEach { child -> child.key?.let { updates[it] = null } }
            if (updates.isNotEmpty()) historicoRef(uid).updateChildren(updates)
        }
    }

    private fun historicoRef(uid: String): DatabaseReference {
        return database.child(FirebasePaths.HISTORICO_JOGOS).child(uid)
    }

    private fun DataSnapshot.toHistoricoJogo(): HistoricoJogo? {
        val id = key.orEmpty()
        if (id.isBlank()) return null
        return HistoricoJogo(
            historicoId = id,
            modo = child(FirebasePaths.MODO).texto(),
            codigoSala = child(FirebasePaths.CODIGO_SALA).texto(),
            nomeCategoria = child(FirebasePaths.NOME_CATEGORIA).texto(),
            pontuacao = child(FirebasePaths.PONTUACAO).doubleValue(),
            recordeFoiBatido = child(FirebasePaths.RECORDE_FOI_BATIDO).getValue(Boolean::class.java) == true,
            respostasCertas = child(FirebasePaths.RESPOSTAS_CERTAS).intValue(),
            totalPerguntas = child(FirebasePaths.TOTAL_PERGUNTAS).intValue(),
            venceu = child(FirebasePaths.VENCEU).getValue(Boolean::class.java) == true,
            empate = child(FirebasePaths.EMPATE).getValue(Boolean::class.java) == true,
            equipa = child(FirebasePaths.EQUIPA).texto(),
            competitivo = child(FirebasePaths.COMPETITIVO).getValue(Boolean::class.java) != false,
            dataHora = child(FirebasePaths.DATA_HORA).longValue(),
            jogadoresDaPartida = child(FirebasePaths.JOGADORES).children.mapNotNull { it.texto().takeIf { nome -> nome.isNotBlank() } }
        )
    }

    private fun DataSnapshot.texto() = getValue(String::class.java).orEmpty()
    private fun DataSnapshot.intValue() = getValue(Int::class.java) ?: getValue(Long::class.java)?.toInt() ?: 0
    private fun DataSnapshot.longValue() = getValue(Long::class.java) ?: getValue(Int::class.java)?.toLong() ?: 0L
    private fun DataSnapshot.doubleValue() = getValue(Double::class.java) ?: getValue(Long::class.java)?.toDouble() ?: 0.0

    private companion object {
        const val LIMITE_HISTORICO = 50
        const val MILLIS_POR_DIA = 24L * 60L * 60L * 1000L
    }
}
