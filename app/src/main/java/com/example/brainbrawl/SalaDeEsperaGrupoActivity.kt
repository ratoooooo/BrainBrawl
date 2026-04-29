package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera1x1Binding
import com.example.brainbrawl.repositories.SalaRepository

class SalaDeEsperaGrupoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySalaDeEspera1x1Binding.inflate(layoutInflater)
    }

    private val salaRepository = SalaRepository()
    private lateinit var codigoSala: String
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var nomeCategoria: String = ""
    private var modoJogo: String = GameConstants.MODO_CLASSICO
    private var admin = false
    private var nomeAtual: String = ""
    private var jogadoresNaSala: List<String> = emptyList()
    private var jogadoresListener: SalaRepository.ListenerHandle? = null
    private var estadoListener: SalaRepository.ListenerHandle? = null
    private var salaListener: SalaRepository.ListenerHandle? = null
    private var saidaManual = false
    private val minimoJogadoresGrupo = 1
    private val jogadoresInfo = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: "Todas as categorias"
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_CLASSICO
        admin = intent.getBooleanExtra(IntentExtras.ADMIN, false)
        nomeAtual = nomeUtilizador ?: nomeJogador ?: ""

        if (codigoSala.isBlank() || nomeAtual.isBlank()) {
            Toast.makeText(this, "Dados da sala inválidos.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.txtTituloSala.text = "Sala de Espera"
        binding.txtCodigoSala.text = "Código da Sala: $codigoSala"
        binding.btnIniciarJogo.isEnabled = false

        garantirJogadorNaSala()
        escutarJogadores()
        escutarEstadoSala()
        escutarSalaApagada()

        binding.btnIniciarJogo.setOnClickListener {
            if (!admin) {
                Toast.makeText(this, "Só o criador da sala pode iniciar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            validarEIniciarJogo()
        }

        binding.btnSairSala.setOnClickListener {
            sairDaSala()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        salaRepository.removerListener(jogadoresListener)
        salaRepository.removerListener(estadoListener)
        salaRepository.removerListener(salaListener)
    }

    private fun garantirJogadorNaSala() {
        salaRepository.garantirJogadorNaSala(codigoSala, nomeAtual, admin)
    }

    private fun escutarJogadores() {
        jogadoresListener = salaRepository.escutarJogadoresDaSala(
            codigoSala,
            onJogadoresAlterados = { jogadores ->
                jogadoresInfo.clear()
                jogadores.forEach { jogador ->
                    jogadoresInfo[jogador.nome] = jogador.isHostOnly
                }
                jogadoresNaSala = jogadores.map { it.nome }
                binding.txtListaJogadores.text = if (jogadoresNaSala.isEmpty()) {
                    "Aguardando jogadores..."
                } else {
                    jogadoresNaSala.joinToString(separator = "\n")
                }
                binding.btnIniciarJogo.isEnabled = admin && jogadoresReais().size >= minimoJogadoresGrupo
            },
            onErro = {
                Toast.makeText(this, "Erro ao carregar jogadores.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun escutarEstadoSala() {
        estadoListener = salaRepository.escutarEstadoDaSala(
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_EM_JOGO) {
                    val intent = Intent(this@SalaDeEsperaGrupoActivity, JogoActivity::class.java)
                    intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
                    intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador ?: "")
                    intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador ?: nomeAtual)
                    intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
                    intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
                    startActivity(intent)
                    finish()
                }
            },
            onErro = {
                Toast.makeText(this, "Erro ao escutar estado da sala.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun jogadoresReais(): List<String> {
        return jogadoresNaSala.filter { jogador ->
            jogador != nomeAtual && jogador != GameConstants.JOGADOR_ADMIN && jogadoresInfo[jogador] != true
        }
    }

    private fun jogadoresReais(jogadores: List<SalaRepository.JogadorSala>): List<String> {
        return jogadores.mapNotNull { jogador ->
            if (jogador.nome != nomeAtual && jogador.nome != GameConstants.JOGADOR_ADMIN && !jogador.isHostOnly) {
                jogador.nome
            } else {
                null
            }
        }
    }

    private fun validarEIniciarJogo() {
        salaRepository.obterJogadoresDaSala(codigoSala)
            .addOnSuccessListener { jogadores ->
                if (jogadoresReais(jogadores).size < minimoJogadoresGrupo) {
                    Toast.makeText(
                        this,
                        "Aguarde pelo menos 1 jogador além do admin.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }
                salaRepository.atualizarEstadoSala(codigoSala, GameConstants.ESTADO_EM_JOGO)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao validar jogadores.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun escutarSalaApagada() {
        salaListener = salaRepository.escutarSalaApagada(
            codigoSala,
            onSalaExisteAlterada = { existe ->
                if (!saidaManual && !existe) {
                    Toast.makeText(this@SalaDeEsperaGrupoActivity, "A sala foi encerrada.", Toast.LENGTH_SHORT).show()
                    abrirMainActivity(this@SalaDeEsperaGrupoActivity, nomeUtilizador, nomeJogador ?: nomeAtual)
                    finish()
                }
            }
        )
    }

    private fun sairDaSala() {
        saidaManual = true
        if (admin) {
            salaRepository.apagarSala(codigoSala)
        } else {
            salaRepository.removerJogadorDaSala(codigoSala, nomeAtual)
        }
        abrirMainActivity(this, nomeUtilizador, nomeJogador ?: nomeAtual)
        finish()
    }
}
