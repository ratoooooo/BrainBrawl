package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityConvidarAmigo2x2Binding
import com.example.brainbrawl.models.UtilizadorSocial
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.services.AuthService

class ConvidarAmigo2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConvidarAmigo2x2Binding.inflate(layoutInflater)
    }
    private val amigosRepository = AmigosRepository()
    private val authService = AuthService()
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var utilizadorAtual: UtilizadorSocial? = null
    private val amigos = mutableListOf<UtilizadorSocial>()
    private lateinit var convidarAmigoAdapter: Convidar2x2AmigoAdapter
    private var nomeCategoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)

        // Adapter para selecioar varios amigos
        convidarAmigoAdapter = Convidar2x2AmigoAdapter(amigos)
        binding.recyclerConvidarAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvidarAmigos.adapter = convidarAmigoAdapter

        // Botão para convidar amigos
        binding.btnConvidar.setOnClickListener {
            val selecionados = convidarAmigoAdapter.getSelecionados()
            if (selecionados.size != 3) {
                Toast.makeText(this, "Seleciona 3 amigos para fechar o 2x2.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Chama a função para enviar convite 2x2
            enviarConvite2x2(selecionados)
        }

        // Chama a função para carregar a lista de amigos
        carregarListaAmigos()
    }

    // Função para enviar convites para o modo 2x2
    private fun enviarConvite2x2(amigosSelecionados: List<UtilizadorSocial>) {
        val utilizador = utilizadorAtual ?: return
        val codigoSala = gerarCodigoSala()
        val categoriaSelecionada = nomeCategoria ?: getString(R.string.categoria5)
        amigosRepository.enviarConvite2x2(
            utilizador,
            amigosSelecionados,
            codigoSala,
            categoriaSelecionada
        ).addOnSuccessListener {
            Toast.makeText(this, "Convite 2x2 enviado!", Toast.LENGTH_SHORT).show()
            // Vai para sala de espera 2x2
            val intent = Intent(this, SalaDeEspera2x2Activity::class.java)
            intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
            intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador.ifBlank { utilizador.nomeDisplay })
            uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
            intent.putExtra(IntentExtras.NOME_CATEGORIA, categoriaSelecionada)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao enviar convite 2x2.", Toast.LENGTH_SHORT).show()
        }
    }

    //Função para carregar a lista de amigos do utilizador
    private fun carregarListaAmigos() {
        amigos.clear()
        amigosRepository.resolverUtilizador(uid.ifBlank { nomeUtilizador }, nomeUtilizador)
            .addOnSuccessListener { utilizador ->
                val atualResolvido = utilizador ?: return@addOnSuccessListener
                val atual = if (atualResolvido.uid.isBlank() && uid.isNotBlank()) {
                    atualResolvido.copy(uid = uid)
                } else {
                    atualResolvido
                }
                utilizadorAtual = atual
                if (nomeUtilizador.isBlank()) nomeUtilizador = atual.nomeDisplay
                amigosRepository.carregarListaAmigos(atual)
                    .addOnSuccessListener { amigosCarregados ->
                        amigos.addAll(amigosCarregados)
                        convidarAmigoAdapter.notifyDataSetChanged()
                    }
            }
    }
}
