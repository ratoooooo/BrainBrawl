package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivitySalaDeEspera2x2Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaDeEspera2x2Activity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivitySalaDeEspera2x2Binding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private var jogadoresNaSala = mutableListOf<String>()
    private var admin = false
    private var categoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        codigoSala = intent.getStringExtra("salaId") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        categoria = intent.getStringExtra("categoria")

        // Mostrar o código da sala
        binding.txtCodigoSala.text = "Código da sala: $codigoSala"

        // Adicionar o jogador à sala
        database.child("sala_2x2").child(codigoSala).child("jogadores").child(nomeUtilizador).setValue(true)

        // Verificar se o jogador é o administrador (primeiro a entrar na sala)
        database.child("sala_2x2").child(codigoSala).child("jogadores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomes = snapshot.children.map { it.key ?: "" }
                    if (nomes.isNotEmpty() && nomes[0] == nomeUtilizador) {
                        admin = true
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Listener para jogadores na sala (ativa botão quando forem 4 e define as equipas)
        database.child("sala_2x2").child(codigoSala).child("jogadores")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    jogadoresNaSala.clear()
                    for (child in snapshot.children) {
                        jogadoresNaSala.add(child.key ?: "")
                    }

                    // Divide os jogadores em duas equipas ( 2 rimeiros A e os dois últimos B )
                    val equipaA = jogadoresNaSala.take(2)
                    val equipaB = jogadoresNaSala.drop(2).take(2)

                    // Atualiza os TextViews com os nomes dos jogadores
                    binding.txtJogadorA1.text = equipaA.getOrNull(0) ?: "Aguardando..."
                    binding.txtJogadorA2.text = equipaA.getOrNull(1) ?: "Aguardando..."
                    binding.txtJogadorB1.text = equipaB.getOrNull(0) ?: "Aguardando..."
                    binding.txtJogadorB2.text = equipaB.getOrNull(1) ?: "Aguardando..."

                    // Permite ao administrador iniciar o jogo quando houver 4 jogadores
                    binding.btnIniciarJogo.isEnabled = (admin && jogadoresNaSala.size == 4)

                    // Se ainda não existirem os nós de equipa, cria-os quando 4 jogadores estiverem presentes
                    if (admin && jogadoresNaSala.size == 4) {
                        val equipaARef = database.child("sala_2x2").child(codigoSala).child("equipaA")
                        val equipaBRef = database.child("sala_2x2").child(codigoSala).child("equipaB")
                        equipaARef.setValue(equipaA.associateWith { true })
                        equipaBRef.setValue(equipaB.associateWith { true })
                        // Opcional: podes também inicializar a pontuação aqui
                        database.child("sala_2x2").child(codigoSala).child("pontuacaoA").setValue(0)
                        database.child("sala_2x2").child(codigoSala).child("pontuacaoB").setValue(0)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Listener do estado da sala para iniciar o jogo
        database.child("sala_2x2").child(codigoSala).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estado = snapshot.getValue(String::class.java)
                    if (estado == "em_jogo") {
                        val intent = Intent(this@SalaDeEspera2x2Activity, Jogo2x2Activity::class.java)
                        intent.putExtra("salaId", codigoSala)
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
            if (admin && jogadoresNaSala.size == 4) {
                database.child("sala_2x2").child(codigoSala).child("estado").setValue("em_jogo")
            }
        }
    }
}