package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.UteisSala.gerarCodigoSala
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
    private var nomeCategoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        nomeCategoria = intent.getStringExtra("nomeCategoria")

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
            // Chama a função para enviar convite 2x2
            enviarConvite2x2(selecionados)
        }

        // Chama a função para carregar a lista de amigos
        carregarListaAmigos()
    }

    // Função para enviar convites para o modo 2x2
    private fun enviarConvite2x2(amigosSelecionados: List<String>) {
        val codigoSala = gerarCodigoSala()
        val jogadores = hashMapOf<String, Any>()
        jogadores[nomeUtilizador] = true

        // Adiciona os amigos selecionados como jogadores
        for (amigo in amigosSelecionados) {
            jogadores[amigo] = true
        }

        // Cria a sala 2x2 no Firebase
        database.child("sala_2x2").child(codigoSala).setValue(
            mapOf(
                "jogadores" to jogadores,
                "admin" to nomeUtilizador,
                "estado" to "em_espera",
                "nomeCategoria" to (nomeCategoria ?: getString(R.string.categoria5) ),
            )
        )
        // Mapa de dados do convite
        val conviteData = mapOf(
            "estado" to "pendente",
            "codigoSala" to codigoSala,
            "modo" to "2x2",
            "nomeCategoria" to (nomeCategoria ?: getString(R.string.categoria5) )
        )

        // Envia o convite para cada amigo selecionado
        for (amigo in amigosSelecionados) {
            database.child("jogadores").child(amigo)
                .child("convites_recebidos").child(nomeUtilizador)
                .setValue(conviteData)
            // Regista o convite enviado pelo utilizador
            database.child("jogadores").child(nomeUtilizador)
                .child("convites_enviados").child(amigo)
                .setValue(conviteData)
        }
        Toast.makeText(this, "Convite 2x2 enviado!", Toast.LENGTH_SHORT).show()
        // Vai para sala de espera 2x2
        val intent = Intent(this, SalaDeEspera2x2Activity::class.java)
        intent.putExtra("codigoSala", codigoSala)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("nomeCategoria", nomeCategoria ?: getString(R.string.categoria5) )
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
