package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.UteisSala.criarSalaCategoriaPublicaEEntrar
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityExplorarCategoriasBinding
import com.example.brainbrawl.repositories.CategoriaRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.ExplorarCategoriasEvent
import com.example.brainbrawl.viewmodels.ExplorarCategoriasUiState
import com.example.brainbrawl.viewmodels.ExplorarCategoriasViewModel

class ExplorarCategoriasActivity : AppCompatActivity() {
    private val binding by lazy { ActivityExplorarCategoriasBinding.inflate(layoutInflater) }
    private val viewModel by lazy {
        ViewModelProvider(this)[ExplorarCategoriasViewModel::class.java]
    }
    private val authService = AuthService()
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid

        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            uid?.let { intent.putExtra(IntentExtras.UID, it) }
            startActivity(intent)
            finish()
        }

        binding.btnCriarCategoria.setOnClickListener {
            abrirCriacaoCategoria()
        }

        configurarObservers()
        viewModel.carregarCategorias()
    }

    override fun onDestroy() {
        viewModel.removerListener()
        super.onDestroy()
    }

    private fun configurarObservers() {
        viewModel.categorias.observe(this) { estado ->
            atualizarEstadoCategorias(estado)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun atualizarEstadoCategorias(estado: ExplorarCategoriasUiState) {
        if (estado.carregando) {
            binding.layoutCategoriasPublicas.removeAllViews()
            binding.txtEstado.text = "A carregar categorias..."
            return
        }

        if (estado.erro) {
            binding.txtEstado.text = "Erro ao carregar categorias públicas."
            return
        }

        preencherLista(estado.categorias)
    }

    private fun tratarEvento(evento: ExplorarCategoriasEvent) {
        when (evento) {
            ExplorarCategoriasEvent.LoginNecessarioGuardar ->
                Toast.makeText(this, "Inicia sessão para guardar categorias.", Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.LoginNecessarioAvaliar ->
                Toast.makeText(this, "Inicia sessão para avaliar categorias.", Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.CategoriaGuardada ->
                Toast.makeText(this, "Categoria guardada nas tuas categorias.", Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.AvaliacaoGuardada ->
                Toast.makeText(this, "Avaliação guardada.", Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.CategoriaJaAvaliada ->
                Toast.makeText(this, "Já avaliaste esta categoria.", Toast.LENGTH_SHORT).show()
            is ExplorarCategoriasEvent.Erro ->
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
        }
    }

    private fun preencherLista(categorias: List<CategoriaRepository.CategoriaPublica>) {
        binding.layoutCategoriasPublicas.removeAllViews()
        binding.txtEstado.text = if (categorias.isEmpty()) {
            "Ainda não há categorias públicas."
        } else {
            ""
        }

        categorias.forEach { categoria ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
                background = getDrawable(R.drawable.botao_branco_arredondado)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(16))
                }
            }

            card.addView(TextView(this).apply {
                text = categoria.nome
                textSize = 20f
                setTextColor(0xFF000000.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = "por ${categoria.criador}"
                textSize = 14f
                setTextColor(0xFF333333.toInt())
                setPadding(0, dp(4), 0, dp(6))
            })
            card.addView(TextView(this).apply {
                text = categoria.descricaoCurta()
                textSize = 15f
                setTextColor(0xFF000000.toInt())
            })
            card.addView(TextView(this).apply {
                text = "${categoria.totalPerguntas} perguntas  •  ${categoria.usos} usos  •  ${categoria.ratingTexto()}"
                textSize = 14f
                setTextColor(0xFF333333.toInt())
                setPadding(0, dp(8), 0, dp(10))
            })

            val botoes = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            botoes.addView(criarBotao("Jogar") { jogarCategoria(categoria) })
            botoes.addView(criarBotao("Guardar") { guardarCategoria(categoria) })
            botoes.addView(criarBotao("Avaliar") { mostrarAvaliacao(categoria) })
            card.addView(botoes)
            binding.layoutCategoriasPublicas.addView(card)
        }
    }

    private fun criarBotao(texto: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = texto
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun jogarCategoria(categoria: CategoriaRepository.CategoriaPublica) {
        if (nomeUtilizador.isNullOrBlank() && nomeJogador.isNullOrBlank()) {
            Toast.makeText(this, "Indica um nome antes de jogar.", Toast.LENGTH_SHORT).show()
            return
        }
        criarSalaCategoriaPublicaEEntrar(
            this,
            gerarCodigoSala(),
            nomeUtilizador,
            nomeJogador,
            categoria.id,
            true,
            GameConstants.MODO_CLASSICO,
            uid
        ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun abrirCriacaoCategoria() {
        val utilizador = nomeUtilizador
        if (uid.isNullOrBlank() && utilizador.isNullOrBlank()) {
            Toast.makeText(this, "Precisas de uma conta registada para criar categorias.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, AdicionarPerguntaActivity::class.java)
        utilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_CLASSICO)
        intent.putExtra(IntentExtras.ADMIN, true)
        startActivity(intent)
    }

    private fun guardarCategoria(categoria: CategoriaRepository.CategoriaPublica) {
        viewModel.guardarCategoria(uid.orEmpty(), nomeUtilizador.orEmpty(), categoria)
    }

    private fun mostrarAvaliacao(categoria: CategoriaRepository.CategoriaPublica) {
        val utilizador = nomeUtilizador
        if (uid.isNullOrBlank() && utilizador.isNullOrBlank()) {
            viewModel.avaliarCategoria(categoria.id, "", "", 1)
            return
        }

        val opcoes = arrayOf("1 estrela", "2 estrelas", "3 estrelas", "4 estrelas", "5 estrelas")
        AlertDialog.Builder(this)
            .setTitle("Avaliar ${categoria.nome}")
            .setItems(opcoes) { _, which ->
                avaliarCategoria(categoria.id, utilizador.orEmpty(), which + 1)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun avaliarCategoria(categoriaId: String, utilizador: String, valor: Int) {
        viewModel.avaliarCategoria(categoriaId, uid.orEmpty(), utilizador, valor)
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()
}
