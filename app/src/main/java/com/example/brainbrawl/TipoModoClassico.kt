package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisNavegacao.abrirEscolherCategoriaActivity
import com.example.brainbrawl.databinding.ActivityTipoModoClassicoBinding

class TipoModoClassico : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy { ActivityTipoModoClassicoBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val modoJogo = intent.getStringExtra("modoJogo")
        val nomeJogador = intent.getStringExtra("nomeJogador") ?: nomeUtilizador

        // Configurar o botao para o modo 1x1, modo 2x2 e modo de todos contra todos
        binding.btnModo1x1.setOnClickListener {
            iniciarModoCompetitivo("1x1", nomeUtilizador)
        }

        binding.btnModo2x2.setOnClickListener {
            iniciarModoCompetitivo("2x2", nomeUtilizador)
        }

        binding.btnModoGrupo.setOnClickListener {
            // Chama a função para abrir a EscolherCategoriaActivity com o modo de jogo selecionado
            abrirEscolherCategoriaActivity(this, modoJogo.toString(), nomeUtilizador, nomeJogador, true)
        }

        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, EscolherModoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            intent.putExtra("admin", false)
            startActivity(intent)
            finish()
        }

        // Configurar o botão de informação sobre todos os modos de jogo
        binding.infoTodos.setOnClickListener {
            mostrarExplicacaoTodosModos()
        }
    }

    // Função para mostrar mensagem de login obrigatório
    private fun mostrarMensagemLoginObrigatorio() {
        UteisDicas.mostrarDicas(
            this,
            "Iniciar sessão necessária",
            listOf("1x1 e 2x2" to "Entra com conta para poderes convidar amigos e guardar estatísticas.")
        )
    }

    // Função para mostrar explicação de todos os modos de jogo
    private fun mostrarExplicacaoTodosModos() {
        UteisDicas.mostrarDicas(
            this,
            "Modo Clássico",
            listOf(
                "1x1" to "Duelo direto entre dois jogadores.",
                "2x2" to "Quatro jogadores em duas equipas.",
                "Todos" to "O admin observa e inicia quando há pelo menos 1 jogador na sala."
            )
        )
    }

    // Função para iniciar o modo competitivo 1x1 ou 2x2
    private fun iniciarModoCompetitivo(modo: String, nomeUtilizador: String?) {
        // Caso seja um jogador temporário, mostra mensagem de login obrigatório
        if (nomeUtilizador.isNullOrEmpty()) {
            mostrarMensagemLoginObrigatorio()
        } else {
            val intent = Intent(this, EscolhaCategoriaModosActivity::class.java)
            intent.putExtra("modoJogo", modo)
            intent.putExtra("nomeUtilizador", nomeUtilizador)
            intent.putExtra("admin", true)
            startActivity(intent)
        }
    }
}
