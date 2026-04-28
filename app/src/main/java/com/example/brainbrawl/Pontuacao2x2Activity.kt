package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.databinding.ActivityPontuacaoMultiBinding
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.services.EstatisticasService
import com.google.firebase.database.FirebaseDatabase

class Pontuacao2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPontuacaoMultiBinding.inflate(layoutInflater)
    }
    private val database = FirebaseDatabase.getInstance().reference
    private val pontuacaoRepository = PontuacaoRepository()
    private val estatisticasService = EstatisticasService()

    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private lateinit var nomeCategoria: String
    private var equipa: String? = null
    private var totalPontos: Double = 0.0
    private var totalRespostasCertas: Int = 0

    private var pontuacaoListener: PontuacaoRepository.ListenerHandle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados pelo intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: nomeUtilizador
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: "Todas as categorias"
        equipa = intent.getStringExtra("equipa")
        totalPontos = intent.getDoubleExtra("totalPontos", 0.0)
        totalRespostasCertas = intent.getIntExtra("totalRespostasCertas", 0)

        // Carregar pontuação dos jogadores
        carregarPontuacao2x2Realtime()

        binding.btnVoltar.setOnClickListener {
            database.child("sala_2x2").child(codigoSala).removeValue()
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador.ifBlank { null })
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removerListenerPontuacao()
    }

    // Fu
    private fun removerListenerPontuacao() {
        pontuacaoRepository.removerListener(pontuacaoListener)
        pontuacaoListener = null
    }

    // Chamar a função para carregar pontuação em tempo real
    private fun carregarPontuacao2x2Realtime() {
        pontuacaoListener = pontuacaoRepository.escutarPontuacoes2x2(
            codigoSala = codigoSala,
            onPontuacoes = { resultado ->
                val podio2x2 = estatisticasService.ordenarPodio2x2(resultado.equipaA, resultado.equipaB)
                val podio = podio2x2.podio

                binding.txtNomeJogador1.text = podio.getOrNull(0)?.nome ?: ""
                binding.txtPontos1.text = podio.getOrNull(0)?.pontos?.toInt()?.toString() ?: ""
                binding.txtNomeJogador2.text = podio.getOrNull(1)?.nome ?: ""
                binding.txtPontos2.text = podio.getOrNull(1)?.pontos?.toInt()?.toString() ?: ""
                binding.txtNomeJogador3.text = podio.getOrNull(2)?.nome ?: ""
                binding.txtPontos3.text = podio.getOrNull(2)?.pontos?.toInt()?.toString() ?: ""
                binding.txtNomeJogador4.text = podio.getOrNull(3)?.nome ?: ""
                binding.txtPontos4.text = podio.getOrNull(3)?.pontos?.toInt()?.toString() ?: ""

                Toast.makeText(
                    this@Pontuacao2x2Activity,
                    estatisticasService.textoVencedor2x2(podio2x2.totalA, podio2x2.totalB),
                    Toast.LENGTH_SHORT
                ).show()

                if (podio.size < 4) {
                    Toast.makeText(this@Pontuacao2x2Activity, "Aguardando todos os jogadores terminarem...", Toast.LENGTH_SHORT).show()
                }

                val resultados = resultado.equipaA + resultado.equipaB
                mostrarNovoRecordSeAplicavel(resultados)
                pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
                    tipoSala = PontuacaoRepository.TipoSala.DOIS_CONTRA_DOIS,
                    codigoSala = codigoSala,
                    resultados = resultados,
                    modo = EstatisticasService.Modo.DOIS_CONTRA_DOIS,
                    totalPerguntas = 8
                )
            }
        )
    }

    private fun mostrarNovoRecordSeAplicavel(resultados: List<EstatisticasService.ResultadoJogador>) {
        val resultadoAtual = resultados.firstOrNull { it.nome == nomeUtilizador } ?: return
        pontuacaoRepository.obterPontuacaoGlobalJogador(nomeUtilizador)
            .addOnSuccessListener { pontuacaoGuardada ->
                if (resultadoAtual.pontos > pontuacaoGuardada) {
                    Toast.makeText(this@Pontuacao2x2Activity, "NOVO RECORD!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
