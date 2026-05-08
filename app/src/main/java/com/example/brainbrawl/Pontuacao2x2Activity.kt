package com.example.brainbrawl

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacaoMultiBinding
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.repositories.HistoricoRepository
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.services.EstatisticasService

class Pontuacao2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPontuacaoMultiBinding.inflate(layoutInflater)
    }
    private val pontuacaoRepository = PontuacaoRepository()
    private val historicoRepository = HistoricoRepository()
    private val estatisticasService = EstatisticasService()
    private val authService = AuthService()

    private lateinit var codigoSala: String
    private var uid: String = ""
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private lateinit var nomeCategoria: String
    private var equipa: String? = null
    private var totalRespostasCertas: Int = 0
    private var totalPerguntas: Int = 8
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var isGuest: Boolean = false

    private var pontuacaoListener: PontuacaoRepository.ListenerHandle? = null
    private var estatisticasAtualizadas = false
    private var historicoGuardado = false
    private var recordeConsultado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: "Todas as categorias"
        equipa = intent.getStringExtra(IntentExtras.EQUIPA)
        totalRespostasCertas = intent.getIntExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, 0)
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 8)
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        isGuest = intent.getBooleanExtra(IntentExtras.IS_GUEST, false) ||
            tipoJogador == GameConstants.TIPO_JOGADOR_GUEST ||
            uid.isBlank()

        carregarPontuacao2x2Realtime()

        binding.btnVoltar.setOnClickListener {
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador.ifBlank { null }, uid.ifBlank { null })
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removerListenerPontuacao()
    }

    private fun removerListenerPontuacao() {
        pontuacaoRepository.removerListener(pontuacaoListener)
        pontuacaoListener = null
    }

    private fun carregarPontuacao2x2Realtime() {
        pontuacaoListener = pontuacaoRepository.escutarPontuacoes2x2(
            codigoSala = codigoSala,
            onPontuacoes = { resultado ->
                val podio2x2 = estatisticasService.ordenarPodio2x2(resultado.equipaA, resultado.equipaB)
                val podio = podio2x2.podio
                val resultados = resultado.equipaA + resultado.equipaB
                val completo = podio.size >= 4

                renderizarPodio(podio)
                renderizarEstadoPontuacao(completo, podio.size, podio2x2.totalA, podio2x2.totalB)

                if (podeGravarPersistente() && completo && !recordeConsultado) {
                    recordeConsultado = true
                    mostrarNovoRecordSeAplicavel(resultados)
                }

                if (podeGravarPersistente() && completo && !estatisticasAtualizadas) {
                    guardarHistoricoSeNecessario(resultados, podio2x2.totalA, podio2x2.totalB)
                    estatisticasAtualizadas = true
                    pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
                        tipoSala = PontuacaoRepository.TipoSala.DOIS_CONTRA_DOIS,
                        codigoSala = codigoSala,
                        resultados = resultados,
                        modo = EstatisticasService.Modo.DOIS_CONTRA_DOIS,
                        totalPerguntas = totalPerguntas,
                        jogadoresParaAtualizar = identificadoresJogadorAtual().toSet()
                    ).addOnFailureListener {
                        estatisticasAtualizadas = false
                    }
                }
            }
        )
    }

    private fun renderizarPodio(podio: List<EstatisticasService.ResultadoJogador>) {
        binding.txtNomeJogador1.text = podio.getOrNull(0)?.nome ?: ""
        binding.txtPontos1.text = podio.getOrNull(0)?.pontos?.toInt()?.toString() ?: ""
        binding.txtNomeJogador2.text = podio.getOrNull(1)?.nome ?: ""
        binding.txtPontos2.text = podio.getOrNull(1)?.pontos?.toInt()?.toString() ?: ""
        binding.txtNomeJogador3.text = podio.getOrNull(2)?.nome ?: ""
        binding.txtPontos3.text = podio.getOrNull(2)?.pontos?.toInt()?.toString() ?: ""
        binding.txtNomeJogador4.text = podio.getOrNull(3)?.nome ?: ""
        binding.txtPontos4.text = podio.getOrNull(3)?.pontos?.toInt()?.toString() ?: ""
    }

    private fun renderizarEstadoPontuacao(
        completo: Boolean,
        jogadoresTerminados: Int,
        totalA: Double,
        totalB: Double
    ) {
        if (completo) {
            binding.txtResultado2x2.text = estatisticasService.textoVencedor2x2(totalA, totalB)
            binding.txtEstadoPontuacao.text = "Resultados finais completos"
        } else {
            binding.txtResultado2x2.text = ""
            binding.txtEstadoPontuacao.text = "A aguardar todos os jogadores terminarem... $jogadoresTerminados/4"
        }
    }

    private fun mostrarNovoRecordSeAplicavel(resultados: List<EstatisticasService.ResultadoJogador>) {
        val resultadoAtual = resultados.firstOrNull { resultado ->
            identificadoresJogadorAtual().any { resultado.corresponde(it) }
        } ?: return
        pontuacaoRepository.obterRecordePontuacaoJogador(uid)
            .addOnSuccessListener { recordeGuardado ->
                if (resultadoAtual.pontos > recordeGuardado) {
                    Toast.makeText(this@Pontuacao2x2Activity, "NOVO RECORD!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun identificadoresJogadorAtual(): List<String> {
        return listOf(
            uid,
            playerKey,
            nomeUtilizador,
            nomeJogador,
            nomeUtilizador.ifBlank { nomeJogador }
        ).filter { it.isNotBlank() }.distinct()
    }

    private fun guardarHistoricoSeNecessario(
        resultados: List<EstatisticasService.ResultadoJogador>,
        totalA: Double,
        totalB: Double
    ) {
        if (historicoGuardado || !podeGravarPersistente() || resultados.size < 4) return
        val resultadoAtual = resultados.firstOrNull { resultado ->
            identificadoresJogadorAtual().any { resultado.corresponde(it) }
        } ?: return

        val equipaAtual = resultadoAtual.equipa ?: equipa.orEmpty()
        val empate = totalA == totalB
        val venceu = when (equipaAtual) {
            GameConstants.EQUIPA_A -> totalA > totalB
            GameConstants.EQUIPA_B -> totalB > totalA
            else -> false
        }

        historicoGuardado = true
        historicoRepository.guardarHistoricoUmaVez(
            uid = uid,
            historico = HistoricoJogo(
                historicoId = "${GameConstants.MODO_2X2}_$codigoSala",
                modo = GameConstants.MODO_2X2,
                codigoSala = codigoSala,
                nomeCategoria = nomeCategoria,
                pontuacao = resultadoAtual.pontos,
                respostasCertas = resultadoAtual.respostasCertas.ifZero(totalRespostasCertas),
                totalPerguntas = totalPerguntas,
                venceu = venceu,
                empate = empate,
                equipa = equipaAtual,
                dataHora = System.currentTimeMillis(),
                jogadoresDaPartida = resultados.map { jogador ->
                    jogador.equipa?.let { "${jogador.nome} ($it)" } ?: jogador.nome
                }
            )
        ).addOnFailureListener {
            historicoGuardado = false
        }
    }

    private fun podeGravarPersistente(): Boolean {
        return uid.isNotBlank() && !isGuest && tipoJogador != GameConstants.TIPO_JOGADOR_GUEST
    }

    private fun Int.ifZero(fallback: Int): Int {
        return if (this == 0) fallback else this
    }
}
