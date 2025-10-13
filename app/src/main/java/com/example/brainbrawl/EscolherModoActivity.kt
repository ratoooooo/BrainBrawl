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

        // Guardar os dados passados pelo Intent
        nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        nomeJogador = intent.getStringExtra("nomeJogador")
        admin = intent.getBooleanExtra("admin", false)

        // Configurar os botoes de medos
        binding.btnModoClassico.setOnClickListener {
            abrirTipoModoClassico("classico")
        }
        binding.btnModoEliminatorias.setOnClickListener {
            abrirEscolherCategoriaActivity(this, "eliminatorias", nomeUtilizador, nomeJogador, true)
            finish()
        }
        binding.btnModoCaotico.setOnClickListener {
            //Verifica se o nome Utilizador ou nomeJogador foi passado
            if (nomeUtilizador != null) {
                criarSalaCaoticaEEntrar(this, nomeUtilizador, null)
            } else if (nomeJogador != null) {
                criarSalaCaoticaEEntrar(this, null, nomeJogador)
            } else {
                Toast.makeText(this, "Indique o seu nome!", Toast.LENGTH_SHORT).show()
            }

        }

        binding.btnVoltar.setOnClickListener {
            // Envia de volta para a MainActivity com os dados necessários
            val intent = Intent(this, MainActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            intent.putExtra("admin", false)
            startActivity(intent)
            finish()
        }
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