package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityEscolhaCategoriaModosBinding

class EscolhaCategoriaModosActivity : AppCompatActivity() {
    // Aceder os elementos do layout
    private val binding by lazy {
        ActivityEscolhaCategoriaModosBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pela Intent
        val modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO)
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)

        // Verificar se o modo de jogo é válido
        if (modoJogo == null) {
            finish()
            return
        }

        // Configurar os botões de categoria
        binding.btnCategoria1.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, nomeJogador, getString(R.string.categoria1))
        }
        binding.btnCategoria2.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, nomeJogador, getString(R.string.categoria2))
        }
        binding.btnCategoria3.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, nomeJogador, getString(R.string.categoria3))
        }
        binding.btnCategoria4.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, nomeJogador, getString(R.string.categoria4))
        }
        binding.btnCategoria5.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, nomeJogador, getString(R.string.categoria5))
        }
        binding.btnCategoria6.setOnClickListener {
            abrirProximaActivity(modoJogo, nomeUtilizador, nomeJogador, getString(R.string.categoria6))
        }

        binding.infoCategorias.setOnClickListener {
            mostrarDicasCategorias()
        }

        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, TipoModoClassico::class.java)
            intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_CLASSICO)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            intent.putExtra(IntentExtras.ADMIN, true)
            startActivity(intent)
            finish()
        }
    }

    // Função para abrir a próxima activity dependendo do modo de jogo selecionado
    private fun abrirProximaActivity(
        modoJogo: String,
        nomeUtilizador: String?,
        nomeJogador: String?,
        nomeCategoria: String
    ) {
        // Redireciona para a activity correta com os dados necessários
        val intent = when (modoJogo) {
            GameConstants.MODO_1X1 -> Intent(this, ConvidarAmigo1x1Activity::class.java)
            GameConstants.MODO_2X2 -> Intent(this, ConvidarAmigo2x2Activity::class.java)
            else -> return
        }
        modoJogo.let { intent.putExtra(IntentExtras.MODO_JOGO, it) }
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        nomeCategoria.let { intent.putExtra(IntentExtras.NOME_CATEGORIA, it) }
        startActivity(intent)
        finish()
    }

    private fun mostrarDicasCategorias() {
        UteisDicas.mostrarDicas(
            this,
            "Categorias",
            listOf(
                "História" to "Datas, povos e acontecimentos marcantes.",
                "Geografia" to "Países, capitais, rios e mapas.",
                "Desporto" to "Modalidades, atletas e grandes provas.",
                "Cultura Geral" to "Conhecimento variado para todos.",
                "Todas" to "Mistura perguntas de várias áreas.",
                "Gentílicos" to "Nomes de povos e localidades."
            )
        )
    }

}
