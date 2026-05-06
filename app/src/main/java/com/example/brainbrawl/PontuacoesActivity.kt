package com.example.brainbrawl

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacaoBinding
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.repositories.HistoricoRepository
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.services.EstatisticasService

class PontuacoesActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityPontuacaoBinding.inflate(layoutInflater)
    }
    private val pontuacaoRepository = PontuacaoRepository()
    private val historicoRepository = HistoricoRepository()
    private val estatisticasService = EstatisticasService()
    private val authService = AuthService()
    private var pontuacoesListener: PontuacaoRepository.ListenerHandle? = null
    private var estatisticasAtualizadas = false
    private var historicoGuardado = false
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeJogador: String
    private var uid: String = ""
    private var totalPontos: Double = 0.0
    private var totalPerguntas: Int = 1
    private var modoJogo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: "Jogador"
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        totalPontos = intent.getDoubleExtra(IntentExtras.TOTAL_PONTOS, 0.0)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 1)
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: ""

        // Chamar a função para carregar pontuação da sala
        carregarPontuacaoSala()

        // Configurar o botao de voltar
        binding.btnVoltar.setOnClickListener {
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador, uid.ifBlank { null })
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pontuacaoRepository.removerListener(pontuacoesListener)
    }

    // Função para carrear a pontuação da sala
    private fun carregarPontuacaoSala() {
        pontuacoesListener = pontuacaoRepository.escutarResultadosGrupo(
            codigoSala,
            onResultados = { resumo ->
                val jogadores = estatisticasService.ordenarPodio(resumo.jogadores)
                val mostrarMvp = resumo.completos
                val maxCertas = if (mostrarMvp) jogadores.maxOfOrNull { it.respostasCertas } ?: 0 else 0
                val mvps = jogadores.filter { it.respostasCertas == maxCertas && maxCertas > 0 }.map { it.nome }

                binding.layoutPodio.removeAllViews()
                if (resumo.totalJogadores == 0) {
                    mostrarMensagemPodio("Sem jogadores na sala.")
                    return@escutarResultadosGrupo
                }
                if (!resumo.completos) {
                    val total = resumo.totalJogadores.coerceAtLeast(1)
                    mostrarMensagemPodio("A aguardar resultados... ${resumo.resultadosGuardados}/$total")
                }
                val inflater = LayoutInflater.from(this@PontuacoesActivity)
                for ((index, jogador) in jogadores.withIndex()) {
                    val view = inflater.inflate(R.layout.item_podio, binding.layoutPodio, false)
                    val txtMedalha = view.findViewById<TextView>(R.id.txt_medalha)
                    val txtNome = view.findViewById<TextView>(R.id.txt_nome_jogador)
                    val txtPontos = view.findViewById<TextView>(R.id.txt_pontos)
                    when (index) {
                        0 -> { txtMedalha.text = "🥇"; txtMedalha.setTextColor("#D8A42F".toColorInt()) }
                        1 -> { txtMedalha.text = "🥈"; txtMedalha.setTextColor("#b0b0b0".toColorInt()) }
                        2 -> { txtMedalha.text = "🥉"; txtMedalha.setTextColor("#ad7e54".toColorInt()) }
                        else -> { txtMedalha.text = "${index+1}"; txtMedalha.setTextColor("#222".toColorInt()) }
                    }
                    val isMVP = mvps.contains(jogador.nome)
                    val mvpTag = if (isMVP) " 🏆 MVP" else ""
                    txtNome.text = jogador.nome + mvpTag
                    txtPontos.text = jogador.pontos.toInt().toString()
                    binding.layoutPodio.addView(view)
                }
                if (resumo.completos && jogadores.isEmpty()) {
                    mostrarMensagemPodio("Sem jogadores na sala.")
                }

                if (resumo.completos && !estatisticasAtualizadas) {
                    guardarHistoricoSeNecessario(jogadores)
                    estatisticasAtualizadas = true
                    pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
                        tipoSala = PontuacaoRepository.TipoSala.GRUPO,
                        codigoSala = codigoSala,
                        resultados = jogadores,
                        modo = EstatisticasService.Modo.SOLO,
                        totalPerguntas = totalPerguntas,
                        jogadoresParaAtualizar = identificadoresJogadorAtual().toSet()
                    ).addOnFailureListener {
                        estatisticasAtualizadas = false
                    }
                }
            },
            onErro = {
                binding.layoutPodio.removeAllViews()
                mostrarMensagemPodio("Erro ao carregar resultados")
            }
        )
    }

    private fun mostrarMensagemPodio(mensagem: String) {
        val textView = TextView(this@PontuacoesActivity)
        textView.text = mensagem
        textView.textSize = 16f
        textView.setTextColor(Color.BLACK)
        binding.layoutPodio.addView(textView)
    }

    private fun identificadoresJogadorAtual(): List<String> {
        return listOf(
            uid,
            nomeUtilizador,
            nomeJogador,
            nomeUtilizador.ifBlank { nomeJogador }
        ).filter { it.isNotBlank() }.distinct()
    }

    private fun guardarHistoricoSeNecessario(jogadores: List<EstatisticasService.ResultadoJogador>) {
        if (historicoGuardado || uid.isBlank() || jogadores.isEmpty()) return
        val resultadoAtual = jogadores.firstOrNull { jogador ->
            identificadoresJogadorAtual().any { jogador.corresponde(it) }
        } ?: return

        historicoGuardado = true
        val maxPontos = jogadores.maxOfOrNull { it.pontos } ?: resultadoAtual.pontos
        val empatadosNoTopo = jogadores.count { it.pontos == maxPontos }
        val empate = resultadoAtual.pontos == maxPontos && empatadosNoTopo > 1
        val venceu = resultadoAtual.pontos == maxPontos && !empate

        historicoRepository.guardarHistoricoUmaVez(
            uid = uid,
            historico = HistoricoJogo(
                historicoId = historicoId(),
                modo = modoJogo.ifBlank { "grupo" },
                codigoSala = codigoSala,
                nomeCategoria = nomeCategoria,
                pontuacao = resultadoAtual.pontos,
                respostasCertas = resultadoAtual.respostasCertas,
                totalPerguntas = totalPerguntas,
                venceu = venceu,
                empate = empate,
                dataHora = System.currentTimeMillis(),
                jogadoresDaPartida = jogadores.map { it.nome }
            )
        ).addOnFailureListener {
            historicoGuardado = false
        }
    }

    private fun historicoId(): String {
        return "${modoJogo.ifBlank { FirebasePaths.SALAS }}_$codigoSala"
    }
}
