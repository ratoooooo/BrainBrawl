package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisNavegacao.abrirEscolherCategoriaActivity
import com.example.brainbrawl.databinding.ActivityTipoModoClassicoBinding

class TipoModoClassico : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy { ActivityTipoModoClassicoBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val modoJogo = intent.getStringExtra("modoJogo")
        val nomeJogador = intent.getStringExtra("nomeJogador") ?: nomeUtilizador
        val admin = intent.getBooleanExtra("admin", false)

        // Configurar o botao para o modo 1x1
        binding.btnModo1x1.setOnClickListener {
            iniciarModoCompetitivo("1x1", nomeUtilizador)
        }

        // Configurar o botao para o modo 2x2
        binding.btnModo2x2.setOnClickListener {
            iniciarModoCompetitivo("2x2", nomeUtilizador)
        }

        // Configurar o botao para o modo de grupo
        binding.btnModoGrupo.setOnClickListener {
            // Chama a função para abrir a EscolherCategoriaActivity com o modo de jogo selecionado
            abrirEscolherCategoriaActivity(this, modoJogo.toString(), nomeUtilizador, nomeJogador, true)
        }

        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, EscolherModoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            intent.putExtra("admin", false)
            startActivity(intent)
            finish()
        }

        // Configurar o botão de informação sobre todos os modos de jogo
        binding.infoTodos.setOnClickListener {
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

    private fun iniciarModoCompetitivo(modo: String, nomeUtilizador: String?) {
        if (nomeUtilizador.isNullOrEmpty()) {
            mostrarMensagemLoginObrigatorio()
        } else {
            val intent = Intent(this, EscolhaCategoriaModosActivity::class.java)
            intent.putExtra("modoJogo", modo)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            intent.putExtra("admin", true)
            startActivity(intent)
        }
    }
}