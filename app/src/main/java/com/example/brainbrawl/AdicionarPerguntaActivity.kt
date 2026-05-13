package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.app.AlertDialog
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.brainbrawl.UteisSala.criarSalaPersonalizadaEEntrar
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.GameConstants
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
    private var modoJogo: String = GameConstants.MODO_CLASSICO
    private var admin: Boolean = true
    private var perguntaEmEdicaoId: String? = null
    private var categoriaEmEdicao: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    voltarParaCategorias()
                }
            }
        )

        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR)
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_CLASSICO
        admin = intent.getBooleanExtra(IntentExtras.ADMIN, true)
        val categoriaInicial = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)

        if (uid.isNullOrBlank() && nomeUtilizador.isBlank()) {
            Toast.makeText(this, R.string.apenas_registados_criar_categorias, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        configurarObservers()

        if (!categoriaInicial.isNullOrBlank()) {
            categoriaEmEdicao = categoriaInicial
            binding.txtTitulo.text = getString(R.string.editar_categoria)
            binding.edtNovaCategoria.setText(categoriaInicial)
            binding.edtNovaCategoria.isEnabled = false
            carregarPerguntasCategoria(categoriaInicial)
        }

        // Configurar o botão para enviar a pergunta
        binding.layoutBtnEnviar.setOnClickListener {
            //Guardar os dados dos editTexts
            val nomeCategoria = categoriaSelecionada()
            val pergunta = binding.edtPergunta.text.toString().trim()
            val opcaoA = binding.edtOpcaoA.text.toString().trim()
            val opcaoB = binding.edtOpcaoB.text.toString().trim()
            val opcaoC = binding.edtOpcaoC.text.toString().trim()
            val opcaoD = binding.edtOpcaoD.text.toString().trim()
            val imagem = binding.edtImagem.text.toString().trim()
            val dificuldade = dificuldadeSelecionada()

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
                categoriasReservadas()
            )
        }

        // Configurar o botão para voltar ao MainActivity
        binding.layoutBtnComecar.setOnClickListener {
            val nomeCategoria = categoriaSelecionada()
            if (nomeCategoria.isBlank()) {
                Toast.makeText(this, R.string.indica_categoria_personalizada, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarEscolhaModo(nomeCategoria)
        }

        binding.layoutBtnVoltar.setOnClickListener {
            voltarParaCategorias()
        }

        binding.btnNovaPergunta.setOnClickListener {
            perguntaEmEdicaoId = null
            limparCampos()
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
            vazio.setTextColor(0xFF000000.toInt())
            binding.layoutPerguntasPersonalizadas.addView(vazio)
            return
        }

        val linhaNumeros = LinearLayout(this)
        linhaNumeros.orientation = LinearLayout.HORIZONTAL
        for ((index, perguntaCategoria) in perguntas.withIndex()) {
            val perguntaId = perguntaCategoria.id ?: continue
            val botao = Button(this)
            botao.text = (index + 1).toString()
            botao.isSelected = perguntaEmEdicaoId == perguntaId
            botao.setOnClickListener {
                perguntaEmEdicaoId = perguntaId
                preencherFormulario(perguntaCategoria)
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

    private fun mostrarEscolhaModo(nomeCategoria: String) {
        val opcoes = resources.getStringArray(R.array.opcoes_modo_personalizado)
        AlertDialog.Builder(this)
            .setTitle(R.string.escolher_modo)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> iniciarCategoriaPersonalizada(nomeCategoria, GameConstants.MODO_CLASSICO)
                    1 -> abrirConviteCategoria(nomeCategoria, GameConstants.MODO_1X1)
                    2 -> abrirConviteCategoria(nomeCategoria, GameConstants.MODO_2X2)
                    3 -> iniciarCategoriaPersonalizada(nomeCategoria, GameConstants.MODO_ELIMINATORIAS)
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun iniciarCategoriaPersonalizada(nomeCategoria: String, modo: String) {
        criarSalaPersonalizadaEEntrar(
            this,
            gerarCodigoSala(),
            nomeUtilizador,
            nomeCategoria,
            true,
            modo,
            uid
        ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun abrirConviteCategoria(nomeCategoria: String, modo: String) {
        if (nomeUtilizador.isBlank()) {
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
        uid?.let {
            intent.putExtra(IntentExtras.UID, it)
            intent.putExtra(IntentExtras.DONO_UID, it)
        }
        intent.putExtra(IntentExtras.DONO_CATEGORIA, nomeUtilizador)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        intent.putExtra(IntentExtras.ADMIN, true)
        startActivity(intent)
    }

    private fun dificuldadeSelecionada(): String? {
        return when (binding.rgDificuldade.checkedRadioButtonId) {
            binding.rbDificuldadeFacil.id -> "facil"
            binding.rbDificuldadeMedia.id -> "media"
            binding.rbDificuldadeDificil.id -> "dificil"
            else -> null
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

}
