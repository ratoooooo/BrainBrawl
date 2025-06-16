package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.criarSalaComCategoriaEEntrar
import com.example.brainbrawl.Uteis.gerarCodigoSala
import com.example.brainbrawl.databinding.ActivityEscolherCategoriaBinding

class EscolherCategoriaActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityEscolherCategoriaBinding.inflate(layoutInflater)
    }
    //Armazenar código da sala, modo de jogo e nome do utilizador
    private lateinit var codigoSala: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar o código da sala
        val modoJogo = intent.getStringExtra("modoJogo")
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val nomeJogador = intent.getStringExtra("nomeJogador") ?: ""
        val admin = intent.getBooleanExtra("admin", false)

        if (modoJogo == null) {
            finish()
            return
        }

        // Gerar código da sala só aqui
        codigoSala = gerarCodigoSala()
        // Mapa para converter nome visível em chave Firebase
        val categoriaFirebase = mapOf(
            getString(R.string.categoria1) to "Historia",
            getString(R.string.categoria2) to "Geografia",
            getString(R.string.categoria3) to "Desporto",
            getString(R.string.categoria4) to "Cultura Geral",
            getString(R.string.categoria5) to "Todas as categorias"
        )

        // Para cada botão, usa o texto visível para ir buscar a chave Firebase correta:
        binding.btnCategoria1.setOnClickListener {
            binding.btnCategoria1.isEnabled = false
            val categoriaEscolhida = categoriaFirebase[getString(R.string.categoria1)] ?: "Historia"
            criarSalaComCategoriaEEntrar(
                context = this,
                codigoSala = codigoSala,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                nomeCategoria = categoriaEscolhida,
                admin = admin,
                modoJogo = modoJogo,
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
        }
        binding.btnCategoria2.setOnClickListener {
            binding.btnCategoria2.isEnabled = false
            val categoriaEscolhida = categoriaFirebase[getString(R.string.categoria2)] ?: "Geografia"
            criarSalaComCategoriaEEntrar(
                context = this,
                codigoSala = codigoSala,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                nomeCategoria = categoriaEscolhida,
                admin = admin,
                modoJogo = modoJogo,
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
        }
        binding.btnCategoria3.setOnClickListener {
            binding.btnCategoria3.isEnabled = false
            val categoriaEscolhida = categoriaFirebase[getString(R.string.categoria3)] ?: "Desporto"
            criarSalaComCategoriaEEntrar(
                context = this,
                codigoSala = codigoSala,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                nomeCategoria = categoriaEscolhida,
                admin = admin,
                modoJogo = modoJogo,
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
        }
        binding.btnCategoria4.setOnClickListener {
            binding.btnCategoria4.isEnabled = false
            val categoriaEscolhida = categoriaFirebase[getString(R.string.categoria4)] ?: "Cultura Geral"
            criarSalaComCategoriaEEntrar(
                context = this,
                codigoSala = codigoSala,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                nomeCategoria = categoriaEscolhida,
                admin = admin,
                modoJogo = modoJogo,
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
        }
        binding.btnCategoria5.setOnClickListener {
            binding.btnCategoria5.isEnabled = false
            val categoriaEscolhida = categoriaFirebase[getString(R.string.categoria6)] ?: "Gentílicos"
            criarSalaComCategoriaEEntrar(
                context = this,
                codigoSala = codigoSala,
                nomeUtilizador = nomeUtilizador,
                nomeJogador = nomeJogador,
                nomeCategoria = categoriaEscolhida,
                admin = admin,
                modoJogo = modoJogo,
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
        }
        binding.btnCriarCategoria.setOnClickListener {
            abrirAdicionarPerguntaActivity(modoJogo, nomeUtilizador)
        }
    }

    //Função para abrir a AdicionarPerguntaActivity
    private fun abrirAdicionarPerguntaActivity(modo: String, nomeUtilizador: String?) {
        codigoSala = gerarCodigoSala()
        val intent = Intent(this, AdicionarPerguntaActivity::class.java)
        codigoSala.let { intent.putExtra("codigoSala", it) }
        modo.let { intent.putExtra("modoJogo", it) }
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        startActivity(intent)
        finish()
    }
}