package com.example.brainbrawl

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMeuPerfilBinding
import com.example.brainbrawl.routes.BottomNavHelper
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.utils.BadgeGridRenderer
import com.example.brainbrawl.viewmodels.MeuPerfilUiState
import com.example.brainbrawl.viewmodels.MeuPerfilViewModel
import java.text.NumberFormat
import java.util.Locale

class MeuPerfilActivity : AppCompatActivity() {

    // Usa o mesmo binding/layout do perfil do amigo
    private val binding by lazy { ActivityMeuPerfilBinding.inflate(layoutInflater) }
    private val viewModel by lazy {
        ViewModelProvider(this)[MeuPerfilViewModel::class.java]
    }
    private val authService = AuthService()
    private var uidAtual: String = ""
    private var nomeUtilizadorAtual: String = ""
    private var nomeJogadorAtual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: ""
        uidAtual = uid
        nomeUtilizadorAtual = nomeUtilizador
        nomeJogadorAtual = nomeJogador
        val email = intent.getStringExtra(IntentExtras.EMAIL) ?: authService.utilizadorAtual()?.email ?: ""
        BottomNavHelper.instalar(this, BottomNavHelper.Item.PERFIL, uid, nomeUtilizador, nomeJogador, email)

        binding.btnVerConquistas.setOnClickListener {
            startActivity(Intent(this, ConquistasActivity::class.java).apply {
                putExtra(IntentExtras.UID, uidAtual)
                putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizadorAtual)
                putExtra(IntentExtras.NOME_JOGADOR, nomeJogadorAtual)
            })
        }
        val abrirEditorPerfil = {
            startActivity(Intent(this, EditarPerfilActivity::class.java).apply {
                putExtra(IntentExtras.UID, uidAtual)
                putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizadorAtual)
                putExtra(IntentExtras.NOME_JOGADOR, nomeJogadorAtual)
            })
        }
        binding.btnEditarPerfil.setOnClickListener { abrirEditorPerfil() }
        binding.cardEditarAvatar.setOnClickListener { abrirEditorPerfil() }
        binding.cardHistorico.setOnClickListener {
            startActivity(Intent(this, HistoricoActivity::class.java).apply {
                putExtra(IntentExtras.UID, uidAtual)
                putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizadorAtual)
                putExtra(IntentExtras.NOME_JOGADOR, nomeJogadorAtual)
            })
        }

        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            Toast.makeText(this, R.string.perfil_indisponivel, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.perfil.observe(this) { perfil ->
            mostrarPerfil(perfil)
        }
        viewModel.carregarPerfil(uid, nomeUtilizador)
    }

    private fun mostrarPerfil(perfil: MeuPerfilUiState) {
        binding.imgAvatarAmigo.setImageResource(AvatarUtils.resolverAvatar(this, perfil.avatar))

        binding.txtNomeAmigo.text = perfil.nome
        binding.txtPontuacao.text = perfil.rankingGlobal?.let {
            getString(R.string.ranking_global_posicao_format, it)
        } ?: getString(R.string.ranking_global_indisponivel)
        binding.txtRecordePontuacao.text = formatarNumero(perfil.recordePontuacao.toInt())
        binding.txtNivel.text = getString(R.string.perfil_nivel_titulo_format, perfil.nivel)
        binding.txtXpProgress.text = getString(
            R.string.xp_progress_format,
            perfil.xpNoNivelAtual,
            perfil.xpNecessarioProximoNivel
        )
        binding.progressXp.max = perfil.xpNecessarioProximoNivel.coerceAtLeast(1)
        binding.progressXp.progress = perfil.xpNoNivelAtual.coerceAtLeast(0)
        binding.txtTotalJogos.text = formatarNumero(perfil.totalJogos)
        binding.txtTotalVitorias.text = formatarNumero(perfil.totalVitorias)
        binding.txtTotalDerrotas.text = getString(R.string.total_de_derrotas_format, perfil.totalDerrotas)
        binding.txtTotalRespostasCertas.text = getString(
            R.string.total_de_respostas_certas_format,
            perfil.totalRespostasCertas
        )
        binding.txtTaxaVitoria.text = formatarPercentagem(perfil.taxaVitoria)
        binding.txtTaxaAcertos.text = formatarPercentagem(perfil.taxaAcertos)
        binding.txtXpTotal.text = getString(R.string.xp_total_format, perfil.xpTotal)
        binding.txtConquistasEstado.text = getString(R.string.melhores_conquistas_resumo)
        BadgeGridRenderer.renderMelhores(this, layoutInflater, binding.gridConquistas, perfil.badges)
    }

    private fun formatarNumero(valor: Int): String {
        return NumberFormat.getIntegerInstance(Locale("pt", "PT")).format(valor)
    }

    private fun formatarPercentagem(valor: Double): String {
        val percentagem = valor.takeIf { it.isFinite() }?.coerceIn(0.0, 100.0) ?: 0.0
        return if (percentagem % 1.0 == 0.0) {
            "${percentagem.toInt()}%"
        } else {
            String.format(Locale("pt", "PT"), "%.1f%%", percentagem)
        }
    }
}
