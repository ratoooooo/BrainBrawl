package com.example.brainbrawl

import android.app.AlertDialog
import android.graphics.Typeface
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
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
import com.example.brainbrawl.routes.BottomNavHelper
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
        BottomNavHelper.instalar(this, BottomNavHelper.Item.MAIN, uid, nomeUtilizador, nomeJogador)

        binding.btnMinhasCategorias.setOnClickListener { abrirMinhasCategorias() }
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

        adicionarTituloSecao(getString(R.string.minhas_categorias), mostrarVerTodas = true) {
            abrirMinhasCategorias()
        }
        if (uid.isNullOrBlank() && nomeUtilizador.isNullOrBlank()) {
            adicionarTextoSecao(getString(R.string.login_minhas_categorias))
        } else if (estado.minhasCategorias.isEmpty()) {
            adicionarTextoSecao(getString(R.string.sem_categorias_personalizadas))
        }

        val publicasIds = estado.categoriasPublicas.map { it.id }.toSet()
        estado.minhasCategorias.take(1).forEach { categoria ->
            val idCompatibilidade = categoriaPublicaId(categoria.chaveDono.ifBlank { donoAtual() }, categoria.nome)
            val jaPublica = !categoria.categoriaPublicaId.isNullOrBlank() || idCompatibilidade in publicasIds
            adicionarCardMinhaCategoria(categoria, jaPublica)
        }
    }

    private fun adicionarCardPublico(categoria: CategoriaRepository.CategoriaPublica) {
        val card = criarCardBase()
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        linha.addView(criarIconeCategoria(categoria.nome, categoria.iconeCategoria))
        linha.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            }
            addView(criarTextoTitulo(categoria.nome))
            addView(criarTextoSecundario(getString(R.string.categoria_criador_format, categoria.criador)))
            addView(criarTextoCorpo(categoria.descricaoCurta()))
            addView(criarTextoSecundario(
                getString(
                    R.string.categoria_metricas_format,
                    categoria.totalPerguntas,
                    categoria.usos,
                    categoria.ratingTexto()
                )
            ))
        })
        card.addView(linha)

        adicionarGrupoBotoes(
            card,
            listOf(
                criarBotao(getString(R.string.jogar), destaque = true) { mostrarEscolhaModo(CategoriaExploravel.Publica(categoria)) },
                criarBotao(getString(R.string.guardar)) { guardarCategoria(categoria) },
                criarBotao(getString(R.string.avaliar)) { mostrarAvaliacao(categoria) }
            )
        )
        binding.layoutCategoriasPublicas.addView(card)
    }

    private fun adicionarCardMinhaCategoria(
        categoria: CategoriaRepository.CategoriaPersonalizada,
        jaPublica: Boolean
    ) {
        val card = criarCardBase()
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        linha.addView(criarIconeCategoria(categoria.nome, categoria.iconeCategoria))
        linha.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            }
            addView(criarTextoTitulo(categoria.nome))
            addView(criarTextoSecundario(getString(R.string.criada_por_ti)))
            addView(criarTextoCorpo(categoria.descricao.ifBlank { getString(R.string.sem_descricao_curta) }))
            addView(criarTextoSecundario(getString(R.string.categoria_minha_metricas_format, categoria.totalPerguntas, categoria.usos)))
        })
        linha.addView(criarBadgeEstado(jaPublica))
        card.addView(linha)

        val botoesCategoria = mutableListOf(
            criarBotao(getString(R.string.jogar), destaque = true) { mostrarEscolhaModo(CategoriaExploravel.Personalizada(categoria)) },
            criarBotao(getString(R.string.editar_categoria)) { abrirEdicaoCategoria(categoria.nome) },
            criarBotao(getString(R.string.eliminar), perigo = true) { confirmarEliminarCategoria(categoria) },
            criarBotao(if (jaPublica) getString(R.string.atualizar_publica) else getString(R.string.tornar_publica)) {
                viewModel.publicarCategoria(uid.orEmpty(), nomeUtilizador.orEmpty(), nomeJogador, categoria.nome)
            }
        )
        if (jaPublica) {
            botoesCategoria.add(
                criarBotao(getString(R.string.remover_publica), perigo = true) {
                    viewModel.removerCategoriaPublica(uid.orEmpty(), nomeUtilizador.orEmpty(), categoria.nome)
                }
            )
        }
        adicionarGrupoBotoes(card, botoesCategoria)
        binding.layoutCategoriasPublicas.addView(card)
    }

    private fun criarCardBase(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = getDrawable(R.drawable.bg_ranking_card)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(12)) }
        }
    }

    private fun adicionarTituloSecao(texto: String, mostrarVerTodas: Boolean = false, onVerTodas: (() -> Unit)? = null) {
        binding.layoutCategoriasPublicas.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(12))
            addView(TextView(this@ExplorarCategoriasActivity).apply {
                text = getString(R.string.section_title_diamond_format, texto)
                textSize = 16f
                setTextColor(getColor(R.color.bb_luso_navy))
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (mostrarVerTodas) {
                addView(TextView(this@ExplorarCategoriasActivity).apply {
                    text = getString(R.string.ver_todas_chevron)
                    textSize = 13f
                    setTextColor(getColor(R.color.bb_luso_navy))
                    setTypeface(typeface, Typeface.BOLD)
                    setOnClickListener { onVerTodas?.invoke() }
                })
            }
        })
    }

    private fun adicionarTextoSecao(texto: String) {
        binding.layoutCategoriasPublicas.addView(TextView(this).apply {
            text = texto
            textSize = 15f
            setTextColor(getColor(R.color.bb_text_secondary))
            setPadding(0, 0, 0, dp(14))
        })
    }

    private fun adicionarGrupoBotoes(card: LinearLayout, botoes: List<Button>) {
        val grupos = if (botoes.size <= 3) listOf(botoes) else botoes.chunked(2)
        grupos.forEach { linhaBotoes ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }
            linhaBotoes.forEachIndexed { index, botao ->
                botao.layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    marginEnd = if (index < linhaBotoes.lastIndex) dp(8) else 0
                }
                linha.addView(botao)
            }
            if (linhaBotoes.size == 1) {
                linha.addView(Space(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                })
            }
            card.addView(linha)
        }
    }

    private fun criarBotao(texto: String, destaque: Boolean = false, perigo: Boolean = false, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = texto
            isAllCaps = false
            minHeight = dp(38)
            minWidth = 0
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setTextColor(getColor(if (destaque) R.color.bb_primary_text else if (perigo) R.color.bb_danger else R.color.bb_luso_navy))
            background = getDrawable(
                when {
                    perigo -> R.drawable.bg_category_action_danger
                    destaque -> R.drawable.bg_category_action_primary
                    else -> R.drawable.bg_category_action
                }
            )
            maxLines = 2
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener {
                isEnabled = false
                postDelayed({ isEnabled = true }, 1200)
                onClick()
            }
        }
    }

    private fun criarIconeCategoria(nome: String, chave: String = ""): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
            background = getDrawable(R.drawable.bg_category_symbol)
            addView(ImageView(this@ExplorarCategoriasActivity).apply {
                setImageResource(iconeCategoria(nome, chave))
                setColorFilter(getColor(R.color.bb_luso_navy))
            }, FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER))
        }
    }

    private fun criarTextoTitulo(texto: String): TextView {
        return TextView(this).apply {
            text = texto
            textSize = 18f
            setTextColor(getColor(R.color.bb_luso_navy))
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
    }

    private fun criarTextoSecundario(texto: String): TextView {
        return TextView(this).apply {
            this.text = texto
            textSize = 12f
            setTextColor(getColor(R.color.bb_text_secondary))
            setPadding(0, dp(4), 0, 0)
        }
    }

    private fun criarTextoCorpo(texto: String): TextView {
        return TextView(this).apply {
            this.text = texto
            textSize = 13f
            setTextColor(getColor(R.color.bb_luso_navy))
            setPadding(0, dp(6), 0, dp(3))
        }
    }

    private fun criarBadgeEstado(publica: Boolean): TextView {
        return TextView(this).apply {
            text = getString(if (publica) R.string.estado_publica else R.string.estado_privada)
            textSize = 12f
            setTextColor(getColor(if (publica) R.color.bb_success else R.color.bb_luso_navy))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = getDrawable(if (publica) R.drawable.bg_category_status_public else R.drawable.bg_category_status_private)
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }
    }

    private fun iconeCategoria(nome: String, chave: String = ""): Int {
        when (chave) {
            "history_ship" -> return R.drawable.ill_caravel_quiz
            "geography_globe" -> return R.drawable.ic_globe
            "math_board" -> return R.drawable.ic_podium_clean
            "cinema_clapper" -> return R.drawable.ic_masks
            "science_atom" -> return R.drawable.ic_chest_clean
            "sports_ball" -> return R.drawable.ic_ball
            "culture_masks" -> return R.drawable.ic_masks
            "portugal_rooster" -> return R.drawable.ic_flag_clean
            "literature_book" -> return R.drawable.ic_book
            "music_guitar" -> return R.drawable.ic_star
            "food_plate" -> return R.drawable.ic_star
            "technology_chip" -> return R.drawable.ic_chest_clean
            "default_star" -> return R.drawable.ic_star
        }
        val normalizado = nome.lowercase()
        return when {
            "hist" in normalizado || "naval" in normalizado -> R.drawable.ill_caravel_quiz
            "geo" in normalizado || "mundo" in normalizado -> R.drawable.ic_compass_simple
            "desporto" in normalizado || "sport" in normalizado -> R.drawable.ic_target_clean
            "cultura" in normalizado || "filme" in normalizado || "série" in normalizado -> R.drawable.ic_masks
            "mat" in normalizado || "desafio" in normalizado -> R.drawable.ic_podium_clean
            "ciência" in normalizado || "tecnologia" in normalizado -> R.drawable.ic_chest_clean
            else -> R.drawable.ic_star
        }
    }

    private fun mostrarEscolhaModo(categoria: CategoriaExploravel) {
        if (nomeUtilizador.isNullOrBlank() && nomeJogador.isNullOrBlank()) {
            Toast.makeText(this, R.string.indica_nome_antes_jogar, Toast.LENGTH_SHORT).show()
            return
        }

        CategoriaModoDialog.mostrar(
            this,
            listOf(
                CategoriaModoDialog.Opcao(getString(R.string.modo_solo_classico), getString(R.string.modo_solo_classico_descricao), R.drawable.ic_solo) {
                    iniciarCategoriaSolo(categoria, GameConstants.MODO_CLASSICO)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_solo_caotico), getString(R.string.modo_solo_caotico_descricao), R.drawable.ic_bomb) {
                    iniciarCategoriaSolo(categoria, GameConstants.MODO_CAOTICO)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_solo_eliminatorias), getString(R.string.modo_solo_eliminatorias_descricao), R.drawable.ic_shield_clean) {
                    iniciarCategoriaSolo(categoria, GameConstants.MODO_ELIMINATORIAS)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_classico_grupo), getString(R.string.modo_classico_grupo_descricao), R.drawable.ic_group) {
                    iniciarCategoria(categoria, GameConstants.MODO_CLASSICO)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_1x1_convite), getString(R.string.modo_1x1_convite_descricao), R.drawable.ic_sword) {
                    abrirConviteCategoria(categoria, GameConstants.MODO_1X1)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_2x2_convite), getString(R.string.modo_2x2_convite_descricao), R.drawable.ic_duo) {
                    abrirConviteCategoria(categoria, GameConstants.MODO_2X2)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_eliminatorias_grupo), getString(R.string.modo_eliminatorias_grupo_descricao), R.drawable.ic_trophy) {
                    iniciarCategoria(categoria, GameConstants.MODO_ELIMINATORIAS)
                }
            )
        )
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

    private fun iniciarCategoriaSolo(categoria: CategoriaExploravel, modo: String) {
        val intent = Intent(this, JogoActivity::class.java)
        intent.putExtra(IntentExtras.MODO_SOLO, true)
        intent.putExtra(IntentExtras.MODO_JOGO, modo)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }

        when (categoria) {
            is CategoriaExploravel.Publica -> {
                intent.putExtra(IntentExtras.NOME_CATEGORIA, categoria.categoria.nome)
                intent.putExtra(IntentExtras.CATEGORIA_PUBLICA_ID, categoria.categoria.id)
                intent.putExtra(IntentExtras.ORIGEM_CATEGORIA, GameConstants.ORIGEM_CATEGORIA_PUBLICA)
            }
            is CategoriaExploravel.Personalizada -> {
                intent.putExtra(IntentExtras.NOME_CATEGORIA, categoria.categoria.nome)
                intent.putExtra(IntentExtras.ORIGEM_CATEGORIA, GameConstants.ORIGEM_CATEGORIA_PERSONALIZADA)
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

    private fun abrirMinhasCategorias() {
        val intent = Intent(this, MinhasCategoriasActivity::class.java)
        nomeUtilizador?.let { intent.putExtra(IntentExtras.NOME_UTILIZADOR, it) }
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
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
