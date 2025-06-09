package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityAdicionarPerguntaBinding
import com.google.firebase.database.FirebaseDatabase

class AdicionarPerguntaActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityAdicionarPerguntaBinding.inflate(layoutInflater)
    }

    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Configurar o botão para enviar a pergunta
        binding.layoutBtnEnviar.setOnClickListener {
            //Guardar os dados dos editTexts
            val nomeCategoria = binding.edtNovaCategoria.text.toString().trim()
            val pergunta = binding.edtPergunta.text.toString().trim()
            val opcaoA = binding.edtOpcaoA.text.toString().trim()
            val opcaoB = binding.edtOpcaoB.text.toString().trim()
            val opcaoC = binding.edtOpcaoC.text.toString().trim()
            val opcaoD = binding.edtOpcaoD.text.toString().trim()

            // Obter a resposta correta
            val respostaCorreta = when (binding.rgOpcoes.checkedRadioButtonId) {
                binding.rbOpcaoA.id -> opcaoA
                binding.rbOpcaoB.id -> opcaoB
                binding.rbOpcaoC.id -> opcaoC
                binding.rbOpcaoD.id -> opcaoD
                else -> ""
            }

            //Verificar se a categoria é permitida
            if (nomeCategoria == getString(R.string.categoria1) || nomeCategoria == getString(R.string.categoria2) ||
                nomeCategoria == getString(R.string.categoria3) || nomeCategoria == getString(R.string.categoria4)) {
                Toast.makeText(this, "Categoria não permitida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Verificar os tamanhos dos campos
            if (nomeCategoria.length > 50 || pergunta.length > 200 || opcaoA.length > 100 || opcaoB.length > 100 || opcaoC.length > 100 || opcaoD.length > 100) {
                Toast.makeText(this, "Campos excedem o tamanho máximo permitido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Verificar se as respostas estão preenchidas
            if (listOf(opcaoA, opcaoB, opcaoC, opcaoD).distinct().size != 4) {
                Toast.makeText(this, "As opções devem ser todas diferentes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Verificar se os campos estão preenchidos
            if (nomeCategoria.isEmpty() || pergunta.isEmpty() || opcaoA.isEmpty() || opcaoB.isEmpty() || opcaoC.isEmpty() || opcaoD.isEmpty() || respostaCorreta.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Criar um mapa com os dados da pergunta
            val perguntasData = mapOf(
                "pergunta" to pergunta,
                "respostaCorreta" to respostaCorreta,
                "opcoes" to listOf(opcaoA, opcaoB, opcaoC, opcaoD)
            )

            //Adicionar a pergunta a base de dados
            database.child("categorias").child(nomeCategoria).child("perguntas").push().setValue(perguntasData)
                //Verificar se a pergunta foi adicionada com sucesso
                .addOnSuccessListener {
                    //Exibir mensagem de sucesso
                    Toast.makeText(this, "Pergunta adicionada com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    //Exibir mensagem de erro
                    Toast.makeText(this, "Erro ao adicionar pergunta: ${error.message}", Toast.LENGTH_SHORT).show()
                }

            //Limpar os campos
            limparCampos()
        }

        // Configurar o botão para voltar ao MainActivity
        binding.layoutBtnComecar.setOnClickListener {
            //Receber dados passados do intent
            val codigoSala = intent.getStringExtra("codigoSala")
            val modoJogo = intent.getStringExtra("modoJogo")
            //Guardar os dados dos editTexts
            val nomeCategoria = binding.edtNovaCategoria.text.toString().trim()
            //Criar um intent para a MainActivity
            val intent = Intent(this, MainActivity::class.java)
            //Passar os dados para a MainActivity
            intent.putExtra("nomeCategoria", nomeCategoria)
            intent.putExtra("codigoSala", codigoSala)
            intent.putExtra("modoJogo", modoJogo)
            //Abrir a MainActivity
            startActivity(intent)
            //Fechar a AdicionarPerguntaActivity
            finish()
        }
    }

    //Limpar os campos
    private fun limparCampos() {
        //Limpar os campos
        binding.edtPergunta.text.clear()
        binding.edtOpcaoA.text.clear()
        binding.edtOpcaoB.text.clear()
        binding.edtOpcaoC.text.clear()
        binding.edtOpcaoD.text.clear()
        binding.rgOpcoes.clearCheck()
    }
}