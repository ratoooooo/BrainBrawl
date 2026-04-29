package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.EstatisticasService.ResultadoJogador
import com.example.brainbrawl.utils.UteisFirebase.doubleValue
import com.example.brainbrawl.utils.UteisFirebase.intValue
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

class PontuacaoRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference,
    private val estatisticasService: EstatisticasService = EstatisticasService()
) {
    enum class TipoSala(val node: String) {
        GRUPO(FirebasePaths.SALAS),
        UM_CONTRA_UM(FirebasePaths.SALA_1X1),
        DOIS_CONTRA_DOIS(FirebasePaths.SALA_2X2)
    }

    data class Resultado2x2(
        val equipaA: List<ResultadoJogador>,
        val equipaB: List<ResultadoJogador>
    )

    data class ResultadosGrupo(
        val jogadores: List<ResultadoJogador>,
        val totalJogadores: Int,
        val resultadosGuardados: Int
    ) {
        val completos: Boolean
            get() = totalJogadores > 0 && resultadosGuardados >= totalJogadores
    }

    data class ListenerHandle internal constructor(
        private val removerListener: () -> Unit
    ) {
        internal fun remover() {
            removerListener()
        }
    }

    fun obterPontuacoesGrupo(codigoSala: String): Task<List<ResultadoJogador>> {
        return salaRef(TipoSala.GRUPO, codigoSala).child(FirebasePaths.JOGADORES).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar resultados.")
            task.result.children.mapNotNull { jogadorSnapshot ->
                jogadorSnapshot.toResultadoGrupo()
            }
        }
    }

    fun escutarResultadosGrupo(
        codigoSala: String,
        onResultados: (ResultadosGrupo) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(TipoSala.GRUPO, codigoSala).child(FirebasePaths.JOGADORES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onResultados(snapshot.toResultadosGrupo())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun obterPontuacaoGlobalJogador(nomeJogador: String): Task<Double> {
        return database.child(FirebasePaths.JOGADORES).child(nomeJogador).child(FirebasePaths.PONTUACAO).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar pontuação.")
            task.result.doubleValue()
        }
    }

    fun escutarPontuacoes1x1(
        codigoSala: String,
        onPontuacoes: (List<ResultadoJogador>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(TipoSala.UM_CONTRA_UM, codigoSala).child(FirebasePaths.PONTUACOES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val resultados = snapshot.children.mapNotNull { pontuacaoSnapshot ->
                    val nome = pontuacaoSnapshot.key ?: return@mapNotNull null
                    ResultadoJogador(
                        nome = nome,
                        pontos = pontuacaoSnapshot.doubleValue(),
                        respostasCertas = 0
                    )
                }
                onPontuacoes(estatisticasService.ordenarPodio(resultados))
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun escutarPontuacoes2x2(
        codigoSala: String,
        onPontuacoes: (Resultado2x2) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(TipoSala.DOIS_CONTRA_DOIS, codigoSala)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onPontuacoes(snapshot.toResultado2x2())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle { reference.removeEventListener(listener) }
    }

    fun atualizarEstatisticasSalaUmaVez(
        tipoSala: TipoSala,
        codigoSala: String,
        resultados: List<ResultadoJogador>,
        modo: EstatisticasService.Modo,
        totalPerguntas: Int,
        jogadoresParaAtualizar: Set<String>? = null
    ): Task<Boolean> {
        val result = TaskCompletionSource<Boolean>()

        if (!estatisticasService.deveAtualizarEstatisticas(false, resultados)) {
            result.setResult(false)
            return result.task
        }

        val vencedores = estatisticasService.vencedores(resultados, modo)
        atualizarProximoJogador(
            tipoSala = tipoSala,
            codigoSala = codigoSala,
            resultados = resultados,
            vencedores = vencedores,
            modo = modo,
            totalPerguntas = totalPerguntas,
            jogadoresParaAtualizar = jogadoresParaAtualizar,
            index = 0,
            result = result
        )

        return result.task
    }

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    private fun atualizarProximoJogador(
        tipoSala: TipoSala,
        codigoSala: String,
        resultados: List<ResultadoJogador>,
        vencedores: Set<String>,
        modo: EstatisticasService.Modo,
        totalPerguntas: Int,
        jogadoresParaAtualizar: Set<String>?,
        index: Int,
        result: TaskCompletionSource<Boolean>
    ) {
        if (index >= resultados.size) {
            result.setResult(true)
            return
        }

        val resultado = resultados[index]
        if (deveIgnorarJogador(resultado.nome) ||
            (jogadoresParaAtualizar != null && !jogadoresParaAtualizar.contains(resultado.nome))) {
            atualizarProximoJogador(
                tipoSala,
                codigoSala,
                resultados,
                vencedores,
                modo,
                totalPerguntas,
                jogadoresParaAtualizar,
                index + 1,
                result
            )
            return
        }

        marcarEstatisticasJogadorComoAtualizadas(tipoSala, codigoSala, resultado.nome)
            .addOnSuccessListener { podeAtualizar ->
                if (!podeAtualizar) {
                    atualizarProximoJogador(
                        tipoSala,
                        codigoSala,
                        resultados,
                        vencedores,
                        modo,
                        totalPerguntas,
                        jogadoresParaAtualizar,
                        index + 1,
                        result
                    )
                    return@addOnSuccessListener
                }

                val jogadorRef = database.child(FirebasePaths.JOGADORES).child(resultado.nome)
                jogadorRef.get()
                    .addOnSuccessListener jogadorListener@{ snapshot ->
                        if (!snapshot.exists() || !snapshot.hasChild(FirebasePaths.PASSWORD)) {
                            atualizarProximoJogador(
                                tipoSala,
                                codigoSala,
                                resultados,
                                vencedores,
                                modo,
                                totalPerguntas,
                                jogadoresParaAtualizar,
                                index + 1,
                                result
                            )
                            return@jogadorListener
                        }

                        val updates = estatisticasService.calcularAtualizacao(
                            estatisticasAtuais = snapshot.toEstatisticasAtuais(),
                            resultado = resultado,
                            modo = modo,
                            venceu = vencedores.contains(resultado.nome),
                            totalPerguntas = totalPerguntas
                        )

                        jogadorRef.updateChildren(updates)
                            .addOnSuccessListener {
                                atualizarProximoJogador(
                                    tipoSala,
                                    codigoSala,
                                    resultados,
                                    vencedores,
                                    modo,
                                    totalPerguntas,
                                    jogadoresParaAtualizar,
                                    index + 1,
                                    result
                                )
                            }
                            .addOnFailureListener { error ->
                                result.setException(error)
                            }
                    }
                    .addOnFailureListener { error ->
                        result.setException(error)
                    }
            }
            .addOnFailureListener { error ->
                result.setException(error)
            }
    }

    private fun marcarEstatisticasJogadorComoAtualizadas(
        tipoSala: TipoSala,
        codigoSala: String,
        nomeJogador: String
    ): Task<Boolean> {
        val result = TaskCompletionSource<Boolean>()
        val reference = salaRef(tipoSala, codigoSala)
            .child(FirebasePaths.ESTATISTICAS_ATUALIZADAS)
            .child(nomeJogador)

        reference.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.getValue(Boolean::class.java) == true) {
                    return Transaction.abort()
                }
                currentData.value = true
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    result.setException(error.toException())
                    return
                }
                result.setResult(committed)
            }
        })

        return result.task
    }

    private fun DataSnapshot.toResultadoGrupo(): ResultadoJogador? {
        val nome = key ?: return null
        val isHostOnly = child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true
        if (deveIgnorarJogador(nome) || isHostOnly) return null

        return ResultadoJogador(
            nome = nome,
            pontos = child(FirebasePaths.PONTUACAO).doubleValue(),
            respostasCertas = respostasCertasGrupo()
        )
    }

    private fun DataSnapshot.toResultadosGrupo(): ResultadosGrupo {
        val jogadoresReais = children.filter { jogadorSnapshot ->
            val nome = jogadorSnapshot.key.orEmpty()
            val isHostOnly = jogadorSnapshot.child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true
            !deveIgnorarJogador(nome) && !isHostOnly
        }
        val resultados = jogadoresReais.mapNotNull { jogadorSnapshot ->
            if (jogadorSnapshot.temResultadoGrupoGuardado()) {
                jogadorSnapshot.toResultadoGrupo()
            } else {
                null
            }
        }
        return ResultadosGrupo(
            jogadores = resultados,
            totalJogadores = jogadoresReais.size,
            resultadosGuardados = resultados.size
        )
    }

    private fun DataSnapshot.temResultadoGrupoGuardado(): Boolean {
        return hasChild(FirebasePaths.TOTAL_RESPOSTAS_CERTAS) || hasChild(FirebasePaths.TOTAL_PERGUNTAS_CERTAS)
    }

    private fun DataSnapshot.respostasCertasGrupo(): Int {
        return if (hasChild(FirebasePaths.TOTAL_RESPOSTAS_CERTAS)) {
            child(FirebasePaths.TOTAL_RESPOSTAS_CERTAS).intValue()
        } else {
            child(FirebasePaths.TOTAL_PERGUNTAS_CERTAS).intValue()
        }
    }

    private fun DataSnapshot.toResultado2x2(): Resultado2x2 {
        val pontuacoesA = child(FirebasePaths.PONTUACOES_A)
        val pontuacoesB = child(FirebasePaths.PONTUACOES_B)
        val respostasA = child(FirebasePaths.TOTAL_PERGUNTAS_CERTAS_A)
        val respostasB = child(FirebasePaths.TOTAL_PERGUNTAS_CERTAS_B)

        val equipaA = child(FirebasePaths.EQUIPA_A).children.mapNotNull { jogadorSnapshot ->
            val nome = jogadorSnapshot.key ?: return@mapNotNull null
            if (deveIgnorarJogador(nome)) return@mapNotNull null
            ResultadoJogador(
                nome = nome,
                pontos = pontuacoesA.child(nome).doubleValue(),
                respostasCertas = respostasA.child(nome).intValue(),
                equipa = GameConstants.EQUIPA_A
            )
        }

        val equipaB = child(FirebasePaths.EQUIPA_B).children.mapNotNull { jogadorSnapshot ->
            val nome = jogadorSnapshot.key ?: return@mapNotNull null
            if (deveIgnorarJogador(nome)) return@mapNotNull null
            ResultadoJogador(
                nome = nome,
                pontos = pontuacoesB.child(nome).doubleValue(),
                respostasCertas = respostasB.child(nome).intValue(),
                equipa = GameConstants.EQUIPA_B
            )
        }

        return Resultado2x2(equipaA = equipaA, equipaB = equipaB)
    }

    private fun DataSnapshot.toEstatisticasAtuais(): EstatisticasService.EstatisticasAtuais {
        return EstatisticasService.EstatisticasAtuais(
            pontuacao = child(FirebasePaths.PONTUACAO).doubleValue(),
            taxaAcertos = child(FirebasePaths.TAXA_ACERTOS).doubleValue(),
            totalJogos = child(FirebasePaths.TOTAL_JOGOS).intValue(),
            totalVitorias = child(FirebasePaths.TOTAL_VITORIAS).intValue(),
            totalRespostasCertas = child(FirebasePaths.TOTAL_RESPOSTAS_CERTAS).intValue(),
            totalVitoriasModo1x1 = child(FirebasePaths.TOTAL_VITORIAS_MODO_1X1).intValue(),
            totalVitoriasModo2x2 = child(FirebasePaths.TOTAL_VITORIAS_MODO_2X2).intValue(),
            totalVitoriasModoSolo = child(FirebasePaths.TOTAL_VITORIAS_MODO_SOLO).intValue()
        )
    }

    private fun deveIgnorarJogador(nome: String): Boolean {
        return nome.isBlank() || nome == GameConstants.JOGADOR_ADMIN
    }

    private fun salaRef(tipoSala: TipoSala, codigoSala: String): DatabaseReference {
        return database.child(tipoSala.node).child(codigoSala)
    }
}
