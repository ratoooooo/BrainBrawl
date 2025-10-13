package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisValidacao.validarCampos
import com.example.brainbrawl.databinding.ActivitySalaDeEsperaBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaDeEsperaActivity : AppCompatActivity() {
    private val binding by lazy { ActivitySalaDeEsperaBinding.inflate(layoutInflater) }
    private val database = FirebaseDatabase.getInstance().reference

    private var estadoSalaListener: ValueEventListener? = null
    private var codigoSalaListener: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val nomeJogador = intent.getStringExtra("nomeJogador")

        // Se for utilizador registado, bloqueia edição do nome
        if (!nomeUtilizador.isNullOrEmpty()) {
            binding.edtNomeJogador.setText(nomeUtilizador)
            binding.edtNomeJogador.isEnabled = false
        } else if (!nomeJogador.isNullOrEmpty()) {
            binding.edtNomeJogador.setText(nomeJogador)
            binding.edtNomeJogador.isEnabled = true
        } else {
            binding.edtNomeJogador.setText("")
            binding.edtNomeJogador.isEnabled = true
        }

        binding.btnEntrarSala.setOnClickListener {
            binding.btnEntrarSala.isEnabled = false

            val codSala = binding.edtCodigoSala.text.toString().trim()
            val nomeJogadorAtual = binding.edtNomeJogador.text.toString().trim()

            // Validação do código da sala: não pode estar vazio
            if (codSala.isEmpty()) {
                Toast.makeText(this, "Insira o código da sala!", Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
                return@setOnClickListener
            }

            val erro = validarCampos(nomeJogadorAtual)
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
                return@setOnClickListener
            }

            // Verifica no Firebase se a sala existe e se o nome já está na sala
            database.child("salas").child(codSala)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            if (snapshot.child("jogadores").hasChild(nomeJogadorAtual)) {
                                Toast.makeText(this@SalaDeEsperaActivity, "Nome de jogador já existe na sala", Toast.LENGTH_SHORT).show()
                                binding.btnEntrarSala.isEnabled = true
                            } else {
                                // Se for utilizador registado, busca avatar real
                                if (!nomeUtilizador.isNullOrEmpty()) {
                                    database.child("jogadores").child(nomeUtilizador).child("avatar")
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                val avatar = snapshot.getValue(String::class.java) ?: "avatar_1_playstore"
                                                adicionarJogadorComAvatar(nomeJogadorAtual, codSala, avatar)
                                                irParaSalaDeEsperaGrupo(codSala, nomeJogadorAtual, nomeUtilizador)
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                adicionarJogadorComAvatar(nomeJogadorAtual, codSala, "avatar_1_playstore")
                                                irParaSalaDeEsperaGrupo(codSala, nomeJogadorAtual, nomeUtilizador)
                                            }
                                        })
                                } else {
                                    adicionarJogadorComAvatar(nomeJogadorAtual, codSala, "avatar_1_playstore")
                                    irParaSalaDeEsperaGrupo(codSala, nomeJogadorAtual, null)
                                }

                                binding.btnEntrarSala.isEnabled = false
                                binding.edtCodigoSala.isEnabled = false
                                binding.edtNomeJogador.isEnabled = false
                                Toast.makeText(this@SalaDeEsperaActivity, "Jogador adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                                codigoSalaListener = codSala
                            }
                        } else {
                            Toast.makeText(this@SalaDeEsperaActivity, "Código da sala inválido", Toast.LENGTH_SHORT).show()
                            binding.btnEntrarSala.isEnabled = true
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SalaDeEsperaActivity, "Erro ao verificar sala: ${error.message}", Toast.LENGTH_SHORT).show()
                        binding.btnEntrarSala.isEnabled = true
                    }
                })
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun adicionarJogadorComAvatar(nomeJogador: String, codigoSala: String, avatar: String) {
        val jogadorData = mapOf(
            "nome" to nomeJogador,
            "pontuacao" to 0,
            "avatar" to avatar,
            "estado" to "on"
        )
        database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador).setValue(jogadorData)
    }

// Função para ir para a sala de espera do grupo
    private fun irParaSalaDeEsperaGrupo(codigoSala: String, nomeJogador: String, nomeUtilizador: String?) {
        // Redireciona para a SalaDeEsperaGrupoActivity com os dados necessários
        val intent = Intent(this, SalaDeEsperaGrupoActivity::class.java)
        intent.putExtra("admin", false)
        codigoSala.let { intent.putExtra("codigoSala", it) }
        nomeJogador.let { intent.putExtra("nomeJogador", it) }
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        estadoSalaListener?.let { listener ->
            codigoSalaListener?.let { sala ->
                database.child("salas").child(sala).child("estado").removeEventListener(listener)
            }
        }
    }
}
