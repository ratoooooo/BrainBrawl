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
    private val binding by lazy { ActivitySalaDeEsperaBinding.inflate(layoutInflater) }
    private val database = FirebaseDatabase.getInstance().reference

    private var estadoSalaListener: ValueEventListener? = null
    private var codigoSalaListener: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val modoJogo = intent.getStringExtra("modoJogo")

        // Corrigido: só preenche se houver nome
        if (!nomeUtilizador.isNullOrEmpty()) {
            binding.edtNomeJogador.setText(nomeUtilizador)
        }

        binding.btnEntrarSala.setOnClickListener {
            // evita duplo clique
            binding.btnEntrarSala.isEnabled = false

            val codSala = binding.edtCodigoSala.text.toString().trim()
            val nomeJogador = binding.edtNomeJogador.text.toString().trim()

            val erro = Uteis.validarCampos(nomeJogador)
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
                return@setOnClickListener
            }

            database.child("salas").child(codSala)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val nomeCategoria = snapshot.child("categoria").getValue(String::class.java)
                            if (nomeCategoria == null) {
                                Toast.makeText(this@SalaDeEsperaActivity, "Erro: Categoria da sala não encontrada", Toast.LENGTH_SHORT).show()
                                binding.btnEntrarSala.isEnabled = true
                                return
                            }
                            if (snapshot.child("jogadores").hasChild(nomeJogador)) {
                                Toast.makeText(this@SalaDeEsperaActivity, "Nome de jogador já existe na sala", Toast.LENGTH_SHORT).show()
                                binding.btnEntrarSala.isEnabled = true
                                return
                            } else {
                                adicionarJogador(nomeJogador, codSala, nomeCategoria)
                                binding.btnEntrarSala.isEnabled = false
                                binding.edtCodigoSala.isEnabled = false
                                binding.edtNomeJogador.isEnabled = false
                                Toast.makeText(this@SalaDeEsperaActivity, "Jogador adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                                codigoSalaListener = codSala
                                esperarAdminIniciarJogo(codSala, nomeCategoria, nomeUtilizador, modoJogo)
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
            finish() // apenas termina a activity, navegação natural do Android
        }
    }

    private fun adicionarJogador(nomeJogador: String, codigoSala: String, nomeCategoria: String) {
        val jogadorData = mapOf(
            "nome" to nomeJogador,
            "pontuacao" to 0,
        )
        database.child("salas").child(codigoSala).child("jogadores").child(nomeJogador).setValue(jogadorData)
    }

    private fun esperarAdminIniciarJogo(codigoSala: String, nomeCategoria: String, nomeUtilizador: String?, modoJogo: String?) {
        estadoSalaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val estado = snapshot.getValue(String::class.java)
                if (estado == "em_jogo") {
                    val intent = Intent(this@SalaDeEsperaActivity, JogoActivity::class.java)
                    var nomeJogador = binding.edtNomeJogador.text.toString()
                    codigoSala.let { intent.putExtra("codigoSala", it) }
                    nomeCategoria.let { intent.putExtra("nomeCategoria", it) }
                    nomeUtilizador.let { intent.putExtra("nomeUtilizador", it) }
                    modoJogo?.let { intent.putExtra("modoJogo", it) }
                    nomeJogador.let { intent.putExtra("nomeJogador", it) }
                    startActivity(intent)
                    finish()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SalaDeEsperaActivity, "Erro ao esperar o jogo: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        database.child("salas").child(codigoSala).child("estado").addValueEventListener(estadoSalaListener!!)
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