package com.example.brainbrawl.repositories

import android.util.Log
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

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}

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
        val nomeJogador: String,
        val playerKey: String = "",
        val tipoJogador: String = "",
        val avatar: String = "",
        val estado: String = GameConstants.ESTADO_ON,
        val pontuacao: Double = 0.0
    )

    data class EquipaJogador(
        val equipa: String,
        val chaveJogador: String,
        val nomeDisplay: String
    )

    data class CodigoSalaInfo(
        val origem: String,
        val entradaFechada: Boolean
    ) {
        val codigoVisivel: Boolean
            get() = !entradaFechada && (origem.isBlank() || origem == GameConstants.ORIGEM_MANUAL)

        val textoPrivado: String
            get() = when (origem) {
                GameConstants.ORIGEM_MATCHMAKING -> "Partida automática"
                else -> "Partida por convite"
            }
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
        jogador: JogadorSalaIdentidade
    ): Task<JogadorCompetitivo> {
        val result = TaskCompletionSource<JogadorCompetitivo>()
        val salaRef = salaRef(modo, codigoSala)
        salaRef.get()
            .addOnSuccessListener { salaSnapshot ->
                if (!salaSnapshot.exists()) {
                    result.setException(IllegalStateException("Sala ${modo.node}/$codigoSala nao existe."))
                    return@addOnSuccessListener
                }

                val limite = salaSnapshot.lotacaoMaxima(modo)
                val jogadoresSnapshot = salaSnapshot.child(FirebasePaths.JOGADORES)
                val entradaFechada = salaSnapshot.entradaFechada()

                if (entradaFechada) {
                    val chaveExistente = jogadoresSnapshot.encontrarChaveJogadorFechado(jogador)
                    val chavePermitida = salaSnapshot.child(FirebasePaths.JOGADORES_PERMITIDOS)
                        .encontrarChavePermitida(jogador)
                    val chaveEntrada = chaveExistente ?: chavePermitida
                    if (chaveEntrada == null) {
                        Log.w(
                            TAG,
                            "Entrada bloqueada em sala fechada: modo=${modo.node} " +
                                "codigo=${codigoSala.maskedLogId()} " +
                                "player=${jogador.playerKey.ifBlank { jogador.uid }.maskedLogId()}"
                        )
                        result.setException(IllegalStateException("Sala fechada para jogadores selecionados."))
                        return@addOnSuccessListener
                    }
                    val jogadorPresente = jogadoresSnapshot.child(chaveEntrada).exists()
                    Log.d(
                        TAG,
                        "Entrada sala fechada: modo=${modo.node} codigo=${codigoSala.maskedLogId()} " +
                            "player=${jogador.playerKey.ifBlank { jogador.uid }.maskedLogId()} " +
                            "chaveEntrada=${chaveEntrada.maskedLogId()} existente=$jogadorPresente"
                    )
                    val taskEntrada = if (jogadorPresente) {
                        marcarJogadorPresente(modo, codigoSala, chaveEntrada)
                    } else {
                        escreverJogadorNaSala(modo, codigoSala, chaveEntrada, jogador).continueWithTask {
                            marcarJogadorPresente(modo, codigoSala, chaveEntrada)
                        }
                    }
                    taskEntrada
                        .addOnSuccessListener {
                            val snapshotJogador = jogadoresSnapshot.child(chaveEntrada)
                            result.setResult(snapshotJogador.toJogadorCompetitivo(chaveEntrada, jogador))
                        }
                        .addOnFailureListener { result.setException(it) }
                    return@addOnSuccessListener
                }

                val chaveExistente = jogadoresSnapshot.encontrarChaveJogador(jogador)
                val chave = chaveExistente ?: jogador.chaveSala
                if (chave.isBlank()) {
                    result.setException(IllegalStateException("Jogador sem chave valida para entrar na sala."))
                    return@addOnSuccessListener
                }

                fun escrever() {
                    escreverJogadorNaSala(modo, codigoSala, chave, jogador)
                        .addOnSuccessListener { result.setResult(it) }
                        .addOnFailureListener { result.setException(it) }
                }

                if (chaveExistente != null) {
                    escrever()
                    return@addOnSuccessListener
                }

                reservarEntradaNaSala(
                    modo = modo,
                    codigoSala = codigoSala,
                    chave = chave,
                    limite = limite,
                    chavesAtuais = jogadoresSnapshot.chavesJogadoresReais()
                )
                    .addOnSuccessListener {
                        escrever()
                    }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    private fun escreverJogadorNaSala(
        modo: ModoCompetitivo,
        codigoSala: String,
        chave: String,
        jogador: JogadorSalaIdentidade
    ): Task<JogadorCompetitivo> {
        val result = TaskCompletionSource<JogadorCompetitivo>()
        salaRef(modo, codigoSala)
            .child(FirebasePaths.JOGADORES)
            .child(chave)
            .setValue(jogador.toFirebaseMap(isHostOnly = false))
            .addOnSuccessListener {
                configurarOfflineAoDesligar(modo, codigoSala, chave)
                result.setResult(
                    JogadorCompetitivo(
                        chave = chave,
                        nomeDisplay = jogador.nomeDisplay,
                        uid = jogador.uid,
                        nomeUtilizador = jogador.nomeUtilizador,
                        nomeJogador = jogador.nomeJogador,
                        playerKey = jogador.playerKey.ifBlank { chave },
                        tipoJogador = jogador.tipoJogador,
                        avatar = jogador.avatar,
                        estado = GameConstants.ESTADO_ON
                    )
                )
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    private fun marcarJogadorPresente(
        modo: ModoCompetitivo,
        codigoSala: String,
        chave: String
    ): Task<Void> {
        val jogadorRef = salaRef(modo, codigoSala)
            .child(FirebasePaths.JOGADORES)
            .child(chave)
        configurarOfflineAoDesligar(modo, codigoSala, chave)
        Log.d(
            TAG,
            "A marcar jogador presente: modo=${modo.node} codigo=${codigoSala.maskedLogId()} " +
                "chave=${chave.maskedLogId()} path=${modo.node}/$codigoSala/${FirebasePaths.JOGADORES}/$chave/${FirebasePaths.ESTADO}"
        )
        return jogadorRef.updateChildren(
            mapOf(
                FirebasePaths.ESTADO to GameConstants.ESTADO_ON
            )
        )
    }

    private fun configurarOfflineAoDesligar(
        modo: ModoCompetitivo,
        codigoSala: String,
        chave: String
    ) {
        val sala = salaRef(modo, codigoSala)
        sala.child(FirebasePaths.JOGADORES)
            .child(chave)
            .onDisconnect()
            .updateChildren(mapOf(FirebasePaths.ESTADO to GameConstants.ESTADO_OFF))
        // Evita "pronto" fantasma após desconexão (1x1 e 2x2 usam o mesmo nó prontos).
        sala.child(FirebasePaths.PRONTOS).child(chave).onDisconnect().removeValue()
        Log.d(
            HOST_REMOVAL_TAG,
            "onDisconnect configurado: modo=${modo.node} codigo=${codigoSala.maskedLogId()} " +
                "chave=${chave.maskedLogId()} removePlayer=false markOffOnly=true removePronto=true"
        )
    }

    private fun reservarEntradaNaSala(
        modo: ModoCompetitivo,
        codigoSala: String,
        chave: String,
        limite: Int,
        chavesAtuais: Set<String>
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val reservasRef = salaRef(modo, codigoSala).child(FirebasePaths.JOGADORES_PERMITIDOS)

        reservasRef.runTransaction(object : Transaction.Handler {
            var salaCheia = false

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                salaCheia = false
                val reservados = currentData.children.mapNotNull { reserva ->
                    reserva.key?.takeIf { reserva.getValue(Boolean::class.java) == true }
                }.toMutableSet()

                reservados.addAll(chavesAtuais)

                if (chave !in reservados && reservados.size >= limite) {
                    salaCheia = true
                    return Transaction.abort()
                }

                reservados.add(chave)
                currentData.value = reservados.associateWith { true }
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    result.setException(error.toException())
                    return
                }
                if (!committed) {
                    Log.w(TAG, "Reserva bloqueada em sala cheia: modo=${modo.node} codigo=${codigoSala.maskedLogId()} limite=$limite")
                    result.setException(
                        IllegalStateException(
                            if (salaCheia) "Sala ${modo.node}/$codigoSala cheia: limite=$limite" else "Entrada nao reservada."
                        )
                    )
                    return
                }
                result.setResult(null)
            }
        })

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

    fun verificarAdmin(
        modo: ModoCompetitivo,
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        chaveJogador: String
    ): Task<Boolean> {
        return salaRef(modo, codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar admin.")
            val salaSnapshot = task.result
            val jogadoresSnapshot = salaSnapshot.child(FirebasePaths.JOGADORES)
            val jogadorSnapshot = jogadoresSnapshot.encontrarJogador(jogador)
                ?: jogadoresSnapshot.child(chaveJogador).takeIf { it.exists() }

            val identidadesJogador = (
                jogador.chavesCompatibilidade +
                    chaveJogador +
                    jogadorSnapshot.identificadoresJogador()
                )
                .filter { it.isNotBlank() }
                .distinct()

            val chavesAdmin = listOf(
                salaSnapshot.child(FirebasePaths.ADMIN_UID).texto(),
                salaSnapshot.child(FirebasePaths.ADMIN_ID).texto(),
                salaSnapshot.child(FirebasePaths.ADMIN).texto()
            ).filter { it.isNotBlank() }.distinct()

            if (chavesAdmin.isEmpty()) {
                val primeiroJogadorReal = jogadoresSnapshot.toJogadoresCompetitivos()
                    .firstOrNull { it.chave != GameConstants.JOGADOR_ADMIN }
                return@continueWith primeiroJogadorReal?.identificadores()
                    ?.any { it in identidadesJogador } == true
            }

            chavesAdmin.any { chaveAdmin -> chaveAdmin in identidadesJogador }
        }
    }

    fun obterCodigoSalaInfo(modo: ModoCompetitivo, codigoSala: String): Task<CodigoSalaInfo> {
        return salaRef(modo, codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar privacidade da sala.")
            val snapshot = task.result
            val origem = snapshot.child(FirebasePaths.ORIGEM).texto()
            Log.d(
                FLOW_TAG,
                "roomInfo path=${modo.node}/$codigoSala origem=${origem.ifBlank { "<empty>" }} " +
                    "entradaFechada=${snapshot.entradaFechada()} estado=${snapshot.child(FirebasePaths.ESTADO).texto()} " +
                    "players=${snapshot.child(FirebasePaths.JOGADORES).children.mapNotNull { it.key?.maskedLogId() }}"
            )
            CodigoSalaInfo(
                origem = origem,
                entradaFechada = snapshot.entradaFechada()
            )
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
                task.result.children.mapNotNull { child ->
                    if (child.getValue(Boolean::class.java) == true) child.key else null
                }
            }
    }

    fun escutarProntos(
        modo: ModoCompetitivo,
        codigoSala: String,
        onProntosAlterados: (Set<String>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(modo, codigoSala).child(FirebasePaths.PRONTOS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onProntosAlterados(snapshot.children.mapNotNull { child ->
                    child.key?.takeIf { child.getValue(Boolean::class.java) == true }
                }.toSet())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun marcarPronto2x2(
        codigoSala: String,
        chaveJogador: String,
        pronto: Boolean = true
    ): Task<Void> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .child(FirebasePaths.PRONTOS)
            .child(chaveJogador)
            .setValue(pronto)
    }

    fun obterProntos2x2(codigoSala: String): Task<List<String>> {
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .child(FirebasePaths.PRONTOS)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao verificar jogadores prontos 2x2.")
                task.result.children.mapNotNull { child ->
                    if (child.getValue(Boolean::class.java) == true) child.key else null
                }
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
                    ?.ifBlank { jogador.nomeJogador } ?: jogador.nomeJogador,
                playerKey = jogadorSnapshot?.child(FirebasePaths.PLAYER_KEY)?.texto()
                    ?.ifBlank { jogador.playerKey } ?: jogador.playerKey,
                tipoJogador = jogadorSnapshot?.child(FirebasePaths.TIPO_JOGADOR)?.texto()
                    ?.ifBlank { jogador.tipoJogador } ?: jogador.tipoJogador,
                avatar = jogadorSnapshot?.child(FirebasePaths.AVATAR)?.texto()
                    ?.ifBlank { jogador.avatar } ?: jogador.avatar,
                estado = jogadorSnapshot?.child(FirebasePaths.ESTADO)?.texto()
                    ?.ifBlank { GameConstants.ESTADO_ON } ?: GameConstants.ESTADO_ON,
                pontuacao = jogadorSnapshot?.child(FirebasePaths.PONTUACAO)?.doubleValue() ?: 0.0
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

    fun iniciarJogo1x1(
        codigoSala: String,
        categoriaFallback: String,
        categoriaTodas: String
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val sala = salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
        sala.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    result.setException(IllegalStateException("Sala 1x1 nao existe."))
                    return@addOnSuccessListener
                }

                val categoria = snapshot.child(FirebasePaths.NOME_CATEGORIA).texto()
                    .ifBlank { categoriaFallback }
                    .ifBlank { categoriaTodas }
                Log.d(
                    HOST_REMOVAL_TAG,
                    "mode=1x1 room=$codigoSala startAtomic prepare category=$categoria " +
                        "statusBefore=${snapshot.child(FirebasePaths.ESTADO).texto()} " +
                        "players=${snapshot.child(FirebasePaths.JOGADORES).children.mapNotNull { it.key?.maskedLogId() }}"
                )
                carregarOuCriarPerguntas(
                    ModoCompetitivo.UM_CONTRA_UM,
                    codigoSala,
                    categoria,
                    categoriaTodas
                ).addOnSuccessListener {
                    val updates = mapOf<String, Any>(
                        FirebasePaths.ESTADO to GameConstants.ESTADO_EM_JOGO
                    )
                    Log.d(
                        HOST_REMOVAL_TAG,
                        "mode=1x1 room=$codigoSala startAtomic writePaths=${updates.keys} " +
                            "category=$categoria questions=${it.size} removesPlayer=false"
                    )
                    sala.updateChildren(updates)
                        .addOnSuccessListener {
                            Log.d(START_TAG, "mode=1x1 room=$codigoSala statusAfter=${GameConstants.ESTADO_EM_JOGO}")
                            result.setResult(null)
                        }
                        .addOnFailureListener { error -> result.setException(error) }
                }.addOnFailureListener { error ->
                    Log.w(HOST_REMOVAL_TAG, "mode=1x1 room=$codigoSala startAtomic failedQuestions=${error.message}")
                    result.setException(error)
                }
            }
            .addOnFailureListener { error -> result.setException(error) }
        return result.task
    }

    fun iniciarJogo2x2(
        codigoSala: String,
        equipaA: List<JogadorCompetitivo>,
        equipaB: List<JogadorCompetitivo>,
        categoriaFallback: String,
        categoriaTodas: String
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        val sala = salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
        sala.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    result.setException(IllegalStateException("Sala 2x2 nao existe."))
                    return@addOnSuccessListener
                }

                val categoria = snapshot.child(FirebasePaths.NOME_CATEGORIA).texto()
                    .ifBlank { categoriaFallback }
                    .ifBlank { categoriaTodas }
                Log.d(
                    HOST_REMOVAL_TAG,
                    "mode=2x2 room=$codigoSala startAtomic prepare category=$categoria " +
                        "statusBefore=${snapshot.child(FirebasePaths.ESTADO).texto()} " +
                        "teamA=${equipaA.map { it.chave.maskedLogId() }} teamB=${equipaB.map { it.chave.maskedLogId() }}"
                )
                carregarOuCriarPerguntas(
                    ModoCompetitivo.DOIS_CONTRA_DOIS,
                    codigoSala,
                    categoria,
                    categoriaTodas
                ).addOnSuccessListener { perguntas ->
                    val updates = linkedMapOf<String, Any?>(
                        FirebasePaths.ESTADO to GameConstants.ESTADO_EM_JOGO
                    )
                    val jogadores = (equipaA + equipaB).distinctBy { it.chave }
                    jogadores.forEach { jogador ->
                        updates["${FirebasePaths.EQUIPA_A}/${jogador.chave}"] = null
                        updates["${FirebasePaths.EQUIPA_B}/${jogador.chave}"] = null
                    }
                    equipaA.forEach { jogador ->
                        updates["${FirebasePaths.EQUIPA_A}/${jogador.chave}"] = jogador.toFirebaseMap()
                    }
                    equipaB.forEach { jogador ->
                        updates["${FirebasePaths.EQUIPA_B}/${jogador.chave}"] = jogador.toFirebaseMap()
                    }
                    Log.d(
                        HOST_REMOVAL_TAG,
                        "mode=2x2 room=$codigoSala startAtomic writePaths=${updates.keys} " +
                            "category=$categoria questions=${perguntas.size} removesPlayer=false"
                    )
                    sala.updateChildren(updates)
                        .addOnSuccessListener {
                            Log.d(START_TAG, "mode=2x2 room=$codigoSala statusAfter=${GameConstants.ESTADO_EM_JOGO}")
                            result.setResult(null)
                        }
                        .addOnFailureListener { error -> result.setException(error) }
                }.addOnFailureListener { error ->
                    Log.w(HOST_REMOVAL_TAG, "mode=2x2 room=$codigoSala startAtomic failedQuestions=${error.message}")
                    result.setException(error)
                }
            }
            .addOnFailureListener { error -> result.setException(error) }
        return result.task
    }

    fun apagarSala(modo: ModoCompetitivo, codigoSala: String): Task<Void> {
        Log.d(HOST_REMOVAL_TAG, "removeRoom method=apagarSala modo=${modo.node} codigo=${codigoSala.maskedLogId()}")
        return salaRef(modo, codigoSala).removeValue()
    }

    fun removerJogador1x1(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        chaveJogador: String
    ): Task<Void> {
        val chaves = (jogador.chavesCompatibilidade + chaveJogador).filter { it.isNotBlank() }.distinct()
        Log.d(
            HOST_REMOVAL_TAG,
            "A remover jogador 1x1: codigo=${codigoSala.maskedLogId()} " +
                "method=removerJogador1x1 chaves=${chaves.map { it.maskedLogId() }}"
        )
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
        Log.d(
            HOST_REMOVAL_TAG,
            "A remover jogador 2x2: codigo=${codigoSala.maskedLogId()} " +
                "method=removerJogador2x2 chaves=${chaves.map { it.maskedLogId() }}"
        )
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(
            chaves.flatMap { chave ->
                listOf(
                    "${FirebasePaths.JOGADORES}/$chave" to null,
                    "${FirebasePaths.PRONTOS}/$chave" to null,
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
        val jogadores = (equipaA + equipaB).distinctBy { it.chave }
        val updates = linkedMapOf<String, Any?>()

        jogadores.forEach { jogador ->
            updates["${FirebasePaths.EQUIPA_A}/${jogador.chave}"] = null
            updates["${FirebasePaths.EQUIPA_B}/${jogador.chave}"] = null
        }
        equipaA.forEach { jogador ->
            updates["${FirebasePaths.EQUIPA_A}/${jogador.chave}"] = jogador.toFirebaseMap()
        }
        equipaB.forEach { jogador ->
            updates["${FirebasePaths.EQUIPA_B}/${jogador.chave}"] = jogador.toFirebaseMap()
        }

        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(updates)
    }

    fun verificarCompetitividade(
        modo: ModoCompetitivo,
        codigoSala: String
    ): Task<Boolean> {
        return salaRef(modo, codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao verificar competitividade.")
            com.example.brainbrawl.utils.UteisEstatisticas.isSalaCompetitiva(task.result)
        }
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
                val perguntas = snapshot.toPerguntas()
                if (perguntas.isNotEmpty()) {
                    result.setResult(perguntas)
                } else {
                    result.setException(IllegalStateException("Sala sem perguntas validas."))
                }
                return@addOnSuccessListener
            }

            buscarPerguntasAleatorias(modo, codigoSala, categoria, categoriaTodas)
                .addOnSuccessListener perguntasAleatoriasListener@ { perguntasAleatorias ->
                    if (perguntasAleatorias.isEmpty()) {
                        result.setException(IllegalStateException("Categoria sem perguntas validas."))
                        return@perguntasAleatoriasListener
                    }
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

    fun atualizarPontuacaoAoVivo1x1(
        codigoSala: String,
        chaveJogador: String,
        totalPontos: Double
    ): Task<Void> {
        return salaRef(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .child(FirebasePaths.JOGADORES)
            .child(chaveJogador)
            .child(FirebasePaths.PONTUACAO)
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
        val pontuacoesPath = if (equipa == GameConstants.EQUIPA_A) {
            FirebasePaths.PONTUACOES_A
        } else {
            FirebasePaths.PONTUACOES_B
        }
        val totalCertasPath = if (equipa == GameConstants.EQUIPA_A) {
            FirebasePaths.TOTAL_PERGUNTAS_CERTAS_A
        } else {
            FirebasePaths.TOTAL_PERGUNTAS_CERTAS_B
        }

        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala).updateChildren(
            mapOf(
                "$pontuacoesPath/$chaveJogador" to totalPontos,
                "$totalCertasPath/$chaveJogador" to totalPerguntasCertas
            )
        )
    }

    fun atualizarPontuacaoAoVivo2x2(
        codigoSala: String,
        equipa: String,
        chaveJogador: String,
        totalPontos: Double
    ): Task<Void> {
        if (equipa != GameConstants.EQUIPA_A && equipa != GameConstants.EQUIPA_B) {
            val result = TaskCompletionSource<Void>()
            result.setException(IllegalStateException("Equipa 2x2 inválida para atualizar pontuação ao vivo."))
            return result.task
        }
        return salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .child(FirebasePaths.JOGADORES)
            .child(chaveJogador)
            .child(FirebasePaths.PONTUACAO)
            .setValue(totalPontos)
    }

    fun escutarEquipas2x2(
        codigoSala: String,
        onEquipasAlteradas: (List<JogadorCompetitivo>, List<JogadorCompetitivo>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jogadoresAoVivo = snapshot.child(FirebasePaths.JOGADORES).toJogadoresCompetitivos()
                val equipaA = snapshot.child(FirebasePaths.EQUIPA_A)
                    .toJogadoresCompetitivos()
                    .comPontuacoesAoVivo(jogadoresAoVivo)
                val equipaB = snapshot.child(FirebasePaths.EQUIPA_B)
                    .toJogadoresCompetitivos()
                    .comPontuacoesAoVivo(jogadoresAoVivo)
                onEquipasAlteradas(
                    equipaA,
                    equipaB
                )
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun escutarPodio2x2(
        codigoSala: String,
        jogadoresPorEquipa: Long = 2,
        onPodioCompleto: () -> Unit,
        onAguardar: () -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val equipaA = snapshot.child(FirebasePaths.EQUIPA_A).chavesJogadoresReais()
                val equipaB = snapshot.child(FirebasePaths.EQUIPA_B).chavesJogadoresReais()

                if (equipaA.size < jogadoresPorEquipa || equipaB.size < jogadoresPorEquipa) {
                    onAguardar()
                    return
                }

                val pontuacoesA = snapshot.child(FirebasePaths.PONTUACOES_A)
                val pontuacoesB = snapshot.child(FirebasePaths.PONTUACOES_B)
                val totaisA = snapshot.child(FirebasePaths.TOTAL_PERGUNTAS_CERTAS_A)
                val totaisB = snapshot.child(FirebasePaths.TOTAL_PERGUNTAS_CERTAS_B)

                val equipaAConcluida = equipaA.all { chave ->
                    pontuacoesA.hasChild(chave) && totaisA.hasChild(chave)
                }
                val equipaBConcluida = equipaB.all { chave ->
                    pontuacoesB.hasChild(chave) && totaisB.hasChild(chave)
                }

                if (equipaAConcluida && equipaBConcluida) {
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

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    private fun buscarPerguntasAleatorias(
        modo: ModoCompetitivo,
        codigoSala: String,
        categoria: String,
        categoriaTodas: String
    ): Task<List<Pergunta>> {
        val result = TaskCompletionSource<List<Pergunta>>()
        salaRef(modo, codigoSala).get()
            .addOnSuccessListener { salaSnapshot ->
                val categoriaPublicaId = salaSnapshot.child(FirebasePaths.CATEGORIA_PUBLICA_ID).texto()
                if (categoriaPublicaId.isNotBlank()) {
                    database.child(FirebasePaths.CATEGORIAS_PUBLICAS)
                        .child(categoriaPublicaId)
                        .child(FirebasePaths.PERGUNTAS)
                        .get()
                        .addOnSuccessListener {
                            val perguntas = it.toPerguntas().shuffled().take(8)
                            if (perguntas.isNotEmpty()) {
                                result.setResult(perguntas)
                            } else {
                                result.setException(IllegalStateException("Categoria publica sem perguntas validas."))
                            }
                        }
                        .addOnFailureListener { result.setException(it) }
                    return@addOnSuccessListener
                }

                if (salaSnapshot.child("categoriaPersonalizada").getValue(Boolean::class.java) == true) {
                    val donosPossiveis = listOf(
                        salaSnapshot.child(FirebasePaths.DONO_UID).texto(),
                        salaSnapshot.child("donoCategoria").texto()
                    ).filter { it.isNotBlank() }.distinct()
                    if (donosPossiveis.isNotEmpty()) {
                        carregarPerguntasPersonalizadas(donosPossiveis, categoria, result)
                        return@addOnSuccessListener
                    }
                }

                buscarPerguntasOficiais(categoria, categoriaTodas)
                    .addOnSuccessListener { result.setResult(it) }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    private fun carregarPerguntasPersonalizadas(
        donosPossiveis: List<String>,
        categoria: String,
        result: TaskCompletionSource<List<Pergunta>>,
        index: Int = 0
    ) {
        if (index >= donosPossiveis.size) {
            result.setException(IllegalStateException("Categoria personalizada sem perguntas validas."))
            return
        }

        database.child(FirebasePaths.JOGADORES)
            .child(donosPossiveis[index])
            .child(FirebasePaths.CATEGORIAS_PERSONALIZADAS)
            .child(categoria)
            .child(FirebasePaths.PERGUNTAS)
            .get()
            .addOnSuccessListener { snapshot ->
                val perguntas = snapshot.toPerguntas().shuffled().take(8)
                if (perguntas.isNotEmpty()) {
                    result.setResult(perguntas)
                } else {
                    carregarPerguntasPersonalizadas(donosPossiveis, categoria, result, index + 1)
                }
            }
            .addOnFailureListener { error ->
                if (index + 1 < donosPossiveis.size) {
                    carregarPerguntasPersonalizadas(donosPossiveis, categoria, result, index + 1)
                } else {
                    result.setException(error)
                }
            }
    }

    private fun buscarPerguntasOficiais(categoria: String, categoriaTodas: String): Task<List<Pergunta>> {
        val categoriasRef = database.child(FirebasePaths.CATEGORIAS)
        return if (categoria.equals(categoriaTodas, ignoreCase = true) || categoria.isEmpty()) {
            categoriasRef.get().continueWith { taskSnapshot ->
                if (!taskSnapshot.isSuccessful) throw taskSnapshot.exception ?: IllegalStateException("Erro ao buscar perguntas.")
                val perguntas = taskSnapshot.result.children
                    .flatMap { categoriaSnapshot -> categoriaSnapshot.child(FirebasePaths.PERGUNTAS).toPerguntas() }
                    .shuffled()
                    .take(8)
                if (perguntas.isEmpty()) {
                    throw IllegalStateException("Sem perguntas validas nas categorias oficiais.")
                }
                perguntas
            }
        } else {
            categoriasRef.child(categoria).child(FirebasePaths.PERGUNTAS).get().continueWith { taskSnapshot ->
                if (!taskSnapshot.isSuccessful) throw taskSnapshot.exception ?: IllegalStateException("Erro ao buscar perguntas.")
                val perguntas = taskSnapshot.result.toPerguntas().shuffled().take(8)
                if (perguntas.isEmpty()) {
                    throw IllegalStateException("Sem perguntas validas para a categoria $categoria.")
                }
                perguntas
            }
        }
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
                    val perguntas = snapshot.toPerguntas()
                    if (perguntas.isNotEmpty()) {
                        result.setResult(perguntas)
                    } else {
                        result.setException(IllegalStateException("Sala sem perguntas validas."))
                    }
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

    private fun ModoCompetitivo.limiteJogadores(): Int {
        return when (this) {
            ModoCompetitivo.UM_CONTRA_UM -> 2
            ModoCompetitivo.DOIS_CONTRA_DOIS -> 4
        }
    }

    private fun DataSnapshot.lotacaoMaxima(modo: ModoCompetitivo): Int {
        return child(FirebasePaths.LOTACAO_MAXIMA).intValue().takeIf { it > 0 }
            ?: modo.limiteJogadores()
    }

    private fun DataSnapshot.entradaFechada(): Boolean {
        return child(FirebasePaths.ENTRADA_FECHADA).getValue(Boolean::class.java) == true ||
            child(FirebasePaths.ORIGEM).texto() == GameConstants.ORIGEM_MATCHMAKING ||
            child(FirebasePaths.ORIGEM).texto() == GameConstants.ORIGEM_CONVITE
    }

    private fun DataSnapshot.encontrarChaveJogador(jogador: JogadorSalaIdentidade): String? {
        return encontrarJogador(jogador)?.key
    }

    private fun DataSnapshot.encontrarChaveJogadorFechado(jogador: JogadorSalaIdentidade): String? {
        val chavePrincipal = jogador.playerKey.ifBlank { jogador.uid.ifBlank { jogador.chaveSala } }
        val chaves = jogador.chavesCompatibilidade
        return children.firstOrNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key.orEmpty()
            chave == chavePrincipal ||
                chave in chaves ||
                jogadorSnapshot.child(FirebasePaths.PLAYER_KEY).texto() == chavePrincipal ||
                jogadorSnapshot.child(FirebasePaths.PLAYER_KEY).texto() in chaves ||
                jogadorSnapshot.child(FirebasePaths.NOME_UTILIZADOR).texto() in chaves ||
                jogadorSnapshot.child(FirebasePaths.NOME_JOGADOR).texto() in chaves ||
                jogadorSnapshot.nomeDisplay() in chaves ||
                (
                    jogador.uid.isNotBlank() &&
                        jogadorSnapshot.child(FirebasePaths.UID).texto() == jogador.uid
                    )
        }?.key
    }

    private fun DataSnapshot.encontrarChavePermitida(jogador: JogadorSalaIdentidade): String? {
        val chavePrincipal = jogador.playerKey.ifBlank { jogador.uid.ifBlank { jogador.chaveSala } }
        val chaves = (jogador.chavesCompatibilidade + chavePrincipal).filter { it.isNotBlank() }.distinct()
        return children.firstOrNull { reserva ->
            reserva.getValue(Boolean::class.java) == true && reserva.key.orEmpty() in chaves
        }?.key
    }

    private fun DataSnapshot.encontrarJogador(jogador: JogadorSalaIdentidade): DataSnapshot? {
        return children.firstOrNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key.orEmpty()
            chave in jogador.chavesCompatibilidade ||
                jogadorSnapshot.child(FirebasePaths.PLAYER_KEY).texto() in jogador.chavesCompatibilidade ||
                jogadorSnapshot.child(FirebasePaths.UID).texto() in jogador.chavesCompatibilidade ||
                jogadorSnapshot.child(FirebasePaths.NOME_UTILIZADOR).texto() in jogador.chavesCompatibilidade ||
                jogadorSnapshot.child(FirebasePaths.NOME_JOGADOR).texto() in jogador.chavesCompatibilidade ||
                jogadorSnapshot.nomeDisplay() in jogador.chavesCompatibilidade
        }
    }

    private fun DataSnapshot.toJogadorCompetitivo(
        chave: String,
        fallback: JogadorSalaIdentidade
    ): JogadorCompetitivo {
        return JogadorCompetitivo(
            chave = chave,
            nomeDisplay = nomeDisplay().ifBlank { fallback.nomeDisplay },
            uid = child(FirebasePaths.UID).texto().ifBlank { fallback.uid },
            nomeUtilizador = child(FirebasePaths.NOME_UTILIZADOR).texto().ifBlank { fallback.nomeUtilizador },
            nomeJogador = child(FirebasePaths.NOME_JOGADOR).texto().ifBlank { fallback.nomeJogador },
            playerKey = child(FirebasePaths.PLAYER_KEY).texto().ifBlank { fallback.playerKey.ifBlank { chave } },
            tipoJogador = child(FirebasePaths.TIPO_JOGADOR).texto().ifBlank { fallback.tipoJogador },
                avatar = child(FirebasePaths.AVATAR).texto().ifBlank { fallback.avatar },
                estado = child(FirebasePaths.ESTADO).texto().ifBlank { GameConstants.ESTADO_ON },
                pontuacao = child(FirebasePaths.PONTUACAO).doubleValue()
            )
        }

    private fun DataSnapshot.toJogadoresCompetitivos(): List<JogadorCompetitivo> {
        return children.mapNotNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key ?: return@mapNotNull null
            JogadorCompetitivo(
                chave = chave,
                nomeDisplay = jogadorSnapshot.nomeDisplay().ifBlank { chave },
                uid = jogadorSnapshot.child(FirebasePaths.UID).texto(),
                nomeUtilizador = jogadorSnapshot.child(FirebasePaths.NOME_UTILIZADOR).texto(),
                nomeJogador = jogadorSnapshot.child(FirebasePaths.NOME_JOGADOR).texto(),
                playerKey = jogadorSnapshot.child(FirebasePaths.PLAYER_KEY).texto().ifBlank { chave },
                tipoJogador = jogadorSnapshot.child(FirebasePaths.TIPO_JOGADOR).texto(),
                avatar = jogadorSnapshot.child(FirebasePaths.AVATAR).texto(),
                estado = jogadorSnapshot.child(FirebasePaths.ESTADO).texto().ifBlank { GameConstants.ESTADO_ON },
                pontuacao = jogadorSnapshot.child(FirebasePaths.PONTUACAO).doubleValue()
            )
        }
    }

    private fun List<JogadorCompetitivo>.comPontuacoesAoVivo(
        jogadoresAoVivo: List<JogadorCompetitivo>
    ): List<JogadorCompetitivo> {
        return map { jogadorEquipa ->
            val jogadorSala = jogadoresAoVivo.firstOrNull { jogadorSala ->
                val idsSala = jogadorSala.identificadores()
                jogadorEquipa.identificadores().any { it in idsSala }
            }
            jogadorEquipa.copy(
                estado = jogadorSala?.estado ?: jogadorEquipa.estado,
                pontuacao = jogadorSala?.pontuacao ?: jogadorEquipa.pontuacao
            )
        }
    }

    private fun JogadorCompetitivo.identificadores(): List<String> {
        return listOf(chave, uid, playerKey, nomeUtilizador, nomeJogador, nomeDisplay)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun DataSnapshot?.identificadoresJogador(): List<String> {
        if (this == null || !exists()) return emptyList()
        return listOf(
            key.orEmpty(),
            child(FirebasePaths.PLAYER_KEY).texto(),
            child(FirebasePaths.UID).texto(),
            child(FirebasePaths.NOME_UTILIZADOR).texto(),
            child(FirebasePaths.NOME_JOGADOR).texto(),
            nomeDisplay()
        ).filter { it.isNotBlank() }.distinct()
    }

    private fun DataSnapshot.chavesJogadoresReais(): Set<String> {
        return children.mapNotNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key ?: return@mapNotNull null
            val isHostOnly = jogadorSnapshot.child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true
            if (chave == GameConstants.JOGADOR_ADMIN || isHostOnly) {
                null
            } else {
                val estado = jogadorSnapshot.child(FirebasePaths.ESTADO).getValue(String::class.java).orEmpty()
                    .ifBlank { GameConstants.ESTADO_ON }
                if (estado == GameConstants.ESTADO_OFF) null else chave
            }
        }.toSet()
    }

    private fun JogadorCompetitivo.toFirebaseMap(): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.NOME to nomeDisplay,
            FirebasePaths.NOME_DISPLAY to nomeDisplay,
            FirebasePaths.PLAYER_KEY to playerKey.ifBlank { chave }
        )
        if (uid.isNotBlank()) dados[FirebasePaths.UID] = uid
        if (nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = nomeUtilizador
        if (nomeJogador.isNotBlank()) dados[FirebasePaths.NOME_JOGADOR] = nomeJogador
        if (tipoJogador.isNotBlank()) dados[FirebasePaths.TIPO_JOGADOR] = tipoJogador
        if (avatar.isNotBlank()) dados[FirebasePaths.AVATAR] = avatar
        dados[FirebasePaths.IS_GUEST] = tipoJogador == GameConstants.TIPO_JOGADOR_GUEST
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

    private fun DataSnapshot.intValue(): Int {
        return getValue(Int::class.java)
            ?: getValue(Long::class.java)?.toInt()
            ?: getValue(Double::class.java)?.toInt()
            ?: 0
    }

    private fun DataSnapshot.doubleValue(): Double {
        return getValue(Double::class.java)
            ?: getValue(Long::class.java)?.toDouble()
            ?: getValue(Int::class.java)?.toDouble()
            ?: 0.0
    }

    private fun DataSnapshot.toPerguntas(): List<Pergunta> {
        return children.mapNotNull { perguntaSnapshot ->
            perguntaSnapshot.toPerguntaValida()
        }
    }

    private fun DataSnapshot.toPerguntaValida(): Pergunta? {
        val pergunta = child(FirebasePaths.PERGUNTA).getValue(String::class.java)?.trim()
        val respostaCorreta = child(FirebasePaths.RESPOSTA_CORRETA).getValue(String::class.java)?.trim()
        val opcoesOrigem = child(FirebasePaths.OPCOES).children.mapNotNull { it.getValue(String::class.java) }
        val opcoes = opcoesOrigem
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (pergunta.isNullOrBlank() || respostaCorreta.isNullOrBlank()) return null
        if (respostaCorreta !in opcoes) return null

        val opcoesErradas = opcoes.filterNot { it == respostaCorreta }
        if (opcoesErradas.size < 3) return null

        return Pergunta(
            pergunta = pergunta,
            respostaCorreta = respostaCorreta,
            opcoes = (listOf(respostaCorreta) + opcoesErradas.take(3)),
            imagem = child(FirebasePaths.IMAGEM).texto(),
            dificuldade = child(FirebasePaths.DIFICULDADE).texto().takeIf { it in DIFICULDADES_VALIDAS }
        )
    }

    private companion object {
        const val TAG = "MATCHMAKING_DEBUG"
        const val START_TAG = "INVITE_START_ROOT_CAUSE"
        const val HOST_REMOVAL_TAG = "HOST_REMOVAL_DEBUG"
        const val FLOW_TAG = "FLOW_SEPARATION_DEBUG"
        val DIFICULDADES_VALIDAS = setOf("facil", "media", "dificil")
    }
}
