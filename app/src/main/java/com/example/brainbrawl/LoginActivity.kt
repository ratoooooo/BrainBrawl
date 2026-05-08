package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityLoginBinding
import com.example.brainbrawl.viewmodels.LoginEvent
import com.example.brainbrawl.viewmodels.LoginViewModel

class LoginActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[LoginViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        configurarObservers()
        configurarPasswordToggle()
        viewModel.verificarSessaoAtual()

        // Configurar os botoes de login, registo e iniciar jogo sem conta
        binding.btnEntrar.setOnClickListener {
            // Guarda os valores inseridos nos campos
            val identificador = binding.edtNomeJogador.text.toString().trim()
            val password = binding.edtPasswordJogador.text.toString()
            viewModel.entrar(identificador, password)
        }
        binding.btnRegisto.setOnClickListener {
            startActivity(Intent(this, RegistarActivity::class.java))
        }
        binding.btnIniciarJogo.setOnClickListener {
            val nomeJogador = binding.edtNomeJogador.text.toString().trim()
            viewModel.entrarComoConvidado(nomeJogador)
        }
    }

    private fun configurarObservers() {
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun tratarEvento(evento: LoginEvent) {
        when (evento) {
            is LoginEvent.ValidacaoFalhou -> {
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
            }
            is LoginEvent.LoginSucesso -> {
                Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, evento.nomeUtilizador)
                evento.uid?.let { intent.putExtra(IntentExtras.UID, it) }
                evento.email?.let { intent.putExtra(IntentExtras.EMAIL, it) }
                startActivity(intent)
                finish()
            }
            LoginEvent.SenhaIncorreta -> {
                Toast.makeText(this, "Senha incorreta", Toast.LENGTH_SHORT).show()
                binding.edtPasswordJogador.text.clear()
            }
            LoginEvent.ErroAutenticacao -> {
                Toast.makeText(this, "Email ou senha incorretos", Toast.LENGTH_SHORT).show()
                binding.edtPasswordJogador.text.clear()
            }
            LoginEvent.ErroPerfilAuth -> {
                Toast.makeText(this, "Conta autenticada sem perfil de jogador", Toast.LENGTH_SHORT).show()
            }
            LoginEvent.JogadorNaoEncontrado -> {
                Toast.makeText(this, "Jogador não encontrado", Toast.LENGTH_SHORT).show()
            }
            LoginEvent.ErroBanco -> {
                Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_SHORT).show()
            }
            LoginEvent.NomeConvidadoVazio -> {
                Toast.makeText(this, "Insira um nome de jogador!", Toast.LENGTH_SHORT).show()
            }
            is LoginEvent.ConvidadoSucesso -> {
                val intent = Intent(this, MainActivity::class.java)
                evento.nomeJogador.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun configurarPasswordToggle() {
        var visivel = false
        binding.btnTogglePassword.setOnClickListener {
            visivel = !visivel
            alternarPassword(binding.edtPasswordJogador, visivel)
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
}
