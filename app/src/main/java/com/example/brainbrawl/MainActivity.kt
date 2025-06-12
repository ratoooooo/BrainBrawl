package com.example.brainbrawl

import Pergunta
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    // Variáveis para armazenar informações do utilizador e da sala
    private var nomeCategoria: String? = null
    private var codigoSala: String? = null
    private var nomeUtilizador: String? = null
    private var modoJogo: String? = null
    private var admin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Recuperar dados do savedInstanceState ou do intent se for a primeira vez
        nomeUtilizador = savedInstanceState?.getString("nomeUtilizador")
            ?: intent.getStringExtra("nomeUtilizador")
        nomeCategoria = savedInstanceState?.getString("nomeCategoria")
            ?: intent.getStringExtra("nomeCategoria")
        codigoSala = savedInstanceState?.getString("codigoSala")
            ?: intent.getStringExtra("codigoSala")
        modoJogo = savedInstanceState?.getString("modoJogo")
            ?: intent.getStringExtra("modoJogo")

        // Se o utilizador estiver autenticado, mostrar mensagem de boas-vindas e botão de amigos
        if (nomeUtilizador != null) {
            binding.txtBoasVindas.text = "Bem-vindo, $nomeUtilizador!"
            binding.btnAddAmigo.visibility = View.VISIBLE
            binding.btnAddAmigo.setOnClickListener {
                val intent = Intent(this, AmigosActivity::class.java)
                intent.putExtra("nomeUtilizador", nomeUtilizador)
                startActivity(intent)
            }
        } else {
            binding.btnAddAmigo.visibility = View.GONE
        }

        // Verificar se existe uma sala criada e configurar perguntas
        if (codigoSala != null && modoJogo != null) {
            binding.txtCodigoSala.text = "Código da Sala: $codigoSala"
            binding.btnIniciarJogo.visibility = View.VISIBLE

            // Se o modo de jogo for caótico, buscar perguntas de todas as categorias
            if (modoJogo == "caotico") {
                obterPerguntas(isCaotico = true)
            } else {
                obterPerguntas(isCaotico = false)
            }
        } else {
            binding.txtCodigoSala.text = "Nenhuma sala criada"
            binding.btnIniciarJogo.visibility = View.GONE
        }

        // Botão para criar nova sala
        binding.btnCriarSala.setOnClickListener {
            if (codigoSala != null) {
                Toast.makeText(this, "Uma sala já foi criada!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, EscolherModoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            startActivity(intent)
        }

        // Botão para entrar numa sala existente
        binding.btnEntrarSala.setOnClickListener {
            val intent = Intent(this, SalaDeEsperaActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            startActivity(intent)
        }

        // Botão para iniciar o jogo (apenas disponível se houver sala)
        binding.btnIniciarJogo.setOnClickListener {
            if (codigoSala == null || modoJogo == null) {
                Toast.makeText(this, "Erro: Dados da sala inválidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Atualizar estado da sala para 'em_jogo'
            database.child("salas").child(codigoSala!!).child("estado").setValue("em_jogo")

            // Iniciar atividade do jogo
            val intent = Intent(this, JogoActivity::class.java)
            admin = true
            intent.putExtra("codigoSala", codigoSala)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            intent.putExtra("nomeCategoria", nomeCategoria)
            intent.putExtra("modoJogo", modoJogo)
            intent.putExtra("admin", admin)
            startActivity(intent)

            // Limpar código da sala e esconder botão de início
            codigoSala = null
            binding.txtCodigoSala.text = "Nenhuma sala criada"
            binding.btnIniciarJogo.visibility = View.GONE
        }

        // Botão para voltar ao ecrã de login
        binding.btnVoltar.setOnClickListener {
            // Mudar estado do jogador para 'off' no Firebase
            database.child("jogadores").child(nomeUtilizador.toString()).child("estado").setValue("off")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Guardar estado da activity para rotações/dispositivo
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("nomeUtilizador", nomeUtilizador)
        outState.putString("nomeCategoria", nomeCategoria)
        outState.putString("codigoSala", codigoSala)
        outState.putString("modoJogo", modoJogo)
    }

    // Função para obter as perguntas do Firebase e preparar a sala
    private fun obterPerguntas(isCaotico: Boolean) {
        if (codigoSala == null || modoJogo == null || (!isCaotico && nomeCategoria == null)) {
            Toast.makeText(this, "Erro: Dados da sala inválidos", Toast.LENGTH_SHORT).show()
            return
        }
        val listaPerguntas = mutableListOf<Pergunta>()
        val perguntasRef = if (isCaotico) {
            database.child("categorias")
        } else {
            database.child("categorias").child(nomeCategoria!!).child("perguntas")
        }
        perguntasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaPerguntas.clear()
                if (isCaotico) {
                    // Buscar perguntas de todas as categorias para o modo caótico
                    for (categoriaSnapshot in snapshot.children) {
                        val perguntasSnapshot = categoriaSnapshot.child("perguntas")
                        for (perguntaSnapshot in perguntasSnapshot.children) {
                            val pergunta = perguntaSnapshot.child("pergunta").getValue(String::class.java)
                            val respostaCorreta = perguntaSnapshot.child("respostaCorreta").getValue(String::class.java)
                            val opcoesSnapshot = perguntaSnapshot.child("opcoes").children
                            val opcoes = mutableListOf<String>()
                            opcoesSnapshot.forEach { opcao ->
                                opcoes.add(opcao.getValue(String::class.java) ?: "")
                            }
                            if (pergunta != null && respostaCorreta != null && opcoes.size == 4) {
                                listaPerguntas.add(Pergunta(pergunta, respostaCorreta, opcoes))
                            }
                        }
                    }
                } else {
                    // Buscar perguntas apenas da categoria selecionada
                    for (perguntaSnapshot in snapshot.children) {
                        val pergunta = perguntaSnapshot.child("pergunta").getValue(String::class.java) ?: ""
                        val respostaCorreta = perguntaSnapshot.child("respostaCorreta").getValue(String::class.java) ?: ""
                        val opcoesSnapshot = perguntaSnapshot.child("opcoes").children
                        val opcoes = mutableListOf<String>()
                        opcoesSnapshot.forEach { opcao ->
                            opcoes.add(opcao.getValue(String::class.java) ?: "")
                        }
                        if (pergunta.isNotEmpty() && respostaCorreta.isNotEmpty() && opcoes.size == 4) {
                            listaPerguntas.add(Pergunta(pergunta, respostaCorreta, opcoes))
                        }
                    }
                }
                // Selecionar 15 perguntas aleatórias
                val perguntasSelecionadas = listaPerguntas.shuffled().take(15)
                if (perguntasSelecionadas.isNotEmpty()) {
                    // Criar a sala no Firebase com as perguntas selecionadas
                    val salaData = mapOf(
                        "horaCriacao" to System.currentTimeMillis(),
                        "admin" to (nomeUtilizador ?: "Admin"),
                        "estado" to "em_espera",
                        "modoJogo" to modoJogo,
                        "jogadores" to emptyMap<String, Any>(),
                        "categoria" to if (isCaotico) "Todas as categorias" else nomeCategoria,
                        "perguntas" to perguntasSelecionadas.map { pergunta ->
                            mapOf(
                                "pergunta" to pergunta.pergunta,
                                "respostaCorreta" to pergunta.respostaCorreta,
                                "opcoes" to pergunta.opcoes
                            )
                        }
                    )
                    database.child("salas").child(codigoSala!!).setValue(salaData)
                        .addOnSuccessListener {
                            Toast.makeText(this@MainActivity, "Sala criada com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(this@MainActivity, "Erro ao criar sala: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this@MainActivity, "Nenhuma pergunta encontrada", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Erro ao carregar perguntas: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}