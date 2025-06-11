package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.abrirEscolherCategoriaActivity
import com.example.brainbrawl.Uteis.abrirMainActivity
import com.example.brainbrawl.databinding.ActivityEscolherModoBinding

class EscolherModoActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityEscolherModoBinding.inflate(layoutInflater)
    }
    // Variáveis para os modos de jogo
    private val modoClassico = "classico"
    private val modoEliminatorias = "eliminatorias"
    private val modoCaotico = "caotico"
    private var nomeUtilizador: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar o nome do utilizador passado pelo Intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador")

        // Configurar o botao do modo clássico
        binding.btnModoClassico.setOnClickListener {
            abrirTipoModoClassico(modoClassico)
        }
        // Configurar o botão do modo eliminatórias
        binding.btnModoEliminatorias.setOnClickListener {
            abrirEscolherCategoriaActivity(this, modoEliminatorias, nomeUtilizador)
            finish()
        }
        //Configurar o botão do modo caótico
        binding.btnModoCaotico.setOnClickListener {
            abrirMainActivity(this, null, modoCaotico, nomeUtilizador)
            finish()
        }
        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
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
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("codigoSala", Uteis.gerarCodigoSala())
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