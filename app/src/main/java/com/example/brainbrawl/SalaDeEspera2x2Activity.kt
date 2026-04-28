package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.databinding.ActivitySalaDeEspera2x2Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaDeEspera2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySalaDeEspera2x2Binding.inflate(layoutInflater)
    }
    private val database = FirebaseDatabase.getInstance().reference
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private var jogadoresNaSala = mutableListOf<String>()
    private var admin = false
    private var categoria: String? = null
    private var jogadoresListener: ValueEventListener? = null
    private var estadoListener: ValueEventListener? = null
    private var salaListener: ValueEventListener? = null
    private var saidaManual = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador") ?: nomeUtilizador
        categoria = intent.getStringExtra("nomeCategoria")
            ?: intent.getStringExtra("categoria")
            ?: getString(R.string.categoria5)

        // Mostrar o código da sala
        binding.txtCodigoSala.text = "Código da sala: $codigoSala"

        // Adicionar o jogador à sala
        database.child("sala_2x2").child(codigoSala).child("jogadores").child(nomeUtilizador).setValue(true)

        // Verificar se o jogador é o administrador (primeiro a entrar na sala)
        database.child("sala_2x2").child(codigoSala).child("admin")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomeAdmin = snapshot.getValue(String::class.java)
                    admin = if (nomeAdmin.isNullOrBlank()) {
                        jogadoresNaSala.firstOrNull() == nomeUtilizador
                    } else {
                        nomeAdmin == nomeUtilizador
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Listener para jogadores na sala (ativa botão quando forem 4 e define as equipas)
        jogadoresListener = object : ValueEventListener {
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
            }
        database.child("sala_2x2").child(codigoSala).child("jogadores")
            .addValueEventListener(jogadoresListener!!)

        // Listener do estado da sala para iniciar o jogo
        estadoListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estado = snapshot.getValue(String::class.java)
                    if (estado == "em_jogo") {
                        val intent = Intent(this@SalaDeEspera2x2Activity, Jogo2x2Activity::class.java)
                        codigoSala.let { intent.putExtra("codigoSala", it) }
                        nomeUtilizador.let { intent.putExtra("nomeUtilizador", it) }
                        nomeJogador.let { intent.putExtra("nomeJogador", it) }
                        categoria?.let {
                            intent.putExtra("nomeCategoria", it)
                            intent.putExtra("categoria", it)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
        database.child("sala_2x2").child(codigoSala).child("estado")
            .addValueEventListener(estadoListener!!)

        escutarSalaApagada()

        // Configurar botão de Iniciar Jogo
        binding.btnIniciarJogo.setOnClickListener {
            if (admin && jogadoresNaSala.size == 4) {
                database.child("sala_2x2").child(codigoSala).child("estado").setValue("em_jogo")
            }
        }

        binding.btnSairSala.setOnClickListener {
            sairDaSala()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jogadoresListener?.let {
            database.child("sala_2x2").child(codigoSala).child("jogadores").removeEventListener(it)
        }
        estadoListener?.let {
            database.child("sala_2x2").child(codigoSala).child("estado").removeEventListener(it)
        }
        salaListener?.let {
            database.child("sala_2x2").child(codigoSala).removeEventListener(it)
        }
    }

    private fun escutarSalaApagada() {
        salaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!saidaManual && !snapshot.exists()) {
                    abrirMainActivity(this@SalaDeEspera2x2Activity, nomeUtilizador, nomeJogador)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("sala_2x2").child(codigoSala).addValueEventListener(salaListener!!)
    }

    private fun sairDaSala() {
        saidaManual = true
        val salaRef = database.child("sala_2x2").child(codigoSala)
        if (admin) {
            salaRef.removeValue()
        } else {
            salaRef.child("jogadores").child(nomeUtilizador).removeValue()
            salaRef.child("equipaA").child(nomeUtilizador).removeValue()
            salaRef.child("equipaB").child(nomeUtilizador).removeValue()
        }
        abrirMainActivity(this, nomeUtilizador, nomeJogador)
        finish()
    }
}
