package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.abrirEscolherCategoriaActivity
import com.example.brainbrawl.Uteis.gerarCodigoSala
import com.example.brainbrawl.databinding.ActivityEscolherModoBinding

class EscolherModoActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityEscolherModoBinding.inflate(layoutInflater)
    }
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar o nome do utilizador e do jogador passado pelo Intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        nomeJogador = intent.getStringExtra("nomeJogador")

        // Configurar o botao do modo clássico
        binding.btnModoClassico.setOnClickListener {
            abrirTipoModoClassico("classico")
        }
        // Configurar o botão do modo eliminatórias
        binding.btnModoEliminatorias.setOnClickListener {
            abrirEscolherCategoriaActivity(this, "eliminatorias", nomeUtilizador, nomeJogador, true)
            finish()
        }
        //Configurar o botão do modo caótico
        binding.btnModoCaotico.setOnClickListener {
            if (nomeJogador.isNullOrEmpty()) {
                // Pede o nome se não estiver preenchido
                Toast.makeText(this, "Por favor insere o teu nome!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Uteis.criarSalaCaoticaEEntrar(this, nomeUtilizador, nomeJogador!!)
            finish()
        }
        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            startActivity(intent)
            finish()
        }
        // Botão de informação
        binding.infoTodos.setOnClickListener {
            mostrarExplicacaoTodosModos()
        }
    }

    // Função para abrir o TipoModoClassico com o modo de jogo selecionado
    private fun abrirTipoModoClassico(modo: String) {
        val intent = Intent(this, TipoModoClassico::class.java)
        modo.let { intent.putExtra("modoJogo", it) }
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
        gerarCodigoSala().let { intent.putExtra("codigoSala", it) }
        startActivity(intent)
        finish()
    }

    // Função para mostrar a explicação de todos os modos de jogo
    private fun mostrarExplicacaoTodosModos() {
        val mensagem = getString(R.string.info_todos_modos)
        AlertDialog.Builder(this)
            .setTitle("Modos de Jogo")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }
}