package com.example.brainbrawl

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityConquistasBinding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.BadgeGridRenderer
import com.example.brainbrawl.viewmodels.MeuPerfilViewModel
import com.example.brainbrawl.viewmodels.PerfilAmigoViewModel

class ConquistasActivity : AppCompatActivity() {
    private val binding by lazy { ActivityConquistasBinding.inflate(layoutInflater) }
    private val authService = AuthService()
    private val meuPerfilViewModel by lazy {
        ViewModelProvider(this)[MeuPerfilViewModel::class.java]
    }
    private val perfilAmigoViewModel by lazy {
        ViewModelProvider(this)[PerfilAmigoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnVoltarConquistas.setOnClickListener { finish() }

        val uidAmigo = intent.getStringExtra(IntentExtras.UID_AMIGO).orEmpty()
        val nomeAmigo = intent.getStringExtra(IntentExtras.NOME_AMIGO).orEmpty()
        if (uidAmigo.isNotBlank() || nomeAmigo.isNotBlank()) {
            carregarConquistasAmigo(uidAmigo, nomeAmigo)
        } else {
            carregarConquistasProprias()
        }
    }

    private fun carregarConquistasProprias() {
        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid.orEmpty()
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR).orEmpty()
        meuPerfilViewModel.perfil.observe(this) { perfil ->
            binding.txtSubtituloConquistas.text = getString(R.string.conquistas_todas_subtitulo)
            BadgeGridRenderer.render(this, layoutInflater, binding.gridConquistas, perfil.badges)
        }
        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            Toast.makeText(this, R.string.perfil_indisponivel, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        meuPerfilViewModel.carregarPerfil(uid, nomeUtilizador)
    }

    private fun carregarConquistasAmigo(uidAmigo: String, nomeAmigo: String) {
        perfilAmigoViewModel.perfil.observe(this) { perfil ->
            binding.txtSubtituloConquistas.text = getString(R.string.conquistas_amigo_publicas)
            BadgeGridRenderer.render(this, layoutInflater, binding.gridConquistas, perfil.badges)
        }
        perfilAmigoViewModel.carregarPerfil(uidAmigo.ifBlank { nomeAmigo }, nomeAmigo)
    }
}
