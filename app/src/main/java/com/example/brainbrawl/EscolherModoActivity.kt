package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.routes.UteisNavegacao.abrirEscolherCategoriaActivity
import com.example.brainbrawl.UteisSala.criarSalaCaoticaEEntrar
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
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
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        admin = intent.getBooleanExtra(IntentExtras.ADMIN, false)

        // Configurar os botoes de medos
        binding.btnModoClassico.setOnClickListener {
            abrirTipoModoClassico(GameConstants.MODO_CLASSICO)
        }
        binding.btnModoEliminatorias.setOnClickListener {
            abrirEscolherCategoriaActivity(this, GameConstants.MODO_ELIMINATORIAS, nomeUtilizador, nomeJogador, true)
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
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            intent.putExtra(IntentExtras.ADMIN, false)
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
        intent.putExtra(IntentExtras.MODO_JOGO, modo)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        intent.putExtra(IntentExtras.ADMIN, true)
        intent.putExtra(IntentExtras.CODIGO_SALA, gerarCodigoSala())
        startActivity(intent)
        finish()
    }

    // Função para mostrar a explicação de todos os modos de jogo
    private fun mostrarExplicacaoTodosModos() {
        UteisDicas.mostrarDicas(
            this,
            "Modos de Jogo",
            listOf(
                "Clássico" to "Perguntas da categoria escolhida. Vence quem somar mais pontos.",
                "Caótico" to "Perguntas misturadas de todas as categorias, com menos tempo.",
                "Eliminatórias" to "Quem falha pode sair da partida. O último resistente vence."
            )
        )
    }
}
