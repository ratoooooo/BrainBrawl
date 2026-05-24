package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.app.AlertDialog
import android.graphics.Typeface
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityAdicionarPerguntaBinding
import com.example.brainbrawl.repositories.CategoriaRepository
import com.example.brainbrawl.services.AuthService
import com.example.brainbrawl.viewmodels.EditarCategoriaEvent
import com.example.brainbrawl.viewmodels.EditarCategoriaViewModel

class AdicionarPerguntaActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityAdicionarPerguntaBinding.inflate(layoutInflater)
    }
    private val viewModel by lazy {
        ViewModelProvider(this)[EditarCategoriaViewModel::class.java]
    }
    private val authService = AuthService()
    private var nomeUtilizador: String = ""
    private var nomeJogador: String? = null
    private var uid: String? = null
    private var perguntaEmEdicaoId: String? = null
    private var categoriaEmEdicao: String? = null
    private var formularioBase = FormularioPergunta()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    confirmarDescartarSeNecessario {
                        voltarParaCategorias()
                    }
                }
            }
        )

        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid
        val categoriaInicial = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)

        if (uid.isNullOrBlank() && nomeUtilizador.isBlank()) {
            Toast.makeText(this, R.string.apenas_registados_criar_categorias, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        configurarObservers()
        configurarSeletorIcone()

        if (!categoriaInicial.isNullOrBlank()) {
            categoriaEmEdicao = categoriaInicial
            binding.txtTitulo.text = getString(R.string.editar_categoria)
            binding.edtNovaCategoria.setText(categoriaInicial)
            binding.edtNovaCategoria.isEnabled = false
            carregarPerguntasCategoria(categoriaInicial)
        }

        // Configurar o botão para enviar a pergunta
        binding.layoutBtnEnviar.setOnClickListener {
            binding.layoutBtnEnviar.isEnabled = false
            //Guardar os dados dos editTexts
            val nomeCategoria = categoriaSelecionada()
            val pergunta = binding.edtPergunta.text.toString().trim()
            val opcaoA = binding.edtOpcaoA.text.toString().trim()
            val opcaoB = binding.edtOpcaoB.text.toString().trim()
            val opcaoC = binding.edtOpcaoC.text.toString().trim()
            val opcaoD = binding.edtOpcaoD.text.toString().trim()
            val imagem = binding.edtImagem.text.toString().trim()
            val dificuldade = dificuldadeSelecionada()
            val iconeCategoria = iconeCategoriaSelecionado()
            val descricaoCategoria = binding.edtDescricaoCategoria.text.toString().trim()

            // Obter a resposta correta
            val respostaCorreta = when (binding.rgOpcoes.checkedRadioButtonId) {
                binding.rbOpcaoA.id -> opcaoA
                binding.rbOpcaoB.id -> opcaoB
                binding.rbOpcaoC.id -> opcaoC
                binding.rbOpcaoD.id -> opcaoD
                else -> ""
            }

            viewModel.guardarPergunta(
                uid.orEmpty(),
                nomeUtilizador,
                nomeCategoria,
                perguntaEmEdicaoId,
                pergunta,
                opcaoA,
                opcaoB,
                opcaoC,
                opcaoD,
                respostaCorreta,
                imagem,
                dificuldade,
                iconeCategoria,
                descricaoCategoria,
                categoriasReservadas()
            )
        }

        binding.layoutBtnVoltar.setOnClickListener {
            confirmarDescartarSeNecessario {
                voltarParaCategorias()
            }
        }

        binding.btnBackHeader.setOnClickListener {
            binding.layoutBtnVoltar.performClick()
        }

        binding.btnNovaPergunta.setOnClickListener {
            confirmarDescartarSeNecessario {
                perguntaEmEdicaoId = null
                limparCampos()
            }
        }

        binding.btnEliminarPerguntaAtual.setOnClickListener {
            confirmarEliminarPerguntaAtual()
        }
    }

    private fun carregarPerguntasCategoria(nomeCategoria: String) {
        viewModel.carregarPerguntasCategoria(uid.orEmpty(), nomeUtilizador, nomeCategoria)
    }

    private fun configurarObservers() {
        viewModel.perguntas.observe(this) { perguntas ->
            preencherListaPerguntas(perguntas)
        }
        viewModel.evento.observe(this) { evento ->
            tratarEvento(evento ?: return@observe)
            viewModel.consumirEvento()
        }
    }

    private fun tratarEvento(evento: EditarCategoriaEvent) {
        binding.layoutBtnEnviar.isEnabled = true
        when (evento) {
            EditarCategoriaEvent.CategoriaCriada -> Unit
            EditarCategoriaEvent.PerguntaGuardada -> {
                Toast.makeText(this, R.string.pergunta_guardada_sucesso, Toast.LENGTH_SHORT).show()
                perguntaEmEdicaoId = null
                limparCampos()
            }
            is EditarCategoriaEvent.PerguntaEliminada -> {
                if (perguntaEmEdicaoId == evento.perguntaId) {
                    perguntaEmEdicaoId = null
                    limparCampos()
                }
            }
            is EditarCategoriaEvent.ValidacaoFalhou -> {
                Toast.makeText(this, evento.mensagem, Toast.LENGTH_SHORT).show()
            }
            is EditarCategoriaEvent.ErroGuardar -> {
                Toast.makeText(this, getString(R.string.erro_guardar_pergunta_format, evento.mensagem), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun preencherListaPerguntas(perguntas: List<CategoriaRepository.PerguntaCategoria>) {
        binding.layoutPerguntasPersonalizadas.removeAllViews()
        binding.txtTotalPerguntas.text = getString(R.string.total_perguntas_format, perguntas.size)
        binding.btnEliminarPerguntaAtual.isEnabled = perguntaEmEdicaoId != null
        if (perguntas.isEmpty()) {
            val vazio = TextView(this)
            vazio.text = getString(R.string.sem_perguntas_categoria)
            vazio.setTextColor(getColor(R.color.bb_text_secondary))
            vazio.background = getDrawable(R.drawable.bg_empty_state_card)
            vazio.setPadding(dp(18), dp(18), dp(18), dp(18))
            binding.layoutPerguntasPersonalizadas.addView(vazio)
            return
        }

        val linhaNumeros = LinearLayout(this)
        linhaNumeros.orientation = LinearLayout.HORIZONTAL
        for ((index, perguntaCategoria) in perguntas.withIndex()) {
            val perguntaId = perguntaCategoria.id ?: continue
            val botao = Button(this).apply {
                text = (index + 1).toString()
                tag = perguntaId
                isAllCaps = false
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                minHeight = dp(44)
                setTextColor(getColor(if (perguntaEmEdicaoId == perguntaId) R.color.bb_primary_text else R.color.bb_secondary_text))
                background = getDrawable(if (perguntaEmEdicaoId == perguntaId) R.drawable.bg_button_primary else R.drawable.bg_button_secondary)
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    marginEnd = dp(6)
                }
            }
            botao.setOnClickListener {
                confirmarDescartarSeNecessario {
                    perguntaEmEdicaoId = perguntaId
                    atualizarSelecaoPerguntas(linhaNumeros, perguntaId)
                    preencherFormulario(perguntaCategoria)
                }
            }
            linhaNumeros.addView(botao)
        }
        binding.layoutPerguntasPersonalizadas.addView(linhaNumeros)
    }

    private fun preencherFormulario(perguntaCategoria: CategoriaRepository.PerguntaCategoria) {
        val opcoes = perguntaCategoria.opcoes
        val respostaCorreta = perguntaCategoria.respostaCorreta
        binding.rgOpcoes.clearCheck()
        binding.edtPergunta.setText(perguntaCategoria.pergunta)
        binding.edtOpcaoA.setText(opcoes.getOrNull(0) ?: "")
        binding.edtOpcaoB.setText(opcoes.getOrNull(1) ?: "")
        binding.edtOpcaoC.setText(opcoes.getOrNull(2) ?: "")
        binding.edtOpcaoD.setText(opcoes.getOrNull(3) ?: "")
        binding.edtImagem.setText(perguntaCategoria.imagem)
        aplicarDificuldade(perguntaCategoria.dificuldade)
        when (respostaCorreta) {
            opcoes.getOrNull(0) -> binding.rbOpcaoA.isChecked = true
            opcoes.getOrNull(1) -> binding.rbOpcaoB.isChecked = true
            opcoes.getOrNull(2) -> binding.rbOpcaoC.isChecked = true
            opcoes.getOrNull(3) -> binding.rbOpcaoD.isChecked = true
        }
        binding.btnEliminarPerguntaAtual.isEnabled = true
        formularioBase = formularioAtual()
    }

    //Limpar os campos
    private fun limparCampos() {
        //Limpar os campos
        binding.edtPergunta.text.clear()
        binding.edtOpcaoA.text.clear()
        binding.edtOpcaoB.text.clear()
        binding.edtOpcaoC.text.clear()
        binding.edtOpcaoD.text.clear()
        binding.edtImagem.text.clear()
        binding.rgOpcoes.clearCheck()
        aplicarDificuldade("media")
        binding.btnEliminarPerguntaAtual.isEnabled = false
        formularioBase = formularioAtual()
    }

    private fun voltarParaCategorias() {
        val intent = Intent(this, ExplorarCategoriasActivity::class.java)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        uid?.let { intent.putExtra(IntentExtras.UID, it) }
        startActivity(intent)
        finish()
    }

    private fun categoriaSelecionada(): String {
        return categoriaEmEdicao ?: binding.edtNovaCategoria.text.toString().trim()
    }

    private fun categoriasReservadas(): Set<String> {
        return setOf(
            getString(R.string.categoria1),
            getString(R.string.categoria2),
            getString(R.string.categoria3),
            getString(R.string.categoria4)
        )
    }

    private fun confirmarEliminarPerguntaAtual() {
        val perguntaId = perguntaEmEdicaoId
        if (perguntaId.isNullOrBlank()) {
            Toast.makeText(this, R.string.escolhe_pergunta_eliminar, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.eliminar_pergunta)
            .setMessage(R.string.confirmar_eliminar_pergunta)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                viewModel.eliminarPergunta(uid.orEmpty(), nomeUtilizador, categoriaSelecionada(), perguntaId)
            }
            .show()
    }

    private fun dificuldadeSelecionada(): String? {
        return when (binding.rgDificuldade.checkedRadioButtonId) {
            binding.rbDificuldadeFacil.id -> "facil"
            binding.rbDificuldadeMedia.id -> "media"
            binding.rbDificuldadeDificil.id -> "dificil"
            else -> null
        }
    }

    private fun iconeCategoriaSelecionado(): String {
        val selecionado = binding.rgIconeCategoriaLinha2.checkedRadioButtonId
            .takeIf { it != -1 }
            ?: binding.rgIconeCategoria.checkedRadioButtonId
        return when (selecionado) {
            binding.rbIconHistory.id -> "history_ship"
            binding.rbIconGeo.id -> "geography_globe"
            binding.rbIconMath.id -> "math_board"
            binding.rbIconCulture.id -> "culture_masks"
            binding.rbIconScience.id -> "science_atom"
            else -> "default_star"
        }
    }

    private fun configurarSeletorIcone() {
        var atualizando = false
        binding.rgIconeCategoria.setOnCheckedChangeListener { _, checkedId ->
            if (!atualizando && checkedId != -1) {
                atualizando = true
                binding.rgIconeCategoriaLinha2.clearCheck()
                atualizando = false
            }
        }
        binding.rgIconeCategoriaLinha2.setOnCheckedChangeListener { _, checkedId ->
            if (!atualizando && checkedId != -1) {
                atualizando = true
                binding.rgIconeCategoria.clearCheck()
                atualizando = false
            }
        }
    }

    private fun aplicarDificuldade(dificuldade: String?) {
        binding.rgDificuldade.clearCheck()
        when (dificuldade) {
            "facil" -> binding.rbDificuldadeFacil.isChecked = true
            "dificil" -> binding.rbDificuldadeDificil.isChecked = true
            else -> binding.rbDificuldadeMedia.isChecked = true
        }
    }

    private fun atualizarSelecaoPerguntas(linha: LinearLayout, perguntaId: String) {
        for (i in 0 until linha.childCount) {
            val botao = linha.getChildAt(i) as? Button ?: continue
            val selecionado = botao.tag == perguntaId
            botao.setTextColor(getColor(if (selecionado) R.color.bb_primary_text else R.color.bb_secondary_text))
            botao.background = getDrawable(if (selecionado) R.drawable.bg_button_primary else R.drawable.bg_button_secondary)
        }
    }

    private fun confirmarDescartarSeNecessario(continuar: () -> Unit) {
        if (!temAlteracoesPorGuardar()) {
            continuar()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.pergunta_por_guardar_titulo)
            .setMessage(R.string.confirmar_descartar_pergunta)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.descartar_pergunta) { _, _ -> continuar() }
            .show()
    }

    private fun temAlteracoesPorGuardar(): Boolean {
        return formularioAtual() != formularioBase
    }

    private fun formularioAtual(): FormularioPergunta {
        return FormularioPergunta(
            pergunta = binding.edtPergunta.text.toString(),
            opcaoA = binding.edtOpcaoA.text.toString(),
            opcaoB = binding.edtOpcaoB.text.toString(),
            opcaoC = binding.edtOpcaoC.text.toString(),
            opcaoD = binding.edtOpcaoD.text.toString(),
            imagem = binding.edtImagem.text.toString(),
            descricaoCategoria = binding.edtDescricaoCategoria.text.toString(),
            iconeCategoria = iconeCategoriaSelecionado(),
            respostaSelecionadaId = binding.rgOpcoes.checkedRadioButtonId,
            dificuldade = dificuldadeSelecionada()
        )
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private data class FormularioPergunta(
        val pergunta: String = "",
        val opcaoA: String = "",
        val opcaoB: String = "",
        val opcaoC: String = "",
        val opcaoD: String = "",
        val imagem: String = "",
        val descricaoCategoria: String = "",
        val iconeCategoria: String = "default_star",
        val respostaSelecionadaId: Int = -1,
        val dificuldade: String? = "media"
    )
}
