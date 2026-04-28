package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.UteisSala.gerarCodigoSala
import com.example.brainbrawl.databinding.ActivityConvidarAmigoBinding
import com.example.brainbrawl.repositories.AmigosRepository

class ConvidarAmigo1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConvidarAmigoBinding.inflate(layoutInflater)
    }
    private val amigosRepository = AmigosRepository()
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
            val categoriaSelecionada = nomeCategoria ?: getString(R.string.categoria5)
            amigosRepository.enviarConvite1x1(
                nomeUtilizador,
                amigoSelecionado,
                codigoSala,
                categoriaSelecionada
            )
            Toast.makeText(this, "Convite enviado para $amigoSelecionado!", Toast.LENGTH_SHORT).show()
            // Envia o utilizador para a sala de espera 1x1
            val intent = Intent(this, SalaDeEspera1x1Activity::class.java)
            intent.putExtra("codigoSala", codigoSala)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            intent.putExtra("nomeCategoria", categoriaSelecionada)
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
        amigosRepository.carregarListaAmigos(nomeUtilizador)
            .addOnSuccessListener { nomesAmigos ->
                amigos.addAll(nomesAmigos)
                convidarAmigoAdapter.notifyDataSetChanged()
            }
    }
}
