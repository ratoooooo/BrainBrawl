package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityConvidarAmigoBinding
import com.example.brainbrawl.models.UtilizadorSocial
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.services.AuthService

class ConvidarAmigo1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityConvidarAmigoBinding.inflate(layoutInflater)
    }
    private val amigosRepository = AmigosRepository()
    private val authService = AuthService()
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var utilizadorAtual: UtilizadorSocial? = null
    private val amigos = mutableListOf<UtilizadorSocial>()
    private lateinit var convidarAmigoAdapter: Convidar1x1AmigoAdapter
    private var nomeCategoria: String? = null
    private var categoriaPublicaId: String? = null
    private var donoUid: String? = null
    private var donoCategoria: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guarda os dados passados pela Intent
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: getString(R.string.categoria5)
        categoriaPublicaId = intent.getStringExtra(IntentExtras.CATEGORIA_PUBLICA_ID)
        donoUid = intent.getStringExtra(IntentExtras.DONO_UID)
        donoCategoria = intent.getStringExtra(IntentExtras.DONO_CATEGORIA)

        // Configurar a lista de amigos para convidar
        convidarAmigoAdapter = Convidar1x1AmigoAdapter(amigos) { amigoSelecionado ->
            val utilizador = utilizadorAtual ?: return@Convidar1x1AmigoAdapter
            val codigoSala = gerarCodigoSala()
            val categoriaSelecionada = nomeCategoria ?: getString(R.string.categoria5)
            amigosRepository.enviarConvite1x1(
                utilizador,
                amigoSelecionado,
                codigoSala,
                categoriaSelecionada,
                dadosCategoriaSelecionada()
            ).addOnSuccessListener {
                Toast.makeText(this, "Convite enviado para ${amigoSelecionado.nomeDisplay}!", Toast.LENGTH_SHORT).show()
                // Envia o utilizador para a sala de espera 1x1
                val intent = Intent(this, SalaDeEspera1x1Activity::class.java)
                intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador.ifBlank { utilizador.nomeDisplay })
                uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                intent.putExtra(IntentExtras.NOME_CATEGORIA, categoriaSelecionada)
                startActivity(intent)
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao enviar convite.", Toast.LENGTH_SHORT).show()
            }
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
        amigosRepository.resolverUtilizador(uid.ifBlank { nomeUtilizador }, nomeUtilizador)
            .addOnSuccessListener { utilizador ->
                val resolvido = utilizador ?: return@addOnSuccessListener
                val atual = if (resolvido.uid.isBlank() && uid.isNotBlank()) {
                    resolvido.copy(uid = uid)
                } else {
                    resolvido
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

    private fun dadosCategoriaSelecionada(): Map<String, Any> {
        categoriaPublicaId?.takeIf { it.isNotBlank() }?.let { id ->
            return mapOf(
                "categoriaPublica" to true,
                FirebasePaths.CATEGORIA_PUBLICA_ID to id
            )
        }

        val donoLegado = donoCategoria.orEmpty().ifBlank { nomeUtilizador }
        return if (!donoUid.isNullOrBlank() || donoLegado.isNotBlank()) {
            val dados = linkedMapOf<String, Any>(
                "categoriaPersonalizada" to true,
                "donoCategoria" to donoLegado
            )
            donoUid?.takeIf { it.isNotBlank() }?.let { dados[FirebasePaths.DONO_UID] = it }
            dados
        } else {
            emptyMap()
        }
    }
}
