package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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

    // Variáveis para a lógica da sala
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private lateinit var nomeCategoria: String
    private val database = FirebaseDatabase.getInstance().reference

    private var jogadoresNaSala: List<String> = emptyList()
    private var admin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados pelo intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: getString(R.string.categoria5)

        // Define o texto do código da sala usando o binding
        binding.txtCodigoSala.text = "Código da Sala: $codigoSala"

        // Adiciona este jogador à sala
        database.child("sala_1x1").child(codigoSala).child("jogadores").child(nomeUtilizador).setValue(true)
        // Marca este jogador como pronto na sala
        database.child("sala_1x1").child(codigoSala).child("prontos").child(nomeUtilizador).setValue(true)

        // Verifica se és o admin
        database.child("sala_1x1").child(codigoSala).child("jogadores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomes = snapshot.children.mapNotNull { it.key }
                    admin = nomes.isNotEmpty() && nomes[0] == nomeUtilizador
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Observa os jogadores na sala e atualiza a lista no ecrã
        database.child("sala_1x1").child(codigoSala).child("jogadores")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomes = snapshot.children.mapNotNull { it.key }
                    jogadoresNaSala = nomes
                    binding.txtListaJogadores.text = if (nomes.isEmpty()) {
                        "Aguardando jogadores..."
                    } else {
                        nomes.joinToString(separator = "\n")
                    }
                    // Ativa o botão de iniciar jogo se for admin e houver 2 jogadores
                    binding.btnIniciarJogo.isEnabled = (admin && nomes.size == 2)
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })

        // Observa o estado da sala para iniciar o jogo para todos ao mesmo tempo
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

        // Listener para o clique no botão de iniciar jogo
        binding.btnIniciarJogo.setOnClickListener {
            if (admin && jogadoresNaSala.size == 2) {
                verificarProntosEAvancar()
            } else {
                Toast.makeText(this, "Ainda a aguardar o adversário!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função para verificar se ambos os jogadores estão prontos antes de iniciar
    private fun verificarProntosEAvancar() {
        database.child("sala_1x1").child(codigoSala).child("prontos")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val prontos = snapshot.children.mapNotNull { it.key }
                    if (prontos.size == 2 && jogadoresNaSala.size == 2) {
                        // Altera o estado da sala para "em_jogo",
                        database.child("sala_1x1").child(codigoSala).child("estado").setValue("em_jogo")
                    } else {
                        Toast.makeText(this@SalaDeEspera1x1Activity, "Ambos os jogadores têm de estar na sala!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}