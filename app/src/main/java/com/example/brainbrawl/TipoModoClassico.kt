package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.abrirEscolherCategoriaActivity
import com.example.brainbrawl.databinding.ActivityTipoModoClassicoBinding

class TipoModoClassico : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityTipoModoClassicoBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // Receber dados passados do intent
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val modoJogo = intent.getStringExtra("modoJogo")

        // Configurar o botao para o modo 1x1
        binding.btnModo1x1.setOnClickListener {
            // Verifica se o jogador esta logado
            if (nomeUtilizador.isNullOrEmpty()) {
                mostrarMensagemLoginObrigatorio()
            } else {
                // Redireciona para a EscolhaCategoriaModosActivity com o modo 1x1
                val intent = Intent(this, EscolhaCategoriaModosActivity::class.java)
                // Passa o modo de jogo e o nome do utilizador
                intent.putExtra("modoJogo", "1x1")
                intent.putExtra("nomeUtilizador", nomeUtilizador)
                startActivity(intent)
            }
        }

        // Configurar o botao para o modo 2x2
        binding.btnModo2x2.setOnClickListener {
            // Verifica se o jogador esta logado
            if (nomeUtilizador.isNullOrEmpty()) {
                mostrarMensagemLoginObrigatorio()
            } else {
                // Redireciona para a EscolhaCategoriaModosActivity com o modo 2x2
                val intent = Intent(this, EscolhaCategoriaModosActivity::class.java)
                // Passa o modo de jogo e o nome do utilizador
                intent.putExtra("modoJogo", "2x2")
                intent.putExtra("nomeUtilizador", nomeUtilizador)
                startActivity(intent)
            }
        }

        // Configurar o botao para o modo de grupo
        binding.btnModoGrupo.setOnClickListener {
            // Chama a função para abrir a EscolherCategoriaActivity com o modo de jogo selecionado
            abrirEscolherCategoriaActivity(this, modoJogo.toString(), nomeUtilizador)
        }

        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            finish()
        }

        // Configurar o botão de informação sobre todos os modos de jogo
        binding.infoTodos.setOnClickListener {
            // Chama a função para mostrar explicação de todos os modos de jogo
            mostrarExplicacaoTodosModos()
        }
    }

    // Função para mostrar mensagem de login obrigatório
    private fun mostrarMensagemLoginObrigatorio() {
        AlertDialog.Builder(this)
            .setTitle("Iniciar sessão necessária")
            .setMessage("Para jogar no modo 1x1 ou 2x2, por favor inicia sessão ou cria uma conta.")
            .setPositiveButton("OK", null)
            .show()
    }

    // Função para mostrar explicação de todos os modos de jogo
    private fun mostrarExplicacaoTodosModos() {
        val mensagem = getString(R.string.info_todos_modos_classico)
        AlertDialog.Builder(this)
            .setTitle("Modos de Jogo")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }
}