package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
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
    private var categoriaPublicaId: String? = null
    private var donoUid: String? = null
    private var donoCategoria: String? = null
    private var envioEmCurso = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados do intent
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)
        categoriaPublicaId = intent.getStringExtra(IntentExtras.CATEGORIA_PUBLICA_ID)
        donoUid = intent.getStringExtra(IntentExtras.DONO_UID)
        donoCategoria = intent.getStringExtra(IntentExtras.DONO_CATEGORIA)

        if (!validarEntrada()) return

        binding.txtResumoModo.text = getString(R.string.modo_resumo_format, getString(R.string.modo_2x2_curto))
        binding.txtResumoCategoria.text = getString(R.string.categoria_resumo_format, nomeCategoria.orEmpty())
        binding.btnBackHeader.setOnClickListener { finish() }

        // Adapter para selecionar varios amigos
        convidarAmigoAdapter = Convidar2x2AmigoAdapter(amigos) { quantidade ->
            atualizarContadorSelecionados(quantidade)
            atualizarBotaoEnviar()
        }
        binding.recyclerConvidarAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvidarAmigos.adapter = convidarAmigoAdapter
        binding.btnVoltarConvidar.setOnClickListener { finish() }

        // Botão para convidar amigos
        binding.btnConvidar.setOnClickListener {
            val selecionados = convidarAmigoAdapter.getSelecionados()
            if (selecionados.size != 3) {
                Toast.makeText(this, getString(R.string.seleciona_3_amigos_2x2), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Chama a função para enviar convite 2x2
            enviarConvite2x2(selecionados)
        }
        atualizarContadorSelecionados(0)
        atualizarBotaoEnviar()

        // Chama a função para carregar a lista de amigos
        carregarListaAmigos()
    }

    private fun validarEntrada(): Boolean {
        val modo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_2X2
        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            Toast.makeText(this, R.string.convites_precisam_conta, Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        if (modo != GameConstants.MODO_2X2) {
            Toast.makeText(this, R.string.fluxo_convite_invalido, Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        if (nomeCategoria.isNullOrBlank()) {
            Toast.makeText(this, R.string.categoria_convite_em_falta, Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        return true
    }

    // Função para enviar convites para o modo 2x2
    private fun enviarConvite2x2(amigosSelecionados: List<UtilizadorSocial>) {
        if (envioEmCurso) return
        val utilizador = utilizadorAtual ?: return
        envioEmCurso = true
        atualizarBotaoEnviar()
        val codigoSala = gerarCodigoSala()
        val categoriaSelecionada = nomeCategoria.orEmpty()
        amigosRepository.enviarConvite2x2(
            utilizador,
            amigosSelecionados,
            codigoSala,
            categoriaSelecionada,
            dadosCategoriaSelecionada()
        ).addOnSuccessListener {
            Log.d(
                FLOW_TAG,
                "flow=${GameConstants.ORIGEM_CONVITE} mode=${GameConstants.MODO_2X2} room=$codigoSala " +
                    "event=createInviteRoom uid=${uid.maskedLogId()} " +
                    "invited=${amigosSelecionados.map { amigo -> amigo.uid.ifBlank { amigo.chaveConvite }.maskedLogId() }} " +
                    "category=$categoriaSelecionada path=${FirebasePaths.SALA_2X2}/$codigoSala"
            )
            Toast.makeText(this, getString(R.string.convite_2x2_enviado), Toast.LENGTH_SHORT).show()
            // Vai para sala de espera 2x2
            val intent = Intent(this, SalaDeEspera2x2Activity::class.java)
            intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
            intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador.ifBlank { utilizador.nomeDisplay })
            uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
            intent.putExtra(IntentExtras.NOME_CATEGORIA, categoriaSelecionada)
            intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_2X2)
            intent.putExtra(IntentExtras.ORIGEM_SALA, GameConstants.ORIGEM_CONVITE)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            envioEmCurso = false
            atualizarBotaoEnviar()
            Toast.makeText(this, getString(R.string.erro_enviar_convite_2x2), Toast.LENGTH_SHORT).show()
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
                        atualizarEstadoVazio()
                        atualizarBotaoEnviar()
                    }
            }
    }

    private fun atualizarEstadoVazio() {
        val vazio = amigos.isEmpty()
        binding.txtEstadoConvidar.visibility = if (vazio) View.VISIBLE else View.GONE
        binding.recyclerConvidarAmigos.visibility = if (vazio) View.GONE else View.VISIBLE
    }

    private fun atualizarContadorSelecionados(quantidade: Int) {
        binding.txtSelecionados.text = getString(R.string.selecionados_2x2_format, quantidade)
    }

    private fun atualizarBotaoEnviar() {
        val ativo = convidarAmigoAdapter.getSelecionados().size == 3 && !envioEmCurso
        binding.btnConvidar.isEnabled = ativo
        binding.btnConvidar.alpha = if (ativo) 1f else 0.45f
    }

    private fun dadosCategoriaSelecionada(): Map<String, Any> {
        categoriaPublicaId?.takeIf { it.isNotBlank() }?.let { id ->
            return mapOf(
                "categoriaPublica" to true,
                FirebasePaths.CATEGORIA_PUBLICA_ID to id,
                FirebasePaths.CATEGORIA_ORIGEM to GameConstants.ORIGEM_CATEGORIA_PUBLICA
            )
        }

        val donoUidExplicito = donoUid.orEmpty().trim()
        val donoLegadoExplicito = donoCategoria.orEmpty().trim()
        return if (donoUidExplicito.isNotBlank() || donoLegadoExplicito.isNotBlank()) {
            val dados = linkedMapOf<String, Any>(
                "categoriaPersonalizada" to true,
                "donoCategoria" to donoLegadoExplicito.ifBlank { nomeUtilizador },
                FirebasePaths.CATEGORIA_ORIGEM to GameConstants.ORIGEM_CATEGORIA_PERSONALIZADA
            )
            donoUidExplicito.takeIf { it.isNotBlank() }?.let { dados[FirebasePaths.DONO_UID] = it }
            dados
        } else {
            mapOf(FirebasePaths.CATEGORIA_ORIGEM to GameConstants.ORIGEM_CATEGORIA_OFICIAL)
        }
    }

    private companion object {
        const val FLOW_TAG = "RoomFlow"
    }
}

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
