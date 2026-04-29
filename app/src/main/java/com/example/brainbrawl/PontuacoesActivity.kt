package com.example.brainbrawl

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacaoBinding
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.services.EstatisticasService

class PontuacoesActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityPontuacaoBinding.inflate(layoutInflater)
    }
    private val pontuacaoRepository = PontuacaoRepository()
    private val estatisticasService = EstatisticasService()
    private var pontuacoesListener: PontuacaoRepository.ListenerHandle? = null
    private var estatisticasAtualizadas = false
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeJogador: String
    private var totalPontos: Double = 0.0
    private var totalPerguntas: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: "Jogador"
        totalPontos = intent.getDoubleExtra(IntentExtras.TOTAL_PONTOS, 0.0)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 1)

        // Chamar a função para carregar pontuação da sala
        carregarPontuacaoSala()

        // Configurar o botao de voltar
        binding.btnVoltar.setOnClickListener {
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador)
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
                        0 -> { txtMedalha.text = "🥇"; txtMedalha.setTextColor("#FFC400".toColorInt()) }
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
                    estatisticasAtualizadas = true
                    pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
                        tipoSala = PontuacaoRepository.TipoSala.GRUPO,
                        codigoSala = codigoSala,
                        resultados = jogadores,
                        modo = EstatisticasService.Modo.SOLO,
                        totalPerguntas = totalPerguntas
                    )
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
}
