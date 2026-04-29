package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisValidacao.validarCampos
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivitySalaDeEsperaBinding
import com.example.brainbrawl.repositories.JogadorRepository
import com.example.brainbrawl.repositories.SalaRepository

class SalaDeEsperaActivity : AppCompatActivity() {
    private val binding by lazy { ActivitySalaDeEsperaBinding.inflate(layoutInflater) }
    private val salaRepository = SalaRepository()
    private val jogadorRepository = JogadorRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)

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

            // Validação do código da sala: não pode estar vazio
            if (codSala.isEmpty()) {
                Toast.makeText(this, "Insira o código da sala!", Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
                return@setOnClickListener
            }

            val erro = validarCampos(nomeJogadorAtual)
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                binding.btnEntrarSala.isEnabled = true
                return@setOnClickListener
            }

            // Verifica no Firebase se a sala existe e se o nome já está na sala
            salaRepository.procurarSalaPorCodigo(codSala, nomeJogadorAtual)
                .addOnSuccessListener { resultado ->
                    if (!resultado.existe) {
                        Toast.makeText(this@SalaDeEsperaActivity, "Código da sala inválido", Toast.LENGTH_SHORT).show()
                        binding.btnEntrarSala.isEnabled = true
                        return@addOnSuccessListener
                    }

                    if (resultado.jogadorJaExiste) {
                        Toast.makeText(this@SalaDeEsperaActivity, "Nome de jogador já existe na sala", Toast.LENGTH_SHORT).show()
                        binding.btnEntrarSala.isEnabled = true
                        return@addOnSuccessListener
                    }

                    // Se for utilizador registado, busca avatar real
                    if (!nomeUtilizador.isNullOrEmpty()) {
                        jogadorRepository.obterAvatar(nomeUtilizador)
                            .addOnSuccessListener { avatar ->
                                adicionarJogadorComAvatar(nomeJogadorAtual, codSala, avatar)
                                irParaSalaDeEsperaGrupo(codSala, nomeJogadorAtual, nomeUtilizador)
                            }
                            .addOnFailureListener {
                                adicionarJogadorComAvatar(nomeJogadorAtual, codSala, "avatar_1_playstore")
                                irParaSalaDeEsperaGrupo(codSala, nomeJogadorAtual, nomeUtilizador)
                            }
                    } else {
                        adicionarJogadorComAvatar(nomeJogadorAtual, codSala, "avatar_1_playstore")
                        irParaSalaDeEsperaGrupo(codSala, nomeJogadorAtual, null)
                    }

                    binding.btnEntrarSala.isEnabled = false
                    binding.edtCodigoSala.isEnabled = false
                    binding.edtNomeJogador.isEnabled = false
                    Toast.makeText(this@SalaDeEsperaActivity, "Jogador adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this@SalaDeEsperaActivity, "Erro ao verificar sala: ${error.message}", Toast.LENGTH_SHORT).show()
                    binding.btnEntrarSala.isEnabled = true
                }
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun adicionarJogadorComAvatar(nomeJogador: String, codigoSala: String, avatar: String) {
        val jogadorData = mapOf(
            "nome" to nomeJogador,
            "pontuacao" to 0,
            "avatar" to avatar,
            "estado" to "on"
        )
        salaRepository.adicionarJogadorASala(codigoSala, nomeJogador, jogadorData)
    }

// Função para ir para a sala de espera do grupo
    private fun irParaSalaDeEsperaGrupo(codigoSala: String, nomeJogador: String, nomeUtilizador: String?) {
        // Redireciona para a SalaDeEsperaGrupoActivity com os dados necessários
        val intent = Intent(this, SalaDeEsperaGrupoActivity::class.java)
        intent.putExtra(IntentExtras.ADMIN, false)
        codigoSala.let { intent.putExtra(IntentExtras.CODIGO_SALA, it) }
        nomeJogador.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        startActivity(intent)
        finish()
    }

}
