package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisNavegacao.abrirEscolherCategoriaActivity
import com.example.brainbrawl.UteisSala.criarSalaCaoticaEEntrar
import com.example.brainbrawl.UteisSala.gerarCodigoSala
import com.example.brainbrawl.databinding.ActivityEscolherModoBinding

class EscolherModoActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy { ActivityEscolherModoBinding.inflate(layoutInflater) }
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var admin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar o nome do utilizador, do jogador e admin passado pelo Intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        nomeJogador = intent.getStringExtra("nomeJogador")
        admin = intent.getBooleanExtra("admin", false)

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
            // Se existir nomeUtilizador (registado), usa esse. Senão, usa nomeJogador (convidado).
            if (nomeUtilizador != null) {
                criarSalaCaoticaEEntrar(this, nomeUtilizador, null)
            } else if (nomeJogador != null) {
                criarSalaCaoticaEEntrar(this, null, nomeJogador)
            } else {
                Toast.makeText(this, "Indique o seu nome!", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            intent.putExtra("admin", false)
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
        intent.putExtra("modoJogo", modo)
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
        intent.putExtra("admin", true)
        intent.putExtra("codigoSala", gerarCodigoSala())
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