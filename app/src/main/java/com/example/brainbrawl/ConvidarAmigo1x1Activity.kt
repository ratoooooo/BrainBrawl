package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityConvidarAmigoBinding
import com.example.brainbrawl.models.UtilizadorSocial
import com.example.brainbrawl.repositories.AmigosRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala

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
    private var nomeCategoria: String? = null
    private var categoriaPublicaId: String? = null
    private var donoUid: String? = null
    private var donoCategoria: String? = null
    private var amigoSelecionado: UtilizadorSocial? = null
    private lateinit var convidarAmigoAdapter: Convidar1x1AmigoAdapter
    private var formatoSelecionado: String = GameConstants.MODO_1X1
    private var envioEmCurso = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guarda os dados passados pela Intent
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)
        categoriaPublicaId = intent.getStringExtra(IntentExtras.CATEGORIA_PUBLICA_ID)
        donoUid = intent.getStringExtra(IntentExtras.DONO_UID)
        donoCategoria = intent.getStringExtra(IntentExtras.DONO_CATEGORIA)
        formatoSelecionado = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_1X1

        if (!validarEntrada()) return

        binding.txtResumoModo.text = getString(R.string.modo_resumo_format, getString(R.string.modo_1x1_curto))
        binding.txtResumoCategoria.text = getString(R.string.categoria_resumo_format, nomeCategoria.orEmpty())
        convidarAmigoAdapter = Convidar1x1AmigoAdapter(amigos) { amigo ->
            amigoSelecionado = amigo
            atualizarBotaoEnviar()
        }
        binding.recyclerConvidarAmigos.layoutManager = LinearLayoutManager(this)
        binding.recyclerConvidarAmigos.adapter = convidarAmigoAdapter

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.btnVoltarConvidar.setOnClickListener { finish() }
        binding.btnEnviarConvite.setOnClickListener {
            enviarConvite1x1()
        }
        atualizarBotaoEnviar()

        // Chama a função para carregar a lista de amigos
        carregarListaAmigos()
    }

    private fun validarEntrada(): Boolean {
        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            Toast.makeText(this, R.string.convites_precisam_conta, Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        if (formatoSelecionado != GameConstants.MODO_1X1) {
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
                        selecionarAmigoInicialSeVeioPorIntent()
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

    private fun selecionarAmigoInicialSeVeioPorIntent() {
        val nomeExtra = intent.getStringExtra(IntentExtras.NOME_AMIGO).orEmpty()
        val uidExtra = intent.getStringExtra(IntentExtras.UID_AMIGO).orEmpty()
        val amigo = amigos.firstOrNull { amigo ->
            (uidExtra.isNotBlank() && amigo.uid == uidExtra) ||
                (nomeExtra.isNotBlank() && amigo.nomeDisplay == nomeExtra)
        }?.takeIf { it.online }
        if (amigo != null) {
            convidarAmigoAdapter.selecionar(amigo)
        }
    }

    private fun atualizarBotaoEnviar() {
        val ativo = amigoSelecionado != null && !envioEmCurso
        binding.btnEnviarConvite.isEnabled = ativo
        binding.btnEnviarConvite.alpha = if (ativo) 1f else 0.45f
    }

    private fun enviarConvite1x1() {
        if (envioEmCurso) return
        val utilizador = utilizadorAtual ?: return
        val amigo = amigoSelecionado ?: run {
            Toast.makeText(this, R.string.sem_amigos_para_convidar, Toast.LENGTH_SHORT).show()
            return
        }
        envioEmCurso = true
        atualizarBotaoEnviar()
        val codigoSala = gerarCodigoSala()
        val categoriaSelecionada = nomeCategoria.orEmpty()
        amigosRepository.enviarConvite1x1(
            utilizador,
            amigo,
            codigoSala,
            categoriaSelecionada,
            dadosCategoriaSelecionada()
        ).addOnSuccessListener {
            Log.d(
                FLOW_TAG,
                "flow=${GameConstants.ORIGEM_CONVITE} mode=${GameConstants.MODO_1X1} room=$codigoSala " +
                    "event=createInviteRoom uid=${uid.maskedLogId()} invited=${amigo.uid.ifBlank { amigo.chaveConvite }.maskedLogId()} " +
                    "category=$categoriaSelecionada path=${FirebasePaths.SALA_1X1}/$codigoSala"
            )
            Toast.makeText(this, getString(R.string.convite_enviado_para_format, amigo.nomeDisplay), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SalaDeEspera1x1Activity::class.java)
            intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
            intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador.ifBlank { utilizador.nomeDisplay })
            uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
            intent.putExtra(IntentExtras.NOME_CATEGORIA, categoriaSelecionada)
            intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_1X1)
            intent.putExtra(IntentExtras.ORIGEM_SALA, GameConstants.ORIGEM_CONVITE)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            envioEmCurso = false
            atualizarBotaoEnviar()
            Toast.makeText(this, getString(R.string.erro_enviar_convite), Toast.LENGTH_SHORT).show()
        }
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
        const val FLOW_TAG = "FLOW_SEPARATION_DEBUG"
    }
}

private fun String.maskedLogId(): String {
    if (isBlank()) return ""
    return if (length <= 6) "***" else "${take(3)}...${takeLast(2)}"
}
