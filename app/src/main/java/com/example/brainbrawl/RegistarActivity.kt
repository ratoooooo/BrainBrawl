package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityRegistarBinding
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.viewmodels.RegistarEvent
import com.example.brainbrawl.viewmodels.RegistarViewModel

class RegistarActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityRegistarBinding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[RegistarViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        var avatarSelecionadoIndex = 0

        configurarObservers()
        configurarPasswordToggles()

        // Varivel para armazenar os avatares
        val avatarResources = arrayOf(
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(0)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(1)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(2)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(3)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(4)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(5)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(6)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(7)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(8)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(9)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(10)),
            AvatarUtils.resolverAvatar(this, AvatarUtils.nomeAvatarPorIndex(11))
        )

        // Adapter para o GridView
        val gridAdapter = AvatarGridAdapter(this, avatarResources)
        binding.gridAvatars.adapter = gridAdapter

        // Inicializa o avatar selecionado
        binding.imgAvatarSelecionado.setImageResource(avatarResources[avatarSelecionadoIndex])

        // Seleção do avatar na grelha
        binding.gridAvatars.setOnItemClickListener { _, _, position, _ ->
            avatarSelecionadoIndex = position
            binding.imgAvatarSelecionado.setImageResource(avatarResources[position])
        }

        // Configurar botão de registo
        binding.btnContinuarRegisto.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPasswordJogador.text.toString()
            val confirmarPassword = binding.edtConfirmarPassword.text.toString()

            val erro = validarEtapaConta(email, password, confirmarPassword)
            if (erro == null) {
                mostrarEtapaPerfil()
            } else {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegistar.setOnClickListener {
            // GGuardar os dados inseridos nos campos de texto
            val nomeUtilizador = binding.edtNomeJogador.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPasswordJogador.text.toString()
            val confirmarPassword = binding.edtConfirmarPassword.text.toString()
            viewModel.registar(nomeUtilizador, email, password, confirmarPassword, avatarSelecionadoIndex)
        }

        binding.btnVoltarEtapa.setOnClickListener {
            mostrarEtapaConta()
        }

        binding.txtLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Configurar botão de voltar
        binding.btnVoltar.setOnClickListener {
            if (binding.pagePerfil.visibility == View.VISIBLE) {
                mostrarEtapaConta()
            } else {
                // Abrir LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }

    private fun configurarPasswordToggles() {
        var passwordVisivel = false
        var confirmarPasswordVisivel = false
        binding.btnTogglePassword.setOnClickListener {
            passwordVisivel = !passwordVisivel
            alternarPassword(binding.edtPasswordJogador, passwordVisivel)
        }
        binding.btnToggleConfirmarPassword.setOnClickListener {
            confirmarPasswordVisivel = !confirmarPasswordVisivel
            alternarPassword(binding.edtConfirmarPassword, confirmarPasswordVisivel)
        }
    }

    private fun alternarPassword(editText: EditText, visivel: Boolean) {
        editText.inputType = InputType.TYPE_CLASS_TEXT or if (visivel) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun validarEtapaConta(email: String, password: String, confirmarPassword: String): String? {
        if (email.isBlank()) return getString(R.string.insere_email)
        if (!email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) return getString(R.string.email_invalido)
        if (password.isBlank()) return getString(R.string.insere_password)
        if (confirmarPassword.isBlank()) return getString(R.string.confirma_password)
        if (password != confirmarPassword) return getString(R.string.passwords_nao_coincidem)
        if (password.length < 8 || password.length > 20) return getString(R.string.password_tamanho_invalido)
        if (!password.any { it.isUpperCase() }) return getString(R.string.password_precisa_maiuscula)
        if (!password.any { it.isLowerCase() }) return getString(R.string.password_precisa_minuscula)
        if (!password.any { it.isDigit() || !it.isLetterOrDigit() }) {
            return getString(R.string.password_precisa_numero_simbolo)
        }
        return null
    }

    private fun mostrarEtapaConta() {
        binding.pageConta.visibility = View.VISIBLE
        binding.pagePerfil.visibility = View.GONE
        binding.txtStepConta.setBackgroundResource(R.drawable.bg_register_step_active)
        binding.txtStepConta.setTextColor(getColor(R.color.bb_text_primary))
        binding.txtStepPerfil.setBackgroundResource(R.drawable.bg_register_step_idle)
        binding.txtStepPerfil.setTextColor(getColor(R.color.bb_text_secondary))
        binding.txtRegisto.text = getString(R.string.cria_tua_conta)
        binding.txtRegistoSubtitulo.text = getString(R.string.registo_subtitulo)
        binding.registerScroll.post { binding.registerScroll.smoothScrollTo(0, 0) }
    }

    private fun mostrarEtapaPerfil() {
        binding.pageConta.visibility = View.GONE
        binding.pagePerfil.visibility = View.VISIBLE
        binding.txtStepConta.setBackgroundResource(R.drawable.bg_register_step_idle)
        binding.txtStepConta.setTextColor(getColor(R.color.bb_text_secondary))
        binding.txtStepPerfil.setBackgroundResource(R.drawable.bg_register_step_active)
        binding.txtStepPerfil.setTextColor(getColor(R.color.bb_text_primary))
        binding.txtRegisto.text = getString(R.string.personaliza_perfil)
        binding.txtRegistoSubtitulo.text = getString(R.string.perfil_subtitulo)
        binding.registerScroll.post { binding.registerScroll.smoothScrollTo(0, 0) }
    }

    private fun configurarObservers() {
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun tratarEvento(evento: RegistarEvent) {
        when (evento) {
            is RegistarEvent.ValidacaoFalhou -> {
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
            }
            RegistarEvent.JogadorJaExiste -> {
                Toast.makeText(this, R.string.jogador_ja_existe, Toast.LENGTH_SHORT).show()
            }
            is RegistarEvent.ErroVerificarJogador -> {
                Toast.makeText(this, getString(R.string.erro_verificar_jogador_format, evento.mensagem), Toast.LENGTH_SHORT).show()
            }
            is RegistarEvent.ErroCriarJogador -> {
                Toast.makeText(this, getString(R.string.erro_criar_jogador_format, evento.mensagem), Toast.LENGTH_SHORT).show()
            }
            is RegistarEvent.ErroCriarAuth -> {
                Toast.makeText(this, getString(R.string.erro_criar_conta_format, evento.mensagem), Toast.LENGTH_SHORT).show()
            }
            is RegistarEvent.RegistoSucesso -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, evento.nomeUtilizador)
                intent.putExtra(IntentExtras.UID, evento.uid)
                intent.putExtra(IntentExtras.EMAIL, evento.email)
                startActivity(intent)
                finish()
            }
        }
    }
}
