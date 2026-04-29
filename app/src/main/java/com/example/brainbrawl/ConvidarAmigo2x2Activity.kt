package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityConvidarAmigo2x2Binding
import com.example.brainbrawl.repositories.AmigosRepository

class ConvidarAmigo2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConvidarAmigo2x2Binding.inflate(layoutInflater)
    }
    private val amigosRepository = AmigosRepository()
    private var nomeUtilizador: String = ""
    private val amigos = mutableListOf<String>()
    private lateinit var convidarAmigoAdapter: Convidar2x2AmigoAdapter
    private var nomeCategoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)

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
        val categoriaSelecionada = nomeCategoria ?: getString(R.string.categoria5)
        amigosRepository.enviarConvite2x2(
            nomeUtilizador,
            amigosSelecionados,
            codigoSala,
            categoriaSelecionada
        )
        Toast.makeText(this, "Convite 2x2 enviado!", Toast.LENGTH_SHORT).show()
        // Vai para sala de espera 2x2
        val intent = Intent(this, SalaDeEspera2x2Activity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, categoriaSelecionada)
        startActivity(intent)
        finish()
    }

    //Função para carregar a lista de amigos do utilizador
    private fun carregarListaAmigos() {
        amigos.clear()
        amigosRepository.carregarListaAmigos(nomeUtilizador)
            .addOnSuccessListener { nomesAmigos ->
                amigos.addAll(nomesAmigos)
                convidarAmigoAdapter.notifyDataSetChanged()
            }
    }
}
