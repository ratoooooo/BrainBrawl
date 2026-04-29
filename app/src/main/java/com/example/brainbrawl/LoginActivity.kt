package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisValidacao.hashPassword
import com.example.brainbrawl.UteisValidacao.validarCampos
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityLoginBinding
import com.example.brainbrawl.repositories.JogadorRepository

class LoginActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val jogadorRepository = JogadorRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Configurar os botoes de login, registo e iniciar jogo sem conta
        binding.btnEntrar.setOnClickListener {
            // Guarda os valores inseridos nos campos
            var nomeUtilizador = binding.edtNomeJogador.text.toString().trim()
            var password = binding.edtPasswordJogador.text.toString().trim()

            // Faz a validação dos campos
            val erro = validarCampos(nomeUtilizador, password)
            // Se existir erro exibir mensagem
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar credenciais no Firebase
            jogadorRepository.obterPerfil(nomeUtilizador)
                .addOnSuccessListener { perfil ->
                    // Verificar se o nome de utilizador existe na base de dados
                    if (perfil != null) {
                        // Guardar a palavra passe
                        val savedHash = perfil.password
                        // Guardar a palavra passe encriptada
                        val inputHash = hashPassword(password)
                        // Verificar se a palavra passe é válida
                        if (savedHash == inputHash) {
                            // Alterar o estado do jogador para on
                            jogadorRepository.marcarOnline(nomeUtilizador)
                            Toast.makeText(this@LoginActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            // Criar um intent para a MainActivity
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            // Passar o nome de utilizador para a MainActivity
                            nomeUtilizador.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
                            // Abrir a MainActivity
                            startActivity(intent)
                            finish()
                        } else {
                            // Exibir mensagem de erro
                            Toast.makeText(this@LoginActivity, "Senha incorreta", Toast.LENGTH_SHORT).show()
                            binding.edtPasswordJogador.text.clear()
                        }
                    } else {
                        // Exibir mensagem de erro
                        Toast.makeText(this@LoginActivity, "Jogador não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this@LoginActivity, "Erro ao acessar o banco de dados", Toast.LENGTH_SHORT).show()
                }
        }
        binding.btnRegisto.setOnClickListener {
            startActivity(Intent(this, RegistarActivity::class.java))
        }
        binding.btnIniciarJogo.setOnClickListener {
            val nomeJogador = binding.edtNomeJogador.text.toString().trim()
            if (nomeJogador.isEmpty()) {
                Toast.makeText(this, "Insira um nome de jogador!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, MainActivity::class.java)
            nomeJogador.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            startActivity(intent)
            finish()
        }
    }

}
