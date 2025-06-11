package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.databinding.ActivityEscolhaCategoriaModosBinding

class EscolhaCategoriaModosActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityEscolhaCategoriaModosBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val modoJogo = intent.getStringExtra("modoJogo")
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")

        if (modoJogo == null) {
            finish()
            return
        }

        // Configurar os botões de categoria
        binding.btnCategoria1.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, getString(R.string.categoria1))
        }
        binding.btnCategoria2.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, getString(R.string.categoria2))
        }
        binding.btnCategoria3.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, getString(R.string.categoria3))
        }
        binding.btnCategoria4.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, getString(R.string.categoria4))
        }
        binding.btnCategoria6.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, getString(R.string.categoria6))
        }
        binding.btnCategoria5.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, getString(R.string.categoria5))
        }
    }

    // Função para abrir a próxima activity dependendo do modo de jogo selecionado
    private fun abrirProximaActivity(modoJogo: String, nomeUtilizador: String?, categoria: String) {
        val intent = when (modoJogo) {
            "1x1" -> Intent(this, ConvidarAmigo1x1Activity::class.java)
            "2x2" -> Intent(this, ConvidarAmigo2x2Activity::class.java)
            else -> return
        }
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("categoria", categoria)
        startActivity(intent)
        finish()
    }

}