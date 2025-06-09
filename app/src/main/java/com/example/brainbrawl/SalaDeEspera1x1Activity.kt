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
    private val binding by lazy {
        ActivitySalaDeEspera1x1Binding.inflate(layoutInflater)
    }

    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var salaId: String
    private lateinit var nomeUtilizador: String
    private var jogadoresNaSala = mutableListOf<String>()
    private var admin = false
    private var categoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        salaId = intent.getStringExtra("salaId") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        categoria = intent.getStringExtra("categoria") ?: "Todas as categorias"

        binding.txtCodigoSala.text = "Código da sala: $salaId"

        // Adiciona jogador à sala (caso não esteja)
        database.child("sala_1x1").child(salaId).child("jogadores").child(nomeUtilizador)
            .setValue(true)

        // Verifica quem é admin (quem criou a sala é admin)
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

        // Listener para jogadores na sala (ativa botão quando forem 2)
        database.child("sala_1x1").child(salaId).child("jogadores")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    jogadoresNaSala.clear()
                    for (child in snapshot.children) {
                        jogadoresNaSala.add(child.key ?: "")
                    }
                    binding.txtListaJogadores.text = jogadoresNaSala.joinToString("\n")
                    binding.btnIniciarJogo.isEnabled = (admin && jogadoresNaSala.size == 2)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Listener de estado para ambos entrarem juntos
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

        // Botão iniciar jogo: só admin pode e só com dois jogadores
        binding.btnIniciarJogo.setOnClickListener {
            if (jogadoresNaSala.size == 2) {
                database.child("sala_1x1").child(salaId).child("estado").setValue("em_jogo")
            }
        }
    }
}