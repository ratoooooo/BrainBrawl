package com.example.brainbrawl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera1x1Binding
import com.example.brainbrawl.models.JogadorSalaIdentidade
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.SalaGrupoEvent
import com.example.brainbrawl.viewmodels.SalaGrupoJogadoresUiState
import com.example.brainbrawl.viewmodels.SalaGrupoViewModel

class SalaDeEsperaGrupoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySalaDeEspera1x1Binding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[SalaGrupoViewModel::class.java]
    }
    private val authService = AuthService()
    private lateinit var codigoSala: String
    private var uid: String = ""
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var nomeCategoria: String = ""
    private var modoJogo: String = GameConstants.MODO_CLASSICO
    private var admin = false
    private var nomeAtual: String = ""
    private var jogadorAtual: JogadorSalaIdentidade = JogadorSalaIdentidade()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: "Todas as categorias"
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_CLASSICO
        admin = intent.getBooleanExtra(IntentExtras.ADMIN, false)
        jogadorAtual = JogadorSalaIdentidade.from(uid, nomeUtilizador, nomeJogador)
        nomeAtual = jogadorAtual.nomeDisplay

        if (codigoSala.isBlank() || nomeAtual.isBlank()) {
            Toast.makeText(this, "Dados da sala inválidos.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.txtTituloSala.text = "Sala de Espera"
        binding.txtCodigoSala.text = "Código da Sala: $codigoSala"
        binding.btnCopiarCodigoSala.setOnClickListener {
            copiarCodigoSala()
        }
        binding.btnIniciarJogo.isEnabled = false

        configurarObservers()
        viewModel.iniciarSala(codigoSala, jogadorAtual, admin)
        viewModel.observarJogadores(codigoSala)
        viewModel.observarEstadoSala(codigoSala)
        viewModel.observarSalaApagada(codigoSala)

        binding.btnIniciarJogo.setOnClickListener {
            if (!admin) {
                Toast.makeText(this, "Só o criador da sala pode iniciar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.validarEIniciarJogo(codigoSala)
        }

        binding.btnSairSala.setOnClickListener {
            sairDaSala()
        }
    }

    override fun onDestroy() {
        viewModel.removerListeners()
        super.onDestroy()
    }

    private fun configurarObservers() {
        viewModel.jogadores.observe(this) { estado ->
            atualizarJogadores(estado)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun atualizarJogadores(estado: SalaGrupoJogadoresUiState) {
        binding.txtListaJogadores.text = if (estado.nomes.isEmpty()) {
            "Aguardando jogadores..."
        } else {
            estado.nomes.joinToString(separator = "\n")
        }
        binding.btnIniciarJogo.isEnabled = estado.podeIniciar
    }

    private fun tratarEvento(evento: SalaGrupoEvent) {
        when (evento) {
            SalaGrupoEvent.ErroCarregarJogadores ->
                Toast.makeText(this, "Erro ao carregar jogadores.", Toast.LENGTH_SHORT).show()
            SalaGrupoEvent.ErroEscutarEstado ->
                Toast.makeText(this, "Erro ao escutar estado da sala.", Toast.LENGTH_SHORT).show()
            SalaGrupoEvent.ErroValidarJogadores ->
                Toast.makeText(this, "Erro ao validar jogadores.", Toast.LENGTH_SHORT).show()
            SalaGrupoEvent.JogadoresInsuficientes ->
                Toast.makeText(this, "Aguarde pelo menos 1 jogador além do admin.", Toast.LENGTH_SHORT).show()
            SalaGrupoEvent.JogoIniciado -> {
                val intent = Intent(this@SalaDeEsperaGrupoActivity, JogoActivity::class.java)
                intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
                uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador ?: "")
                intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador ?: nomeAtual)
                intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
                intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
                startActivity(intent)
                finish()
            }
            SalaGrupoEvent.SalaEncerrada -> {
                Toast.makeText(this@SalaDeEsperaGrupoActivity, "A sala foi encerrada.", Toast.LENGTH_SHORT).show()
                abrirMainActivity(this@SalaDeEsperaGrupoActivity, nomeUtilizador, nomeJogador ?: nomeAtual, uid.ifBlank { null })
                finish()
            }
        }
    }

    private fun sairDaSala() {
        viewModel.sairDaSala(codigoSala, jogadorAtual, admin)
        abrirMainActivity(this, nomeUtilizador, nomeJogador ?: nomeAtual, uid.ifBlank { null })
        finish()
    }

    private fun copiarCodigoSala() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Código da sala", codigoSala))
        Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show()
    }
}
