package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.UteisSala.criarSalaComCategoriaEEntrar
import com.example.brainbrawl.UteisSala.criarSalaPersonalizadaEEntrar
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityEscolherCategoriaBinding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.CategoriasEvent
import com.example.brainbrawl.viewmodels.CategoriasUiState
import com.example.brainbrawl.viewmodels.CategoriasViewModel

class EscolherCategoriaActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEscolherCategoriaBinding.inflate(layoutInflater)
    }
    private lateinit var codigoSala: String
    private val viewModel by lazy {
        ViewModelProvider(this)[CategoriasViewModel::class.java]
    }
    private val authService = AuthService()
    private var contextoCategorias: ContextoCategorias? = null
    private var dialogCategoriasPersonalizadas: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pela Intent
        val modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO)
        val nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        val nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        val uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid
        val admin = intent.getBooleanExtra(IntentExtras.ADMIN, false)

        if (modoJogo == null) {
            finish()
            return
        }

        configurarObservers()

        // Guardar o código da sala
        codigoSala = gerarCodigoSala()
        // Mapear as categorias para os nomes em português
        val categoriaFirebase = mapOf(
            getString(R.string.categoria1) to "História",
            getString(R.string.categoria2) to "Geografia",
            getString(R.string.categoria3) to "Desporto",
            getString(R.string.categoria4) to "Cultura Geral",
            getString(R.string.categoria5) to "Gentílicos"
        )

        // Função lambda para criar uma sala com a categoria escolhida e entrar nela
        val criarSala = { categoriaEscolhida: String ->
            criarSalaComCategoriaEEntrar(
                this, codigoSala, nomeUtilizador, nomeJogador, categoriaEscolhida, admin, modoJogo, uid
            ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
        }

        // Configurar os botões de categoria
        binding.btnCategoria1.setOnClickListener {
            binding.btnCategoria1.isEnabled = false
            criarSala(categoriaFirebase[getString(R.string.categoria1)] ?: "História")
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
            val intent = Intent(this, ExplorarCategoriasActivity::class.java)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            uid?.let { intent.putExtra(IntentExtras.UID, it) }
            startActivity(intent)
        }
        binding.infoCategorias.setOnClickListener {
            mostrarDicasCategorias()
        }
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, EscolherModoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
            uid?.let { intent.putExtra(IntentExtras.UID, it) }
            admin.let { intent.putExtra(IntentExtras.ADMIN, it) }
            startActivity(intent)
            finish()
        }
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
                "Gentílicos" to "Nomes de povos e localidades.",
                "Explorar" to "Categorias públicas e personalizadas ficam no ecrã Explorar Categorias."
            )
        )
    }

    private fun mostrarCategoriasPersonalizadas(
        modo: String,
        nomeUtilizador: String,
        nomeJogador: String?,
        admin: Boolean,
        uid: String?
    ) {
        contextoCategorias = ContextoCategorias(modo, nomeUtilizador, nomeJogador, admin, uid)
        viewModel.carregarCategoriasPersonalizadas(uid.orEmpty(), nomeUtilizador)
    }

    private fun configurarObservers() {
        viewModel.categorias.observe(this) { estado ->
            val contexto = contextoCategorias ?: return@observe
            mostrarDialogCategoriasPersonalizadas(estado, contexto)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEventoCategorias(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun tratarEventoCategorias(evento: CategoriasEvent) {
        when (evento) {
            CategoriasEvent.CategoriaCriada -> Unit
            CategoriasEvent.CategoriaEliminada -> {
                Toast.makeText(this, "Categoria eliminada.", Toast.LENGTH_SHORT).show()
                recarregarDialogCategorias()
            }
            CategoriasEvent.CategoriaPublicada -> {
                Toast.makeText(this, "Categoria pública guardada.", Toast.LENGTH_SHORT).show()
                recarregarDialogCategorias()
            }
            CategoriasEvent.CategoriaPublicaRemovida -> {
                Toast.makeText(this, "Categoria pública removida.", Toast.LENGTH_SHORT).show()
                recarregarDialogCategorias()
            }
            is CategoriasEvent.Erro -> {
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun recarregarDialogCategorias() {
        val contexto = contextoCategorias ?: return
        dialogCategoriasPersonalizadas?.dismiss()
        mostrarCategoriasPersonalizadas(contexto.modo, contexto.nomeUtilizador, contexto.nomeJogador, contexto.admin, contexto.uid)
    }

    private fun mostrarDialogCategoriasPersonalizadas(
                estado: CategoriasUiState,
        contexto: ContextoCategorias
    ) {
                val categoriasPublicasIds = estado.publicas.map { it.id }.toSet()
                val scrollView = ScrollView(this)
                val lista = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 8, 24, 8)
                }
                scrollView.addView(lista)

                lateinit var dialog: AlertDialog

                val btnCriar = Button(this).apply {
                    text = "Criar nova categoria"
                    setOnClickListener {
                        dialog.dismiss()
                        abrirAdicionarPerguntaActivity(
                            contexto.modo,
                            contexto.nomeUtilizador,
                            contexto.nomeJogador,
                            contexto.admin,
                            null
                        )
                    }
                }
                lista.addView(btnCriar)

                if (estado.personalizadas.isEmpty()) {
                    lista.addView(TextView(this).apply {
                        text = "Ainda não tens categorias personalizadas."
                        textSize = 16f
                        setPadding(0, 16, 0, 8)
                    })
                }

                estado.personalizadas.forEach { categoria ->
                    val categoriaPublicaId = categoriaPublicaId(categoria.chaveDono.ifBlank { contexto.identificadorDono }, categoria.nome)
                    val jaPublica = !categoria.categoriaPublicaId.isNullOrBlank() ||
                        categoriasPublicasIds.contains(categoriaPublicaId)
                    val container = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(0, 20, 0, 12)
                    }

                    container.addView(TextView(this).apply {
                        text = categoria.nome
                        textSize = 18f
                        setPadding(0, 0, 0, 8)
                    })
                    container.addView(TextView(this).apply {
                        text = if (jaPublica) "Pública" else "Privada"
                        textSize = 14f
                        setPadding(0, 0, 0, 8)
                    })

                    val botoes = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    botoes.addView(criarBotaoCategoria("Jogar") {
                        dialog.dismiss()
                        codigoSala = gerarCodigoSala()
                        criarSalaPersonalizadaEEntrar(
                            this,
                            codigoSala,
                            contexto.nomeUtilizador,
                            categoria.nome,
                            true,
                            contexto.modo,
                            contexto.uid
                        ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                    })

                    botoes.addView(criarBotaoCategoria("Editar") {
                        dialog.dismiss()
                        abrirAdicionarPerguntaActivity(
                            contexto.modo,
                            contexto.nomeUtilizador,
                            contexto.nomeJogador,
                            contexto.admin,
                            categoria.nome
                        )
                    })

                    botoes.addView(criarBotaoCategoria("Eliminar") {
                        confirmarEliminarCategoria(categoria.nome)
                    })

                    container.addView(botoes)

                    val botoesPublicos = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    botoesPublicos.addView(criarBotaoCategoria(if (jaPublica) "Atualizar pública" else "Tornar pública") {
                        viewModel.publicarCategoria(contexto.uid.orEmpty(), contexto.nomeUtilizador, contexto.nomeJogador, categoria.nome)
                    })
                    if (jaPublica) {
                        botoesPublicos.addView(criarBotaoCategoria("Remover pública") {
                            viewModel.removerCategoriaPublica(contexto.uid.orEmpty(), contexto.nomeUtilizador, categoria.nome)
                        })
                    }
                    container.addView(botoesPublicos)
                    lista.addView(container)
                }

                dialog = AlertDialog.Builder(this)
                    .setTitle("Categorias personalizadas")
                    .setView(scrollView)
                    .setNegativeButton("Voltar", null)
                    .create()
                dialog.show()
                dialogCategoriasPersonalizadas = dialog
    }

    private fun criarBotaoCategoria(texto: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = texto
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = 8
            }
            setOnClickListener { onClick() }
        }
    }

    private fun confirmarEliminarCategoria(
        categoria: String
    ) {
        val contexto = contextoCategorias ?: return
        AlertDialog.Builder(this)
            .setTitle("Eliminar categoria")
            .setMessage("Queres eliminar \"$categoria\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarCategoria(contexto.uid.orEmpty(), contexto.nomeUtilizador, categoria)
            }
            .show()
    }

    private fun abrirAdicionarPerguntaActivity(
        modo: String,
        nomeUtilizador: String?,
        nomeJogador: String?,
        admin: Boolean,
        categoriaInicial: String?
    ) {
        codigoSala = gerarCodigoSala()
        val intent = Intent(this, AdicionarPerguntaActivity::class.java)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        contextoCategorias?.uid?.let { intent.putExtra(IntentExtras.UID, it) }
        categoriaInicial?.let { intent.putExtra(IntentExtras.NOME_CATEGORIA, it) }
        codigoSala.let { intent.putExtra(IntentExtras.CODIGO_SALA, it) }
        modo.let { intent.putExtra(IntentExtras.MODO_JOGO, it) }
        intent.putExtra(IntentExtras.ADMIN, admin)
        startActivity(intent)
        finish()
    }

    private fun categoriaPublicaId(nomeUtilizador: String, categoria: String): String {
        val bruto = "${nomeUtilizador}_${categoria}".lowercase()
        return bruto.replace(Regex("[.#$\\[\\]/]"), "_").replace(Regex("\\s+"), "_")
    }

    private data class ContextoCategorias(
        val modo: String,
        val nomeUtilizador: String,
        val nomeJogador: String?,
        val admin: Boolean,
        val uid: String?
    ) {
        val identificadorDono: String
            get() = uid.orEmpty().ifBlank { nomeUtilizador }
    }
}
