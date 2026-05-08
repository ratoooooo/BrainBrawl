package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacao1x1Binding
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.repositories.HistoricoRepository
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.EstatisticasService.ResultadoJogador
import com.example.brainbrawl.viewmodels.Pontuacao1x1Event
import com.example.brainbrawl.viewmodels.Pontuacao1x1ViewModel

class Pontuacao1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPontuacao1x1Binding.inflate(layoutInflater)
    }
    private val pontuacaoRepository = PontuacaoRepository()
    private val historicoRepository = HistoricoRepository()
    private val authService = AuthService()
    private val viewModel by lazy {
        ViewModelProvider(this)[Pontuacao1x1ViewModel::class.java]
    }

    private lateinit var codigoSala: String
    private var uid: String = ""
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private var totalRespostasCertas: Int = 0
    private var totalPerguntas: Int = 8
    private var nomeCategoria: String = ""
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var avatar: String = ""
    private var isGuest: Boolean = false

    private var pontuacaoListener: PontuacaoRepository.ListenerHandle? = null
    private var jogadorAtualResultado: ResultadoJogador? = null
    private var historicoGuardado = false
    private var estatisticasAtualizadas = false
    private var navegouParaDesforra = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        totalRespostasCertas = intent.getIntExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, 0)
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 8)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        avatar = intent.getStringExtra(IntentExtras.AVATAR) ?: ""
        isGuest = intent.getBooleanExtra(IntentExtras.IS_GUEST, false) ||
            tipoJogador == GameConstants.TIPO_JOGADOR_GUEST ||
            uid.isBlank()

        configurarDesforra()
        viewModel.iniciar(codigoSala, nomeCategoria)
        carregarPontuacao1x1Realtime()

        binding.btnVoltar.setOnClickListener {
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador.ifBlank { null }, uid.ifBlank { null })
            finish()
        }

        binding.btnDesforra.setOnClickListener {
            atualizarJogadorDesforraAtual()
            viewModel.pedirDesforra()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pontuacaoRepository.removerListener(pontuacaoListener)
    }

    private fun configurarDesforra() {
        viewModel.estadoDesforra.observe(this) { estado ->
            binding.txtEstadoDesforra.text = estado.mensagem
            binding.txtEstadoDesforra.visibility = if (estado.mensagem.isBlank()) View.GONE else View.VISIBLE
            binding.btnDesforra.isEnabled = !estado.desforraPedida
        }

        viewModel.evento.observe(this) { evento ->
            when (evento) {
                is Pontuacao1x1Event.AbrirNovaSalaDesforra -> abrirSalaDesforra(evento.codigoSala)
                is Pontuacao1x1Event.MostrarMensagem -> Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
                null -> Unit
            }
            if (evento != null) viewModel.consumirEvento()
        }
    }

    private fun abrirSalaDesforra(novaSala: String) {
        if (navegouParaDesforra) return
        navegouParaDesforra = true
        val intent = Intent(this, SalaDeEspera1x1Activity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, novaSala)
        uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        adicionarExtrasMatchmaking(intent)
        startActivity(intent)
        finish()
    }

    private fun carregarPontuacao1x1Realtime() {
        pontuacaoListener = pontuacaoRepository.escutarPontuacoes1x1(
            codigoSala = codigoSala,
            onPontuacoes = { jogadores ->
                atualizarUiPontuacoes(jogadores)
                atualizarEstatisticasJogadorAtual(jogadores)
            },
            onErro = {
                Toast.makeText(this@Pontuacao1x1Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun atualizarUiPontuacoes(jogadores: List<ResultadoJogador>) {
        jogadorAtualResultado = jogadores.firstOrNull { jogadorAtualCorresponde(it) } ?: jogadorAtualResultado
        atualizarJogadorDesforraAtual()

        if (jogadores.isNotEmpty()) {
            binding.txtNomeJogador1.text = jogadores[0].nome
            binding.txtPontos1.text = jogadores[0].pontos.toInt().toString()
        }
        if (jogadores.size > 1) {
            binding.txtNomeJogador2.text = jogadores[1].nome
            binding.txtPontos2.text = jogadores[1].pontos.toInt().toString()
        }
        if (jogadores.size <= 1) {
            binding.txtNomeJogador2.text = "Aguardando adversário..."
            binding.txtPontos2.text = ""
        }
    }

    private fun atualizarEstatisticasJogadorAtual(jogadores: List<ResultadoJogador>) {
        if (!podeGravarPersistente() || jogadores.size < 2 || estatisticasAtualizadas) return
        val resultadosComRespostas = jogadores.map { jogador ->
            if (jogadorAtualCorresponde(jogador)) {
                jogador.copy(respostasCertas = totalRespostasCertas)
            } else {
                jogador
            }
        }

        guardarHistoricoSeNecessario(jogadores)
        estatisticasAtualizadas = true
        pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
            tipoSala = PontuacaoRepository.TipoSala.UM_CONTRA_UM,
            codigoSala = codigoSala,
            resultados = resultadosComRespostas,
            modo = EstatisticasService.Modo.UM_CONTRA_UM,
            totalPerguntas = totalPerguntas,
            jogadoresParaAtualizar = identificadoresJogadorAtual().toSet()
        ).addOnFailureListener {
            estatisticasAtualizadas = false
        }
    }

    private fun guardarHistoricoSeNecessario(jogadores: List<ResultadoJogador>) {
        if (historicoGuardado || !podeGravarPersistente() || jogadores.size < 2) return
        val atual = jogadores.firstOrNull { jogadorAtualCorresponde(it) } ?: return
        val outro = jogadores.firstOrNull { !jogadorAtualCorresponde(it) } ?: return

        historicoGuardado = true
        historicoRepository.guardarHistoricoUmaVez(
            uid = uid,
            historico = HistoricoJogo(
                historicoId = "${GameConstants.MODO_1X1}_$codigoSala",
                modo = GameConstants.MODO_1X1,
                codigoSala = codigoSala,
                nomeCategoria = nomeCategoria,
                pontuacao = atual.pontos,
                respostasCertas = totalRespostasCertas,
                totalPerguntas = totalPerguntas,
                venceu = atual.pontos > outro.pontos,
                empate = atual.pontos == outro.pontos,
                dataHora = System.currentTimeMillis(),
                jogadoresDaPartida = jogadores.map { it.nome }
            )
        ).addOnFailureListener {
            historicoGuardado = false
        }
    }

    private fun atualizarJogadorDesforraAtual() {
        val chave = chaveSalaAtual()
        if (chave.isBlank()) return
        viewModel.atualizarJogadorAtual(
            PontuacaoRepository.JogadorDesforra(
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
        )
    }

    private fun chaveSalaAtual(): String {
        return jogadorAtualResultado?.chave?.takeIf { it.isNotBlank() } ?: chavePrimariaAtual()
    }

    private fun chavePrimariaAtual(): String {
        return uid.ifBlank { playerKey.ifBlank { nomeJogador.ifBlank { nomeUtilizador } } }
    }

    private fun nomeDisplayAtual(): String {
        return nomeUtilizador.ifBlank { nomeJogador.ifBlank { uid.ifBlank { playerKey } } }
    }

    private fun jogadorAtualCorresponde(jogador: ResultadoJogador): Boolean {
        return identificadoresJogadorAtual().any { jogador.corresponde(it) }
    }

    private fun identificadoresJogadorAtual(): List<String> {
        return listOf(
            uid,
            playerKey,
            jogadorAtualResultado?.chave.orEmpty(),
            nomeUtilizador,
            nomeJogador,
            nomeDisplayAtual()
        ).filter { it.isNotBlank() }.distinct()
    }

    private fun podeGravarPersistente(): Boolean {
        return uid.isNotBlank() && !isGuest && tipoJogador != GameConstants.TIPO_JOGADOR_GUEST
    }

    private fun adicionarExtrasMatchmaking(intent: Intent) {
        playerKey.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.PLAYER_KEY, it) }
        tipoJogador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.TIPO_JOGADOR, it) }
        avatar.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.AVATAR, it) }
        intent.putExtra(IntentExtras.IS_GUEST, isGuest)
    }
}
