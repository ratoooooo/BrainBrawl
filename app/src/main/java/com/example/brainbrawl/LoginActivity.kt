package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.hashPassword
import com.example.brainbrawl.databinding.ActivityLoginBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference

    // Armazenar nome de utilizador e senha
    private var nomeUtilizador = ""
    private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Configurar botão de login
        binding.btnEntrar.setOnClickListener {
            nomeUtilizador = binding.edtNomeJogador.text.toString().trim()
            password = binding.edtPasswordJogador.text.toString().trim()

            val erro = Uteis.validarCampos(nomeUtilizador, password)
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar credenciais no Firebase
            database.child("jogadores").child(nomeUtilizador).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Verificar se o nome de utilizador existe na base de dados
                    if (snapshot.exists()) {
                        // Guardar a palavra passe
                        val savedHash = snapshot.child("password").value.toString()
                        // Guardar a palavra passe encriptada
                        val inputHash = hashPassword(password)
                        // Verificar se a palavra passe é válida
                        if (savedHash == inputHash) {
                            Toast.makeText(this@LoginActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            // Criar um intent para a MainActivity
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            // Passar o nome de utilizador para a MainActivity
                            intent.putExtra("nomeUtilizador", nomeUtilizador)
                            // Abrir a MainActivity
                            startActivity(intent)
                            finish()
                        } else {
                            // Exibir mensagem de erro
                            Toast.makeText(this@LoginActivity, "Senha incorreta", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Exibir mensagem de erro
                        Toast.makeText(this@LoginActivity, "Jogador não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LoginActivity, "Erro ao acessar o banco de dados", Toast.LENGTH_SHORT).show()
                }
            })
        }
        // Configurar botão de registo
        binding.btnRegisto.setOnClickListener {
            startActivity(Intent(this, RegistarActivity::class.java))
        }
        // Configurar botão de iniciar jogo sem conta
        binding.btnIniciarJogo.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

}