package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.abrirEscolherCategoriaActivity
import com.example.brainbrawl.databinding.ActivityEscolherModoBinding

class EscolherModoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEscolherModoBinding.inflate(layoutInflater)
    }

    private val modoClassico = "classico"
    private val modoEliminatorias = "eliminatorias"
    private val modoCaotico = "caotico"
    private var nomeUtilizador: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra("nomeUtilizador")

        // Botão Clássico
        binding.btnModoClassico.setOnClickListener {
            abrirTipoModoClassico(modoClassico)
        }
        // Botão Eliminatória
        binding.btnModoEliminatorias.setOnClickListener {
            abrirEscolherCategoriaActivity(this, modoEliminatorias, nomeUtilizador)
        }
        // Botão Caótico:
        binding.btnModoCaotico.setOnClickListener {
            Uteis.abrirMainActivity(this, null, modoCaotico, nomeUtilizador)
        }
        // Botão Voltar
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            startActivity(intent)
        }
        // Botão de informação
        binding.infoTodos.setOnClickListener {
            mostrarExplicacaoTodosModos()
        }
    }

    private fun abrirTipoModoClassico(modo: String) {
        val intent = Intent(this, TipoModoClassico::class.java)
        intent.putExtra("modoJogo", modo)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("codigoSala", Uteis.gerarCodigoSala())
        startActivity(intent)
        finish()
    }

    private fun mostrarExplicacaoTodosModos() {
        val mensagem = getString(R.string.info_todos_modos)
        AlertDialog.Builder(this)
            .setTitle("Modos de Jogo")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }
}