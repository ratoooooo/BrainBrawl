package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPerfilAmigoBinding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.utils.BadgeGridRenderer
import com.example.brainbrawl.utils.UteisConquistas
import com.example.brainbrawl.viewmodels.PerfilAmigoEvent
import com.example.brainbrawl.viewmodels.PerfilAmigoUiState
import com.example.brainbrawl.viewmodels.PerfilAmigoViewModel

class PerfilAmigoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPerfilAmigoBinding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[PerfilAmigoViewModel::class.java]
    }
    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pelo Intent
        val nomeAmigo = intent.getStringExtra(IntentExtras.NOME_AMIGO) ?: "Amigo Desconhecido"
        val uidAmigo = intent.getStringExtra(IntentExtras.UID_AMIGO) ?: ""
        val uidUtilizador = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""

        binding.btnVoltarPerfil.setOnClickListener {
            val intent = Intent(this, AmigosActivity::class.java)
            intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
            uidUtilizador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
            startActivity(intent)
            finish()
        }

        viewModel.perfil.observe(this) { perfil ->
            mostrarPerfil(perfil, uidUtilizador, nomeUtilizador)
        }
        viewModel.evento.observe(this) { evento ->
            if (evento == PerfilAmigoEvent.AmigoRemovido) {
                Toast.makeText(this, R.string.amigo_removido_sucesso, Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AmigosActivity::class.java)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                uidUtilizador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                startActivity(intent)
                viewModel.consumirEvento()
                finish()
            }
        }
        viewModel.carregarPerfil(uidAmigo.ifBlank { nomeAmigo }, nomeAmigo)
    }

    private fun mostrarPerfil(perfil: PerfilAmigoUiState, uidUtilizador: String, nomeUtilizador: String) {
        if (!perfil.perfilExiste) {
            binding.imgAvatarAmigo.setImageResource(AvatarUtils.resolverAvatar(this, null))
            binding.imgTotalJogos.visibility = View.GONE
            binding.imgTotalVitorias.visibility = View.GONE
            binding.imgTotalRespostasCertas.visibility = View.GONE
            binding.gridConquistas.removeAllViews()
            binding.txtNomeAmigo.text = perfil.nome
            binding.txtPontuacao.text = getString(R.string.pontuacao_format, 0)
            binding.txtTotalJogos.text = getString(R.string.total_de_jogos_format, 0)
            binding.txtTotalVitorias.text = getString(R.string.total_de_vitorias_format, 0)
            binding.txtTaxaAcertos.text = getString(R.string.taxa_de_acertos_format, 0.0)
            return
        }

        // Atualizar os badges de conquistas
        UteisConquistas.obterBadgePartidasJogadas(resources, packageName, perfil.totalJogos)?.let {
            binding.imgTotalJogos.visibility = View.VISIBLE
            binding.imgTotalJogos.setImageResource(it)
        } ?: run {
            binding.imgTotalJogos.visibility = View.GONE
        }

        UteisConquistas.obterBadgeVitorias(resources, packageName, perfil.totalVitorias)?.let {
            binding.imgTotalVitorias.visibility = View.VISIBLE
            binding.imgTotalVitorias.setImageResource(it)
        } ?: run {
            binding.imgTotalVitorias.visibility = View.GONE
        }

        UteisConquistas.obterBadgeRespostasCertas(resources, packageName, perfil.totalRespostasCertas)?.let {
            binding.imgTotalRespostasCertas.visibility = View.VISIBLE
            binding.imgTotalRespostasCertas.setImageResource(it)
        } ?: run {
            binding.imgTotalRespostasCertas.visibility = View.GONE
        }

        binding.imgAvatarAmigo.setImageResource(AvatarUtils.resolverAvatar(this, perfil.avatar))

        // Mostrar os dados do amigo no layout
        binding.txtNomeAmigo.text = perfil.nome
        binding.txtPontuacao.text = getString(R.string.pontuacao_format, perfil.pontuacao.toInt())
        binding.txtTotalJogos.text = getString(R.string.total_de_jogos_format, perfil.totalJogos)
        binding.txtTotalVitorias.text = getString(R.string.total_de_vitorias_format, perfil.totalVitorias)
        binding.txtTaxaAcertos.text = getString(R.string.taxa_de_acertos_format, perfil.taxaAcertos)
        BadgeGridRenderer.render(this, layoutInflater, binding.gridConquistas, perfil.badges)

        if (perfil.perfilExiste) {
            binding.btnRemoverAmigo.setOnClickListener {
                viewModel.removerAmigo(uidUtilizador, nomeUtilizador, perfil.utilizador)
                binding.btnRemoverAmigo.isEnabled = false
            }
        }
    }
}
