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
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class JogoRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class SalaInfo(
        val admin: Boolean,
        val modoJogo: String,
        val categoriaCompetitiva: Boolean
    )

    data class JogadorEliminatorias(
        val chave: String,
        val nome: String,
        val estado: String,
        val isHostOnly: Boolean,
        val pontos: Double = 0.0,
        val respostasCertas: Int = 0,
        val perguntasRespondidas: Int = 0
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
        jogador: JogadorSalaIdentidade
    ): Task<SalaInfo> {
        return salaRef(codigoSala).get().continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("Erro ao carregar sala.")
            }

            val sala = task.result
            val admin = sala.child(FirebasePaths.ADMIN).texto()
            val adminId = sala.child(FirebasePaths.ADMIN_ID).texto()
            val adminUid = sala.child(FirebasePaths.ADMIN_UID).texto()
            val jogadorNaSala = sala.child(FirebasePaths.JOGADORES).encontrarJogador(jogador)
            val isHostOnly = jogadorNaSala?.child(FirebasePaths.IS_HOST_ONLY)?.getValue(Boolean::class.java) == true

            val isAdmin = admin.isNotBlank() && admin in jogador.chavesCompatibilidade ||
                    adminId.isNotBlank() && adminId in jogador.chavesCompatibilidade ||
                    adminUid.isNotBlank() && adminUid in jogador.chavesCompatibilidade ||
                    isHostOnly

            SalaInfo(
                admin = isAdmin,
                modoJogo = sala.child(FirebasePaths.MODO_JOGO).getValue(String::class.java)
                    ?: GameConstants.MODO_CLASSICO,
                categoriaCompetitiva = sala.eSalaCompetitiva()
            )
        }
    }

    fun carregarPerguntas(codigoSala: String): Task<List<Pergunta>> {
        return salaRef(codigoSala).child(FirebasePaths.PERGUNTAS).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar perguntas.")
            task.result.children
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

    fun registarResposta(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        acertou: Boolean,
        totalPontos: Double,
        totalRespostasCertas: Int,
        perguntasRespondidas: Int
    ): Task<Void> {
        return resolverChaveJogador(codigoSala, jogador).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar jogador.")
            val chave = task.result
            salaRef(codigoSala).updateChildren(
                mapOf(
                    "${FirebasePaths.PERGUNTA_ATUAL}/${FirebasePaths.RESPOSTAS}/$chave" to acertou,
                    "${FirebasePaths.JOGADORES}/$chave/${FirebasePaths.PONTUACAO}" to totalPontos,
                    "${FirebasePaths.JOGADORES}/$chave/${FirebasePaths.TOTAL_RESPOSTAS_CERTAS}" to totalRespostasCertas,
                    "${FirebasePaths.JOGADORES}/$chave/${FirebasePaths.TOTAL_PERGUNTAS}" to perguntasRespondidas,
                    "${FirebasePaths.JOGADORES}/$chave/${FirebasePaths.ESTADO}" to GameConstants.ESTADO_EM_JOGO
                )
            )
        }
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
                val chave = jogadorSnapshot.key ?: return@mapNotNull null
                JogadorEliminatorias(
                    chave = chave,
                    nome = jogadorSnapshot.nomeDisplay().ifBlank { chave },
                    estado = jogadorSnapshot.child(FirebasePaths.ESTADO).getValue(String::class.java).orEmpty(),
                    isHostOnly = jogadorSnapshot.child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true,
                    pontos = jogadorSnapshot.child(FirebasePaths.PONTUACAO).doubleValue(),
                    respostasCertas = jogadorSnapshot.respostasCertas(),
                    perguntasRespondidas = jogadorSnapshot.child(FirebasePaths.TOTAL_PERGUNTAS).intValue()
                )
            }
        }
    }

    fun escutarJogadoresEliminatorias(
        codigoSala: String,
        onJogadoresAlterados: (List<JogadorEliminatorias>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(codigoSala).child(FirebasePaths.JOGADORES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jogadores = snapshot.children.mapNotNull { jogadorSnapshot ->
                    val chave = jogadorSnapshot.key ?: return@mapNotNull null
                    JogadorEliminatorias(
                        chave = chave,
                        nome = jogadorSnapshot.nomeDisplay().ifBlank { chave },
                        estado = jogadorSnapshot.child(FirebasePaths.ESTADO).getValue(String::class.java).orEmpty(),
                        isHostOnly = jogadorSnapshot.child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true,
                        pontos = jogadorSnapshot.child(FirebasePaths.PONTUACAO).doubleValue(),
                        respostasCertas = jogadorSnapshot.respostasCertas(),
                        perguntasRespondidas = jogadorSnapshot.child(FirebasePaths.TOTAL_PERGUNTAS).intValue()
                    )
                }
                onJogadoresAlterados(jogadores)
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun removerJogador(codigoSala: String, jogador: JogadorSalaIdentidade): Task<Void> {
        return resolverChaveJogador(codigoSala, jogador).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar jogador.")
            salaRef(codigoSala).child(FirebasePaths.JOGADORES).child(task.result).removeValue()
        }
    }

    fun marcarJogadorEliminado(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        totalPontos: Double,
        totalRespostasCertas: Int
    ): Task<Void> {
        return atualizarDadosJogador(
            codigoSala,
            jogador,
            mapOf(
                FirebasePaths.NOME_DISPLAY to jogador.nomeDisplay,
                FirebasePaths.ESTADO to GameConstants.ESTADO_ELIMINADO,
                FirebasePaths.PONTUACAO to totalPontos,
                FirebasePaths.TOTAL_RESPOSTAS_CERTAS to totalRespostasCertas
            )
        )
    }

    fun guardarResultadoJogador(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        totalPontos: Double,
        totalRespostasCertas: Int
    ): Task<Void> {
        return atualizarDadosJogador(
            codigoSala,
            jogador,
            mapOf(
                FirebasePaths.NOME_DISPLAY to jogador.nomeDisplay,
                FirebasePaths.PONTUACAO to totalPontos,
                FirebasePaths.TOTAL_RESPOSTAS_CERTAS to totalRespostasCertas,
                FirebasePaths.ESTADO to GameConstants.ESTADO_TERMINADO
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

    private fun resolverChaveJogador(codigoSala: String, jogador: JogadorSalaIdentidade): Task<String> {
        return salaRef(codigoSala).child(FirebasePaths.JOGADORES).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao identificar jogador.")
            task.result.encontrarJogador(jogador)?.key ?: jogador.chaveSala
        }
    }

    private fun atualizarDadosJogador(
        codigoSala: String,
        jogador: JogadorSalaIdentidade,
        dados: Map<String, Any>
    ): Task<Void> {
        val result = TaskCompletionSource<Void>()
        resolverChaveJogador(codigoSala, jogador)
            .addOnSuccessListener { chave ->
                salaRef(codigoSala).child(FirebasePaths.JOGADORES).child(chave).updateChildren(dados)
                    .addOnSuccessListener { result.setResult(null) }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
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

    private fun DataSnapshot.nomeDisplay(): String {
        return child(FirebasePaths.NOME_DISPLAY).texto()
            .ifBlank { child(FirebasePaths.NOME_UTILIZADOR).texto() }
            .ifBlank { child(FirebasePaths.NOME_JOGADOR).texto() }
            .ifBlank { child(FirebasePaths.NOME).texto() }
    }

    private fun DataSnapshot.texto(): String {
        return getValue(String::class.java).orEmpty()
    }

    private fun DataSnapshot.doubleValue(): Double {
        return getValue(Double::class.java)
            ?: getValue(Long::class.java)?.toDouble()
            ?: getValue(Int::class.java)?.toDouble()
            ?: 0.0
    }

    private fun DataSnapshot.respostasCertas(): Int {
        return if (hasChild(FirebasePaths.TOTAL_RESPOSTAS_CERTAS)) {
            child(FirebasePaths.TOTAL_RESPOSTAS_CERTAS).intValue()
        } else {
            child(FirebasePaths.TOTAL_PERGUNTAS_CERTAS).intValue()
        }
    }

    private fun DataSnapshot.eSalaCompetitiva(): Boolean {
        val ePublica = child("categoriaPublica").getValue(Boolean::class.java) == true ||
            child(FirebasePaths.CATEGORIA_PUBLICA_ID).texto().isNotBlank()
        val ePersonalizada = child("categoriaPersonalizada").getValue(Boolean::class.java) == true ||
            child(FirebasePaths.DONO_UID).texto().isNotBlank()
        return !ePublica && !ePersonalizada
    }

    private fun DataSnapshot.intValue(): Int {
        return getValue(Int::class.java)
            ?: getValue(Long::class.java)?.toInt()
            ?: getValue(Double::class.java)?.toInt()
            ?: 0
    }
}
