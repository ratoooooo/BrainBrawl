package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEsperaBinding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.SalaEntradaEvent
import com.example.brainbrawl.viewmodels.SalaGrupoViewModel

class SalaDeEsperaActivity : AppCompatActivity() {
    private val binding by lazy { ActivitySalaDeEsperaBinding.inflate(layoutInflater) }
    private val viewModel by lazy {
        ViewModelProvider(this)[SalaGrupoViewModel::class.java]
    }
    private val authService = AuthService()
    private var uid: String = ""
    private var nomeUtilizador: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        uid = intent.getStringExtra(IntentExtras.UID)
            ?: authService.utilizadorAtual()?.uid
            ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)

        configurarObservers()

        // Se for utilizador registado, bloqueia edição do nome
        if (!nomeUtilizador.isNullOrEmpty()) {
            binding.edtNomeJogador.setText(nomeUtilizador)
            binding.edtNomeJogador.isEnabled = false
        } else if (!nomeJogador.isNullOrEmpty()) {
            binding.edtNomeJogador.setText(nomeJogador)
            binding.edtNomeJogador.isEnabled = true
        } else {
            binding.edtNomeJogador.setText("")
            binding.edtNomeJogador.isEnabled = true
        }

        binding.btnEntrarSala.setOnClickListener {
            binding.btnEntrarSala.isEnabled = false

            val codSala = binding.edtCodigoSala.text.toString().trim()
            val nomeJogadorAtual = binding.edtNomeJogador.text.toString().trim()
            viewModel.entrarEmSala(codSala, nomeJogadorAtual, uid, nomeUtilizador)
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun configurarObservers() {
        viewModel.entrada.observe(this) { evento ->
            tratarEntrada(evento ?: return@observe)
            viewModel.consumirEntrada()
        }
    }

    private fun tratarEntrada(evento: SalaEntradaEvent) {
        when (evento) {
            SalaEntradaEvent.CodigoVazio -> {
                Toast.makeText(this, "Insira o código da sala!", Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
            }
            is SalaEntradaEvent.ValidacaoFalhou -> {
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
            }
            SalaEntradaEvent.CodigoInvalido -> {
                Toast.makeText(this, "Código da sala inválido", Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
            }
            SalaEntradaEvent.NomeJaExiste -> {
                Toast.makeText(this, "Nome de jogador já existe na sala", Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
            }
            is SalaEntradaEvent.ErroVerificarSala -> {
                Toast.makeText(this, "Erro ao verificar sala: ${evento.mensagem}", Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
            }
            is SalaEntradaEvent.JogadorAdicionado -> {
                binding.btnEntrarSala.isEnabled = false
                binding.edtCodigoSala.isEnabled = false
                binding.edtNomeJogador.isEnabled = false
                Toast.makeText(this, "Jogador adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                irParaSalaDeEsperaGrupo(evento.codigoSala, evento.nomeJogador, evento.nomeUtilizador, evento.uid)
            }
        }
    }

// Função para ir para a sala de espera do grupo
    private fun irParaSalaDeEsperaGrupo(
        codigoSala: String,
        nomeJogador: String,
        nomeUtilizador: String?,
        uid: String
    ) {
        // Redireciona para a SalaDeEsperaGrupoActivity com os dados necessários
        val intent = Intent(this, SalaDeEsperaGrupoActivity::class.java)
        intent.putExtra(IntentExtras.ADMIN, false)
        codigoSala.let { intent.putExtra(IntentExtras.CODIGO_SALA, it) }
        nomeJogador.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        startActivity(intent)
        finish()
    }

}
