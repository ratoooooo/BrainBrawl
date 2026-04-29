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
import com.example.brainbrawl.UteisSala.criarSalaCategoriaPublicaEEntrar
import com.example.brainbrawl.UteisSala.gerarCodigoSala
import com.example.brainbrawl.databinding.ActivityExplorarCategoriasBinding
import com.example.brainbrawl.repositories.CategoriaRepository

class ExplorarCategoriasActivity : AppCompatActivity() {
    private val binding by lazy { ActivityExplorarCategoriasBinding.inflate(layoutInflater) }
    private val categoriaRepository = CategoriaRepository()
    private var categoriasListener: CategoriaRepository.ListenerHandle? = null
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        nomeJogador = intent.getStringExtra("nomeJogador")

        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            startActivity(intent)
            finish()
        }

        binding.btnCriarCategoria.setOnClickListener {
            abrirCriacaoCategoria()
        }

        carregarCategorias()
    }

    override fun onDestroy() {
        super.onDestroy()
        categoriaRepository.removerListener(categoriasListener)
    }

    private fun carregarCategorias() {
        binding.layoutCategoriasPublicas.removeAllViews()
        binding.txtEstado.text = "A carregar categorias..."
        categoriasListener = categoriaRepository.escutarCategoriasPublicas(
            onCategoriasAlteradas = { categorias ->
                preencherLista(categorias)
            },
            onErro = {
                binding.txtEstado.text = "Erro ao carregar categorias públicas."
            }
        )
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
            "classico"
        ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun abrirCriacaoCategoria() {
        val utilizador = nomeUtilizador
        if (utilizador.isNullOrBlank()) {
            Toast.makeText(this, "Precisas de uma conta registada para criar categorias.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, AdicionarPerguntaActivity::class.java)
        intent.putExtra("nomeUtilizador", utilizador)
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
        intent.putExtra("modoJogo", "classico")
        intent.putExtra("admin", true)
        startActivity(intent)
    }

    private fun guardarCategoria(categoria: CategoriaRepository.CategoriaPublica) {
        val utilizador = nomeUtilizador
        if (utilizador.isNullOrBlank()) {
            Toast.makeText(this, "Inicia sessão para guardar categorias.", Toast.LENGTH_SHORT).show()
            return
        }

        categoriaRepository.guardarCopiaCategoriaPublica(utilizador, categoria.id)
            .addOnSuccessListener {
                Toast.makeText(this, "Categoria guardada nas tuas categorias.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, error.message ?: "Erro ao guardar categoria.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarAvaliacao(categoria: CategoriaRepository.CategoriaPublica) {
        val utilizador = nomeUtilizador
        if (utilizador.isNullOrBlank()) {
            Toast.makeText(this, "Inicia sessão para avaliar categorias.", Toast.LENGTH_SHORT).show()
            return
        }

        val opcoes = arrayOf("1 estrela", "2 estrelas", "3 estrelas", "4 estrelas", "5 estrelas")
        AlertDialog.Builder(this)
            .setTitle("Avaliar ${categoria.nome}")
            .setItems(opcoes) { _, which ->
                avaliarCategoria(categoria.id, utilizador, which + 1)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun avaliarCategoria(categoriaId: String, utilizador: String, valor: Int) {
        categoriaRepository.avaliarCategoria(categoriaId, utilizador, valor)
            .addOnSuccessListener { resultado ->
                when (resultado) {
                    CategoriaRepository.ResultadoAvaliacao.GUARDADA ->
                        Toast.makeText(this, "Avaliação guardada.", Toast.LENGTH_SHORT).show()
                    CategoriaRepository.ResultadoAvaliacao.JA_AVALIADA ->
                        Toast.makeText(this, "Já avaliaste esta categoria.", Toast.LENGTH_SHORT).show()
                    else ->
                        Toast.makeText(this, "Erro ao avaliar.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao avaliar.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()
}
