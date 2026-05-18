package com.example.brainbrawl

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityEditarPerfilBinding
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils

class EditarPerfilActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditarPerfilBinding.inflate(layoutInflater) }
    private val jogadorRepository = JogadorRepository()
    private val authService = AuthService()
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var avatarSelecionadoIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid.orEmpty()
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR).orEmpty()
        binding.txtNomePerfil.text = nomeUtilizador.ifBlank { uid }

        val avatarResources = Array(12) { index ->
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(index))
        }
        binding.gridAvatars.adapter = AvatarGridAdapter(this, avatarResources)
        binding.imgAvatarAtual.setImageResource(avatarResources[avatarSelecionadoIndex])
        binding.gridAvatars.setOnItemClickListener { _, _, position, _ ->
            avatarSelecionadoIndex = position
            binding.imgAvatarAtual.setImageResource(avatarResources[position])
        }

        binding.btnVoltarEditarPerfil.setOnClickListener { finish() }
        binding.btnGuardarPerfil.setOnClickListener {
            guardarAvatar()
        }
    }

    private fun guardarAvatar() {
        val identificador = uid.ifBlank { nomeUtilizador }
        if (identificador.isBlank()) {
            Toast.makeText(this, R.string.perfil_indisponivel, Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnGuardarPerfil.isEnabled = false
        jogadorRepository.atualizarAvatar(identificador, AvatarUtils.nomeAvatarPorIndex(avatarSelecionadoIndex))
            .addOnSuccessListener {
                Toast.makeText(this, R.string.perfil_guardado, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { error ->
                binding.btnGuardarPerfil.isEnabled = true
                Toast.makeText(this, error.message ?: getString(R.string.erro_guardar_perfil), Toast.LENGTH_SHORT).show()
            }
    }
}
