package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.UteisSala.criarSalaPersonalizadaEEntrar
import com.example.brainbrawl.UteisSala.gerarCodigoSala
import com.example.brainbrawl.databinding.ActivityAdicionarPerguntaBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase

class AdicionarPerguntaActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityAdicionarPerguntaBinding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference
    private var nomeUtilizador: String = ""
    private var nomeJogador: String? = null
    private var modoJogo: String = "classico"
    private var admin: Boolean = true
    private var perguntaEmEdicaoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: ""
        nomeJogador = intent.getStringExtra("nomeJogador")
        modoJogo = intent.getStringExtra("modoJogo") ?: "classico"
        admin = intent.getBooleanExtra("admin", true)
        val categoriaInicial = intent.getStringExtra("nomeCategoria")

        if (nomeUtilizador.isBlank()) {
            Toast.makeText(this, "Só jogadores registados podem criar categorias personalizadas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!categoriaInicial.isNullOrBlank()) {
            binding.edtNovaCategoria.setText(categoriaInicial)
            carregarPerguntasCategoria(categoriaInicial)
        }

        // Configurar o botão para enviar a pergunta
        binding.layoutBtnEnviar.setOnClickListener {
            //Guardar os dados dos editTexts
            val nomeCategoria = binding.edtNovaCategoria.text.toString().trim()
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

            // Criar um mapa com os dados da pergunta
            val perguntasData = mapOf(
                "pergunta" to pergunta,
                "respostaCorreta" to respostaCorreta,
                "opcoes" to listOf(opcaoA, opcaoB, opcaoC, opcaoD)
            )

            val perguntasRef = database.child("jogadores").child(nomeUtilizador)
                .child("categoriasPersonalizadas").child(nomeCategoria).child("perguntas")
            val operacao = perguntaEmEdicaoId?.let { perguntasRef.child(it).setValue(perguntasData) }
                ?: perguntasRef.push().setValue(perguntasData)
            database.child("jogadores").child(nomeUtilizador)
                .child("categoriasPersonalizadas").child(nomeCategoria).child("nome").setValue(nomeCategoria)

            operacao
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
            val nomeCategoria = binding.edtNovaCategoria.text.toString().trim()
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
        database.child("jogadores").child(nomeUtilizador)
            .child("categoriasPersonalizadas").child(nomeCategoria).child("perguntas")
            .get().addOnSuccessListener { snapshot ->
                preencherListaPerguntas(snapshot)
            }
    }

    private fun preencherListaPerguntas(snapshot: DataSnapshot) {
        binding.layoutPerguntasPersonalizadas.removeAllViews()
        if (!snapshot.exists()) {
            val vazio = TextView(this)
            vazio.text = "Sem perguntas guardadas nesta categoria."
            vazio.setTextColor(0xFF000000.toInt())
            binding.layoutPerguntasPersonalizadas.addView(vazio)
            return
        }

        for (perguntaSnapshot in snapshot.children) {
            val perguntaId = perguntaSnapshot.key ?: continue
            val pergunta = perguntaSnapshot.child("pergunta").getValue(String::class.java) ?: continue
            val respostaCorreta = perguntaSnapshot.child("respostaCorreta").getValue(String::class.java) ?: ""
            val opcoes = perguntaSnapshot.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }

            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(0, 16, 0, 16)

            val texto = TextView(this)
            texto.text = "$pergunta\nResposta correta: $respostaCorreta"
            texto.setTextColor(0xFF000000.toInt())
            texto.textSize = 16f
            container.addView(texto)

            val botoes = LinearLayout(this)
            botoes.orientation = LinearLayout.HORIZONTAL

            val btnEditar = Button(this)
            btnEditar.text = "Editar"
            btnEditar.setOnClickListener {
                perguntaEmEdicaoId = perguntaId
                preencherFormulario(pergunta, opcoes, respostaCorreta)
            }
            botoes.addView(btnEditar)

            val btnEliminar = Button(this)
            btnEliminar.text = "Eliminar"
            btnEliminar.setOnClickListener {
                val categoria = binding.edtNovaCategoria.text.toString().trim()
                database.child("jogadores").child(nomeUtilizador)
                    .child("categoriasPersonalizadas").child(categoria).child("perguntas")
                    .child(perguntaId).removeValue()
                    .addOnSuccessListener { carregarPerguntasCategoria(categoria) }
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
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
        intent.putExtra("admin", admin)
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        voltarParaCategorias()
    }
}
