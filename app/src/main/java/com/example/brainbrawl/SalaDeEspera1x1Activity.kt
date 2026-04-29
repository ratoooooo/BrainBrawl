package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEspera1x1Binding
import com.example.brainbrawl.repositories.JogoCompetitivoRepository
import com.example.brainbrawl.repositories.JogoCompetitivoRepository.ModoCompetitivo

class SalaDeEspera1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySalaDeEspera1x1Binding.inflate(layoutInflater)
    }
    private val jogoCompetitivoRepository = JogoCompetitivoRepository()

    // Variáveis para a lógica da sala
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private lateinit var nomeCategoria: String

    private var jogadoresNaSala: List<String> = emptyList()
    private var admin: Boolean = false
    private var jogadoresListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var estadoListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var salaListener: JogoCompetitivoRepository.ListenerHandle? = null
    private var saidaManual = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar dados passados pelo intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: getString(R.string.categoria5)

        // Define o texto do código da sala usando o binding
        binding.txtCodigoSala.text = "Código da Sala: $codigoSala"

        // Adiciona este jogador à sala
        jogoCompetitivoRepository.adicionarJogador(ModoCompetitivo.UM_CONTRA_UM, codigoSala, nomeUtilizador)
        // Marca este jogador como pronto na sala
        jogoCompetitivoRepository.marcarPronto1x1(codigoSala, nomeUtilizador)

        // Verifica se és o admin
        jogoCompetitivoRepository.obterAdmin(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
            .addOnSuccessListener { nomeAdmin ->
                admin = if (nomeAdmin.isNullOrBlank()) {
                    jogadoresNaSala.firstOrNull() == nomeUtilizador
                } else {
                    nomeAdmin == nomeUtilizador
                }
                atualizarEstadoBotaoIniciar()
            }

        // Observa os jogadores na sala e atualiza a lista no ecrã
        jogadoresListener = jogoCompetitivoRepository.escutarJogadores(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            onJogadoresAlterados = { nomes ->
                jogadoresNaSala = nomes
                binding.txtListaJogadores.text = if (nomes.isEmpty()) {
                    "Aguardando jogadores..."
                } else {
                    nomes.joinToString(separator = "\n")
                }
                // Ativa o botão de iniciar jogo se for admin e houver 2 jogadores
                atualizarEstadoBotaoIniciar()
            }
        )

        // Observa o estado da sala para iniciar o jogo para todos ao mesmo tempo
        estadoListener = jogoCompetitivoRepository.escutarEstadoSala(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            onEstadoAlterado = { estado ->
                if (estado == GameConstants.ESTADO_EM_JOGO) {
                    val intent = Intent(this@SalaDeEspera1x1Activity, Jogo1x1Activity::class.java)
                    intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
                    intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                    intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                    intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
                    startActivity(intent)
                    finish()
                }
            }
        )

        escutarSalaApagada()

        // Listener para o clique no botão de iniciar jogo
        binding.btnIniciarJogo.setOnClickListener {
            if (admin && jogadoresNaSala.size == 2) {
                verificarProntosEAvancar()
            } else {
                Toast.makeText(this, "Ainda a aguardar o adversário!", Toast.LENGTH_SHORT).show()
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

    private fun atualizarEstadoBotaoIniciar() {
        binding.btnIniciarJogo.isEnabled = admin && jogadoresNaSala.size == 2
    }

    // Função para verificar se ambos os jogadores estão prontos antes de iniciar
    private fun verificarProntosEAvancar() {
        jogoCompetitivoRepository.obterProntos1x1(codigoSala)
            .addOnSuccessListener { prontos ->
                if (prontos.size == 2 && jogadoresNaSala.size == 2) {
                    // Altera o estado da sala para "em_jogo",
                    jogoCompetitivoRepository.atualizarEstadoSala(
                        ModoCompetitivo.UM_CONTRA_UM,
                        codigoSala,
                        GameConstants.ESTADO_EM_JOGO
                    )
                } else {
                    Toast.makeText(this@SalaDeEspera1x1Activity, "Ambos os jogadores têm de estar na sala!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun escutarSalaApagada() {
        salaListener = jogoCompetitivoRepository.escutarSalaApagada(
            ModoCompetitivo.UM_CONTRA_UM,
            codigoSala,
            onSalaExisteAlterada = { existe ->
                if (!saidaManual && !existe) {
                    Toast.makeText(this@SalaDeEspera1x1Activity, "A sala foi encerrada.", Toast.LENGTH_SHORT).show()
                    abrirMainActivity(this@SalaDeEspera1x1Activity, nomeUtilizador, nomeJogador)
                    finish()
                }
            }
        )
    }

    private fun sairDaSala() {
        saidaManual = true
        if (admin) {
            jogoCompetitivoRepository.apagarSala(ModoCompetitivo.UM_CONTRA_UM, codigoSala)
        } else {
            jogoCompetitivoRepository.removerJogador1x1(codigoSala, nomeUtilizador)
        }
        abrirMainActivity(this, nomeUtilizador, nomeJogador)
        finish()
    }
}
