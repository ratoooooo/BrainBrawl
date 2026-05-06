package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityRegistarBinding
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

        // Varivel para armazenar os avatares
        val avatarResources = arrayOf(
            R.drawable.avatar_1_playstore,
            R.drawable.avatar_2_playstore,
            R.drawable.avatar_3_playstore,
            R.drawable.avatar_4_playstore,
            R.drawable.avatar_5_playstore,
            R.drawable.avatar_6_playstore,
            R.drawable.avatar_7_playstore,
            R.drawable.avatar_8_playstore,
            R.drawable.avatar_9_playstore,
            R.drawable.avatar_10_playstore,
            R.drawable.avatar_11_playstore,
            R.drawable.avatar_12_playstore
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
            val password = binding.edtPasswordJogador.text.toString().trim()
            val confirmarPassword = binding.edtConfirmarPassword.text.toString().trim()

            when {
                email.isBlank() -> Toast.makeText(this, "Insere o e-mail", Toast.LENGTH_SHORT).show()
                password.isBlank() -> Toast.makeText(this, "Insere a palavra-passe", Toast.LENGTH_SHORT).show()
                password != confirmarPassword -> Toast.makeText(this, "As palavras-passe não coincidem", Toast.LENGTH_SHORT).show()
                else -> mostrarEtapaPerfil()
            }
        }

        binding.btnRegistar.setOnClickListener {
            // GGuardar os dados inseridos nos campos de texto
            val nomeUtilizador = binding.edtNomeJogador.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPasswordJogador.text.toString().trim()
            viewModel.registar(nomeUtilizador, email, password, avatarSelecionadoIndex)
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

    private fun mostrarEtapaConta() {
        binding.pageConta.visibility = View.VISIBLE
        binding.pagePerfil.visibility = View.GONE
        binding.txtStepConta.setBackgroundResource(R.drawable.bg_register_step_active)
        binding.txtStepConta.setTextColor(getColor(R.color.bb_text_primary))
        binding.txtStepPerfil.setBackgroundResource(R.drawable.bg_register_step_idle)
        binding.txtStepPerfil.setTextColor(getColor(R.color.bb_text_secondary))
        binding.txtRegisto.text = "Cria a tua conta"
        binding.txtRegistoSubtitulo.text = "Junta-te à batalha do conhecimento!"
        binding.registerScroll.post { binding.registerScroll.smoothScrollTo(0, 0) }
    }

    private fun mostrarEtapaPerfil() {
        binding.pageConta.visibility = View.GONE
        binding.pagePerfil.visibility = View.VISIBLE
        binding.txtStepConta.setBackgroundResource(R.drawable.bg_register_step_idle)
        binding.txtStepConta.setTextColor(getColor(R.color.bb_text_secondary))
        binding.txtStepPerfil.setBackgroundResource(R.drawable.bg_register_step_active)
        binding.txtStepPerfil.setTextColor(getColor(R.color.bb_text_primary))
        binding.txtRegisto.text = "Personaliza o teu perfil"
        binding.txtRegistoSubtitulo.text = "Escolhe o teu nome e avatar."
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
                Toast.makeText(this, "Jogador já existe", Toast.LENGTH_SHORT).show()
            }
            is RegistarEvent.ErroVerificarJogador -> {
                Toast.makeText(this, "Erro ao verificar jogador: ${evento.mensagem}", Toast.LENGTH_SHORT).show()
            }
            is RegistarEvent.ErroCriarJogador -> {
                Toast.makeText(this, "Erro ao criar jogador: ${evento.mensagem}", Toast.LENGTH_SHORT).show()
            }
            is RegistarEvent.ErroCriarAuth -> {
                Toast.makeText(this, "Erro ao criar conta: ${evento.mensagem}", Toast.LENGTH_SHORT).show()
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
