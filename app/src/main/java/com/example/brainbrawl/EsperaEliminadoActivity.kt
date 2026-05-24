package com.example.brainbrawl

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityEsperaEliminadoBinding
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.utils.AvatarUtils
import com.example.brainbrawl.viewmodels.EsperaEliminadoEvent
import com.example.brainbrawl.viewmodels.EsperaEliminadoViewModel
import com.example.brainbrawl.viewmodels.RankingParcialEliminadoUi

class EsperaEliminadoActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEsperaEliminadoBinding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[EsperaEliminadoViewModel::class.java]
    }
    private val authService = AuthService()

    private lateinit var codigoSala: String
    private var uid: String = ""
    private lateinit var nomeJogador: String
    private lateinit var nomeCategoria: String
    private lateinit var nomeUtilizador: String
    private var modoJogo: String? = null
    private var totalPontos = 0.0
    private var numeroPerguntasCertas = 0
    private var totalPerguntascertas = 0
    private var totalPerguntas = 1
    private var categoriaCompetitiva = true
    private var isAdmin = false
    private var podioAberto = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: "Jogador"
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_ELIMINATORIAS
        totalPontos = intent.getDoubleExtra(IntentExtras.TOTAL_PONTOS, 0.0)
        numeroPerguntasCertas = intent.getIntExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, 0)
        totalPerguntascertas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, 0)
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 1)
        categoriaCompetitiva = intent.getBooleanExtra(IntentExtras.CATEGORIA_COMPETITIVA, true)
        isAdmin = intent.getBooleanExtra(IntentExtras.ADMIN, false)

        binding.txtCodigoSala.text = getString(R.string.grupo_eliminacao_contexto)
        binding.btnBackHeader.setOnClickListener {
            Toast.makeText(this, R.string.aguardar_fim_partida, Toast.LENGTH_SHORT).show()
        }
        configurarObservers()
        viewModel.escutarFimJogo(codigoSala)
    }

    override fun onDestroy() {
        viewModel.removerListener()
        super.onDestroy()
    }

    private fun configurarObservers() {
        viewModel.evento.observe(this) { evento ->
            when (evento ?: return@observe) {
                EsperaEliminadoEvent.DadosInvalidos -> {
                    Toast.makeText(this, getString(R.string.dados_sala_invalidos), Toast.LENGTH_SHORT).show()
                    viewModel.consumirEvento()
                    finish()
                }
                EsperaEliminadoEvent.ErroAguardarFim -> {
                    Toast.makeText(this, getString(R.string.erro_aguardar_fim_jogo), Toast.LENGTH_SHORT).show()
                    viewModel.consumirEvento()
                }
                EsperaEliminadoEvent.JogoTerminado -> {
                    viewModel.consumirEvento()
                    abrirPodio()
                }
            }
        }
        viewModel.ranking.observe(this) { ranking ->
            renderizarRankingParcial(ranking)
        }
    }

    private fun renderizarRankingParcial(ranking: List<RankingParcialEliminadoUi>) {
        binding.layoutAindaJogar.removeAllViews()
        binding.layoutEliminados.removeAllViews()
        binding.txtTotalJogadoresHeader.text = ranking.size.toString()

        if (ranking.isEmpty()) {
            binding.txtAindaJogarTitulo.text = getString(R.string.ainda_a_jogar_count, 0)
            binding.txtEliminadosTitulo.text = getString(R.string.eliminados_count, 0)
            binding.layoutAindaJogar.addView(criarEstadoVazio(getString(R.string.a_carregar_jogadores)))
            return
        }

        val ativos = ranking.filter { it.ativo }
        val eliminados = ranking.filterNot { it.ativo }

        binding.txtAindaJogarTitulo.text = getString(R.string.ainda_a_jogar_count, ativos.size)
        binding.txtEliminadosTitulo.text = getString(R.string.eliminados_count, eliminados.size)

        if (ativos.isEmpty()) {
            binding.layoutAindaJogar.addView(criarEstadoVazio(getString(R.string.sem_jogadores_ativos)))
        } else {
            ativos.forEach { item ->
                binding.layoutAindaJogar.addView(criarLinhaJogador(item, ativo = true))
            }
        }

        if (eliminados.isEmpty()) {
            binding.layoutEliminados.addView(criarEstadoVazio(getString(R.string.sem_eliminados_ainda)))
        } else {
            eliminados.forEach { item ->
                binding.layoutEliminados.addView(criarLinhaJogador(item, ativo = false))
            }
        }
    }

    private fun criarEstadoVazio(texto: String): TextView {
        return TextView(this).apply {
            text = texto
            textSize = 14f
            setTextColor(getColor(R.color.bb_text_secondary))
            gravity = android.view.Gravity.CENTER
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                setColor(getColor(R.color.bb_luso_surface))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), getColor(R.color.bb_luso_border))
            }
        }
    }

    private fun criarLinhaJogador(item: RankingParcialEliminadoUi, ativo: Boolean): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = getDrawable(R.drawable.bg_spectator_player_row)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(10)) }
        }

        card.addView(ImageView(this).apply {
            setImageResource(AvatarUtils.resolverAvatar(this@EsperaEliminadoActivity, item.avatar))
            background = getDrawable(R.drawable.bg_game_avatar)
            setPadding(dp(1), dp(1), dp(1), dp(1))
            alpha = if (ativo) 1f else 0.62f
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        })

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(10); marginEnd = dp(8) }
        }
        textos.addView(TextView(this).apply {
            text = if (!ativo && item.nome == nomeJogador) {
                getString(R.string.jogador_tu_format, item.nome)
            } else {
                item.nome
            }
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.bb_luso_navy))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        textos.addView(TextView(this).apply {
            text = if (ativo) getString(R.string.ainda_a_jogar_estado) else item.detalhe
            textSize = 12f
            setTextColor(getColor(if (item.ativo) R.color.bb_success else R.color.bb_text_secondary))
        })
        card.addView(textos)

        card.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_shield_clean)
            alpha = if (ativo) 1f else 0.35f
            setColorFilter(getColor(if (ativo) R.color.bb_luso_navy else R.color.bb_text_secondary))
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
        })

        return card
    }

    private fun abrirPodio() {
        if (podioAberto) return
        podioAberto = true
        viewModel.removerListener()

        val intent = Intent(this, PontuacoesActivity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
        uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
        intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
        intent.putExtra(IntentExtras.TOTAL_PONTOS, totalPontos)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
        intent.putExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, numeroPerguntasCertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, totalPerguntascertas)
        intent.putExtra(IntentExtras.RESPOSTAS_CERTAS, totalPerguntascertas)
        intent.putExtra(IntentExtras.TOTAL_PERGUNTAS, totalPerguntas)
        intent.putExtra(IntentExtras.CATEGORIA_COMPETITIVA, categoriaCompetitiva)
        intent.putExtra(IntentExtras.ADMIN, isAdmin)
        startActivity(intent)
        finish()
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()
}
