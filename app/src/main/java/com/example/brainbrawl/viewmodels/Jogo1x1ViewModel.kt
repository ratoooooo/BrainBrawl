package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.models.Pergunta
import com.example.brainbrawl.repositories.JogoCompetitivoRepository
import com.example.brainbrawl.repositories.JogoCompetitivoRepository.ModoCompetitivo
import com.example.brainbrawl.services.ScoreCompetitivoService

class Jogo1x1ViewModel(
    private val jogoCompetitivoRepository: JogoCompetitivoRepository = JogoCompetitivoRepository(),
    private val scoreCompetitivoService: ScoreCompetitivoService = ScoreCompetitivoService()
) : ViewModel() {

    private val _pergunta = MutableLiveData<JogoCompetitivoPerguntaUiState>()
    val pergunta: LiveData<JogoCompetitivoPerguntaUiState> = _pergunta

    private val _evento = MutableLiveData<Jogo1x1Event?>()
    val evento: LiveData<Jogo1x1Event?> = _evento

    private val perguntas = mutableListOf<Pergunta>()
    private var codigoSala: String = ""
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var nomeJogador: String = ""
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()
    private var chaveJogador: String = ""
    private var categoria: String = ""
    private var perguntaAtualIndex = 0
    private var totalPontos = 0.0
    private var numeroPerguntasCertas = 0
    private var totalPerguntasRespondidas = 0
    private var totalPerguntascertas = 0
    private var bonus = 50
    private var serverTimeOffset: Long = 0L
    private var categoriaCompetitiva: Boolean = false

    private var offsetListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var podioListener: JogoCompetitivoRepository.ListenerHandle? = null

    fun iniciar(
        codigoSala: String,
        uid: String,
        nomeUtilizador: String,
        nomeJogador: String,
        playerKey: String = "",
        tipoJogador: String = "",
        avatar: String = "",
        categoriaPadrao: String,
        categoriaTodas: String
    ) {
        this.codigoSala = codigoSala
        this.uid = uid
        this.nomeUtilizador = nomeUtilizador
        this.nomeJogador = nomeJogador
        this.jogadorAtual = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador, playerKey, tipoJogador, avatar)
        this.chaveJogador = jogadorAtual.chaveSala

        observarOffsetServidor()
        jogoCompetitivoRepository.resolverJogador(ModoCompetitivo.UM_CONTRA_UM, codigoSala, jogadorAtual)
            .addOnSuccessListener { jogadorNaSala ->
                chaveJogador = jogadorNaSala.chave
                carregarCategoriaEContinuar(categoriaPadrao, categoriaTodas)
            }
            .addOnFailureListener {
                carregarCategoriaEContinuar(categoriaPadrao, categoriaTodas)
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
                bonus
            )
            bonusAplicado = resultadoPontuacao.bonusAplicado
            totalPontos += resultadoPontuacao.pontos
        } else if (numeroOpcao in 0..3) {
            numeroPerguntasCertas = 0
        }

        return JogoCompetitivoRespostaResultado(acertou, bonusAplicado)
    }

    fun avancarPergunta() {
        perguntaAtualIndex++
        prepararPerguntaAtual()
    }

    fun finalizarJogo() {
        jogoCompetitivoRepository.guardarPontuacao1x1(codigoSala, chaveJogador, totalPontos)
            .addOnSuccessListener {
                aguardarPodioCompleto()
            }
            .addOnFailureListener {
                _evento.value = Jogo1x1Event.ErroGuardarPontuacao
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
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun carregarOuCriarPerguntas(categoriaTodas: String) {
        jogoCompetitivoRepository.carregarOuCriarPerguntas(
            ModoCompetitivo.UM_CONTRA_UM,
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
            _evento.value = Jogo1x1Event.ErroPerguntas(mensagem)
        }
    }

    private fun carregarCategoriaEContinuar(categoriaPadrao: String, categoriaTodas: String) {
        jogoCompetitivoRepository.carregarNomeCategoria(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            categoriaPadrao
        ).addOnSuccessListener { nomeCategoria ->
            categoria = nomeCategoria
            verificarCompetitividadeECarregarPerguntas(categoriaTodas)
        }.addOnFailureListener {
            _evento.value = Jogo1x1Event.ErroLerCategoria
        }
    }

    private fun verificarCompetitividadeECarregarPerguntas(categoriaTodas: String) {
        jogoCompetitivoRepository.verificarCompetitividade(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .addOnSuccessListener { competitiva ->
                categoriaCompetitiva = competitiva
                carregarOuCriarPerguntas(categoriaTodas)
            }
            .addOnFailureListener {
                categoriaCompetitiva = false
                carregarOuCriarPerguntas(categoriaTodas)
            }
    }

    private fun prepararPerguntaAtual() {
        if (perguntaAtualIndex >= perguntas.size) {
            _evento.value = Jogo1x1Event.FinalizarJogo
            return
        }

        _pergunta.value = JogoCompetitivoPerguntaUiState(
            pergunta = perguntas[perguntaAtualIndex],
            indice = perguntaAtualIndex,
            totalPerguntas = perguntas.size
        )
        sincronizarInicioPergunta()
    }

    private fun sincronizarInicioPergunta() {
        jogoCompetitivoRepository.sincronizarInicioPergunta(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            perguntaAtualIndex,
            tempoServidorAtual()
        ).addOnSuccessListener { inicio ->
            _evento.value = Jogo1x1Event.IniciarCronometro(inicio)
        }.addOnFailureListener {
            _evento.value = Jogo1x1Event.IniciarCronometro(tempoServidorAtual())
        }
    }

    private fun aguardarPodioCompleto() {
        jogoCompetitivoRepository.removerListener(podioListener)
        podioListener = jogoCompetitivoRepository.escutarPodio1x1(
            codigoSala,
            onPodioCompleto = {
                _evento.value = Jogo1x1Event.AbrirPontuacoes(dadosPontuacao())
            },
            onAguardar = {
                _evento.value = Jogo1x1Event.AguardarAdversario
            }
        ) {
            _evento.value = Jogo1x1Event.ErroPodio
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

    private fun dadosPontuacao(): JogoCompetitivoPontuacaoDados {
        return JogoCompetitivoPontuacaoDados(
            codigoSala = codigoSala,
            modoJogo = GameConstants.MODO_1X1,
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
            equipa = null,
            categoriaCompetitiva = categoriaCompetitiva
        )
    }
}

sealed class Jogo1x1Event {
    data class IniciarCronometro(val horaInicio: Long) : Jogo1x1Event()
    data class ErroPerguntas(val mensagem: String) : Jogo1x1Event()
    data class AbrirPontuacoes(val dados: JogoCompetitivoPontuacaoDados) : Jogo1x1Event()
    data object ErroLerCategoria : Jogo1x1Event()
    data object ErroGuardarPontuacao : Jogo1x1Event()
    data object ErroPodio : Jogo1x1Event()
    data object AguardarAdversario : Jogo1x1Event()
    data object FinalizarJogo : Jogo1x1Event()
}
