package com.example.brainbrawl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera2x2Binding
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils
import com.example.brainbrawl.viewmodels.Sala2x2Event
import com.example.brainbrawl.viewmodels.Sala2x2UiState
import com.example.brainbrawl.viewmodels.Sala2x2ViewModel

class SalaDeEspera2x2Activity : AppCompatActivity() {

    private val binding by lazy {
        ActivitySalaDeEspera2x2Binding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[Sala2x2ViewModel::class.java]
    }

    private val authService = AuthService()

    private lateinit var codigoSala: String
    private var uid: String = ""
    private var nomeUtilizador: String = ""
    private var nomeJogador: String = ""
    private var categoria: String? = null
    private var playerKey: String = ""
    private var tipoJogador: String = ""
    private var avatar: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = CodigoSalaUtils.normalizarCodigo(intent.getStringExtra(IntentExtras.CODIGO_SALA).orEmpty())
        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
                    ?: ""

        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
            ?: nomeUtilizador

        categoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)
            ?: intent.getStringExtra(IntentExtras.CATEGORIA_LEGACY)
                    ?: getString(R.string.categoria5)
        playerKey = intent.getStringExtra(IntentExtras.PLAYER_KEY) ?: ""
        tipoJogador = intent.getStringExtra(IntentExtras.TIPO_JOGADOR) ?: ""
        avatar = intent.getStringExtra(IntentExtras.AVATAR) ?: ""

        binding.txtCodigoSala.text = getString(R.string.a_carregar_sala)
        binding.btnCopiarCodigoSala.visibility = View.GONE
        binding.btnCopiarCodigoSala.setOnClickListener {
            copiarCodigoSala()
        }

        configurarObservers()

        viewModel.carregarExposicaoCodigo(codigoSala)
        viewModel.iniciar(codigoSala, uid, nomeUtilizador, nomeJogador, playerKey, tipoJogador, avatar)
        viewModel.observarJogadores(codigoSala)
        viewModel.observarEstadoSala(codigoSala)
        viewModel.observarSalaApagada(codigoSala)

        binding.btnIniciarJogo.setOnClickListener {
            viewModel.iniciarJogo(codigoSala)
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
        viewModel.estado.observe(this) { estado ->
            atualizarEstadoSala(estado)
        }

        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun atualizarEstadoSala(estado: Sala2x2UiState) {
        binding.txtJogadorA1.text = estado.equipaA.getOrNull(0) ?: getString(R.string.aguardando)
        binding.txtJogadorA2.text = estado.equipaA.getOrNull(1) ?: getString(R.string.aguardando)
        binding.txtJogadorB1.text = estado.equipaB.getOrNull(0) ?: getString(R.string.aguardando)
        binding.txtJogadorB2.text = estado.equipaB.getOrNull(1) ?: getString(R.string.aguardando)
        atualizarCodigoSala(estado)
        binding.btnIniciarJogo.isEnabled = estado.podeIniciar
    }

    private fun atualizarCodigoSala(estado: Sala2x2UiState) {
        if (estado.codigoSalaVisivel) {
            binding.txtCodigoSala.text = getString(R.string.codigo_sala_format, codigoSala)
            binding.btnCopiarCodigoSala.visibility = View.VISIBLE
        } else {
            binding.txtCodigoSala.text = estado.textoCodigoSalaPrivado.ifBlank { getString(R.string.partida_por_convite) }
            binding.btnCopiarCodigoSala.visibility = View.GONE
        }
    }

    private fun tratarEvento(evento: Sala2x2Event) {
        when (evento) {
            Sala2x2Event.JogoIniciado -> {
                val intent = Intent(this@SalaDeEspera2x2Activity, Jogo2x2Activity::class.java)

                intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)

                uid.takeIf { it.isNotBlank() }?.let {
                    intent.putExtra(IntentExtras.UID, it)
                }

                intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                adicionarExtrasMatchmaking(intent)

                categoria?.let {
                    intent.putExtra(IntentExtras.NOME_CATEGORIA, it)
                    intent.putExtra(IntentExtras.CATEGORIA_LEGACY, it)
                }

                startActivity(intent)
                finish()
            }

            Sala2x2Event.SalaEncerrada -> {
                abrirMainActivity(
                    this@SalaDeEspera2x2Activity,
                    nomeUtilizador,
                    nomeJogador,
                    uid.ifBlank { null }
                )
                finish()
            }

            Sala2x2Event.ErroIniciarJogo -> {
                Toast.makeText(this, R.string.erro_iniciar_jogo_2x2, Toast.LENGTH_SHORT).show()
            }

            Sala2x2Event.EntradaBloqueada -> {
                Toast.makeText(this, R.string.sala_2x2_cheia, Toast.LENGTH_SHORT).show()
                abrirMainActivity(
                    this@SalaDeEspera2x2Activity,
                    nomeUtilizador,
                    nomeJogador,
                    uid.ifBlank { null }
                )
                finish()
            }
        }
    }

    private fun sairDaSala() {
        viewModel.sairDaSala(codigoSala)
        abrirMainActivity(this, nomeUtilizador, nomeJogador, uid.ifBlank { null })
        finish()
    }

    private fun adicionarExtrasMatchmaking(intent: Intent) {
        playerKey.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.PLAYER_KEY, it) }
        tipoJogador.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.TIPO_JOGADOR, it) }
        avatar.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.AVATAR, it) }
        intent.putExtra(IntentExtras.IS_GUEST, tipoJogador == com.example.brainbrawl.config.GameConstants.TIPO_JOGADOR_GUEST)
    }

    private fun copiarCodigoSala() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clipboard_codigo_sala), codigoSala))
        Toast.makeText(this, R.string.codigo_copiado, Toast.LENGTH_SHORT).show()
    }
}
