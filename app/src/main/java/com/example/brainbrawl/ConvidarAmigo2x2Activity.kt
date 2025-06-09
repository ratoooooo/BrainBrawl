package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.databinding.ActivityConvidarAmigo2x2Binding
import com.google.firebase.database.FirebaseDatabase

class ConvidarAmigo2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConvidarAmigo2x2Binding.inflate(layoutInflater)
    }
    private val database = FirebaseDatabase.getInstance().reference
    private var nomeUtilizador: String = ""
    private val amigos = mutableListOf<String>()
    private lateinit var convidarAmigoAdapter: Convidar2x2AmigoAdapter
    private var categoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        categoria = intent.getStringExtra("categoria")

        // Adapter para selecioar varios amigos
        convidarAmigoAdapter = Convidar2x2AmigoAdapter(amigos)
        binding.recyclerConvidarAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvidarAmigos.adapter = convidarAmigoAdapter

        // Botão para convidar amigos
        binding.btnConvidar.setOnClickListener {
            val selecionados = convidarAmigoAdapter.getSelecionados()
            if (selecionados.size < 2 || selecionados.size > 3) {
                Toast.makeText(this, "Seleciona entre 2 e 3 amigos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            enviarConvite2x2(selecionados)
        }

        carregarListaAmigos()
    }


    // Função para enviar convites para o modo 2x2
    private fun enviarConvite2x2(amigosSelecionados: List<String>) {
        val salaId = Uteis.gerarCodigoSala()
        val jogadores = hashMapOf<String, Any>()
        jogadores[nomeUtilizador] = true

        // Adiciona os amigos selecionados como jogadores
        for (amigo in amigosSelecionados) {
            jogadores[amigo] = true
        }

        database.child("sala_2x2").child(salaId).setValue(
            mapOf(
                "jogadores" to jogadores,
                "estado" to "em_espera",
                "categoria" to (categoria ?: "Todas as categorias"),
            )
        )
        val conviteData = mapOf(
            "estado" to "pendente",
            "salaId" to salaId,
            "modo" to "2x2",
            "categoria" to (categoria ?: "Todas as categorias")
        )

        // Adiciona convite para cada amigo
        for (amigo in amigosSelecionados) {
            database.child("jogadores").child(amigo)
                .child("convites_recebidos").child(nomeUtilizador)
                .setValue(conviteData)
            database.child("jogadores").child(nomeUtilizador)
                .child("convites_enviados").child(amigo)
                .setValue(conviteData)
        }
        Toast.makeText(this, "Convite 2x2 enviado!", Toast.LENGTH_SHORT).show()
        // Vai para sala de espera 2x2
        val intent = Intent(this, SalaDeEspera2x2Activity::class.java)
        intent.putExtra("salaId", salaId)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("categoria", categoria ?: "Todas as categorias")
        startActivity(intent)
        finish()
    }

    //Função para carregar a lista de amigos do utilizador
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