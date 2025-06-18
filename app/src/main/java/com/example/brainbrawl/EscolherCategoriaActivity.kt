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
    private val binding by lazy { ActivityEscolherCategoriaBinding.inflate(layoutInflater) }
    private lateinit var codigoSala: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar o código da sala
        val modoJogo = intent.getStringExtra("modoJogo")
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val nomeJogador = intent.getStringExtra("nomeJogador")
        val admin = intent.getBooleanExtra("admin", false)

        if (modoJogo == null) {
            finish()
            return
        }

        codigoSala = gerarCodigoSala()
        val categoriaFirebase = mapOf(
            getString(R.string.categoria1) to "Historia",
            getString(R.string.categoria2) to "Geografia",
            getString(R.string.categoria3) to "Desporto",
            getString(R.string.categoria4) to "Cultura Geral",
            getString(R.string.categoria5) to "Gentílicos"
        )

        val criarSala = { categoriaEscolhida: String ->
            criarSalaComCategoriaEEntrar(
                this, codigoSala, nomeUtilizador, nomeJogador, categoriaEscolhida, admin, modoJogo
            ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
        }

        binding.btnCategoria1.setOnClickListener {
            binding.btnCategoria1.isEnabled = false
            criarSala(categoriaFirebase[getString(R.string.categoria1)] ?: "Historia")
        }
        binding.btnCategoria2.setOnClickListener {
            binding.btnCategoria2.isEnabled = false
            criarSala(categoriaFirebase[getString(R.string.categoria2)] ?: "Geografia")
        }
        binding.btnCategoria3.setOnClickListener {
            binding.btnCategoria3.isEnabled = false
            criarSala(categoriaFirebase[getString(R.string.categoria3)] ?: "Desporto")
        }
        binding.btnCategoria4.setOnClickListener {
            binding.btnCategoria4.isEnabled = false
            criarSala(categoriaFirebase[getString(R.string.categoria4)] ?: "Cultura Geral")
        }
        binding.btnCategoria5.setOnClickListener {
            binding.btnCategoria5.isEnabled = false
            criarSala(categoriaFirebase[getString(R.string.categoria5)] ?: "Gentílicos")
        }
        binding.btnCriarCategoria.setOnClickListener {
            abrirAdicionarPerguntaActivity(modoJogo, nomeUtilizador)
        }
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, EscolherModoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            intent.putExtra("admin", admin)
            startActivity(intent)
            finish()
        }
    }

    private fun abrirAdicionarPerguntaActivity(modo: String, nomeUtilizador: String?) {
        codigoSala = gerarCodigoSala()
        val intent = Intent(this, AdicionarPerguntaActivity::class.java)
        intent.putExtra("codigoSala", codigoSala)
        intent.putExtra("modoJogo", modo)
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        startActivity(intent)
        finish()
    }
}