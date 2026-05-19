package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.routes.UteisNavegacao.abrirEscolherCategoriaActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityTipoModoClassicoBinding
import com.example.brainbrawl.services.AuthService

class TipoModoClassico : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy { ActivityTipoModoClassicoBinding.inflate(layoutInflater) }
    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Receber dados passados do intent
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        val modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_CLASSICO
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid

        binding.btnModoSolo.setOnClickListener {
            abrirEscolherCategoriaActivity(this, modoJogo, nomeUtilizador, nomeJogador, false, uid, modoSolo = true)
            finish()
        }

        val modoCompetitivoDiretoDisponivel = modoJogo == GameConstants.MODO_CLASSICO
        binding.btnModo1x1.visibility = if (modoCompetitivoDiretoDisponivel) View.VISIBLE else View.GONE
        binding.btnModo2x2.visibility = if (modoCompetitivoDiretoDisponivel) View.VISIBLE else View.GONE

        // Configurar o botao para o modo 1x1, modo 2x2 e modo de todos contra todos
        binding.btnModo1x1.setOnClickListener {
            iniciarModoCompetitivo(GameConstants.MODO_1X1, nomeUtilizador, nomeJogador, uid)
        }

        binding.btnModo2x2.setOnClickListener {
            iniciarModoCompetitivo(GameConstants.MODO_2X2, nomeUtilizador, nomeJogador, uid)
        }

        binding.btnModoGrupo.setOnClickListener {
            // Chama a função para abrir a EscolherCategoriaActivity com o modo de jogo selecionado
            abrirEscolherCategoriaActivity(this, modoJogo, nomeUtilizador, nomeJogador, true, uid)
        }

        // Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, EscolherModoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            uid?.let { intent.putExtra(IntentExtras.UID, it) }
            intent.putExtra(IntentExtras.ADMIN, false)
            startActivity(intent)
            finish()
        }

        // Configurar o botão de informação sobre todos os modos de jogo
        binding.infoTodos.setOnClickListener {
            mostrarExplicacaoTodosModos(modoJogo, modoCompetitivoDiretoDisponivel)
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
    private fun mostrarExplicacaoTodosModos(modoJogo: String, modoCompetitivoDiretoDisponivel: Boolean) {
        val titulo = when (modoJogo) {
            GameConstants.MODO_CAOTICO -> getString(R.string.modo_caotico)
            GameConstants.MODO_ELIMINATORIAS -> getString(R.string.modo_eliminatorias)
            else -> getString(R.string.modo_classico)
        }
        val dicas = mutableListOf(
            "Solo" to "Joga sozinho, sem sala, código ou espera por outros jogadores.",
            "Grupo" to "Cria uma sala com código e inicia quando houver pelo menos 2 jogadores presentes."
        )
        if (modoCompetitivoDiretoDisponivel) {
            dicas.add(0, "2x2" to "Quatro jogadores em duas equipas.")
            dicas.add(0, "1x1" to "Duelo direto entre dois jogadores.")
        }
        UteisDicas.mostrarDicas(
            this,
            titulo,
            dicas
        )
    }

    // Função para iniciar o modo competitivo 1x1 ou 2x2
    private fun iniciarModoCompetitivo(modo: String, nomeUtilizador: String?, nomeJogador: String?, uid: String?) {
        // Caso seja um jogador temporário, mostra mensagem de login obrigatório
        if (nomeUtilizador.isNullOrEmpty()) {
            mostrarMensagemLoginObrigatorio()
        } else {
            val intent = Intent(this, EscolhaCategoriaModosActivity::class.java)
            intent.putExtra(IntentExtras.MODO_JOGO, modo)
            intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            uid?.let { intent.putExtra(IntentExtras.UID, it) }
            intent.putExtra(IntentExtras.ADMIN, true)
            startActivity(intent)
        }
    }
}
