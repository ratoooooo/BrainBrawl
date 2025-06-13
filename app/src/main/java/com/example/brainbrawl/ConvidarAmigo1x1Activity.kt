package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.databinding.ActivityConvidarAmigoBinding
import com.google.firebase.database.FirebaseDatabase

class ConvidarAmigo1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConvidarAmigoBinding.inflate(layoutInflater)
    }
    private val database = FirebaseDatabase.getInstance().reference
    private var nomeUtilizador: String = ""
    private val amigos = mutableListOf<String>()
    private lateinit var convidarAmigoAdapter: Convidar1x1AmigoAdapter
    private var categoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        categoria = intent.getStringExtra("categoria")

        // Adapter para mostrar amigos com botão desafiar
        convidarAmigoAdapter = Convidar1x1AmigoAdapter(amigos) { amigoSelecionado ->
            // Ao desafiar um amigo, criar convite e sala 1x1
            val codigoSala = Uteis.gerarCodigoSala()
            val salaRef = database.child("sala_1x1").child(codigoSala)

            salaRef.setValue(
                mapOf(
                    "jogadores" to mapOf(nomeUtilizador to true, amigoSelecionado to true),
                    "estado" to "em_espera",
                    "categoria" to (categoria ?: "Todas as categorias")
                )
            )

            val conviteData = mapOf(
                "estado" to "pendente",
                "codigoSala" to codigoSala,
                "categoria" to (categoria ?: "Todas as categorias")
            )

            // Adicionar convite para o amigo
            database.child("jogadores").child(amigoSelecionado)
                .child("convites_recebidos").child(nomeUtilizador)
                .setValue(conviteData)
            // E registo do convite enviado para quem enviou
            database.child("jogadores").child(nomeUtilizador)
                .child("convites_enviados").child(amigoSelecionado)
                .setValue(conviteData)
            Toast.makeText(this, "Convite enviado para $amigoSelecionado!", Toast.LENGTH_SHORT).show()

            // Iniciar a atividade de sala de espera 1x1
            val intent = Intent(this, SalaDeEspera1x1Activity::class.java)
            intent.putExtra("codigoSala", codigoSala)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            intent.putExtra("categoria", categoria ?: "Todas as categorias")
            startActivity(intent)
            finish()
        }
        binding.recyclerConvidarAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvidarAmigos.adapter = convidarAmigoAdapter

        carregarListaAmigos()
    }

    private fun carregarListaAmigos() {
        amigos.clear()
        database.child("jogadores").child(nomeUtilizador).child("amigos")
            .get().addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    amigos.add(child.key ?: "")
                }
                convidarAmigoAdapter.notifyDataSetChanged()
            }
    }
}