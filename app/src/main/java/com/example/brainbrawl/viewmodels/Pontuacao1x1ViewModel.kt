package com.example.brainbrawl.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.repositories.HistoricoRepository
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.EstatisticasService.ResultadoJogador

class Pontuacao1x1ViewModel(
    private val pontuacaoRepository: PontuacaoRepository = PontuacaoRepository(),
    private val historicoRepository: HistoricoRepository = HistoricoRepository()
) : ViewModel() {

    private val _pontuacaoUiState = MutableLiveData(Pontuacao1x1UiState())
    val pontuacaoUiState: LiveData<Pontuacao1x1UiState> = _pontuacaoUiState

    private val _estadoDesforra = MutableLiveData<Pontuacao1x1DesforraUiState>()
    val estadoDesforra: LiveData<Pontuacao1x1DesforraUiState> = _estadoDesforra

    private val _evento = MutableLiveData<Pontuacao1x1Event?>()
    val evento: LiveData<Pontuacao1x1Event?> = _evento

    private var codigoSala: String = ""
    private var nomeCategoria: String = ""
    private var inputPontuacao: Pontuacao1x1Input? = null
    private var jogadorAtual: PontuacaoRepository.JogadorDesforra? = null
    private var pontuacaoListener: PontuacaoRepository.ListenerHandle? = null
    private var novaSalaListener: PontuacaoRepository.ListenerHandle? = null
    private var desforraListener: PontuacaoRepository.ListenerHandle? = null
    private var jogadorAtualResultado: ResultadoJogador? = null
    private var historicoGuardado = false
    private var estatisticasAtualizadas = false
    private var aCriarDesforra = false
    private var navegacaoEmitida = false

    fun iniciar(codigoSala: String, nomeCategoria: String) {
        if (this.codigoSala == codigoSala) return
        this.codigoSala = codigoSala
        this.nomeCategoria = nomeCategoria
        _estadoDesforra.value = Pontuacao1x1DesforraUiState()
        observarNovaSala()
    }

    fun iniciarPontuacao(input: Pontuacao1x1Input) {
        inputPontuacao = input
        historicoGuardado = false
        estatisticasAtualizadas = false
        jogadorAtualResultado = null
        iniciar(input.codigoSala, input.nomeCategoria)

        pontuacaoRepository.removerListener(pontuacaoListener)
        pontuacaoListener = pontuacaoRepository.escutarPontuacoes1x1(
            codigoSala = input.codigoSala,
            onPontuacoes = { jogadores ->
                atualizarPontuacoes(input, jogadores)
                atualizarPersistencia(input, jogadores)
            },
            onErro = {
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao carregar pontuação")
            }
        )
    }

    fun atualizarJogadorAtual(jogador: PontuacaoRepository.JogadorDesforra) {
        val mudou = jogadorAtual?.chave != jogador.chave
        jogadorAtual = jogador
        if (mudou) {
            observarDesforras()
        }
    }

    fun pedirDesforra() {
        val jogador = jogadorAtual ?: run {
            _evento.value = Pontuacao1x1Event.MostrarMensagem("Ainda a carregar dados da partida.")
            return
        }
        _estadoDesforra.value = Pontuacao1x1DesforraUiState("A aguardar adversário...", desforraPedida = true)
        pontuacaoRepository.marcarDesforra1x1(codigoSala, jogador.chave)
            .addOnFailureListener {
                _estadoDesforra.value = Pontuacao1x1DesforraUiState()
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao pedir desforra.")
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }

    fun removerListeners() {
        pontuacaoRepository.removerListener(pontuacaoListener)
        pontuacaoListener = null
        pontuacaoRepository.removerListener(novaSalaListener)
        novaSalaListener = null
        pontuacaoRepository.removerListener(desforraListener)
        desforraListener = null
    }

    override fun onCleared() {
        removerListeners()
        super.onCleared()
    }

    private fun observarNovaSala() {
        pontuacaoRepository.removerListener(novaSalaListener)
        novaSalaListener = pontuacaoRepository.escutarNovaSalaDesforra1x1(
            codigoSala = codigoSala,
            onNovaSala = { novaSala ->
                emitirAbrirSala(novaSala)
            },
            onErro = {
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao observar desforra.")
            }
        )
    }

    private fun observarDesforras() {
        val jogador = jogadorAtual ?: return
        pontuacaoRepository.removerListener(desforraListener)
        desforraListener = pontuacaoRepository.escutarDesforra1x1(
            codigoSala = codigoSala,
            chaveJogadorAtual = jogador.chave,
            onAdversarioAceitou = { adversario ->
                criarDesforra(jogador, adversario)
            },
            onAguardar = {
                _estadoDesforra.value = Pontuacao1x1DesforraUiState("A aguardar adversário...", desforraPedida = true)
            },
            onErro = {
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao observar pedidos de desforra.")
            }
        )
    }

    private fun criarDesforra(
        jogador: PontuacaoRepository.JogadorDesforra,
        adversario: PontuacaoRepository.JogadorDesforra
    ) {
        if (aCriarDesforra || navegacaoEmitida) return
        aCriarDesforra = true
        val origem = if (inputPontuacao?.categoriaCompetitiva == true) {
            GameConstants.ORIGEM_CATEGORIA_OFICIAL
        } else {
            // Se não for competitivo, assumimos que é pública ou personalizada.
            // Para simplificar, usamos pública se não soubermos.
            GameConstants.ORIGEM_CATEGORIA_PUBLICA 
        }
        _estadoDesforra.value = Pontuacao1x1DesforraUiState("Desforra aceite. A criar nova sala...", desforraPedida = true)
        pontuacaoRepository.criarOuObterSalaDesforra1x1(codigoSala, jogador, adversario, nomeCategoria, origem)
            .addOnSuccessListener { novaSala ->
                aCriarDesforra = false
                emitirAbrirSala(novaSala)
            }
            .addOnFailureListener {
                aCriarDesforra = false
                _evento.value = Pontuacao1x1Event.MostrarMensagem("Erro ao criar desforra.")
            }
    }

    private fun emitirAbrirSala(codigoNovaSala: String) {
        if (navegacaoEmitida) return
        navegacaoEmitida = true
        _evento.value = Pontuacao1x1Event.AbrirNovaSalaDesforra(codigoNovaSala)
    }

    private fun atualizarPontuacoes(input: Pontuacao1x1Input, jogadores: List<ResultadoJogador>) {
        jogadorAtualResultado = jogadores.firstOrNull { input.correspondeAoJogadorAtual(it, jogadorAtualResultado) }
            ?: jogadorAtualResultado
        atualizarJogadorAtual(input.toJogadorDesforra(jogadorAtualResultado))

        val podio = listOf(
            PontuacaoJogadorUi(
                nome = jogadores.getOrNull(0)?.nome.orEmpty(),
                pontos = jogadores.getOrNull(0)?.pontos?.toInt()?.toString().orEmpty()
            ),
            PontuacaoJogadorUi(
                nome = jogadores.getOrNull(1)?.nome ?: "Aguardando adversário...",
                pontos = jogadores.getOrNull(1)?.pontos?.toInt()?.toString().orEmpty()
            )
        )

        _pontuacaoUiState.value = Pontuacao1x1UiState(
            podio = podio,
            aguardandoAdversario = jogadores.size <= 1
        )
    }

    private fun atualizarPersistencia(input: Pontuacao1x1Input, jogadores: List<ResultadoJogador>) {
        if (jogadores.size < 2) {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip 1x1 persistence: waiting for opponent uid=${input.uid} room=${input.codigoSala} players=${jogadores.size}"
            )
            return
        }

        val resultadosComRespostas = jogadores.map { jogador ->
            if (input.correspondeAoJogadorAtual(jogador, jogadorAtualResultado)) {
                jogador.copy(respostasCertas = input.totalRespostasCertas)
            } else {
                jogador
            }
        }

        guardarHistoricoSeNecessario(input, jogadores)
        if (!input.podeGravarPersistente() || estatisticasAtualizadas) return
        if (!input.categoriaCompetitiva) {
            estatisticasAtualizadas = true
            return
        }

        estatisticasAtualizadas = true
        pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
            tipoSala = PontuacaoRepository.TipoSala.UM_CONTRA_UM,
            codigoSala = input.codigoSala,
            resultados = resultadosComRespostas,
            modo = EstatisticasService.Modo.UM_CONTRA_UM,
            totalPerguntas = input.totalPerguntas,
            jogadoresParaAtualizar = input.identificadoresJogadorAtual(jogadorAtualResultado).toSet()
        ).addOnFailureListener {
            estatisticasAtualizadas = false
        }
    }

    private fun guardarHistoricoSeNecessario(input: Pontuacao1x1Input, jogadores: List<ResultadoJogador>) {
        if (historicoGuardado) return
        if (!input.podeGravarHistorico()) {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip 1x1 history: invalid persistent identity uid=${input.uid} playerKey=${input.playerKey} " +
                    "isGuest=${input.isGuest} tipo=${input.tipoJogador} room=${input.codigoSala}"
            )
            return
        }
        if (jogadores.size < 2) return
        val atual = jogadores.firstOrNull { input.correspondeAoJogadorAtual(it, jogadorAtualResultado) } ?: run {
            Log.d(
                HISTORY_DEBUG_TAG,
                "skip 1x1 history: current player not in results uid=${input.uid} playerKey=${input.playerKey} room=${input.codigoSala}"
            )
            return
        }
        val outro = jogadores.firstOrNull { !input.correspondeAoJogadorAtual(it, jogadorAtualResultado) } ?: return

        historicoGuardado = true
        Log.d(
            HISTORY_DEBUG_TAG,
            "saving 1x1 history uid=${input.uid} playerKey=${input.playerKey} category=${input.nomeCategoria} " +
                "competitivo=${input.categoriaCompetitiva} room=${input.codigoSala}"
        )
        historicoRepository.guardarHistoricoUmaVez(
            uid = input.uid,
            historico = HistoricoJogo(
                historicoId = "${GameConstants.MODO_1X1}_${input.codigoSala}",
                modo = GameConstants.MODO_1X1,
                codigoSala = input.codigoSala,
                nomeCategoria = input.nomeCategoria,
                pontuacao = atual.pontos,
                respostasCertas = input.totalRespostasCertas,
                totalPerguntas = input.totalPerguntas,
                venceu = atual.pontos > outro.pontos,
                empate = atual.pontos == outro.pontos,
                competitivo = input.categoriaCompetitiva,
                dataHora = System.currentTimeMillis(),
                jogadoresDaPartida = jogadores.map { it.nome }
            )
        ).addOnFailureListener {
            historicoGuardado = false
        }
    }
}

data class Pontuacao1x1Input(
    val codigoSala: String,
    val uid: String,
    val nomeUtilizador: String,
    val nomeJogador: String,
    val totalRespostasCertas: Int,
    val totalPerguntas: Int,
    val nomeCategoria: String,
    val playerKey: String,
    val tipoJogador: String,
    val avatar: String,
    val isGuest: Boolean,
    val categoriaCompetitiva: Boolean = true
) {
    fun podeGravarPersistente(): Boolean {
        return podeGravarHistorico()
    }

    fun podeGravarHistorico(): Boolean {
        return uid.isNotBlank() &&
            !uid.startsWith("guest_") &&
            !isGuest &&
            tipoJogador != GameConstants.TIPO_JOGADOR_GUEST
    }

    fun correspondeAoJogadorAtual(jogador: ResultadoJogador, resultadoAtual: ResultadoJogador?): Boolean {
        return identificadoresJogadorAtual(resultadoAtual).any { jogador.corresponde(it) }
    }

    fun identificadoresJogadorAtual(resultadoAtual: ResultadoJogador?): List<String> {
        return listOf(
            uid,
            playerKey,
            resultadoAtual?.chave.orEmpty(),
            nomeUtilizador,
            nomeJogador,
            nomeDisplayAtual()
        ).filter { it.isNotBlank() }.distinct()
    }

    fun toJogadorDesforra(resultadoAtual: ResultadoJogador?): PontuacaoRepository.JogadorDesforra {
        val chave = resultadoAtual?.chave?.takeIf { it.isNotBlank() } ?: chavePrimariaAtual()
        return PontuacaoRepository.JogadorDesforra(
            chave = chave,
            nomeDisplay = nomeDisplayAtual(),
            uid = uid,
            nomeUtilizador = nomeUtilizador,
            nomeJogador = nomeJogador,
            playerKey = playerKey.ifBlank { chave },
            tipoJogador = tipoJogador.ifBlank {
                if (isGuest) GameConstants.TIPO_JOGADOR_GUEST else GameConstants.TIPO_JOGADOR_AUTH
            },
            avatar = avatar
        )
    }

    private fun chavePrimariaAtual(): String {
        return uid.ifBlank { playerKey.ifBlank { nomeJogador.ifBlank { nomeUtilizador } } }
    }

    private fun nomeDisplayAtual(): String {
        return nomeUtilizador.ifBlank { nomeJogador.ifBlank { uid.ifBlank { playerKey } } }
    }
}

data class PontuacaoJogadorUi(
    val nome: String = "",
    val pontos: String = ""
)

data class Pontuacao1x1UiState(
    val podio: List<PontuacaoJogadorUi> = listOf(PontuacaoJogadorUi(), PontuacaoJogadorUi()),
    val aguardandoAdversario: Boolean = true
)

data class Pontuacao1x1DesforraUiState(
    val mensagem: String = "",
    val desforraPedida: Boolean = false
)

sealed class Pontuacao1x1Event {
    data class AbrirNovaSalaDesforra(val codigoSala: String) : Pontuacao1x1Event()
    data class MostrarMensagem(val mensagem: String) : Pontuacao1x1Event()
}

private const val HISTORY_DEBUG_TAG = "HISTORY_DEBUG"
