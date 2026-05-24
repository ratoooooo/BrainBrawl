package com.example.brainbrawl

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
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
import com.example.brainbrawl.UteisSala.criarSalaPersonalizadaEEntrar
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityMinhasCategoriasBinding
import com.example.brainbrawl.repositories.CategoriaRepository
import com.example.brainbrawl.routes.BottomNavHelper
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.viewmodels.ExplorarCategoriasEvent
import com.example.brainbrawl.viewmodels.ExplorarCategoriasUiState
import com.example.brainbrawl.viewmodels.ExplorarCategoriasViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MinhasCategoriasActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMinhasCategoriasBinding.inflate(layoutInflater) }
    private val viewModel by lazy { ViewModelProvider(this)[ExplorarCategoriasViewModel::class.java] }
    private val authService = AuthService()
    private var nomeUtilizador: String? = null
    private var nomeJogador: String? = null
    private var uid: String? = null
    private var estadoAtual = ExplorarCategoriasUiState()
    private var filtro = FiltroMinhas.MINHAS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR)
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid
        BottomNavHelper.instalar(this, BottomNavHelper.Item.MAIN, uid, nomeUtilizador, nomeJogador)

        binding.btnVoltar.setOnClickListener { finish() }
        binding.tabMinhas.setOnClickListener {
            filtro = FiltroMinhas.MINHAS
            atualizarTabs()
            renderizar()
        }
        binding.tabAguardando.setOnClickListener {
            filtro = FiltroMinhas.AGUARDANDO
            atualizarTabs()
            renderizar()
        }

        viewModel.categorias.observe(this) { estado ->
            estadoAtual = estado
            renderizar()
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }

        atualizarTabs()
        viewModel.carregarCategorias(uid.orEmpty(), nomeUtilizador.orEmpty())
    }

    override fun onDestroy() {
        viewModel.removerListener()
        super.onDestroy()
    }

    private fun renderizar() {
        binding.layoutMinhasCategorias.removeAllViews()
        binding.txtEstado.text = when {
            estadoAtual.carregando -> getString(R.string.a_carregar_categorias)
            estadoAtual.erro -> getString(R.string.erro_carregar_categorias)
            else -> ""
        }
        if (estadoAtual.carregando || estadoAtual.erro) return

        val publicasIds = estadoAtual.categoriasPublicas.map { it.id }.toSet()
        val categorias = estadoAtual.minhasCategorias.filter { categoria ->
            val jaPublica = categoria.jaPublica(publicasIds)
            when (filtro) {
                FiltroMinhas.MINHAS -> true
                FiltroMinhas.AGUARDANDO -> !jaPublica && categoria.estadoPublicacao == GameConstants.ESTADO_AGUARDANDO
            }
        }

        if (categorias.isEmpty()) {
            binding.txtEstado.text = if (filtro == FiltroMinhas.AGUARDANDO) {
                getString(R.string.sem_categorias_aguardando)
            } else {
                getString(R.string.sem_categorias_personalizadas)
            }
            return
        }

        categorias.forEach { categoria ->
            binding.layoutMinhasCategorias.addView(criarCardCategoria(categoria, categoria.jaPublica(publicasIds)))
        }
    }

    private fun criarCardCategoria(
        categoria: CategoriaRepository.CategoriaPersonalizada,
        jaPublica: Boolean
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_ranking_card)
            setPadding(dp(16), dp(16), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }

            val topo = LinearLayout(this@MinhasCategoriasActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            topo.addView(criarIconeCategoria(categoria.nome, categoria.iconeCategoria))
            topo.addView(LinearLayout(this@MinhasCategoriasActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(16)
                }
                addView(criarTextoTitulo(categoria.nome))
                addView(criarTextoSecundario(getString(R.string.perguntas_count_format, categoria.totalPerguntas)))
                addView(criarTextoSecundario(getString(R.string.categoria_jogadas_atualizada_format, categoria.usos, categoria.dataAtualizacao.textoData())))
            })
            topo.addView(criarBadgeEstado(jaPublica, categoria.estadoPublicacao))
            topo.addView(ImageView(this@MinhasCategoriasActivity).apply {
                setImageResource(R.drawable.ic_chevron_right)
                setColorFilter(getColor(R.color.bb_luso_navy))
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginStart = dp(8) }
                setOnClickListener { abrirEdicaoCategoria(categoria.nome) }
            })
            addView(topo)

            addView(View(this@MinhasCategoriasActivity).apply {
                setBackgroundColor(getColor(R.color.bb_luso_border))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply {
                    topMargin = dp(14)
                    bottomMargin = dp(10)
                }
            })

            adicionarGrupoBotoes(
                this,
                buildList {
                    add(criarBotao(getString(R.string.jogar), destaque = true) { mostrarEscolhaModo(categoria) })
                    add(criarBotao(getString(R.string.editar_categoria)) { abrirEdicaoCategoria(categoria.nome) })
                    add(criarBotao(getString(R.string.eliminar), perigo = true) { confirmarEliminarCategoria(categoria) })
                    if (!jaPublica) {
                        add(criarBotao(getString(R.string.tornar_publica)) {
                            viewModel.publicarCategoria(uid.orEmpty(), nomeUtilizador.orEmpty(), nomeJogador, categoria.nome)
                        })
                    }
                }
            )
        }
    }

    private fun mostrarEscolhaModo(categoria: CategoriaRepository.CategoriaPersonalizada) {
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
                    iniciarCategoriaGrupo(categoria, GameConstants.MODO_CLASSICO)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_1x1_convite), getString(R.string.modo_1x1_convite_descricao), R.drawable.ic_sword) {
                    abrirConviteCategoria(categoria, GameConstants.MODO_1X1)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_2x2_convite), getString(R.string.modo_2x2_convite_descricao), R.drawable.ic_duo) {
                    abrirConviteCategoria(categoria, GameConstants.MODO_2X2)
                },
                CategoriaModoDialog.Opcao(getString(R.string.modo_eliminatorias_grupo), getString(R.string.modo_eliminatorias_grupo_descricao), R.drawable.ic_trophy) {
                    iniciarCategoriaGrupo(categoria, GameConstants.MODO_ELIMINATORIAS)
                }
            )
        )
    }

    private fun iniciarCategoriaGrupo(categoria: CategoriaRepository.CategoriaPersonalizada, modo: String) {
        criarSalaPersonalizadaEEntrar(
            this,
            gerarCodigoSala(),
            nomeUtilizador.orEmpty(),
            categoria.nome,
            true,
            modo,
            uid
        ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun iniciarCategoriaSolo(categoria: CategoriaRepository.CategoriaPersonalizada, modo: String) {
        startActivity(Intent(this, JogoActivity::class.java).apply {
            putExtra(IntentExtras.MODO_SOLO, true)
            putExtra(IntentExtras.MODO_JOGO, modo)
            putExtra(IntentExtras.NOME_CATEGORIA, categoria.nome)
            putExtra(IntentExtras.ORIGEM_CATEGORIA, GameConstants.ORIGEM_CATEGORIA_PERSONALIZADA)
            putExtra(IntentExtras.DONO_CATEGORIA, categoria.chaveDono.ifBlank { nomeUtilizador.orEmpty() })
            categoria.uid.takeIf { it.isNotBlank() }?.let { putExtra(IntentExtras.DONO_UID, it) }
            nomeUtilizador?.let { putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { putExtra(IntentExtras.NOME_JOGADOR, it) }
            uid?.let { putExtra(IntentExtras.UID, it) }
        })
    }

    private fun abrirConviteCategoria(categoria: CategoriaRepository.CategoriaPersonalizada, modo: String) {
        if (nomeUtilizador.isNullOrBlank()) {
            Toast.makeText(this, R.string.convites_precisam_conta, Toast.LENGTH_SHORT).show()
            return
        }
        val destino = if (modo == GameConstants.MODO_2X2) ConvidarAmigo2x2Activity::class.java else ConvidarAmigo1x1Activity::class.java
        startActivity(Intent(this, destino).apply {
            putExtra(IntentExtras.MODO_JOGO, modo)
            putExtra(IntentExtras.ADMIN, true)
            putExtra(IntentExtras.NOME_CATEGORIA, categoria.nome)
            categoria.uid.takeIf { it.isNotBlank() }?.let { putExtra(IntentExtras.DONO_UID, it) }
            putExtra(IntentExtras.DONO_CATEGORIA, categoria.chaveDono.ifBlank { nomeUtilizador.orEmpty() })
            nomeUtilizador?.let { putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { putExtra(IntentExtras.NOME_JOGADOR, it) }
            uid?.let { putExtra(IntentExtras.UID, it) }
        })
    }

    private fun abrirEdicaoCategoria(nomeCategoria: String) {
        startActivity(Intent(this, AdicionarPerguntaActivity::class.java).apply {
            putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
            putExtra(IntentExtras.MODO_JOGO, GameConstants.MODO_CLASSICO)
            putExtra(IntentExtras.ADMIN, true)
            nomeUtilizador?.let { putExtra(IntentExtras.NOME_UTILIZADOR, it) }
            nomeJogador?.let { putExtra(IntentExtras.NOME_JOGADOR, it) }
            uid?.let { putExtra(IntentExtras.UID, it) }
        })
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

    private fun tratarEvento(evento: ExplorarCategoriasEvent) {
        val res = when (evento) {
            ExplorarCategoriasEvent.LoginNecessarioGuardar -> R.string.login_guardar_categorias
            ExplorarCategoriasEvent.LoginNecessarioAvaliar -> R.string.login_avaliar_categorias
            ExplorarCategoriasEvent.LoginNecessarioGerir -> R.string.login_gerir_categorias
            ExplorarCategoriasEvent.CategoriaGuardada -> R.string.categoria_guardada
            ExplorarCategoriasEvent.CategoriaEliminada -> R.string.categoria_eliminada
            ExplorarCategoriasEvent.CategoriaPublicada -> R.string.categoria_publica_guardada
            ExplorarCategoriasEvent.CategoriaPublicaRemovida -> R.string.categoria_publica_removida
            ExplorarCategoriasEvent.AvaliacaoGuardada -> R.string.avaliacao_guardada
            ExplorarCategoriasEvent.CategoriaJaAvaliada -> R.string.categoria_ja_avaliada
            is ExplorarCategoriasEvent.Erro -> null
        }
        Toast.makeText(this, res?.let { getString(it) } ?: (evento as ExplorarCategoriasEvent.Erro).mensagem, Toast.LENGTH_SHORT).show()
    }

    private fun atualizarTabs() {
        val ativaMinhas = filtro == FiltroMinhas.MINHAS
        binding.tabMinhas.setTextColor(getColor(if (ativaMinhas) R.color.bb_luso_navy else R.color.bb_text_secondary))
        binding.tabAguardando.setTextColor(getColor(if (!ativaMinhas) R.color.bb_luso_navy else R.color.bb_text_secondary))
        binding.tabMinhas.setTypeface(binding.tabMinhas.typeface, if (ativaMinhas) Typeface.BOLD else Typeface.NORMAL)
        binding.tabAguardando.setTypeface(binding.tabAguardando.typeface, if (!ativaMinhas) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun adicionarGrupoBotoes(card: LinearLayout, botoes: List<Button>) {
        botoes.chunked(2).forEach { linhaBotoes ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(8)
                }
            }
            linhaBotoes.forEachIndexed { index, botao ->
                botao.layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    marginEnd = if (index < linhaBotoes.lastIndex) dp(8) else 0
                }
                linha.addView(botao)
            }
            if (linhaBotoes.size == 1) {
                linha.addView(Space(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
            }
            card.addView(linha)
        }
    }

    private fun criarBotao(texto: String, destaque: Boolean = false, perigo: Boolean = false, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = texto
            isAllCaps = false
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
            setOnClickListener { onClick() }
        }
    }

    private fun criarIconeCategoria(nome: String, chave: String = ""): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(68))
            background = getDrawable(R.drawable.bg_category_symbol)
            addView(ImageView(this@MinhasCategoriasActivity).apply {
                setImageResource(iconeCategoria(nome, chave))
                setColorFilter(getColor(R.color.bb_luso_navy))
            }, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
        }
    }

    private fun criarTextoTitulo(texto: String): TextView = TextView(this).apply {
        text = texto
        textSize = 18f
        setTextColor(getColor(R.color.bb_luso_navy))
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }

    private fun criarTextoSecundario(texto: String): TextView = TextView(this).apply {
        this.text = texto
        textSize = 12f
        setTextColor(getColor(R.color.bb_text_secondary))
        setPadding(0, dp(4), 0, 0)
    }

    private fun criarBadgeEstado(publica: Boolean, estado: String?): TextView {
        val aguardando = estado == GameConstants.ESTADO_AGUARDANDO
        return TextView(this).apply {
            text = getString(
                when {
                    aguardando -> R.string.aguardando_publicacao
                    publica -> R.string.estado_publica
                    else -> R.string.estado_privada
                }
            )
            textSize = 11f
            setTextColor(getColor(if (publica) R.color.bb_success else R.color.bb_luso_navy))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = getDrawable(
                when {
                    aguardando -> R.drawable.bg_category_status_pending
                    publica -> R.drawable.bg_category_status_public
                    else -> R.drawable.bg_category_status_private
                }
            )
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }
    }

    private fun CategoriaRepository.CategoriaPersonalizada.jaPublica(publicasIds: Set<String>): Boolean {
        val idCompatibilidade = categoriaPublicaId(chaveDono.ifBlank { donoAtual() }, nome)
        return !categoriaPublicaId.isNullOrBlank() || estadoPublicacao == GameConstants.ESTADO_PUBLICA || idCompatibilidade in publicasIds
    }

    private fun Long?.textoData(): String {
        return this?.let {
            getString(R.string.atualizada_em_format, SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT")).format(Date(it)))
        } ?: getString(R.string.atualizada_recentemente)
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

    private fun donoAtual(): String = uid.orEmpty().ifBlank { nomeUtilizador.orEmpty() }

    private fun categoriaPublicaId(nomeUtilizador: String, categoria: String): String {
        val bruto = "${nomeUtilizador}_${categoria}".lowercase()
        return bruto.replace(Regex("[.#$\\[\\]/]"), "_").replace(Regex("\\s+"), "_")
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private enum class FiltroMinhas {
        MINHAS,
        AGUARDANDO
    }
}
