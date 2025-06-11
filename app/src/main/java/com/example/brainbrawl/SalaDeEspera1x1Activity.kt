package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivitySalaDeEspera1x1Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaDeEspera1x1Activity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivitySalaDeEspera1x1Binding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var salaId: String
    private lateinit var nomeUtilizador: String
    private var jogadoresNaSala = mutableListOf<String>()
    private var admin = false
    private var categoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        salaId = intent.getStringExtra("salaId") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        categoria = intent.getStringExtra("categoria") ?: "Todas as categorias"

        // Mostrar o código da sala
        binding.txtCodigoSala.text = "Código da sala: $salaId"

        // Adicionar o jogador à sala
        database.child("sala_1x1").child(salaId).child("jogadores").child(nomeUtilizador).setValue(true)

        // Verificar se o jogador é o administrador (primeiro a entrar na sala)
        database.child("sala_1x1").child(salaId).child("jogadores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomes = snapshot.children.map { it.key ?: "" }
                    if (nomes.isNotEmpty() && nomes[0] == nomeUtilizador) {
                        admin = true
                        binding.btnIniciarJogo.isEnabled = false // Só ativa com 2 jogadores
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Listener para jogadores na sala (ativa botão quando forem 4 e define as equipas)
        database.child("sala_1x1").child(salaId).child("jogadores")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    jogadoresNaSala.clear()
                    for (child in snapshot.children) {
                        jogadoresNaSala.add(child.key ?: "")
                    }

                    //
                    binding.txtListaJogadores.text = jogadoresNaSala.joinToString("\n")
                    // Permite ao administrador iniciar o jogo quando houver 2 jogadores
                    binding.btnIniciarJogo.isEnabled = (admin && jogadoresNaSala.size == 2)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Listener do estado da sala para iniciar o jogo
        database.child("sala_1x1").child(salaId).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estado = snapshot.getValue(String::class.java)
                    if (estado == "em_jogo") {
                        val intent = Intent(this@SalaDeEspera1x1Activity, Jogo1x1Activity::class.java)
                        intent.putExtra("salaId", salaId)
                        intent.putExtra("nomeUtilizador", nomeUtilizador)
                        intent.putExtra("categoria", categoria)
                        startActivity(intent)
                        finish()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Configurar botão de Iniciar Jogo
        binding.btnIniciarJogo.setOnClickListener {
            if (jogadoresNaSala.size == 2) {
                database.child("sala_1x1").child(salaId).child("estado").setValue("em_jogo")
            }
        }
    }
}