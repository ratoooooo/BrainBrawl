package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera2x2Binding
import com.example.brainbrawl.repositories.JogoCompetitivoRepository
import com.example.brainbrawl.repositories.JogoCompetitivoRepository.ModoCompetitivo

class SalaDeEspera2x2Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySalaDeEspera2x2Binding.inflate(layoutInflater)
    }
    private val jogoCompetitivoRepository = JogoCompetitivoRepository()
    // Variáveis para armazenar informações da sala e do jogador
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private var jogadoresNaSala = mutableListOf<String>()
    private var admin = false
    private var categoria: String? = null
    private var jogadoresListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var estadoListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var salaListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var saidaManual = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        categoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)
            ?: intent.getStringExtra(IntentExtras.CATEGORIA_LEGACY)
            ?: getString(R.string.categoria5)

        // Mostrar o código da sala
        binding.txtCodigoSala.text = "Código da sala: $codigoSala"

        // Adicionar o jogador à sala
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala, nomeUtilizador)

        // Verificar se o jogador é o administrador (primeiro a entrar na sala)
        jogoCompetitivoRepository.obterAdmin(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
            .addOnSuccessListener { nomeAdmin ->
                admin = if (nomeAdmin.isNullOrBlank()) {
                    jogadoresNaSala.firstOrNull() == nomeUtilizador
                } else {
                    nomeAdmin == nomeUtilizador
                }
                atualizarBotaoEEquipas()
            }

        // Listener para jogadores na sala (ativa botão quando forem 4 e define as equipas)
        jogadoresListener = jogoCompetitivoRepository.escutarJogadores(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onJogadoresAlterados = { nomes ->
                jogadoresNaSala.clear()
                jogadoresNaSala.addAll(nomes)

                // Divide os jogadores em duas equipas ( 2 rimeiros A e os dois últimos B )
                val equipaA = jogadoresNaSala.take(2)
                val equipaB = jogadoresNaSala.drop(2).take(2)

                // Atualiza os TextViews com os nomes dos jogadores
                binding.txtJogadorA1.text = equipaA.getOrNull(0) ?: "Aguardando..."
                binding.txtJogadorA2.text = equipaA.getOrNull(1) ?: "Aguardando..."
                binding.txtJogadorB1.text = equipaB.getOrNull(0) ?: "Aguardando..."
                binding.txtJogadorB2.text = equipaB.getOrNull(1) ?: "Aguardando..."

                atualizarBotaoEEquipas()
            }
        )

        // Listener do estado da sala para iniciar o jogo
        estadoListener = jogoCompetitivoRepository.escutarEstadoSala(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_EM_JOGO) {
                    val intent = Intent(this@SalaDeEspera2x2Activity, Jogo2x2Activity::class.java)
                    codigoSala.let { intent.putExtra(IntentExtras.CODIGO_SALA, it) }
                    nomeUtilizador.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
                    nomeJogador.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
                    categoria?.let {
                        intent.putExtra(IntentExtras.NOME_CATEGORIA, it)
                        intent.putExtra(IntentExtras.CATEGORIA_LEGACY, it)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        )

        escutarSalaApagada()

        // Configurar botão de Iniciar Jogo
        binding.btnIniciarJogo.setOnClickListener {
            if (admin && jogadoresNaSala.size == 4) {
                jogoCompetitivoRepository.atualizarEstadoSala(
                    ModoCompetitivo.DOIS_CONTRA_DOIS,
                    codigoSala,
                    GameConstants.ESTADO_EM_JOGO
                )
            }
        }

        binding.btnSairSala.setOnClickListener {
            sairDaSala()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jogoCompetitivoRepository.removerListener(jogadoresListener)
        jogoCompetitivoRepository.removerListener(estadoListener)
        jogoCompetitivoRepository.removerListener(salaListener)
    }

    private fun atualizarBotaoEEquipas() {
        val salaCompleta = jogadoresNaSala.size == 4
        binding.btnIniciarJogo.isEnabled = admin && salaCompleta

        if (admin && salaCompleta) {
            val equipaA = jogadoresNaSala.take(2)
            val equipaB = jogadoresNaSala.drop(2).take(2)
            jogoCompetitivoRepository.guardarEquipas2x2(codigoSala, equipaA, equipaB)
        }
    }

    private fun escutarSalaApagada() {
        salaListener = jogoCompetitivoRepository.escutarSalaApagada(
            ModoCompetitivo.DOIS_CONTRA_DOIS,
            codigoSala,
            onSalaExisteAlterada = { existe ->
                if (!saidaManual && !existe) {
                    abrirMainActivity(this@SalaDeEspera2x2Activity, nomeUtilizador, nomeJogador)
                    finish()
                }
            }
        )
    }

    private fun sairDaSala() {
        saidaManual = true
        if (admin) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.DOIS_CONTRA_DOIS, codigoSala)
        } else {
            jogoCompetitivoRepository.removerJogador2x2(codigoSala, nomeUtilizador)
        }
        abrirMainActivity(this, nomeUtilizador, nomeJogador)
        finish()
    }
}
