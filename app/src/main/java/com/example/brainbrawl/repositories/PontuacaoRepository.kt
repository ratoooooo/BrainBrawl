package com.example.brainbrawl.repositories

import com.example.brainbrawl.UteisFirebase.doubleValue
import com.example.brainbrawl.UteisFirebase.intValue
import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.EstatisticasService.ResultadoJogador
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
        GRUPO("salas"),
        UM_CONTRA_UM("sala_1x1"),
        DOIS_CONTRA_DOIS("sala_2x2")
    }

    data class Resultado2x2(
        val equipaA: List<ResultadoJogador>,
        val equipaB: List<ResultadoJogador>
    )

    data class ListenerHandle internal constructor(
        private val removerListener: () -> Unit
    ) {
        internal fun remover() {
            removerListener()
        }
    }

    fun obterPontuacoesGrupo(codigoSala: String): Task<List<ResultadoJogador>> {
        return salaRef(TipoSala.GRUPO, codigoSala).child("jogadores").get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar resultados.")
            task.result.children.mapNotNull { jogadorSnapshot ->
                jogadorSnapshot.toResultadoGrupo()
            }
        }
    }

    fun obterPontuacaoGlobalJogador(nomeJogador: String): Task<Double> {
        return database.child("jogadores").child(nomeJogador).child("pontuacao").get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar pontuação.")
            task.result.doubleValue()
        }
    }

    fun escutarPontuacoes1x1(
        codigoSala: String,
        onPontuacoes: (List<ResultadoJogador>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(TipoSala.UM_CONTRA_UM, codigoSala).child("pontuacoes")
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

                val jogadorRef = database.child("jogadores").child(resultado.nome)
                jogadorRef.get()
                    .addOnSuccessListener jogadorListener@{ snapshot ->
                        if (!snapshot.exists() || !snapshot.hasChild("password")) {
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
            .child("estatisticasAtualizadas")
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
        val isHostOnly = child("isHostOnly").getValue(Boolean::class.java) == true
        if (deveIgnorarJogador(nome) || isHostOnly) return null

        return ResultadoJogador(
            nome = nome,
            pontos = child("pontuacao").doubleValue(),
            respostasCertas = child("totalPerguntasCertas").intValue()
        )
    }

    private fun DataSnapshot.toResultado2x2(): Resultado2x2 {
        val pontuacoesA = child("pontuacoes_A")
        val pontuacoesB = child("pontuacoes_B")
        val respostasA = child("totalPerguntasCertas_A")
        val respostasB = child("totalPerguntasCertas_B")

        val equipaA = child("equipaA").children.mapNotNull { jogadorSnapshot ->
            val nome = jogadorSnapshot.key ?: return@mapNotNull null
            if (deveIgnorarJogador(nome)) return@mapNotNull null
            ResultadoJogador(
                nome = nome,
                pontos = pontuacoesA.child(nome).doubleValue(),
                respostasCertas = respostasA.child(nome).intValue(),
                equipa = "A"
            )
        }

        val equipaB = child("equipaB").children.mapNotNull { jogadorSnapshot ->
            val nome = jogadorSnapshot.key ?: return@mapNotNull null
            if (deveIgnorarJogador(nome)) return@mapNotNull null
            ResultadoJogador(
                nome = nome,
                pontos = pontuacoesB.child(nome).doubleValue(),
                respostasCertas = respostasB.child(nome).intValue(),
                equipa = "B"
            )
        }

        return Resultado2x2(equipaA = equipaA, equipaB = equipaB)
    }

    private fun DataSnapshot.toEstatisticasAtuais(): EstatisticasService.EstatisticasAtuais {
        return EstatisticasService.EstatisticasAtuais(
            pontuacao = child("pontuacao").doubleValue(),
            taxaAcertos = child("taxaAcertos").doubleValue(),
            totalJogos = child("totalJogos").intValue(),
            totalVitorias = child("totalVitorias").intValue(),
            totalRespostasCertas = child("totalRespostasCertas").intValue(),
            totalVitoriasModo1x1 = child("totalVitoriasModo1x1").intValue(),
            totalVitoriasModo2x2 = child("totalVitoriasModo2x2").intValue(),
            totalVitoriasModoSolo = child("totalVitoriasModoSolo").intValue()
        )
    }

    private fun deveIgnorarJogador(nome: String): Boolean {
        return nome.isBlank() || nome == "admin"
    }

    private fun salaRef(tipoSala: TipoSala, codigoSala: String): DatabaseReference {
        return database.child(tipoSala.node).child(codigoSala)
    }
}
