package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.UteisSala.gerarCodigoSala
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
    private var nomeCategoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guarda os dados passados pela Intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        nomeCategoria = intent.getStringExtra("nomeCategoria") ?: getString(R.string.categoria5)

        // Configurar a lista de amigos para convidar
        convidarAmigoAdapter = Convidar1x1AmigoAdapter(amigos) { amigoSelecionado ->
            val codigoSala = gerarCodigoSala()
            val salaRef = database.child("sala_1x1").child(codigoSala)
            salaRef.setValue(
                mapOf(
                    "jogadores" to mapOf(nomeUtilizador to true, amigoSelecionado to true),
                    "admin" to nomeUtilizador,
                    "estado" to "em_espera",
                    "nomeCategoria" to (nomeCategoria ?: getString(R.string.categoria5))
                )
            )
            val conviteData = mapOf(
                "estado" to "pendente",
                "codigoSala" to codigoSala,
                "nomeCategoria" to (nomeCategoria ?: getString(R.string.categoria5))
            )
            // Envia o convite para o amigo selecionado
            database.child("jogadores").child(amigoSelecionado)
                .child("convites_recebidos").child(nomeUtilizador)
                .setValue(conviteData)
            // Regista o convite enviado pelo utilizador
            database.child("jogadores").child(nomeUtilizador)
                .child("convites_enviados").child(amigoSelecionado)
                .setValue(conviteData)
            Toast.makeText(this, "Convite enviado para $amigoSelecionado!", Toast.LENGTH_SHORT).show()
            // Envia o utilizador para a sala de espera 1x1
            val intent = Intent(this, SalaDeEspera1x1Activity::class.java)
            intent.putExtra("codigoSala", codigoSala)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            intent.putExtra("nomeCategoria", nomeCategoria ?: getString(R.string.categoria5))
            startActivity(intent)
            finish()
        }
        binding.recyclerConvidarAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvidarAmigos.adapter = convidarAmigoAdapter

        // Chama a função para carregar a lista de amigos
        carregarListaAmigos()
    }

    // Função que carrega a lista de amigos do utilizador
    private fun carregarListaAmigos() {
        amigos.clear()
        // Só mostra amigos aceites!
        database.child("jogadores").child(nomeUtilizador).child("amigos")
            .get().addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    amigos.add(child.key ?: "")
                }
                convidarAmigoAdapter.notifyDataSetChanged()
            }
    }
}
