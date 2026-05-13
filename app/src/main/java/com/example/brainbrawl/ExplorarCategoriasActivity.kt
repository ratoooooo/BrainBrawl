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
import com.example.brainbrawl.UteisSala.criarSalaPersonalizadaEEntrar
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityExplorarCategoriasBinding
import com.example.brainbrawl.repositories.CategoriaRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
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

        binding.btnCriarCategoria.setOnClickListener { abrirCriacaoCategoria() }

        configurarObservers()
        viewModel.carregarCategorias(uid.orEmpty(), nomeUtilizador.orEmpty())
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
            binding.txtEstado.text = getString(R.string.a_carregar_categorias)
            return
        }

        if (estado.erro) {
            binding.txtEstado.text = getString(R.string.erro_carregar_categorias)
            return
        }

        preencherLista(estado)
    }

    private fun tratarEvento(evento: ExplorarCategoriasEvent) {
        when (evento) {
            ExplorarCategoriasEvent.LoginNecessarioGuardar ->
                Toast.makeText(this, R.string.login_guardar_categorias, Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.LoginNecessarioAvaliar ->
                Toast.makeText(this, R.string.login_avaliar_categorias, Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.LoginNecessarioGerir ->
                Toast.makeText(this, R.string.login_gerir_categorias, Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.CategoriaGuardada ->
                Toast.makeText(this, R.string.categoria_guardada, Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.CategoriaEliminada ->
                Toast.makeText(this, R.string.categoria_eliminada, Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.CategoriaPublicada ->
                Toast.makeText(this, R.string.categoria_publica_guardada, Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.CategoriaPublicaRemovida ->
                Toast.makeText(this, R.string.categoria_publica_removida, Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.AvaliacaoGuardada ->
                Toast.makeText(this, R.string.avaliacao_guardada, Toast.LENGTH_SHORT).show()
            ExplorarCategoriasEvent.CategoriaJaAvaliada ->
                Toast.makeText(this, R.string.categoria_ja_avaliada, Toast.LENGTH_SHORT).show()
            is ExplorarCategoriasEvent.Erro ->
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
        }
    }

    private fun preencherLista(estado: ExplorarCategoriasUiState) {
        binding.layoutCategoriasPublicas.removeAllViews()
        binding.txtEstado.text = if (estado.categoriasPublicas.isEmpty() && estado.minhasCategorias.isEmpty()) {
            getString(R.string.sem_categorias_mostrar)
        } else {
            ""
        }

        adicionarTituloSecao(getString(R.string.categorias_publicas))
        if (estado.categoriasPublicas.isEmpty()) {
            adicionarTextoSecao(getString(R.string.sem_categorias_publicas))
        }
        estado.categoriasPublicas.forEach { categoria ->
            adicionarCardPublico(categoria)
        }

        adicionarTituloSecao(getString(R.string.minhas_categorias))
        if (uid.isNullOrBlank() && nomeUtilizador.isNullOrBlank()) {
            adicionarTextoSecao("Inicia sessão para criar e editar as tuas categorias.")
        } else if (estado.minhasCategorias.isEmpty()) {
            adicionarTextoSecao("Ainda não tens categorias personalizadas.")
        }

        val publicasIds = estado.categoriasPublicas.map { it.id }.toSet()
        estado.minhasCategorias.forEach { categoria ->
            val idCompatibilidade = categoriaPublicaId(categoria.chaveDono.ifBlank { donoAtual() }, categoria.nome)
            val jaPublica = !categoria.categoriaPublicaId.isNullOrBlank() || idCompatibilidade in publicasIds
            adicionarCardMinhaCategoria(categoria, jaPublica)
        }
    }

    private fun adicionarCardPublico(categoria: CategoriaRepository.CategoriaPublica) {
        val card = criarCardBase()
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

        val botoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        botoes.addView(criarBotao("Jogar") { mostrarEscolhaModo(CategoriaExploravel.Publica(categoria)) })
        botoes.addView(criarBotao("Guardar") { guardarCategoria(categoria) })
        botoes.addView(criarBotao("Avaliar") { mostrarAvaliacao(categoria) })
        card.addView(botoes)
        binding.layoutCategoriasPublicas.addView(card)
    }

    private fun adicionarCardMinhaCategoria(
        categoria: CategoriaRepository.CategoriaPersonalizada,
        jaPublica: Boolean
    ) {
        val card = criarCardBase()
        card.addView(TextView(this).apply {
            text = categoria.nome
            textSize = 20f
            setTextColor(0xFF000000.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text = if (jaPublica) "Pública" else "Privada"
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(4), 0, dp(10))
        })

        val botoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        botoes.addView(criarBotao("Jogar") { mostrarEscolhaModo(CategoriaExploravel.Personalizada(categoria)) })
        botoes.addView(criarBotao("Editar") { abrirEdicaoCategoria(categoria.nome) })
        botoes.addView(criarBotao("Eliminar") { confirmarEliminarCategoria(categoria) })
        card.addView(botoes)

        val botoesPublicos = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        botoesPublicos.addView(criarBotao(if (jaPublica) "Atualizar pública" else "Tornar pública") {
            viewModel.publicarCategoria(uid.orEmpty(), nomeUtilizador.orEmpty(), nomeJogador, categoria.nome)
        })
        if (jaPublica) {
            botoesPublicos.addView(criarBotao("Remover pública") {
                viewModel.removerCategoriaPublica(uid.orEmpty(), nomeUtilizador.orEmpty(), categoria.nome)
            })
        }
        card.addView(botoesPublicos)
        binding.layoutCategoriasPublicas.addView(card)
    }

    private fun criarCardBase(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = getDrawable(R.drawable.botao_branco_arredondado)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(16)) }
        }
    }

    private fun adicionarTituloSecao(texto: String) {
        binding.layoutCategoriasPublicas.addView(TextView(this).apply {
            text = texto
            textSize = 20f
            setTextColor(0xFF000000.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(10))
        })
    }

    private fun adicionarTextoSecao(texto: String) {
        binding.layoutCategoriasPublicas.addView(TextView(this).apply {
            text = texto
            textSize = 15f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, dp(14))
        })
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

    private fun mostrarEscolhaModo(categoria: CategoriaExploravel) {
        if (nomeUtilizador.isNullOrBlank() && nomeJogador.isNullOrBlank()) {
            Toast.makeText(this, R.string.indica_nome_antes_jogar, Toast.LENGTH_SHORT).show()
            return
        }

        val opcoes = resources.getStringArray(R.array.opcoes_modo_personalizado)
        AlertDialog.Builder(this)
            .setTitle(R.string.escolher_modo)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> iniciarCategoria(categoria, GameConstants.MODO_CLASSICO)
                    1 -> abrirConviteCategoria(categoria, GameConstants.MODO_1X1)
                    2 -> abrirConviteCategoria(categoria, GameConstants.MODO_2X2)
                    3 -> iniciarCategoria(categoria, GameConstants.MODO_ELIMINATORIAS)
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun iniciarCategoria(categoria: CategoriaExploravel, modo: String) {
        when (categoria) {
            is CategoriaExploravel.Publica -> criarSalaCategoriaPublicaEEntrar(
                this,
                gerarCodigoSala(),
                nomeUtilizador,
                nomeJogador,
                categoria.categoria.id,
                true,
                modo,
                uid
            ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

            is CategoriaExploravel.Personalizada -> criarSalaPersonalizadaEEntrar(
                this,
                gerarCodigoSala(),
                nomeUtilizador.orEmpty(),
                categoria.categoria.nome,
                true,
                modo,
                uid
            ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun abrirConviteCategoria(categoria: CategoriaExploravel, modo: String) {
        if (nomeUtilizador.isNullOrBlank()) {
            Toast.makeText(this, R.string.convites_precisam_conta, Toast.LENGTH_SHORT).show()
            return
        }

        val destino = if (modo == GameConstants.MODO_2X2) {
            ConvidarAmigo2x2Activity::class.java
        } else {
            ConvidarAmigo1x1Activity::class.java
        }
        val intent = Intent(this, destino)
        intent.putExtra(IntentExtras.MODO_JOGO, modo)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.ADMIN, true)
        when (categoria) {
            is CategoriaExploravel.Publica -> {
                intent.putExtra(IntentExtras.NOME_CATEGORIA, categoria.categoria.nome)
                intent.putExtra(IntentExtras.CATEGORIA_PUBLICA_ID, categoria.categoria.id)
            }
            is CategoriaExploravel.Personalizada -> {
                intent.putExtra(IntentExtras.NOME_CATEGORIA, categoria.categoria.nome)
                categoria.categoria.uid.takeIf { it.isNotBlank() }?.let {
                    intent.putExtra(IntentExtras.DONO_UID, it)
                }
                intent.putExtra(
                    IntentExtras.DONO_CATEGORIA,
                    categoria.categoria.chaveDono.ifBlank { categoria.categoria.nomeUtilizador.ifBlank { nomeUtilizador.orEmpty() } }
                )
            }
        }
        startActivity(intent)
    }

    private fun abrirCriacaoCategoria() {
        if (uid.isNullOrBlank() && nomeUtilizador.isNullOrBlank()) {
            Toast.makeText(this, R.string.conta_registada_criar_categorias, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, AdicionarPerguntaActivity::class.java)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_CLASSICO)
        intent.putExtra(IntentExtras.ADMIN, true)
        startActivity(intent)
    }

    private fun abrirEdicaoCategoria(nomeCategoria: String) {
        val intent = Intent(this, AdicionarPerguntaActivity::class.java)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        intent.putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_CLASSICO)
        intent.putExtra(IntentExtras.ADMIN, true)
        startActivity(intent)
    }

    private fun confirmarEliminarCategoria(categoria: CategoriaRepository.CategoriaPersonalizada) {
        AlertDialog.Builder(this)
            .setTitle(R.string.eliminar_categoria)
            .setMessage(getString(R.string.confirmar_eliminar_categoria_format, categoria.nome))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                viewModel.eliminarCategoria(uid.orEmpty(), nomeUtilizador.orEmpty(), categoria)
            }
            .show()
    }

    private fun guardarCategoria(categoria: CategoriaRepository.CategoriaPublica) {
        viewModel.guardarCategoria(uid.orEmpty(), nomeUtilizador.orEmpty(), categoria)
    }

    private fun mostrarAvaliacao(categoria: CategoriaRepository.CategoriaPublica) {
        if (uid.isNullOrBlank() && nomeUtilizador.isNullOrBlank()) {
            viewModel.avaliarCategoria(categoria.id, "", "", 1)
            return
        }

        val opcoes = resources.getStringArray(R.array.opcoes_avaliacao)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.avaliar_categoria_format, categoria.nome))
            .setItems(opcoes) { _, which ->
                viewModel.avaliarCategoria(categoria.id, uid.orEmpty(), nomeUtilizador.orEmpty(), which + 1)
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private fun donoAtual(): String = uid.orEmpty().ifBlank { nomeUtilizador.orEmpty() }

    private fun categoriaPublicaId(nomeUtilizador: String, categoria: String): String {
        val bruto = "${nomeUtilizador}_${categoria}".lowercase()
        return bruto.replace(Regex("[.#$\\[\\]/]"), "_").replace(Regex("\\s+"), "_")
    }

    private sealed class CategoriaExploravel {
        data class Publica(val categoria: CategoriaRepository.CategoriaPublica) : CategoriaExploravel()
        data class Personalizada(val categoria: CategoriaRepository.CategoriaPersonalizada) : CategoriaExploravel()
    }
}
