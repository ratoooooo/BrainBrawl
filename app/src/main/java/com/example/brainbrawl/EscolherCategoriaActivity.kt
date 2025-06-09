package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.abrirMainActivity
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

        //Receber dados passados do intent
        val modoJogo = intent.getStringExtra("modoJogo")
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")

        if (modoJogo == null) {
            finish()
            return
        }

        //Configurar os botões para abrir a MainActivity com a categoria correspondente
        binding.btnCategoria1.setOnClickListener {
                abrirMainActivity(this, getString(R.string.categoria1), modoJogo, nomeUtilizador)
        }
        binding.btnCategoria2.setOnClickListener {
                abrirMainActivity(this, getString(R.string.categoria2), modoJogo, nomeUtilizador)
        }
        binding.btnCategoria3.setOnClickListener {
                abrirMainActivity(this, getString(R.string.categoria3), modoJogo, nomeUtilizador)
        }
        binding.btnCategoria4.setOnClickListener {
                abrirMainActivity(this, getString(R.string.categoria4), modoJogo, nomeUtilizador)
        }
        binding.btnCategoria5.setOnClickListener {
                abrirMainActivity(this, getString(R.string.categoria6), modoJogo, nomeUtilizador)
        }
        binding.btnCriarCategoria.setOnClickListener {
                abrirAdicionarPerguntaActivity(modoJogo, nomeUtilizador)
        }
    }

    //Abrir a AdicionarPerguntaActivity
    private fun abrirAdicionarPerguntaActivity(modo: String, nome: String?) {
        codigoSala = gerarCodigoSala()
        val intent = Intent(this, AdicionarPerguntaActivity::class.java)
        intent.putExtra("codigoSala", codigoSala)
        intent.putExtra("modoJogo", modo)
        intent.putExtra("nomeUtilizador", nome)
        startActivity(intent)
        finish()
    }
}