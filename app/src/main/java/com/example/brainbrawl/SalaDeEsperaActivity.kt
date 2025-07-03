// MANO O CODIGO NAO AVANÇA NA MESMA PARA A PROXIMA PERGUNTA EU NAO ENTENDO O PROBLEMA
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

        // Verifica e preenche o EditText com nomeUtilizador ou nomeJogador
        when {
            !nomeUtilizador.isNullOrEmpty() -> binding.edtNomeJogador.setText(nomeUtilizador)
            !nomeJogador.isNullOrEmpty() -> binding.edtNomeJogador.setText(nomeJogador)
            else -> binding.edtNomeJogador.setText("") // Deixa vazio se nenhum for passado
        }

        binding.btnEntrarSala.setOnClickListener {
            // evita duplo clique
            binding.btnEntrarSala.isEnabled = false

            val codSala = binding.edtCodigoSala.text.toString().trim()
            val nomeJogador = binding.edtNomeJogador.text.toString().trim()

            val erro = validarCampos(nomeJogador)
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
                return@setOnClickListener
            }

            // Vai buscar os dados da sala ao Firebase só pelo código
            database.child("salas").child(codSala)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Não precisamos de modo nem categoria: só garantir que a sala existe!
                            if (snapshot.child("jogadores").hasChild(nomeJogador)) {
                                Toast.makeText(this@SalaDeEsperaActivity, "Nome de jogador já existe na sala", Toast.LENGTH_SHORT).show()
                                binding.btnEntrarSala.isEnabled = true
                                return
                            } else {
                                // Adiciona o jogador à sala no Firebase
                                adicionarJogador(nomeJogador, codSala)
                                binding.btnEntrarSala.isEnabled = false
                                binding.edtCodigoSala.isEnabled = false
                                binding.edtNomeJogador.isEnabled = false
                                Toast.makeText(this@SalaDeEsperaActivity, "Jogador adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                                codigoSalaListener = codSala
                                irParaSalaDeEsperaGrupo(codSala, nomeJogador, nomeUtilizador)
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

    // Função que adiciona o jogador à sala no Firebase
    private fun adicionarJogador(nomeJogador: String, codigoSala: String) {
        val jogadorData = mapOf(
            "nome" to nomeJogador,
            "pontuacao" to 0
        )
        database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador).setValue(jogadorData)
    }

    // Função que leva SEMPRE para a sala de espera de grupo
    private fun irParaSalaDeEsperaGrupo(codigoSala: String, nomeJogador: String, nomeUtilizador: String?) {
        val intent = Intent(this, SalaDeEsperaGrupoActivity::class.java)
        intent.putExtra("codigoSala", codigoSala)
        intent.putExtra("nomeJogador", nomeJogador)
        intent.putExtra("admin", false) // convidados nunca são admin
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        // As próximas activities podem ir buscar categoria/modo ao Firebase se precisarem
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener se existir
        estadoSalaListener?.let { listener ->
            codigoSalaListener?.let { sala ->
                database.child("salas").child(sala).child("estado").removeEventListener(listener)
            }
        }
    }
}