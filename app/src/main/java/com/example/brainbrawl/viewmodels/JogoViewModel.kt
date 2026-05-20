package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.models.Pergunta
import com.example.brainbrawl.repositories.CategoriaRepository
import com.example.brainbrawl.repositories.JogoRepository
import com.example.brainbrawl.services.GameService
import com.example.brainbrawl.services.ScoreService

class JogoViewModel(
    private val jogoRepository: JogoRepository = JogoRepository(),
    private val categoriaRepository: CategoriaRepository = CategoriaRepository(),
    private val gameService: GameService = GameService(),
    private val scoreService: ScoreService = ScoreService()
) : ViewModel() {

    private val _sala = MutableLiveData<JogoSalaUiState>()
    val sala: LiveData<JogoSalaUiState> = _sala

    private val _pergunta = MutableLiveData<JogoPerguntaUiState>()
    val pergunta: LiveData<JogoPerguntaUiState> = _pergunta

    private val _evento = MutableLiveData<JogoEvent?>()
    val evento: LiveData<JogoEvent?> = _evento

    private val perguntas = mutableListOf<Pergunta>()

    private var codigoSala: String = ""
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var nomeJogador: String = ""
    private var nomeCategoria: String = ""
    private var origemCategoria: String = GameConstants.ORIGEM_CATEGORIA_OFICIAL
    private var categoriaPublicaId: String = ""
    private var donoUid: String = ""
    private var donoCategoria: String = ""
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()
    private var modoJogo: String? = null
    private var modoSolo = false
    private var partidaId = ""
    private var categoriaCompetitiva = true
    private var admin = false
    private var perguntaAtualIndex = 0
    private var totalPontos = 0.0
    private var numeroPerguntasCertas = 0
    private var totalPerguntascertas = 0
    private var perguntasRespondidas = 0
    private var bonus = 50
    private var jaRespondeu = false
    private var acertouUltimaPergunta = false
    private var serverTimeOffset: Long = 0L
    private var eliminacaoEmCurso = false
    private var navegacaoPontuacoesIniciada = false

    private var perguntaIndexListener: JogoRepository.ListenerHandle? = null
    private var serverTimeOffsetListener: JogoRepository.ListenerHandle? = null
    private var estadoSalaListener: JogoRepository.ListenerHandle? = null

    fun iniciar(
        codigoSala: String,
        uid: String,
        nomeUtilizador: String,
        nomeJogador: String,
        nomeCategoria: String,
        modoJogoSolo: String? = null,
        modoSolo: Boolean = false,
        origemCategoria: String = GameConstants.ORIGEM_CATEGORIA_OFICIAL,
        categoriaPublicaId: String = "",
        donoUid: String = "",
        donoCategoria: String = ""
    ) {
        this.codigoSala = codigoSala
        this.uid = uid
        this.nomeUtilizador = nomeUtilizador
        this.nomeJogador = nomeJogador
        this.nomeCategoria = nomeCategoria
        this.modoSolo = modoSolo || codigoSala.isBlank()
        this.modoJogo = modoJogoSolo ?: GameConstants.MODO_CLASSICO
        this.origemCategoria = origemCategoria.ifBlank { GameConstants.ORIGEM_CATEGORIA_OFICIAL }
        this.categoriaPublicaId = categoriaPublicaId
        this.donoUid = donoUid
        this.donoCategoria = donoCategoria
        this.partidaId = "solo_${System.currentTimeMillis()}"
        this.categoriaCompetitiva = this.origemCategoria == GameConstants.ORIGEM_CATEGORIA_OFICIAL
        this.jogadorAtual = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador)

        if (this.modoSolo) {
            admin = false
            _sala.value = JogoSalaUiState(admin = false, modoJogo = this.modoJogo)
            carregarPerguntasSolo()
            return
        }

        observarOffsetServidor()
        jogoRepository.obterInfoSala(codigoSala, jogadorAtual)
            .addOnSuccessListener { infoSala ->
                admin = infoSala.admin
                modoJogo = infoSala.modoJogo
                categoriaCompetitiva = infoSala.categoriaCompetitiva
                _sala.value = JogoSalaUiState(admin, modoJogo)
                escutarFimEliminatorias()
                carregarPerguntas()
            }
            .addOnFailureListener { erro ->
                _evento.value = JogoEvent.ErroCarregarSala(erro.message.orEmpty())
            }
    }

    fun enviarResposta(
        numeroOpcao: Int,
        opcaoEscolhida: String,
        respostaCorreta: String,
        tempoRestante: Double
    ): JogoRespostaResultado? {
        if (admin || jaRespondeu) return null
        jaRespondeu = true
        acertouUltimaPergunta = false
        perguntasRespondidas++

        var bonusAplicado = 0
        if (numeroOpcao in 0..3 && opcaoEscolhida == respostaCorreta) {
            acertouUltimaPergunta = true
            numeroPerguntasCertas++
            totalPerguntascertas++
            val resultado = scoreService.calcularPontuacao(
                modoJogo = modoJogo,
                tempoRestante = tempoRestante,
                numeroPerguntasCertas = numeroPerguntasCertas,
                bonus = bonus
            )
            bonusAplicado = resultado.bonusAplicado
            totalPontos += resultado.pontos
        } else if (numeroOpcao in 0..3) {
            numeroPerguntasCertas = 0
        }

        if (!modoSolo) {
            jogoRepository.registarResposta(
                codigoSala = codigoSala,
                jogador = jogadorAtual,
                acertou = acertouUltimaPergunta,
                totalPontos = totalPontos,
                totalRespostasCertas = totalPerguntascertas,
                perguntasRespondidas = perguntasRespondidas
            )
        }

        return JogoRespostaResultado(
            acertou = acertouUltimaPergunta,
            bonusAplicado = bonusAplicado,
            deveEliminar = !modoSolo && modoJogo == GameConstants.MODO_ELIMINATORIAS && !admin && !acertouUltimaPergunta,
            deveFinalizarSolo = modoSolo && modoJogo == GameConstants.MODO_ELIMINATORIAS && !acertouUltimaPergunta,
            deveAvancarSolo = modoSolo
        )
    }

    fun avancarSoloAposResposta() {
        if (!modoSolo || navegacaoPontuacoesIniciada) return
        if (modoJogo == GameConstants.MODO_ELIMINATORIAS && !acertouUltimaPergunta) {
            finalizarSolo()
            return
        }

        perguntaAtualIndex++
        if (perguntaAtualIndex < perguntas.size) {
            prepararPerguntaSolo()
        } else {
            if (modoJogo == GameConstants.MODO_ELIMINATORIAS) {
                _evento.value = JogoEvent.BancoPerguntasConcluido
            }
            finalizarSolo()
        }
    }

    fun eliminarJogador() {
        if (admin || eliminacaoEmCurso) return
        eliminacaoEmCurso = true
        jogoRepository.marcarJogadorEliminado(
            codigoSala,
            jogadorAtual,
            totalPontos,
            totalPerguntascertas
        )
            .addOnSuccessListener {
                _evento.value = JogoEvent.AbrirEsperaEliminado(dadosNavegacao())
            }
            .addOnFailureListener {
                eliminacaoEmCurso = false
                _evento.value = JogoEvent.ErroEliminarJogador
            }
    }

    fun adminTempoTerminou() {
        if (modoJogo == GameConstants.MODO_ELIMINATORIAS) {
            verificarFimEliminatoriasOuAvancar()
        } else {
            perguntaAtualIndex++
            jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
            prepararPerguntaAdmin()
        }
    }

    fun finalizarJogo() {
        if (perguntaAtualIndex >= perguntas.size) {
            if (modoJogo == GameConstants.MODO_ELIMINATORIAS && admin) {
                terminarEliminatoriasEEnviar()
                return
            }

            jogoRepository.obterEstadoSala(codigoSala)
                .addOnSuccessListener {
                    guardarResultadoEEnviarPontuacoes()
                }
                .addOnFailureListener { erro ->
                    _evento.value = JogoEvent.ErroEstadoSala(erro.message.orEmpty())
                }
            return
        }

        if (modoJogo == GameConstants.MODO_ELIMINATORIAS) {
            verificarFimEliminatoriasOuAvancar()
        } else {
            perguntaAtualIndex++
            if (admin) {
                jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
                prepararPerguntaAdmin()
            }
        }
    }

    fun tempoTotal(): Double {
        return gameService.tempoTotal(modoJogo)
    }

    fun tempoServidorAtual(): Long {
        return System.currentTimeMillis() + serverTimeOffset
    }

    fun jaRespondeuPergunta(): Boolean {
        return jaRespondeu
    }

    fun removerListeners() {
        jogoRepository.removerListener(perguntaIndexListener)
        perguntaIndexListener = null
        jogoRepository.removerListener(serverTimeOffsetListener)
        serverTimeOffsetListener = null
        jogoRepository.removerListener(estadoSalaListener)
        estadoSalaListener = null
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun carregarPerguntas() {
        jogoRepository.carregarPerguntas(codigoSala)
            .addOnSuccessListener { perguntasCarregadas ->
                perguntas.clear()
                perguntas.addAll(perguntasCarregadas)
                if (perguntas.isEmpty()) {
                    _evento.value = JogoEvent.FinalizarJogo
                    return@addOnSuccessListener
                }

                if (admin) {
                    prepararPerguntaAdmin()
                } else {
                    escutarIndicePergunta()
                }
            }
            .addOnFailureListener { erro ->
                _evento.value = JogoEvent.ErroCarregarPerguntas(erro.message.orEmpty())
            }
    }

    private fun carregarPerguntasSolo() {
        val task = when (origemCategoria) {
            GameConstants.ORIGEM_CATEGORIA_PUBLICA -> categoriaRepository.carregarCategoriaPublica(categoriaPublicaId)
            GameConstants.ORIGEM_CATEGORIA_PERSONALIZADA -> categoriaRepository.carregarPerguntasCategoriaPersonalizada(
                uid = donoUid,
                nomeUtilizador = donoCategoria.ifBlank { nomeUtilizador },
                nomeCategoria = nomeCategoria,
                minimoOpcoes = 4
            )
            else -> if (nomeCategoria.equals("Todas as Categorias", ignoreCase = true)) {
                categoriaRepository.carregarTodasPerguntasOficiais()
            } else {
                categoriaRepository.carregarPerguntasCategoriaOficial(nomeCategoria)
            }
        }

        task.addOnSuccessListener { resultado ->
            val perguntasCarregadas = when (resultado) {
                is CategoriaRepository.CategoriaPublicaDetalhe -> resultado.perguntas.mapNotNull { it.toPergunta() }
                is List<*> -> resultado.mapNotNull { (it as? Map<*, *>)?.toPergunta() }
                else -> emptyList()
            }

            perguntas.clear()
            val perguntasEmJogo = perguntasCarregadas.shuffled()
            perguntas.addAll(if (modoJogo == GameConstants.MODO_ELIMINATORIAS) perguntasEmJogo else perguntasEmJogo.take(8))
            if (perguntas.isEmpty()) {
                _evento.value = JogoEvent.ErroCarregarPerguntas("Sem perguntas válidas para esta categoria.")
                return@addOnSuccessListener
            }
            prepararPerguntaSolo()
        }.addOnFailureListener { erro ->
            _evento.value = JogoEvent.ErroCarregarPerguntas(erro.message.orEmpty())
        }
    }

    private fun prepararPerguntaSolo() {
        if (perguntaAtualIndex >= perguntas.size) {
            finalizarSolo()
            return
        }

        jaRespondeu = false
        acertouUltimaPergunta = false
        publicarPergunta(adminPergunta = false)
        _evento.value = JogoEvent.IniciarCronometro(tempoServidorAtual(), admin = false)
    }

    private fun escutarIndicePergunta() {
        removerIndicePerguntaListener()
        perguntaIndexListener = jogoRepository.escutarIndicePergunta(
            codigoSala,
            onIndiceAlterado = { novoIndex ->
                if (novoIndex != perguntaAtualIndex) {
                    perguntaAtualIndex = novoIndex
                    if (perguntaAtualIndex < perguntas.size) {
                        prepararPerguntaJogador()
                    } else {
                        _evento.value = JogoEvent.FinalizarJogo
                    }
                }
            }
        )

        jogoRepository.obterIndicePergunta(codigoSala)
            .addOnSuccessListener { idx ->
                perguntaAtualIndex = idx
                if (perguntaAtualIndex < perguntas.size) {
                    prepararPerguntaJogador()
                } else {
                    _evento.value = JogoEvent.FinalizarJogo
                }
            }
    }

    private fun prepararPerguntaJogador() {
        if (perguntaAtualIndex >= perguntas.size) {
            _evento.value = JogoEvent.FinalizarJogo
            return
        }

        jaRespondeu = false
        acertouUltimaPergunta = false
        publicarPergunta(adminPergunta = false)
        jogoRepository.obterHoraInicioPergunta(codigoSala)
            .addOnSuccessListener { horaInicio ->
                _evento.value = JogoEvent.IniciarCronometro(horaInicio ?: tempoServidorAtual(), admin = false)
            }
            .addOnFailureListener {
                _evento.value = JogoEvent.IniciarCronometro(tempoServidorAtual(), admin = false)
            }
    }

    private fun prepararPerguntaAdmin() {
        if (perguntaAtualIndex >= perguntas.size) {
            _evento.value = JogoEvent.FinalizarJogo
            return
        }

        jaRespondeu = false
        acertouUltimaPergunta = false
        publicarPergunta(adminPergunta = true)
        jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
        jogoRepository.limparRespostasPergunta(codigoSala)
        jogoRepository.obterHoraInicioPergunta(codigoSala)
            .addOnSuccessListener { horaInicio ->
                _evento.value = JogoEvent.IniciarCronometro(horaInicio ?: tempoServidorAtual(), admin = true)
            }
            .addOnFailureListener {
                _evento.value = JogoEvent.IniciarCronometro(tempoServidorAtual(), admin = true)
            }
    }

    private fun publicarPergunta(adminPergunta: Boolean) {
        _pergunta.value = JogoPerguntaUiState(
            pergunta = perguntas[perguntaAtualIndex],
            indice = perguntaAtualIndex,
            totalPerguntas = perguntas.size,
            admin = adminPergunta
        )
    }

    private fun observarOffsetServidor() {
        jogoRepository.removerListener(serverTimeOffsetListener)
        serverTimeOffsetListener = jogoRepository.escutarOffsetServidor(
            onOffsetAlterado = { offset ->
                serverTimeOffset = offset
            }
        )
    }

    private fun verificarFimEliminatoriasOuAvancar() {
        if (!admin && !acertouUltimaPergunta) {
            eliminarJogador()
            return
        }

        jogoRepository.obterJogadoresEliminatorias(codigoSala)
            .addOnSuccessListener { jogadores ->
                val jogadoresRestantes = jogadores
                    .filter { jogador ->
                        jogador.nome != GameConstants.JOGADOR_ADMIN &&
                            jogador.chave != GameConstants.JOGADOR_ADMIN &&
                            !jogador.isHostOnly &&
                            jogador.estado != GameConstants.ESTADO_ELIMINADO
                    }
                    .map { it.nome }

                if (gameService.deveTerminarEliminatorias(jogadoresRestantes)) {
                    _evento.value = JogoEvent.MensagemFimEliminatorias
                    terminarEliminatoriasEEnviar()
                } else {
                    perguntaAtualIndex++
                    jogoRepository.atualizarPerguntaAtual(codigoSala, perguntaAtualIndex)
                    if (admin) prepararPerguntaAdmin()
                }
            }
            .addOnFailureListener {
                _evento.value = JogoEvent.ErroVerificarJogadores
            }
    }

    private fun escutarFimEliminatorias() {
        if (modoJogo != GameConstants.MODO_ELIMINATORIAS || estadoSalaListener != null) return

        estadoSalaListener = jogoRepository.escutarEstadoSala(
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_TERMINADO) {
                    guardarResultadoEEnviarPontuacoes()
                }
            }
        )
    }

    private fun terminarEliminatoriasEEnviar() {
        if (navegacaoPontuacoesIniciada) return

        jogoRepository.atualizarEstadoSala(codigoSala, GameConstants.ESTADO_TERMINADO)
            .addOnCompleteListener {
                guardarResultadoEEnviarPontuacoes()
            }
    }

    private fun guardarResultadoEEnviarPontuacoes() {
        if (navegacaoPontuacoesIniciada) return
        navegacaoPontuacoesIniciada = true
        removerListeners()

        if (modoSolo || admin) {
            _evento.value = JogoEvent.AbrirPontuacoes(dadosNavegacao())
            return
        }

        // Grupo (incl. eliminatórias): o anfitrião/admin que ainda joga também tem de gravar
        // estado+pontuação em Firebase; caso contrário o pódio fica incompleto (ex.: sobrevivente admin).
        jogoRepository.guardarResultadoJogador(
            codigoSala,
            jogadorAtual,
            totalPontos,
            totalPerguntascertas
        ).addOnCompleteListener {
            _evento.value = JogoEvent.AbrirPontuacoes(dadosNavegacao())
        }
    }

    private fun finalizarSolo() {
        if (navegacaoPontuacoesIniciada) return
        navegacaoPontuacoesIniciada = true
        removerListeners()
        _evento.value = JogoEvent.AbrirPontuacoes(dadosNavegacao())
    }

    private fun dadosNavegacao(): JogoResultadoDados {
        return JogoResultadoDados(
            codigoSala = codigoSala,
            uid = uid,
            nomeJogador = nomeJogador,
            totalPontos = totalPontos,
            partidaId = partidaId,
            nomeCategoria = nomeCategoria,
            nomeUtilizador = nomeUtilizador,
            modoJogo = modoJogo,
            numeroPerguntasCertas = numeroPerguntasCertas,
            totalPerguntasCertas = totalPerguntascertas,
            totalPerguntas = if (modoSolo) perguntasRespondidas.coerceAtLeast(totalPerguntascertas) else perguntas.size,
            modoSolo = modoSolo,
            categoriaCompetitiva = categoriaCompetitiva,
            admin = admin
        )
    }

    private fun removerIndicePerguntaListener() {
        jogoRepository.removerListener(perguntaIndexListener)
        perguntaIndexListener = null
    }
}

private fun Map<*, *>.toPergunta(): Pergunta? {
    val texto = this["pergunta"] as? String ?: return null
    val resposta = this["respostaCorreta"] as? String ?: return null
    val opcoes = (this["opcoes"] as? List<*>)
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    if (texto.isBlank() || resposta.isBlank() || opcoes.size < 4) return null
    return Pergunta(
        pergunta = texto,
        respostaCorreta = resposta,
        opcoes = opcoes,
        imagem = this["imagem"] as? String,
        dificuldade = this["dificuldade"] as? String
    )
}

data class JogoSalaUiState(
    val admin: Boolean,
    val modoJogo: String?
)

data class JogoPerguntaUiState(
    val pergunta: Pergunta,
    val indice: Int,
    val totalPerguntas: Int,
    val admin: Boolean
)

data class JogoRespostaResultado(
    val acertou: Boolean,
    val bonusAplicado: Int,
    val deveEliminar: Boolean,
    val deveFinalizarSolo: Boolean = false,
    val deveAvancarSolo: Boolean = false
)

data class JogoResultadoDados(
    val codigoSala: String,
    val uid: String,
    val nomeJogador: String,
    val totalPontos: Double,
    val partidaId: String,
    val nomeCategoria: String,
    val nomeUtilizador: String,
    val modoJogo: String?,
    val numeroPerguntasCertas: Int,
    val totalPerguntasCertas: Int,
    val totalPerguntas: Int,
    val modoSolo: Boolean,
    val categoriaCompetitiva: Boolean,
    val admin: Boolean = false
)

sealed class JogoEvent {
    data class IniciarCronometro(val horaInicio: Long, val admin: Boolean) : JogoEvent()
    data class ErroCarregarSala(val mensagem: String) : JogoEvent()
    data class ErroCarregarPerguntas(val mensagem: String) : JogoEvent()
    data class ErroEstadoSala(val mensagem: String) : JogoEvent()
    data object ErroVerificarJogadores : JogoEvent()
    data object ErroEliminarJogador : JogoEvent()
    data object FinalizarJogo : JogoEvent()
    data object MensagemFimEliminatorias : JogoEvent()
    data object BancoPerguntasConcluido : JogoEvent()
    data class AbrirEsperaEliminado(val dados: JogoResultadoDados) : JogoEvent()
    data class AbrirPontuacoes(val dados: JogoResultadoDados) : JogoEvent()
}
