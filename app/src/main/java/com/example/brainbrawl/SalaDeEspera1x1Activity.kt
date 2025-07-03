package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaDeEspera1x1Activity : AppCompatActivity() {
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeCategoria: String
    private val database = FirebaseDatabase.getInstance().reference

    private lateinit var txtCodigoSala: TextView
    private lateinit var txtListaJogadores: TextView
    private lateinit var btnIniciarJogo: Button

    private var jogadoresNaSala: List<String> = emptyList()
    private var admin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sala_de_espera_1x1)

        txtCodigoSala = findViewById(R.id.txtCodigoSala)
        txtListaJogadores = findViewById(R.id.txtListaJogadores)
        btnIniciarJogo = findViewById(R.id.btnIniciarJogo)

        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: getString(R.string.categoria5)

        txtCodigoSala.text = "Código da Sala: $codigoSala"

        // Adiciona este jogador à sala (garante que está no nó jogadores)
        database.child("sala_1x1").child(codigoSala).child("jogadores").child(nomeUtilizador).setValue(true)
        // Marca este jogador como pronto na sala
        database.child("sala_1x1").child(codigoSala).child("prontos").child(nomeUtilizador).setValue(true)

        // Verifica se és admin (primeiro jogador na lista)
        database.child("sala_1x1").child(codigoSala).child("jogadores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomes = snapshot.children.mapNotNull { it.key }
                    admin = nomes.isNotEmpty() && nomes[0] == nomeUtilizador
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Observa os jogadores da sala e atualiza a lista no ecrã
        database.child("sala_1x1").child(codigoSala).child("jogadores")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomes = snapshot.children.mapNotNull { it.key }
                    jogadoresNaSala = nomes
                    txtListaJogadores.text = if (nomes.isEmpty()) {
                        "Aguardando jogadores..."
                    } else {
                        nomes.joinToString(separator = "\n")
                    }
                    btnIniciarJogo.isEnabled = (admin && nomes.size == 2)
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })

        // Listener sincronizado do estado da sala
        database.child("sala_1x1").child(codigoSala).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estado = snapshot.getValue(String::class.java)
                    if (estado == "em_jogo") {
                        val intent = Intent(this@SalaDeEspera1x1Activity, Jogo1x1Activity::class.java)
                        intent.putExtra("codigoSala", codigoSala)
                        intent.putExtra("nomeUtilizador", nomeUtilizador)
                        intent.putExtra("nomeCategoria", nomeCategoria)
                        startActivity(intent)
                        finish()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        btnIniciarJogo.setOnClickListener {
            if (admin && jogadoresNaSala.size == 2) {
                verificarProntosEAvancar()
            } else {
                Toast.makeText(this, "Ainda a aguardar o adversário!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarProntosEAvancar() {
        database.child("sala_1x1").child(codigoSala).child("prontos")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val prontos = snapshot.children.mapNotNull { it.key }
                    if (prontos.size == 2 && jogadoresNaSala.size == 2) {
                        // Sincroniza o início para os dois
                        database.child("sala_1x1").child(codigoSala).child("estado").setValue("em_jogo")
                    } else {
                        Toast.makeText(this@SalaDeEspera1x1Activity, "Ambos têm de estar prontos!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}