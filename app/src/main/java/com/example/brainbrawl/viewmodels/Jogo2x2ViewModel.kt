package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.models.Pergunta
import com.example.brainbrawl.repositories.JogoCompetitivoRepository
import com.example.brainbrawl.repositories.JogoCompetitivoRepository.ModoCompetitivo
import com.example.brainbrawl.services.ScoreCompetitivoService

class Jogo2x2ViewModel(
    private val jogoCompetitivoRepository: JogoCompetitivoRepository = JogoCompetitivoRepository(),
    private val scoreCompetitivoService: ScoreCompetitivoService = ScoreCompetitivoService()
) : ViewModel() {

    private val _pergunta = MutableLiveData<JogoCompetitivoPerguntaUiState>()
    val pergunta: LiveData<JogoCompetitivoPerguntaUiState> = _pergunta

    private val _evento = MutableLiveData<Jogo2x2Event?>()
    val evento: LiveData<Jogo2x2Event?> = _evento
    private val _placar = MutableLiveData<Jogo2x2PlacarUiState>()
    val placar: LiveData<Jogo2x2PlacarUiState> = _placar

    private val perguntas = mutableListOf<Pergunta>()
    private var codigoSala: String = ""
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var nomeJogador: String = ""
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()
    private var chaveJogador: String = ""
    private var categoria: String = ""
    private var equipaDoJogador: String = ""
    private var perguntaAtualIndex = 0
    private var totalPontos = 0.0
    private var numeroPerguntasCertas = 0
    private var totalPerguntasRespondidas = 0
    private var totalPerguntascertas = 0
    private var bonus = 50
    private var serverTimeOffset: Long = 0L
    private var categoriaCompetitiva: Boolean = false
    private var tempoTotalPergunta = GameConstants.COMPETITIVE_DEFAULT_QUESTION_TIME_SECONDS
    private val retryHandler = Handler(Looper.getMainLooper())

    private var offsetListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var podioListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var equipasListener: JogoCompetitivoRepository.ListenerHandle? = null

    fun iniciar(
        codigoSala: String,
        uid: String,
        nomeUtilizador: String,
        nomeJogador: String,
        playerKey: String = "",
        tipoJogador: String = "",
        avatar: String = "",
        categoriaPadrao: String,
        categoriaTodas: String,
        tempoTotalPergunta: Double = GameConstants.COMPETITIVE_DEFAULT_QUESTION_TIME_SECONDS
    ) {
        this.codigoSala = codigoSala
        this.uid = uid
        this.nomeUtilizador = nomeUtilizador
        this.nomeJogador = nomeJogador
        this.tempoTotalPergunta = tempoTotalPergunta
        this.jogadorAtual = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador, playerKey, tipoJogador, avatar)
        this.chaveJogador = jogadorAtual.chaveSala

        observarOffsetServidor()
        jogoCompetitivoRepository.carregarNomeCategoria(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            categoriaPadrao
        ).addOnSuccessListener { nomeCategoria ->
            categoria = nomeCategoria
            Log.d(
                GAME_CATEGORY_TAG,
                "mode=2x2 room=$codigoSala uid=${uid.maskedLogId()} categoryFromFirebase=$categoria " +
                    "categoryFallback=$categoriaPadrao"
            )
            identificarEquipaECarregarPerguntas(categoriaTodas)
        }.addOnFailureListener {
            Log.w(
                GAME_CATEGORY_TAG,
                "mode=2x2 room=$codigoSala uid=${uid.maskedLogId()} failedCategoryLoad fallback=$categoriaPadrao"
            )
            Log.w(
                START_TAG,
                "mode=2x2 room=$codigoSala errorEvent=ErroLerCategoria reason=category_load_failed " +
                    "uid=${uid.maskedLogId()} key=${chaveJogador.maskedLogId()}"
            )
            _evento.value = Jogo2x2Event.ErroLerCategoria
        }
    }

    fun responder(
        numeroOpcao: Int,
        opcaoEscolhida: String,
        respostaCorreta: String,
        tempoRestante: Double
    ): JogoCompetitivoRespostaResultado {
        totalPerguntasRespondidas++
        var bonusAplicado = 0
        var acertou = false

        if (numeroOpcao in 0..3 && opcaoEscolhida == respostaCorreta) {
            acertou = true
            numeroPerguntasCertas++
            totalPerguntascertas++
            val resultadoPontuacao = scoreCompetitivoService.calcularPontuacao(
                tempoRestante,
                numeroPerguntasCertas,
                bonus,
                tempoTotalPergunta
            )
            bonusAplicado = resultadoPontuacao.bonusAplicado
            totalPontos += resultadoPontuacao.pontos
        } else if (numeroOpcao in 0..3) {
            numeroPerguntasCertas = 0
        }

        jogoCompetitivoRepository.guardarResposta2x2(
            codigoSala,
            chaveJogador,
            perguntaAtualIndex,
            opcaoEscolhida
        )
        jogoCompetitivoRepository.atualizarPontuacaoAoVivo2x2(
            codigoSala,
            equipaDoJogador,
            chaveJogador,
            totalPontos
        )

        return JogoCompetitivoRespostaResultado(acertou, bonusAplicado)
    }

    fun avancarPergunta() {
        perguntaAtualIndex++
        prepararPerguntaAtual()
    }

    fun finalizarJogo() {
        if (equipaDoJogador != GameConstants.EQUIPA_A && equipaDoJogador != GameConstants.EQUIPA_B) {
            _evento.value = Jogo2x2Event.ErroCarregarEquipa
            return
        }

        jogoCompetitivoRepository.guardarResultado2x2(
            codigoSala,
            equipaDoJogador,
            chaveJogador,
            totalPontos,
            totalPerguntascertas
        ).addOnSuccessListener {
            aguardarPodioCompleto()
        }.addOnFailureListener {
            _evento.value = Jogo2x2Event.ErroPodio
        }
    }

    fun tempoServidorAtual(): Long {
        return System.currentTimeMillis() + serverTimeOffset
    }

    fun removerListeners() {
        jogoCompetitivoRepository.removerListener(offsetListener)
        offsetListener = null
        jogoCompetitivoRepository.removerListener(podioListener)
        podioListener = null
        jogoCompetitivoRepository.removerListener(equipasListener)
        equipasListener = null
        retryHandler.removeCallbacksAndMessages(null)
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun identificarEquipaECarregarPerguntas(categoriaTodas: String, retryFeito: Boolean = false) {
        jogoCompetitivoRepository.identificarEquipa2x2(codigoSala, jogadorAtual)
            .addOnSuccessListener { equipaJogador ->
                equipaDoJogador = equipaJogador.equipa
                chaveJogador = equipaJogador.chaveJogador
                Log.d(
                    START_TAG,
                    "mode=2x2 room=$codigoSala teamResolved equipe=${equipaDoJogador.ifBlank { "<empty>" }} " +
                        "uid=${uid.maskedLogId()} playerKey=${jogadorAtual.playerKey.maskedLogId()} " +
                        "resolvedKey=${chaveJogador.maskedLogId()} retry=$retryFeito"
                )
                if (equipaDoJogador == GameConstants.EQUIPA_A || equipaDoJogador == GameConstants.EQUIPA_B) {
                    observarPlacar()
                    verificarCompetitividadeECarregarPerguntas(categoriaTodas)
                } else if (!retryFeito) {
                    Log.w(
                        START_TAG,
                        "mode=2x2 room=$codigoSala teamMissing retryOnce uid=${uid.maskedLogId()} " +
                            "resolvedKey=${chaveJogador.maskedLogId()}"
                    )
                    retryHandler.postDelayed({
                        identificarEquipaECarregarPerguntas(categoriaTodas, retryFeito = true)
                    }, TEAM_RETRY_DELAY_MS)
                } else {
                    Log.w(
                        START_TAG,
                        "mode=2x2 room=$codigoSala errorEvent=ErroCarregarEquipa reason=team_missing_after_retry " +
                            "uid=${uid.maskedLogId()} resolvedKey=${chaveJogador.maskedLogId()}"
                    )
                    _evento.value = Jogo2x2Event.ErroCarregarEquipa
                }
            }
            .addOnFailureListener { error ->
                Log.w(
                    START_TAG,
                    "mode=2x2 room=$codigoSala errorEvent=ErroCarregarEquipa reason=team_lookup_failed " +
                        "uid=${uid.maskedLogId()} message=${error.message}"
                )
                _evento.value = Jogo2x2Event.ErroCarregarEquipa
            }
    }

    private fun verificarCompetitividadeECarregarPerguntas(categoriaTodas: String) {
        jogoCompetitivoRepository.verificarCompetitividade(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .addOnSuccessListener { competitiva ->
                categoriaCompetitiva = competitiva
                carregarOuCriarPerguntas(categoriaTodas)
            }
            .addOnFailureListener {
                categoriaCompetitiva = false
                carregarOuCriarPerguntas(categoriaTodas)
            }
    }

    private fun carregarOuCriarPerguntas(categoriaTodas: String) {
        jogoCompetitivoRepository.carregarOuCriarPerguntas(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            categoria,
            categoriaTodas
        ).addOnSuccessListener { perguntasCarregadas ->
            perguntas.clear()
            perguntas.addAll(perguntasCarregadas)
            if (perguntas.isNotEmpty()) {
                prepararPerguntaAtual()
            }
        }.addOnFailureListener { erro ->
            val mensagem = if (erro.message?.contains("buscar perguntas", ignoreCase = true) == true) {
                "Erro ao buscar perguntas!"
            } else {
                "Erro ao carregar perguntas"
            }
            Log.w(
                START_TAG,
                "mode=2x2 room=$codigoSala errorEvent=ErroPerguntas reason=${erro.message} " +
                    "category=${categoria.ifBlank { "<empty>" }} uid=${uid.maskedLogId()} " +
                    "key=${chaveJogador.maskedLogId()} team=${equipaDoJogador.ifBlank { "<empty>" }}"
            )
            _evento.value = Jogo2x2Event.ErroPerguntas(mensagem)
        }
    }

    private fun prepararPerguntaAtual() {
        if (perguntaAtualIndex >= perguntas.size) {
            _evento.value = Jogo2x2Event.FinalizarJogo
            return
        }

        _pergunta.value = JogoCompetitivoPerguntaUiState(
            pergunta = perguntas[perguntaAtualIndex],
            indice = perguntaAtualIndex,
            totalPerguntas = perguntas.size,
            categoria = categoria
        )
        sincronizarInicioPergunta()
    }

    private fun sincronizarInicioPergunta() {
        jogoCompetitivoRepository.sincronizarInicioPergunta(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            perguntaAtualIndex,
            tempoServidorAtual()
        ).addOnSuccessListener { inicio ->
            _evento.value = Jogo2x2Event.IniciarCronometro(inicio)
        }.addOnFailureListener {
            _evento.value = Jogo2x2Event.IniciarCronometro(tempoServidorAtual())
        }
    }

    private fun aguardarPodioCompleto() {
        jogoCompetitivoRepository.removerListener(podioListener)
        podioListener = jogoCompetitivoRepository.escutarPodio2x2(
            codigoSala,
            onPodioCompleto = {
                _evento.value = Jogo2x2Event.AbrirPontuacoes(dadosPontuacao())
            },
            onAguardar = {
                _evento.value = Jogo2x2Event.AguardarJogadores
            }
        ) {
            _evento.value = Jogo2x2Event.ErroPodio
        }
    }

    private fun observarOffsetServidor() {
        jogoCompetitivoRepository.removerListener(offsetListener)
        offsetListener = jogoCompetitivoRepository.escutarOffsetServidor(
            onOffsetAlterado = { offset ->
                serverTimeOffset = offset
            }
        )
    }

    private fun observarPlacar() {
        jogoCompetitivoRepository.removerListener(equipasListener)
        equipasListener = jogoCompetitivoRepository.escutarEquipas2x2(
            codigoSala = codigoSala,
            onEquipasAlteradas = { equipaA, equipaB ->
                val idsAtuais = jogadorAtual.chavesCompatibilidade + chaveJogador
                val equipaAUi = equipaA.toEquipaUi("Equipa Lusa", idsAtuais)
                val equipaBUi = equipaB.toEquipaUi("Os Descobridores", idsAtuais)
                _placar.value = Jogo2x2PlacarUiState(
                    equipaA = equipaAUi,
                    equipaB = equipaBUi
                )
            }
        )
    }

    private fun List<JogoCompetitivoRepository.JogadorCompetitivo>.toEquipaUi(
        nome: String,
        idsAtuais: List<String>
    ): EquipaCompetitivaUi {
        val jogadoresUi = filter { it.chave != GameConstants.JOGADOR_ADMIN && it.estado != GameConstants.ESTADO_OFF }
            .distinctBy { it.chave }
            .take(2)
            .map { jogador ->
                JogadorCompetitivoUi(
                    chave = jogador.chave,
                    nome = jogador.nomeDisplay,
                    avatar = jogador.avatar,
                    pontuacao = jogador.pontuacao,
                    atual = jogador.chave in idsAtuais ||
                        jogador.uid in idsAtuais ||
                        jogador.playerKey in idsAtuais
                )
            }
        return EquipaCompetitivaUi(
            nome = nome,
            jogadores = jogadoresUi,
            pontuacao = jogadoresUi.sumOf { it.pontuacao }
        )
    }

    private fun dadosPontuacao(): JogoCompetitivoPontuacaoDados {
        return JogoCompetitivoPontuacaoDados(
            codigoSala = codigoSala,
            modoJogo = GameConstants.MODO_2X2,
            uid = uid,
            nomeUtilizador = nomeUtilizador,
            nomeJogador = nomeJogador.ifBlank { jogadorAtual.nomeDisplay },
            playerKey = chaveJogador,
            tipoJogador = jogadorAtual.tipoJogador,
            avatar = jogadorAtual.avatar,
            totalPontos = totalPontos,
            categoria = categoria,
            totalPerguntasCertas = totalPerguntascertas,
            numeroPerguntasCertas = numeroPerguntasCertas,
            totalPerguntas = perguntas.size,
            equipa = equipaDoJogador,
            categoriaCompetitiva = categoriaCompetitiva
        )
    }
}

data class JogoCompetitivoPerguntaUiState(
    val pergunta: Pergunta,
    val indice: Int,
    val totalPerguntas: Int,
    val categoria: String = ""
)

data class JogoCompetitivoRespostaResultado(
    val acertou: Boolean,
    val bonusAplicado: Int
)

data class JogoCompetitivoPontuacaoDados(
    val codigoSala: String,
    val modoJogo: String,
    val uid: String,
    val nomeUtilizador: String,
    val nomeJogador: String,
    val playerKey: String = "",
    val tipoJogador: String = "",
    val avatar: String = "",
    val totalPontos: Double,
    val categoria: String,
    val totalPerguntasCertas: Int,
    val numeroPerguntasCertas: Int,
    val totalPerguntas: Int,
    val equipa: String?,
    val categoriaCompetitiva: Boolean = true
)

sealed class Jogo2x2Event {
    data class IniciarCronometro(val horaInicio: Long) : Jogo2x2Event()
    data class ErroPerguntas(val mensagem: String) : Jogo2x2Event()
    data class AbrirPontuacoes(val dados: JogoCompetitivoPontuacaoDados) : Jogo2x2Event()
    data object ErroLerCategoria : Jogo2x2Event()
    data object ErroCarregarEquipa : Jogo2x2Event()
    data object ErroPodio : Jogo2x2Event()
    data object AguardarJogadores : Jogo2x2Event()
    data object FinalizarJogo : Jogo2x2Event()
}

private const val GAME_CATEGORY_TAG = "GAME_CATEGORY_DEBUG"
private const val START_TAG = "INVITE_START_ROOT_CAUSE"
private const val TEAM_RETRY_DELAY_MS = 250L

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
