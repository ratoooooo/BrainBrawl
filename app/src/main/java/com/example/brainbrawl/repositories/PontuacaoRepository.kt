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

    private data class PerfilResolvido(
        val chave: String,
        val snapshot: DataSnapshot
    )

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

    fun obterPontuacaoGlobalJogador(identificador: String): Task<Double> {
        val result = TaskCompletionSource<Double>()
        val resultado = ResultadoJogador(nome = identificador, pontos = 0.0, uid = identificador)

        resolverPerfilJogador(resultado)
            .addOnSuccessListener { perfil ->
                result.setResult(perfil?.snapshot?.child(FirebasePaths.PONTUACAO)?.doubleValue() ?: 0.0)
            }
            .addOnFailureListener { error ->
                result.setException(error)
            }

        return result.task
    }

    fun escutarPontuacoes1x1(
        codigoSala: String,
        onPontuacoes: (List<ResultadoJogador>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = salaRef(TipoSala.UM_CONTRA_UM, codigoSala)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jogadoresSnapshot = snapshot.child(FirebasePaths.JOGADORES)
                val resultados = snapshot.child(FirebasePaths.PONTUACOES).children.mapNotNull { pontuacaoSnapshot ->
                    val chave = pontuacaoSnapshot.key ?: return@mapNotNull null
                    if (deveIgnorarJogador(chave)) return@mapNotNull null

                    jogadoresSnapshot.child(chave).toResultadoJogador(
                        chave = chave,
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
            !deveAtualizarResultado(resultado, jogadoresParaAtualizar)
        ) {
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

        marcarEstatisticasJogadorComoAtualizadas(
            tipoSala,
            codigoSala,
            resultado.identificadorEstatisticas
        ).addOnSuccessListener { podeAtualizar ->
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

            resolverPerfilJogador(resultado)
                .addOnSuccessListener jogadorListener@{ perfil ->
                    val snapshot = perfil?.snapshot

                    if (perfil == null || snapshot == null || !snapshot.ePerfilJogador()) {
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

                    val jogadorRef = database.child(FirebasePaths.JOGADORES).child(perfil.chave)

                    val updates = estatisticasService.calcularAtualizacao(
                        estatisticasAtuais = snapshot.toEstatisticasAtuais(),
                        resultado = resultado,
                        modo = modo,
                        venceu = vencedores.contains(resultado.identificadorEstatisticas),
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
        }.addOnFailureListener { error ->
            result.setException(error)
        }
    }

    private fun marcarEstatisticasJogadorComoAtualizadas(
        tipoSala: TipoSala,
        codigoSala: String,
        identificadorJogador: String
    ): Task<Boolean> {
        val result = TaskCompletionSource<Boolean>()

        if (identificadorJogador.isBlank()) {
            result.setResult(false)
            return result.task
        }

        val reference = salaRef(tipoSala, codigoSala)
            .child(FirebasePaths.ESTATISTICAS_ATUALIZADAS)
            .child(identificadorJogador)

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
        val chave = key ?: return null
        val isHostOnly = child(FirebasePaths.IS_HOST_ONLY).getValue(Boolean::class.java) == true

        if (deveIgnorarJogador(chave) || isHostOnly) return null

        return toResultadoJogador(
            chave = chave,
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
        return hasChild(FirebasePaths.PONTUACAO) &&
                (hasChild(FirebasePaths.TOTAL_RESPOSTAS_CERTAS) || hasChild(FirebasePaths.TOTAL_PERGUNTAS_CERTAS))
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
            val chave = jogadorSnapshot.key ?: return@mapNotNull null
            if (deveIgnorarJogador(chave)) return@mapNotNull null

            if (!pontuacoesA.hasChild(chave) || !respostasA.hasChild(chave)) {
                return@mapNotNull null
            }

            jogadorSnapshot.toResultadoJogador(
                chave = chave,
                pontos = pontuacoesA.child(chave).doubleValue(),
                respostasCertas = respostasA.child(chave).intValue(),
                equipa = GameConstants.EQUIPA_A
            )
        }

        val equipaB = child(FirebasePaths.EQUIPA_B).children.mapNotNull { jogadorSnapshot ->
            val chave = jogadorSnapshot.key ?: return@mapNotNull null
            if (deveIgnorarJogador(chave)) return@mapNotNull null

            if (!pontuacoesB.hasChild(chave) || !respostasB.hasChild(chave)) {
                return@mapNotNull null
            }

            jogadorSnapshot.toResultadoJogador(
                chave = chave,
                pontos = pontuacoesB.child(chave).doubleValue(),
                respostasCertas = respostasB.child(chave).intValue(),
                equipa = GameConstants.EQUIPA_B
            )
        }

        return Resultado2x2(equipaA = equipaA, equipaB = equipaB)
    }

    private fun DataSnapshot.toResultadoJogador(
        chave: String,
        pontos: Double,
        respostasCertas: Int,
        equipa: String? = null
    ): ResultadoJogador {
        return ResultadoJogador(
            nome = nomeDisplay().ifBlank { chave },
            pontos = pontos,
            respostasCertas = respostasCertas,
            equipa = equipa,
            uid = child(FirebasePaths.UID).texto(),
            chave = chave,
            nomeUtilizador = child(FirebasePaths.NOME_UTILIZADOR).texto(),
            nomeJogador = child(FirebasePaths.NOME_JOGADOR).texto()
        )
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
            totalVitoriasModoSolo = child(FirebasePaths.TOTAL_VITORIAS_MODO_SOLO).intValue(),
            xpTotal = child(FirebasePaths.XP_TOTAL).intValue()
        )
    }

    private fun resolverPerfilJogador(resultado: ResultadoJogador): Task<PerfilResolvido?> {
        val result = TaskCompletionSource<PerfilResolvido?>()
        procurarPerfilDireto(resultado.chavesCompatibilidade, 0, result, resultado)
        return result.task
    }

    private fun procurarPerfilDireto(
        candidatos: List<String>,
        index: Int,
        result: TaskCompletionSource<PerfilResolvido?>,
        resultado: ResultadoJogador
    ) {
        if (index >= candidatos.size) {
            val consultas = listOf(
                FirebasePaths.UID to resultado.uid,
                FirebasePaths.NOME_UTILIZADOR to resultado.nomeUtilizador,
                FirebasePaths.NOME_UTILIZADOR to resultado.nome,
                FirebasePaths.NOME to resultado.nome
            ).filter { it.second.isNotBlank() }.distinct()

            procurarPerfilPorCampo(consultas, 0, result)
            return
        }

        val candidato = candidatos[index]

        database.child(FirebasePaths.JOGADORES).child(candidato).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    result.setResult(PerfilResolvido(candidato, snapshot))
                } else {
                    procurarPerfilDireto(candidatos, index + 1, result, resultado)
                }
            }
            .addOnFailureListener { error ->
                result.setException(error)
            }
    }

    private fun procurarPerfilPorCampo(
        consultas: List<Pair<String, String>>,
        index: Int,
        result: TaskCompletionSource<PerfilResolvido?>
    ) {
        if (index >= consultas.size) {
            result.setResult(null)
            return
        }

        val (campo, valor) = consultas[index]

        database.child(FirebasePaths.JOGADORES)
            .orderByChild(campo)
            .equalTo(valor)
            .limitToFirst(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val perfil = snapshot.children.firstOrNull()
                val chave = perfil?.key

                if (perfil != null && chave != null) {
                    result.setResult(PerfilResolvido(chave, perfil))
                } else {
                    procurarPerfilPorCampo(consultas, index + 1, result)
                }
            }
            .addOnFailureListener { error ->
                result.setException(error)
            }
    }

    private fun DataSnapshot.ePerfilJogador(): Boolean {
        return exists() && (
                hasChild(FirebasePaths.UID) ||
                        hasChild(FirebasePaths.NOME_UTILIZADOR) ||
                        hasChild(FirebasePaths.PASSWORD) ||
                        hasChild(FirebasePaths.EMAIL)
                )
    }

    private fun deveAtualizarResultado(
        resultado: ResultadoJogador,
        jogadoresParaAtualizar: Set<String>?
    ): Boolean {
        return jogadoresParaAtualizar == null || jogadoresParaAtualizar.any { resultado.corresponde(it) }
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

    private fun deveIgnorarJogador(nome: String): Boolean {
        return nome.isBlank() || nome == GameConstants.JOGADOR_ADMIN
    }

    private fun salaRef(tipoSala: TipoSala, codigoSala: String): DatabaseReference {
        return database.child(tipoSala.node).child(codigoSala)
    }
}