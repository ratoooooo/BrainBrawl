package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.databinding.ActivitySalaDeEspera1x1Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SalaDeEsperaGrupoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySalaDeEspera1x1Binding.inflate(layoutInflater)
    }

    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var codigoSala: String
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var nomeCategoria: String = ""
    private var modoJogo: String = "classico"
    private var admin = false
    private var nomeAtual: String = ""
    private var jogadoresNaSala: List<String> = emptyList()
    private var jogadoresListener: ValueEventListener? = null
    private var estadoListener: ValueEventListener? = null
    private var salaListener: ValueEventListener? = null
    private var saidaManual = false
    private val minimoJogadoresGrupo = 1
    private val jogadoresInfo = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra("codigoSala") ?: ""
        nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        nomeJogador = intent.getStringExtra("nomeJogador")
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: "Todas as categorias"
        modoJogo = intent.getStringExtra("modoJogo") ?: "classico"
        admin = intent.getBooleanExtra("admin", false)
        nomeAtual = nomeUtilizador ?: nomeJogador ?: ""

        if (codigoSala.isBlank() || nomeAtual.isBlank()) {
            Toast.makeText(this, "Dados da sala inválidos.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.txtTituloSala.text = "Sala de Espera"
        binding.txtCodigoSala.text = "Código da Sala: $codigoSala"
        binding.btnIniciarJogo.isEnabled = false

        garantirJogadorNaSala()
        escutarJogadores()
        escutarEstadoSala()
        escutarSalaApagada()

        binding.btnIniciarJogo.setOnClickListener {
            if (!admin) {
                Toast.makeText(this, "Só o criador da sala pode iniciar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            validarEIniciarJogo()
        }

        binding.btnSairSala.setOnClickListener {
            sairDaSala()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jogadoresListener?.let {
            database.child("salas").child(codigoSala).child("jogadores").removeEventListener(it)
        }
        estadoListener?.let {
            database.child("salas").child(codigoSala).child("estado").removeEventListener(it)
        }
        salaListener?.let {
            database.child("salas").child(codigoSala).removeEventListener(it)
        }
    }

    private fun garantirJogadorNaSala() {
        val jogadorRef = database.child("salas").child(codigoSala).child("jogadores").child(nomeAtual)
        jogadorRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                jogadorRef.setValue(
                    mapOf(
                        "nome" to nomeAtual,
                        "pontuacao" to 0.0,
                        "totalRespostasCertas" to 0,
                        "estado" to "on",
                        "isHostOnly" to admin
                    )
                )
            } else {
                jogadorRef.updateChildren(
                    mapOf(
                        "estado" to "on",
                        "isHostOnly" to admin
                    )
                )
            }
        }
    }

    private fun escutarJogadores() {
        jogadoresListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                jogadoresInfo.clear()
                jogadoresNaSala = snapshot.children.mapNotNull { jogadorSnapshot ->
                    val nome = jogadorSnapshot.key ?: return@mapNotNull null
                    val isHostOnly = jogadorSnapshot.child("isHostOnly").getValue(Boolean::class.java) == true
                    jogadoresInfo[nome] = isHostOnly
                    nome
                }
                binding.txtListaJogadores.text = if (jogadoresNaSala.isEmpty()) {
                    "Aguardando jogadores..."
                } else {
                    jogadoresNaSala.joinToString(separator = "\n")
                }
                binding.btnIniciarJogo.isEnabled = admin && jogadoresReais().size >= minimoJogadoresGrupo
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SalaDeEsperaGrupoActivity, "Erro ao carregar jogadores.", Toast.LENGTH_SHORT).show()
            }
        }
        database.child("salas").child(codigoSala).child("jogadores")
            .addValueEventListener(jogadoresListener!!)
    }

    private fun escutarEstadoSala() {
        estadoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(String::class.java) == "em_jogo") {
                    val intent = Intent(this@SalaDeEsperaGrupoActivity, JogoActivity::class.java)
                    intent.putExtra("codigoSala", codigoSala)
                    intent.putExtra("nomeUtilizador", nomeUtilizador ?: "")
                    intent.putExtra("nomeJogador", nomeJogador ?: nomeAtual)
                    intent.putExtra("nomeCategoria", nomeCategoria)
                    intent.putExtra("modoJogo", modoJogo)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SalaDeEsperaGrupoActivity, "Erro ao escutar estado da sala.", Toast.LENGTH_SHORT).show()
            }
        }
        database.child("salas").child(codigoSala).child("estado")
            .addValueEventListener(estadoListener!!)
    }

    private fun jogadoresReais(): List<String> {
        return jogadoresNaSala.filter { jogador ->
            jogador != nomeAtual && jogador != "admin" && jogadoresInfo[jogador] != true
        }
    }

    private fun jogadoresReais(snapshot: DataSnapshot): List<String> {
        return snapshot.children.mapNotNull { jogadorSnapshot ->
            val nome = jogadorSnapshot.key ?: return@mapNotNull null
            val isHostOnly = jogadorSnapshot.child("isHostOnly").getValue(Boolean::class.java) == true
            if (nome != nomeAtual && nome != "admin" && !isHostOnly) nome else null
        }
    }

    private fun validarEIniciarJogo() {
        database.child("salas").child(codigoSala).child("jogadores").get()
            .addOnSuccessListener { snapshot ->
                if (jogadoresReais(snapshot).size < minimoJogadoresGrupo) {
                    Toast.makeText(
                        this,
                        "Aguarde pelo menos 1 jogador além do admin.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }
                database.child("salas").child(codigoSala).child("estado").setValue("em_jogo")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao validar jogadores.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun escutarSalaApagada() {
        salaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!saidaManual && !snapshot.exists()) {
                    Toast.makeText(this@SalaDeEsperaGrupoActivity, "A sala foi encerrada.", Toast.LENGTH_SHORT).show()
                    abrirMainActivity(this@SalaDeEsperaGrupoActivity, nomeUtilizador, nomeJogador ?: nomeAtual)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("salas").child(codigoSala).addValueEventListener(salaListener!!)
    }

    private fun sairDaSala() {
        saidaManual = true
        val salaRef = database.child("salas").child(codigoSala)
        if (admin) {
            salaRef.removeValue()
        } else {
            salaRef.child("jogadores").child(nomeAtual).removeValue()
        }
        abrirMainActivity(this, nomeUtilizador, nomeJogador ?: nomeAtual)
        finish()
    }
}
