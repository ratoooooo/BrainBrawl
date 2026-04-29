package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisSala.criarSalaPersonalizadaEEntrar
import com.example.brainbrawl.UteisSala.gerarCodigoSala
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityAdicionarPerguntaBinding
import com.example.brainbrawl.repositories.CategoriaRepository

class AdicionarPerguntaActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityAdicionarPerguntaBinding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val categoriaRepository = CategoriaRepository()
    private var nomeUtilizador: String = ""
    private var nomeJogador: String? = null
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
        modoJogo = intent.getStringExtra(IntentExtras.MODO_JOGO) ?: GameConstants.MODO_CLASSICO
        admin = intent.getBooleanExtra(IntentExtras.ADMIN, true)
        val categoriaInicial = intent.getStringExtra(IntentExtras.NOME_CATEGORIA)

        if (nomeUtilizador.isBlank()) {
            Toast.makeText(this, "Só jogadores registados podem criar categorias personalizadas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!categoriaInicial.isNullOrBlank()) {
            categoriaEmEdicao = categoriaInicial
            binding.txtTitulo.text = "Editar Categoria"
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

            // Obter a resposta correta
            val respostaCorreta = when (binding.rgOpcoes.checkedRadioButtonId) {
                binding.rbOpcaoA.id -> opcaoA
                binding.rbOpcaoB.id -> opcaoB
                binding.rbOpcaoC.id -> opcaoC
                binding.rbOpcaoD.id -> opcaoD
                else -> ""
            }

            // Verificar se a categoria é permitida
            if (nomeCategoria == getString(R.string.categoria1) || nomeCategoria == getString(R.string.categoria2) ||
                nomeCategoria == getString(R.string.categoria3) || nomeCategoria == getString(R.string.categoria4)) {
                Toast.makeText(this, "Categoria não permitida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar os tamanhos dos campos
            if (nomeCategoria.length > 50 || pergunta.length > 200 || opcaoA.length > 100 || opcaoB.length > 100 || opcaoC.length > 100 || opcaoD.length > 100) {
                Toast.makeText(this, "Campos excedem o tamanho máximo permitido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar se as respostas estão preenchidas
            if (listOf(opcaoA, opcaoB, opcaoC, opcaoD).distinct().size != 4) {
                Toast.makeText(this, "As opções devem ser todas diferentes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar se os campos estão preenchidos
            if (nomeCategoria.isEmpty() || pergunta.isEmpty() || opcaoA.isEmpty() || opcaoB.isEmpty() || opcaoC.isEmpty() || opcaoD.isEmpty() || respostaCorreta.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            categoriaRepository.guardarPerguntaPersonalizada(
                nomeUtilizador,
                nomeCategoria,
                perguntaEmEdicaoId,
                CategoriaRepository.PerguntaCategoria(
                    pergunta = pergunta,
                    respostaCorreta = respostaCorreta,
                    opcoes = listOf(opcaoA, opcaoB, opcaoC, opcaoD)
                )
            )
                // Verificar se a pergunta foi adicionada com sucesso
                .addOnSuccessListener {
                    // Exibir mensagem de sucesso
                    Toast.makeText(this, "Pergunta guardada com sucesso!", Toast.LENGTH_SHORT).show()
                    perguntaEmEdicaoId = null
                    carregarPerguntasCategoria(nomeCategoria)
                    limparCampos()
                }
                .addOnFailureListener { error ->
                    // Exibir mensagem de erro
                    Toast.makeText(this, "Erro ao guardar pergunta: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Configurar o botão para voltar ao MainActivity
        binding.layoutBtnComecar.setOnClickListener {
            //Guardar os dados dos editTexts
            val nomeCategoria = categoriaSelecionada()
            if (nomeCategoria.isBlank()) {
                Toast.makeText(this, "Indica a categoria personalizada.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            criarSalaPersonalizadaEEntrar(
                this,
                gerarCodigoSala(),
                nomeUtilizador,
                nomeCategoria,
                true,
                modoJogo
            ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
        }

        binding.layoutBtnVoltar.setOnClickListener {
            voltarParaCategorias()
        }
    }

    private fun carregarPerguntasCategoria(nomeCategoria: String) {
        if (nomeCategoria.isBlank()) return
        categoriaRepository.carregarPerguntasEditaveis(nomeUtilizador, nomeCategoria)
            .addOnSuccessListener { perguntas ->
                preencherListaPerguntas(perguntas)
            }
    }

    private fun preencherListaPerguntas(perguntas: List<CategoriaRepository.PerguntaCategoria>) {
        binding.layoutPerguntasPersonalizadas.removeAllViews()
        if (perguntas.isEmpty()) {
            val vazio = TextView(this)
            vazio.text = "Sem perguntas guardadas nesta categoria."
            vazio.setTextColor(0xFF000000.toInt())
            binding.layoutPerguntasPersonalizadas.addView(vazio)
            return
        }

        for (perguntaCategoria in perguntas) {
            val perguntaId = perguntaCategoria.id ?: continue
            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(0, 16, 0, 16)

            val texto = TextView(this)
            texto.text = "${perguntaCategoria.pergunta}\nResposta correta: ${perguntaCategoria.respostaCorreta}"
            texto.setTextColor(0xFF000000.toInt())
            texto.textSize = 16f
            container.addView(texto)

            val botoes = LinearLayout(this)
            botoes.orientation = LinearLayout.HORIZONTAL

            val btnEditar = Button(this)
            btnEditar.text = "Editar"
            btnEditar.setOnClickListener {
                perguntaEmEdicaoId = perguntaId
                preencherFormulario(perguntaCategoria.pergunta, perguntaCategoria.opcoes, perguntaCategoria.respostaCorreta)
            }
            botoes.addView(btnEditar)

            val btnEliminar = Button(this)
            btnEliminar.text = "Eliminar"
            btnEliminar.setOnClickListener {
                val categoria = categoriaSelecionada()
                categoriaRepository.eliminarPerguntaPersonalizada(nomeUtilizador, categoria, perguntaId)
                    .addOnSuccessListener {
                        if (perguntaEmEdicaoId == perguntaId) {
                            perguntaEmEdicaoId = null
                            limparCampos()
                        }
                        carregarPerguntasCategoria(categoria)
                    }
            }
            botoes.addView(btnEliminar)

            container.addView(botoes)
            val separador = View(this)
            separador.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            )
            separador.setBackgroundColor(0x33000000)
            binding.layoutPerguntasPersonalizadas.addView(container)
            binding.layoutPerguntasPersonalizadas.addView(separador)
        }
    }

    private fun preencherFormulario(pergunta: String, opcoes: List<String>, respostaCorreta: String) {
        binding.rgOpcoes.clearCheck()
        binding.edtPergunta.setText(pergunta)
        binding.edtOpcaoA.setText(opcoes.getOrNull(0) ?: "")
        binding.edtOpcaoB.setText(opcoes.getOrNull(1) ?: "")
        binding.edtOpcaoC.setText(opcoes.getOrNull(2) ?: "")
        binding.edtOpcaoD.setText(opcoes.getOrNull(3) ?: "")
        when (respostaCorreta) {
            opcoes.getOrNull(0) -> binding.rbOpcaoA.isChecked = true
            opcoes.getOrNull(1) -> binding.rbOpcaoB.isChecked = true
            opcoes.getOrNull(2) -> binding.rbOpcaoC.isChecked = true
            opcoes.getOrNull(3) -> binding.rbOpcaoD.isChecked = true
        }
    }

    //Limpar os campos
    private fun limparCampos() {
        //Limpar os campos
        binding.edtPergunta.text.clear()
        binding.edtOpcaoA.text.clear()
        binding.edtOpcaoB.text.clear()
        binding.edtOpcaoC.text.clear()
        binding.edtOpcaoD.text.clear()
        binding.rgOpcoes.clearCheck()
    }

    private fun voltarParaCategorias() {
        val intent = Intent(this, EscolherCategoriaActivity::class.java)
        intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
        intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        nomeJogador?.let { intent.putExtra(IntentExtras.NOME_JOGADOR, it) }
        intent.putExtra(IntentExtras.ADMIN, admin)
        startActivity(intent)
        finish()
    }

    private fun categoriaSelecionada(): String {
        return categoriaEmEdicao ?: binding.edtNovaCategoria.text.toString().trim()
    }

}
