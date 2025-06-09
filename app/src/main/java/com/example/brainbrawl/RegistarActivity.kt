package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.hashPassword
import com.example.brainbrawl.databinding.ActivityRegistarBinding
import com.google.firebase.database.FirebaseDatabase

class RegistarActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityRegistarBinding.inflate(layoutInflater)
    }

    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Configurar botão de registo
        binding.btnRegistar.setOnClickListener {
            //GGuardar os dados inseridos nos campos de texto
            val nomeUtilizador = binding.edtNomeJogador.text.toString().trim()
            val password = binding.edtPasswordJogador.text.toString().trim()

            val erro = Uteis.validarCampos(nomeUtilizador, password)
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Acessar a base de dados e ao nó "jogadores"
            database.child("jogadores").child(nomeUtilizador).get()
                .addOnSuccessListener { snapshot ->
                    //Verificar se jogador já existe
                    if (snapshot.exists()) {
                        // Exibir mensagem de erro
                        Toast.makeText(this, "Jogador já existe", Toast.LENGTH_SHORT).show()
                    } else {
                        // Adicionar jogador ao Firebase
                        adicionarJogador(nomeUtilizador, password)
                        // Abrir LoginActivity
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    // Exibir mensagem de erro
                    Toast.makeText(this, "Erro ao verificar jogador: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Configurar botão de voltar
        binding.btnVoltar.setOnClickListener {
            // Abrir LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    // Adicionar jogador ao Firebase com senha encriptada
    private fun adicionarJogador(nomeUtilizador: String, password: String) {
        val hashedPassword = hashPassword(password)
        val jogadorData = mapOf(
            "password" to hashedPassword,
            "pontuacao" to 0.0,
            "totalJogos" to 0,
            "totalVitorias" to 0,
            "totalRespostasCertas" to 0,
            "totalPerguntasRespondidas" to 0,
            "taxaAcertos" to 0.0,
        )
        database.child("jogadores").child(nomeUtilizador).setValue(jogadorData)
    }
}