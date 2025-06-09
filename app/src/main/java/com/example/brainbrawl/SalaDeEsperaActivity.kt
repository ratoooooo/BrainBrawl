package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivitySalaDeEsperaBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaDeEsperaActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivitySalaDeEsperaBinding.inflate(layoutInflater)
    }
    // Acessar o banco de dados do Firebase
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val modoJogo = intent.getStringExtra("modoJogo")

        // Exibir nome do utilizador, se disponível
        if (nomeUtilizador.isNullOrEmpty()) {
            binding.edtNomeJogador.setText(nomeUtilizador)
        }

        // Configurar botão de entrar na sala
        binding.btnEntrarSala.setOnClickListener {
            // Guardar os valores dos campos
            val codSala = binding.edtCodigoSala.text.toString().trim()
            val nomeJogador = binding.edtNomeJogador.text.toString().trim()

            // Validar preenchimento dos campos
            val erro = Uteis.validarCampos(nomeJogador)
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

                // Verificar se a sala existe no Firebase
                database.child("salas").child(codSala)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                // Guardar nome da categoria da sala
                                val nomeCategoria = snapshot.child("categoria").getValue(String::class.java)
                                if (nomeCategoria == null) {
                                    Toast.makeText(this@SalaDeEsperaActivity, "Erro: Categoria da sala não encontrada", Toast.LENGTH_SHORT).show()
                                    return
                                }

                                // Verificar se jogador já está na sala
                                if (snapshot.child("jogadores").hasChild(nomeJogador)) {
                                    Toast.makeText(this@SalaDeEsperaActivity, "Nome de jogador já existe na sala", Toast.LENGTH_SHORT).show()
                                    return
                                } else {
                                    // Chama a função para adicionar jogador à sala
                                    adicionarJogador(nomeJogador, codSala, nomeCategoria)

                                    // Desativar os campos apos o jogador ser adicionado
                                    binding.btnEntrarSala.isEnabled = false
                                    binding.edtCodigoSala.isEnabled = false
                                    binding.edtNomeJogador.isEnabled = false

                                    Toast.makeText(this@SalaDeEsperaActivity, "Jogador adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                                    // Chama a função para esperar o administrador iniciar o jogo
                                    esperarAdminIniciarJogo(codSala, nomeCategoria, nomeUtilizador, modoJogo)
                                }
                            } else {
                                Toast.makeText(this@SalaDeEsperaActivity, "Código da sala inválido", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Exibir mensagem de erro em caso de falha
                            Toast.makeText(this@SalaDeEsperaActivity, "Erro ao verificar sala", Toast.LENGTH_SHORT).show()
                        }
                    })
        }

        // Configurar botão de voltar
        binding.btnVoltar.setOnClickListener {
            // Redirecionar para a MainActivity com o nome do utilizador
            var intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            startActivity(intent)
            finish()
        }
    }

    // Função para adicionar os jogadores à sala
    private fun adicionarJogador(nomeJogador: String, codigoSala: String, nomeCategoria: String) {
        // Criar estrutura de dados do jogador
        val jogadorData = mapOf(
            "nome" to nomeJogador,
            "pontuacao" to 0.0
        )

        // Salvar jogador na sala no Firebase
        database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador)
            .setValue(jogadorData)


    }

    // Função para monitorar o estado da sala e iniciar o jogo quando o administrador der início
    private fun esperarAdminIniciarJogo(codigoSala: String, nomeCategoria: String, nomeUtilizador: String?, modoJogo: String?) {
        // Escutar mudanças no estado da sala
        database.child("salas").child(codigoSala).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Guardar o estado da sala
                    val estado = snapshot.getValue(String::class.java)
                    // Verificar se o estado é "em_jogo"
                    if (estado == "em_jogo") {
                        // Redirecionar para a JogoActivity com os dados necessários
                        val intent = Intent(this@SalaDeEsperaActivity, JogoActivity::class.java)
                        intent.putExtra("codigoSala", codigoSala)
                        intent.putExtra("nomeCategoria", nomeCategoria)
                        intent.putExtra("nomeUtilizador", nomeUtilizador)
                        intent.putExtra("nomeJogador", binding.edtNomeJogador.text.toString())
                        intent.putExtra("modoJogo", modoJogo)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Exibir mensagem de erro em caso de falha
                    Toast.makeText(this@SalaDeEsperaActivity, "Erro ao esperar o jogo", Toast.LENGTH_SHORT).show()
                }
            })
    }
}