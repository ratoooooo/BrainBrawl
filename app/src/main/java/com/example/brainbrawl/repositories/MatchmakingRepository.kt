package com.example.brainbrawl.repositories

import android.util.Log
import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.MatchmakingPlayer
import com.example.brainbrawl.models.MatchmakingResult
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}

private fun List<MatchmakingPlayer>.maskedPlayerKeys(): List<String> {
    return map { it.playerKey.maskedLogId() }
}

class MatchmakingRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class ListenerHandle internal constructor(
        private val removerListener: () -> Unit
    ) {
        internal fun remover() {
            removerListener()
        }
    }

    data class MatchCriado(
        val codigoSala: String,
        val jogadores: List<MatchmakingPlayer>
    )

    fun entrarNaFila(player: MatchmakingPlayer, modo: String): Task<Void> {
        Log.d(
            TAG,
            "A escrever fila: modo=$modo player=${player.playerKey.maskedLogId()} " +
                "tipo=${player.tipoJogador} uidPresente=${player.uid.isNotBlank()} estado=${player.estado}"
        )
        val updates = hashMapOf<String, Any?>(
            "${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.FILA}/${player.playerKey}" to player.toFirebaseMap(),
            "${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.RESULTADOS}/${player.playerKey}" to null,
            "${FirebasePaths.MATCHMAKING}/${outroModo(modo)}/${FirebasePaths.FILA}/${player.playerKey}" to null,
            "${FirebasePaths.MATCHMAKING}/${outroModo(modo)}/${FirebasePaths.RESULTADOS}/${player.playerKey}" to null
        )

        if (player.isGuest) {
            listOf(modo, outroModo(modo)).forEach { modoParaLimpar ->
                updates.putAll(guestComMesmoNomeUpdates(modoParaLimpar, player))
            }
        }

        limparStale(modo)
        filaRef(modo).child(player.playerKey).onDisconnect().removeValue()
        return database.updateChildren(updates)
    }

    fun cancelar(playerKey: String, modo: String): Task<Boolean> {
        val result = TaskCompletionSource<Boolean>()
        resultadoRef(modo, playerKey).get()
            .addOnSuccessListener { resultado ->
                val resultadoMatch = MatchmakingResult.fromSnapshot(resultado)
                if (resultadoMatch != null) {
                    verificarSalaExiste(resultadoMatch.modo, resultadoMatch.codigoSala)
                        .addOnSuccessListener { salaExiste ->
                            if (salaExiste) {
                                cancelarOnDisconnect(playerKey, modo)
                                result.setResult(false)
                            } else {
                                limparFilaEResultado(modo, playerKey)
                                    .addOnSuccessListener { result.setResult(true) }
                                    .addOnFailureListener { result.setException(it) }
                            }
                        }
                        .addOnFailureListener { result.setException(it) }
                    return@addOnSuccessListener
                }

                avaliarCancelamentoSemResultado(modo, playerKey)
                    .addOnSuccessListener { podeLimpar ->
                        if (podeLimpar) {
                            limparFilaEResultado(modo, playerKey)
                                .addOnSuccessListener { result.setResult(true) }
                                .addOnFailureListener { result.setException(it) }
                        } else {
                            cancelarOnDisconnect(playerKey, modo)
                            result.setResult(false)
                        }
                    }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun cancelarOnDisconnect(playerKey: String, modo: String): Task<Void> {
        return filaRef(modo).child(playerKey).onDisconnect().cancel()
    }

    fun consumirResultado(modo: String, playerKey: String): Task<Void> {
        return resultadoRef(modo, playerKey).removeValue()
    }

    fun verificarSalaExiste(modo: String, codigoSala: String): Task<Boolean> {
        return database.child(salaNode(modo)).child(codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao verificar sala.")
            task.result.exists()
        }
    }

    fun verificarJogadorNaSala(modo: String, codigoSala: String, playerKey: String): Task<Boolean> {
        return database.child(salaNode(modo)).child(codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao verificar sala.")
            val sala = task.result
            if (!sala.exists() || playerKey.isBlank()) return@continueWith false
            val jogadores = sala.child(FirebasePaths.JOGADORES)
            jogadores.child(playerKey).exists() ||
                jogadores.children.any { jogador ->
                    jogador.child(FirebasePaths.PLAYER_KEY).texto() == playerKey ||
                        jogador.child(FirebasePaths.UID).texto() == playerKey
                }
        }
    }

    private fun avaliarCancelamentoSemResultado(modo: String, playerKey: String): Task<Boolean> {
        val result = TaskCompletionSource<Boolean>()
        filaRef(modo).child(playerKey).get()
            .addOnSuccessListener { filaJogador ->
                if (!filaJogador.exists()) {
                    result.setResult(true)
                    return@addOnSuccessListener
                }

                val estado = filaJogador.child(FirebasePaths.ESTADO).texto()
                    .ifBlank { GameConstants.ESTADO_AGUARDANDO }
                if (estado == GameConstants.ESTADO_AGUARDANDO) {
                    result.setResult(true)
                    return@addOnSuccessListener
                }

                val codigoSala = filaJogador.child(FirebasePaths.CODIGO_SALA).texto()
                if (estado != GameConstants.ESTADO_ENCONTRADO || codigoSala.isBlank()) {
                    result.setResult(true)
                    return@addOnSuccessListener
                }

                verificarSalaExiste(modo, codigoSala)
                    .addOnSuccessListener { salaExiste ->
                        val timestamp = filaJogador.child(FirebasePaths.TIMESTAMP_ENTRADA).longValue()
                        val timeoutCriacao = timestamp > 0 &&
                            System.currentTimeMillis() - timestamp > MATCH_CREATION_TIMEOUT_MS
                        result.setResult(!salaExiste || timeoutCriacao)
                    }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    private fun limparFilaEResultado(modo: String, playerKey: String): Task<Void> {
        cancelarOnDisconnect(playerKey, modo)
        return database.updateChildren(
            mapOf<String, Any?>(
                "${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.FILA}/$playerKey" to null,
                "${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.RESULTADOS}/$playerKey" to null
            )
        )
    }

    private fun rollbackMatch(
        modo: String,
        codigoSala: String,
        matchId: String,
        jogadores: List<MatchmakingPlayer>,
        removerSala: Boolean
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        matchmakingModoRef(modo).child(FirebasePaths.RESULTADOS).get()
            .addOnSuccessListener { resultados ->
                val updates = hashMapOf<String, Any?>(
                    "${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.MATCHES}/$matchId" to null
                )
                if (removerSala) {
                    updates["${salaNode(modo)}/$codigoSala"] = null
                }
                jogadores.forEach { jogador ->
                    if (!resultados.child(jogador.playerKey).exists()) {
                        updates["${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.FILA}/${jogador.playerKey}"] = null
                        updates["${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.RESULTADOS}/${jogador.playerKey}"] = null
                    }
                }
                database.updateChildren(updates)
                    .addOnSuccessListener { result.setResult(null) }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun observarFila(
        modo: String,
        onFila: (List<MatchmakingPlayer>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = filaRef(modo)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val agora = System.currentTimeMillis()
                onFila(snapshot.children.mapNotNull { MatchmakingPlayer.fromSnapshot(it) }
                    .filter { it.estado == GameConstants.ESTADO_AGUARDANDO || it.estado == GameConstants.ESTADO_ENCONTRADO }
                    .filter { it.timestampEntrada == 0L || agora - it.timestampEntrada <= STALE_MS }
                    .distinctBy { it.playerKey }
                    .sortedBy { it.timestampEntrada }
                    .also { Log.d(TAG, "Fila Firebase observada: modo=$modo jogadores=${it.size}") })
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun observarResultado(
        modo: String,
        playerKey: String,
        onResultado: (MatchmakingResult) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = resultadoRef(modo, playerKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                MatchmakingResult.fromSnapshot(snapshot)?.let(onResultado)
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun tentarCriarMatch(modo: String, nomeCategoria: String, criadorKey: String): Task<MatchCriado?> {
        val result = TaskCompletionSource<MatchCriado?>()
        val modoRef = matchmakingModoRef(modo)
        val limite = limiteJogadores(modo)
        val agora = System.currentTimeMillis()
        var jogadoresSelecionados: List<MatchmakingPlayer> = emptyList()
        var criadorSelecionado: MatchmakingPlayer? = null
        var codigoSala = ""
        var matchId = ""

        modoRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                jogadoresSelecionados = emptyList()
                criadorSelecionado = null
                codigoSala = ""
                matchId = ""
                removerStaleTransaction(currentData, agora)

                val fila = currentData.child(FirebasePaths.FILA).children.mapNotNull { child ->
                    child.toMatchmakingPlayer()
                }
                    .filter { it.estado == GameConstants.ESTADO_AGUARDANDO }
                    .distinctBy { it.playerKey }
                    .sortedBy { it.timestampEntrada }

                if (fila.size < limite) {
                    return Transaction.abort()
                }

                jogadoresSelecionados = fila.take(limite)
                if (!jogadoresSelecionados.temLimiteExato(limite)) {
                    Log.w(
                        TAG,
                        "Selecao invalida para match: modo=$modo limite=$limite " +
                            "quantidade=${jogadoresSelecionados.size} jogadores=${jogadoresSelecionados.maskedPlayerKeys()}"
                    )
                    jogadoresSelecionados = emptyList()
                    return Transaction.abort()
                }
                val criador = jogadoresSelecionados.firstOrNull { it.playerKey == criadorKey }
                    ?: return Transaction.abort()
                criadorSelecionado = criador
                matchId = "match_${jogadoresSelecionados.matchIdentity().hashCode().absoluteKey()}"
                if (currentData.child(FirebasePaths.MATCHES).child(matchId).value != null) {
                    return Transaction.abort()
                }

                codigoSala = gerarCodigoSala()
                Log.d(
                    TAG,
                    "Match claim: modo=$modo sala=${codigoSala.maskedLogId()} " +
                        "criador=${criador.playerKey.maskedLogId()} jogadores=${jogadoresSelecionados.maskedPlayerKeys()}"
                )

                currentData.child(FirebasePaths.MATCHES).child(matchId).value = mapOf(
                    FirebasePaths.ESTADO to GameConstants.ESTADO_CRIANDO,
                    FirebasePaths.CODIGO_SALA to codigoSala,
                    FirebasePaths.MODO to modo,
                    FirebasePaths.CRIADOR_ID to criador.playerKey,
                    FirebasePaths.CRIADOR_UID to criador.uid,
                    FirebasePaths.TIMESTAMP_ENTRADA to agora
                )

                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    logErroFirebase(
                        "Transacao matchmaking falhou: modo=$modo criador=${criadorKey.maskedLogId()}",
                        error.toException()
                    )
                    result.setException(error.toException())
                    return
                }
                val criador = criadorSelecionado
                if (!committed || jogadoresSelecionados.isEmpty() || codigoSala.isBlank() || criador == null) {
                    result.setResult(null)
                    return
                }

                criarSalaEPublicarResultados(modo, nomeCategoria, codigoSala, matchId, criador, jogadoresSelecionados)
                    .addOnSuccessListener {
                        result.setResult(MatchCriado(codigoSala, jogadoresSelecionados))
                    }
                    .addOnFailureListener { result.setException(it) }
            }
        })

        return result.task
    }

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    private fun criarSalaEPublicarResultados(
        modo: String,
        nomeCategoria: String,
        codigoSala: String,
        matchId: String,
        criador: MatchmakingPlayer,
        jogadores: List<MatchmakingPlayer>
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val salaNode = salaNode(modo)
        val salaRef = database.child(salaNode).child(codigoSala)
        val limite = limiteJogadores(modo)

        fun falharComRollback(exception: Exception, removerSala: Boolean) {
            logErroFirebase(
                "Falha ao criar/publicar match. Rollback: modo=$modo " +
                    "sala=${codigoSala.maskedLogId()} removerSala=$removerSala",
                exception
            )
            rollbackMatch(modo, codigoSala, matchId, jogadores, removerSala)
                .addOnCompleteListener {
                    result.setException(exception)
                }
        }

        if (!jogadores.temLimiteExato(limite)) {
            falharComRollback(
                IllegalStateException(
                    "Selecao invalida para sala $modo: esperado=$limite recebido=${jogadores.size} " +
                        "jogadores=${jogadores.maskedPlayerKeys()}"
                ),
                removerSala = false
            )
            return result.task
        }

        salaRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value != null) return Transaction.abort()
                val payload = salaMap(modo, nomeCategoria, criador, jogadores)
                Log.d(
                    TAG,
                    "A criar sala: node=$salaNode codigo=${codigoSala.maskedLogId()} modo=$modo " +
                        "adminId=${criador.playerKey.maskedLogId()} adminUid=${criador.uid.maskedLogId()} " +
                        "jogadores=${jogadores.maskedPlayerKeys()} quantidade=${jogadores.size} " +
                        "campos=${payload.keys}"
                )
                currentData.value = payload
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    falharComRollback(error.toException(), removerSala = false)
                    return
                }
                if (!committed) {
                    falharComRollback(IllegalStateException("Código de sala já existe."), removerSala = false)
                    return
                }
                val jogadoresNaSala = snapshot?.child(FirebasePaths.JOGADORES)?.childrenCount?.toInt() ?: 0
                val permitidosNaSala = snapshot?.child(FirebasePaths.JOGADORES_PERMITIDOS)?.childrenCount?.toInt() ?: 0
                if (jogadoresNaSala != limite || permitidosNaSala != limite) {
                    falharComRollback(
                        IllegalStateException(
                            "Sala criada com numero invalido de jogadores: modo=$modo " +
                                "esperado=$limite jogadores=$jogadoresNaSala permitidos=$permitidosNaSala"
                        ),
                        removerSala = true
                    )
                    return
                }

                val updates = hashMapOf<String, Any?>(
                    "${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.MATCHES}/$matchId/${FirebasePaths.ESTADO}" to GameConstants.ESTADO_ENCONTRADO
                )
                val jogadoresMap = jogadores.associate { it.playerKey to it.toFirebaseMap(GameConstants.ESTADO_ENCONTRADO) }
                jogadores.forEach { jogador ->
                    updates["${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.FILA}/${jogador.playerKey}"] = null
                    updates["${FirebasePaths.MATCHMAKING}/$modo/${FirebasePaths.RESULTADOS}/${jogador.playerKey}"] = resultadoMap(
                        modo,
                        nomeCategoria,
                        codigoSala,
                        criador,
                        jogador,
                        jogadoresMap
                    )
                }
                Log.d(
                    TAG,
                    "A publicar resultados: modo=$modo sala=${codigoSala.maskedLogId()} " +
                        "match=${matchId.maskedLogId()} jogadores=${jogadores.maskedPlayerKeys()} " +
                        "quantidade=${jogadores.size} updates=${updates.size}"
                )
                database.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d(
                            TAG,
                            "Resultados publicados: modo=$modo sala=${codigoSala.maskedLogId()} " +
                                "jogadores=${jogadores.maskedPlayerKeys()}"
                        )
                        result.setResult(null)
                    }
                    .addOnFailureListener { falharComRollback(it, removerSala = true) }
            }
        })

        return result.task
    }

    private fun salaMap(
        modo: String,
        nomeCategoria: String,
        criador: MatchmakingPlayer,
        jogadores: List<MatchmakingPlayer>
    ): Map<String, Any> {
        val limite = limiteJogadores(modo)
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.JOGADORES to jogadores.associate { it.playerKey to it.toSalaJogadorMap() },
            FirebasePaths.JOGADORES_PERMITIDOS to jogadores.associate { it.playerKey to true },
            FirebasePaths.ADMIN to criador.nomeDisplay,
            FirebasePaths.ADMIN_ID to criador.playerKey,
            FirebasePaths.ESTADO to GameConstants.ESTADO_EM_ESPERA,
            FirebasePaths.NOME_CATEGORIA to nomeCategoria,
            FirebasePaths.ORIGEM to GameConstants.ORIGEM_MATCHMAKING,
            FirebasePaths.LOTACAO_MAXIMA to limite,
            FirebasePaths.ENTRADA_FECHADA to true
        )
        if (criador.uid.isNotBlank()) dados[FirebasePaths.ADMIN_UID] = criador.uid
        return dados
    }

    private fun resultadoMap(
        modo: String,
        nomeCategoria: String,
        codigoSala: String,
        criador: MatchmakingPlayer,
        jogador: MatchmakingPlayer,
        jogadores: Map<String, Map<String, Any>>
    ): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.PLAYER_KEY to jogador.playerKey,
            FirebasePaths.UID to jogador.uid,
            FirebasePaths.TIPO_JOGADOR to jogador.tipoJogador,
            FirebasePaths.CODIGO_SALA to codigoSala,
            FirebasePaths.MODO to modo,
            FirebasePaths.NOME_CATEGORIA to nomeCategoria,
            FirebasePaths.CRIADOR_ID to criador.playerKey,
            FirebasePaths.CRIADOR_UID to criador.uid,
            FirebasePaths.ESTADO to GameConstants.ESTADO_ENCONTRADO,
            FirebasePaths.TIMESTAMP_ENTRADA to System.currentTimeMillis(),
            FirebasePaths.JOGADORES to jogadores
        )
        return dados
    }

    private fun limparStale(modo: String) {
        filaRef(modo).get().addOnSuccessListener { snapshot ->
            val agora = System.currentTimeMillis()
            val updates = snapshot.children.mapNotNull { child ->
                val jogador = MatchmakingPlayer.fromSnapshot(child) ?: return@mapNotNull null
                if (jogador.timestampEntrada > 0 && agora - jogador.timestampEntrada > STALE_MS) {
                    child.key?.let { it to null }
                } else {
                    null
                }
            }.toMap()
            if (updates.isNotEmpty()) filaRef(modo).updateChildren(updates)
        }
    }

    private fun guestComMesmoNomeUpdates(modo: String, player: MatchmakingPlayer): Map<String, Any?> {
        val result = hashMapOf<String, Any?>()
        filaRef(modo).get().addOnSuccessListener { snapshot ->
            val updates = snapshot.children.mapNotNull { child ->
                val outro = MatchmakingPlayer.fromSnapshot(child) ?: return@mapNotNull null
                if (outro.isGuest &&
                    outro.playerKey != player.playerKey &&
                    outro.nomeJogador == player.nomeJogador &&
                    outro.estado == GameConstants.ESTADO_AGUARDANDO
                ) {
                    child.key?.let { it to null }
                } else {
                    null
                }
            }.toMap()
            if (updates.isNotEmpty()) filaRef(modo).updateChildren(updates)
        }
        return result
    }

    private fun removerStaleTransaction(currentData: MutableData, agora: Long) {
        currentData.child(FirebasePaths.FILA).children.forEach { child ->
            val timestamp = child.child(FirebasePaths.TIMESTAMP_ENTRADA).getValue(Long::class.java)
                ?: child.child(FirebasePaths.TIMESTAMP_ENTRADA).getValue(Int::class.java)?.toLong()
                ?: 0L
            if (timestamp > 0 && agora - timestamp > STALE_MS) {
                child.value = null
            }
        }
    }

    private fun MutableData.toMatchmakingPlayer(): MatchmakingPlayer? {
        val playerKey = child(FirebasePaths.PLAYER_KEY).getValue(String::class.java)
            ?: key
            ?: return null
        val uid = child(FirebasePaths.UID).getValue(String::class.java).orEmpty()
        val tipo = child(FirebasePaths.TIPO_JOGADOR).getValue(String::class.java)
            ?: if (uid.isBlank()) GameConstants.TIPO_JOGADOR_GUEST else GameConstants.TIPO_JOGADOR_AUTH
        val nomeUtilizador = child(FirebasePaths.NOME_UTILIZADOR).getValue(String::class.java).orEmpty()
        val nomeJogador = child(FirebasePaths.NOME_JOGADOR).getValue(String::class.java).orEmpty()
        val nomeDisplay = child(FirebasePaths.NOME_DISPLAY).getValue(String::class.java)
            ?: nomeUtilizador.ifBlank { nomeJogador.ifBlank { uid.ifBlank { playerKey } } }
        val timestamp = child(FirebasePaths.TIMESTAMP_ENTRADA).getValue(Long::class.java)
            ?: child(FirebasePaths.TIMESTAMP_ENTRADA).getValue(Int::class.java)?.toLong()
            ?: 0L
        return MatchmakingPlayer(
            playerKey = playerKey,
            uid = uid,
            tipoJogador = tipo,
            nomeUtilizador = nomeUtilizador,
            nomeJogador = nomeJogador,
            nomeDisplay = nomeDisplay,
            avatar = child(FirebasePaths.AVATAR).getValue(String::class.java).orEmpty(),
            timestampEntrada = timestamp,
            estado = child(FirebasePaths.ESTADO).getValue(String::class.java) ?: GameConstants.ESTADO_AGUARDANDO
        )
    }

    private fun matchmakingModoRef(modo: String): DatabaseReference {
        return database.child(FirebasePaths.MATCHMAKING).child(modo)
    }

    private fun filaRef(modo: String): DatabaseReference {
        return matchmakingModoRef(modo).child(FirebasePaths.FILA)
    }

    private fun resultadoRef(modo: String, playerKey: String): DatabaseReference {
        return matchmakingModoRef(modo).child(FirebasePaths.RESULTADOS).child(playerKey)
    }

    private fun outroModo(modo: String): String {
        return if (modo == GameConstants.MODO_2X2) GameConstants.MODO_1X1 else GameConstants.MODO_2X2
    }

    private fun limiteJogadores(modo: String): Int {
        return if (modo == GameConstants.MODO_2X2) 4 else 2
    }

    private fun salaNode(modo: String): String {
        return if (modo == GameConstants.MODO_2X2) FirebasePaths.SALA_2X2 else FirebasePaths.SALA_1X1
    }

    private fun Int.absoluteKey(): String {
        return toLong().let { if (it < 0) -it else it }.toString()
    }

    private fun List<MatchmakingPlayer>.matchIdentity(): String {
        return map { jogador ->
            "${jogador.playerKey}:${jogador.timestampEntrada}"
        }.sorted().joinToString("_")
    }

    private fun List<MatchmakingPlayer>.temLimiteExato(limite: Int): Boolean {
        val chaves = map { it.playerKey }
        return size == limite &&
            chaves.all { it.isNotBlank() } &&
            chaves.distinct().size == limite
    }

    private fun logErroFirebase(mensagem: String, exception: Exception) {
        val detalhe = exception.message.orEmpty()
        val permissao = detalhe.contains("permission_denied", ignoreCase = true) ||
            detalhe.contains("Permission denied", ignoreCase = true)
        if (permissao) {
            Log.e(TAG, "$mensagem Firebase permission_denied: $detalhe", exception)
        } else {
            Log.e(TAG, "$mensagem Firebase erro: $detalhe", exception)
        }
    }

    private fun DataSnapshot.texto(): String {
        return getValue(String::class.java).orEmpty()
    }

    private fun DataSnapshot.longValue(): Long {
        return getValue(Long::class.java)
            ?: getValue(Int::class.java)?.toLong()
            ?: getValue(Double::class.java)?.toLong()
            ?: 0L
    }

    private companion object {
        const val TAG = "MatchmakingRepo"
        const val STALE_MS = 2 * 60 * 1000L
        const val MATCH_CREATION_TIMEOUT_MS = 15 * 1000L
    }
}
