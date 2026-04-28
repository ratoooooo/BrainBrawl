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
import com.example.brainbrawl.UteisSala.criarSalaComCategoriaEEntrar
import com.example.brainbrawl.UteisSala.criarSalaPersonalizadaEEntrar
import com.example.brainbrawl.UteisSala.gerarCodigoSala
import com.example.brainbrawl.databinding.ActivityEscolherCategoriaBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class EscolherCategoriaActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEscolherCategoriaBinding.inflate(layoutInflater)
    }
    private lateinit var codigoSala: String
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pela Intent
        val modoJogo = intent.getStringExtra("modoJogo")
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador")
        val nomeJogador = intent.getStringExtra("nomeJogador")
        val admin = intent.getBooleanExtra("admin", false)

        if (modoJogo == null) {
            finish()
            return
        }

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
                this, codigoSala, nomeUtilizador, nomeJogador, categoriaEscolhida, admin, modoJogo
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
            if (nomeUtilizador.isNullOrBlank()) {
                Toast.makeText(this, "Inicia sessão para criar categorias personalizadas.", Toast.LENGTH_SHORT).show()
            } else {
                mostrarCategoriasPersonalizadas(modoJogo, nomeUtilizador, nomeJogador, admin)
            }
        }
        binding.btnVoltar.setOnClickListener {
            val intent = Intent(this, EscolherModoActivity::class.java)
            nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
            nomeJogador?.let { intent.putExtra("nomeJogador", it) }
            admin.let { intent.putExtra("admin", it) }
            startActivity(intent)
            finish()
        }
    }

    private fun mostrarCategoriasPersonalizadas(
        modo: String,
        nomeUtilizador: String,
        nomeJogador: String?,
        admin: Boolean
    ) {
        database.child("jogadores").child(nomeUtilizador).child("categoriasPersonalizadas")
            .get().addOnSuccessListener { snapshot ->
                database.child("categoriasPublicas").get().addOnSuccessListener { publicasSnapshot ->
                    mostrarDialogCategoriasPersonalizadas(
                        snapshot,
                        publicasSnapshot,
                        modo,
                        nomeUtilizador,
                        nomeJogador,
                        admin
                    )
                }.addOnFailureListener {
                    mostrarDialogCategoriasPersonalizadas(
                        snapshot,
                        null,
                        modo,
                        nomeUtilizador,
                        nomeJogador,
                        admin
                    )
                }
            }
    }

    private fun mostrarDialogCategoriasPersonalizadas(
        snapshot: DataSnapshot,
        publicasSnapshot: DataSnapshot?,
        modo: String,
        nomeUtilizador: String,
        nomeJogador: String?,
        admin: Boolean
    ) {
                val categorias = snapshot.children.mapNotNull { it.key }.sorted()
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
                        abrirAdicionarPerguntaActivity(modo, nomeUtilizador, nomeJogador, admin, null)
                    }
                }
                lista.addView(btnCriar)

                if (categorias.isEmpty()) {
                    lista.addView(TextView(this).apply {
                        text = "Ainda não tens categorias personalizadas."
                        textSize = 16f
                        setPadding(0, 16, 0, 8)
                    })
                }

                categorias.forEach { categoria ->
                    val categoriaPublicaId = categoriaPublicaId(nomeUtilizador, categoria)
                    val jaPublica = snapshot.child(categoria).child("categoriaPublicaId").exists() ||
                        publicasSnapshot?.child(categoriaPublicaId)?.exists() == true
                    val container = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(0, 20, 0, 12)
                    }

                    container.addView(TextView(this).apply {
                        text = categoria
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
                            nomeUtilizador,
                            categoria,
                            true,
                            modo
                        ) { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                    })

                    botoes.addView(criarBotaoCategoria("Editar") {
                        dialog.dismiss()
                        abrirAdicionarPerguntaActivity(modo, nomeUtilizador, nomeJogador, admin, categoria)
                    })

                    botoes.addView(criarBotaoCategoria("Eliminar") {
                        confirmarEliminarCategoria(categoria, modo, nomeUtilizador, nomeJogador, admin, dialog)
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
                        publicarCategoria(categoria, nomeUtilizador, nomeJogador) {
                            dialog.dismiss()
                            mostrarCategoriasPersonalizadas(modo, nomeUtilizador, nomeJogador, admin)
                        }
                    })
                    if (jaPublica) {
                        botoesPublicos.addView(criarBotaoCategoria("Remover pública") {
                            removerCategoriaPublica(categoria, nomeUtilizador) {
                                dialog.dismiss()
                                mostrarCategoriasPersonalizadas(modo, nomeUtilizador, nomeJogador, admin)
                            }
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
        categoria: String,
        modo: String,
        nomeUtilizador: String,
        nomeJogador: String?,
        admin: Boolean,
        dialogLista: AlertDialog
    ) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar categoria")
            .setMessage("Queres eliminar \"$categoria\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                database.child("jogadores").child(nomeUtilizador)
                    .child("categoriasPersonalizadas").child(categoria)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Categoria eliminada.", Toast.LENGTH_SHORT).show()
                        dialogLista.dismiss()
                        mostrarCategoriasPersonalizadas(modo, nomeUtilizador, nomeJogador, admin)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao eliminar categoria.", Toast.LENGTH_SHORT).show()
                    }
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
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
        categoriaInicial?.let { intent.putExtra("nomeCategoria", it) }
        codigoSala.let { intent.putExtra("codigoSala", it) }
        modo.let { intent.putExtra("modoJogo", it) }
        intent.putExtra("admin", admin)
        startActivity(intent)
        finish()
    }

    private fun publicarCategoria(
        categoria: String,
        nomeUtilizador: String,
        nomeJogador: String?,
        onComplete: () -> Unit
    ) {
        val categoriaRef = database.child("jogadores").child(nomeUtilizador)
            .child("categoriasPersonalizadas").child(categoria)
        categoriaRef.get().addOnSuccessListener { snapshot ->
            val perguntasValidas = perguntasValidas(snapshot.child("perguntas"))
            if (perguntasValidas.isEmpty()) {
                Toast.makeText(this, "A categoria precisa de perguntas válidas para ser pública.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val categoriaPublicaId = snapshot.child("categoriaPublicaId").getValue(String::class.java)
                ?: categoriaPublicaId(nomeUtilizador, categoria)
            val publicaRef = database.child("categoriasPublicas").child(categoriaPublicaId)
            publicaRef.get().addOnSuccessListener publicaListener@ { publicaSnapshot ->
                val criadorExistente = publicaSnapshot.child("criadorId").getValue(String::class.java)
                if (publicaSnapshot.exists() && criadorExistente != nomeUtilizador) {
                    Toast.makeText(this, "Só o criador pode atualizar esta categoria pública.", Toast.LENGTH_SHORT).show()
                    return@publicaListener
                }
                val agora = System.currentTimeMillis()
                val dadosPublicos = hashMapOf<String, Any>(
                    "id" to categoriaPublicaId,
                    "nome" to categoria,
                    "descricao" to (snapshot.child("descricao").getValue(String::class.java) ?: ""),
                    "criador" to (nomeJogador ?: nomeUtilizador),
                    "criadorId" to nomeUtilizador,
                    "nomeUtilizador" to nomeUtilizador,
                    "perguntas" to perguntasValidas,
                    "usos" to (publicaSnapshot.child("usos").getValue(Int::class.java) ?: 0),
                    "ratingMedio" to (publicaSnapshot.child("ratingMedio").getValue(Double::class.java) ?: 0.0),
                    "totalAvaliacoes" to (publicaSnapshot.child("totalAvaliacoes").getValue(Int::class.java) ?: 0),
                    "dataCriacao" to (snapshot.child("dataCriacao").getValue(Long::class.java) ?: agora),
                    "dataPublicacao" to (publicaSnapshot.child("dataPublicacao").getValue(Long::class.java) ?: agora)
                )
                publicaRef.updateChildren(dadosPublicos).addOnSuccessListener {
                    categoriaRef.updateChildren(
                        mapOf(
                            "categoriaPublicaId" to categoriaPublicaId,
                            "estadoPublicacao" to "publica",
                            "dataPublicacao" to ServerValue.TIMESTAMP
                        )
                    )
                    Toast.makeText(this, "Categoria pública guardada.", Toast.LENGTH_SHORT).show()
                    onComplete()
                }.addOnFailureListener {
                    Toast.makeText(this, "Erro ao publicar categoria.", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao ler categoria.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removerCategoriaPublica(categoria: String, nomeUtilizador: String, onComplete: () -> Unit) {
        val categoriaRef = database.child("jogadores").child(nomeUtilizador)
            .child("categoriasPersonalizadas").child(categoria)
        val categoriaPublicaId = categoriaPublicaId(nomeUtilizador, categoria)
        database.child("categoriasPublicas").child(categoriaPublicaId).get().addOnSuccessListener { snapshot ->
            val criadorId = snapshot.child("criadorId").getValue(String::class.java)
            if (snapshot.exists() && criadorId != nomeUtilizador) {
                Toast.makeText(this, "Só o criador pode remover esta categoria pública.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            database.child("categoriasPublicas").child(categoriaPublicaId).removeValue().addOnSuccessListener {
                categoriaRef.child("categoriaPublicaId").removeValue()
                categoriaRef.child("estadoPublicacao").setValue("privada")
                Toast.makeText(this, "Categoria pública removida.", Toast.LENGTH_SHORT).show()
                onComplete()
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao remover categoria pública.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun perguntasValidas(perguntasSnapshot: DataSnapshot): List<Map<String, Any>> {
        return perguntasSnapshot.children.mapNotNull { perguntaSnap ->
            val pergunta = perguntaSnap.child("pergunta").getValue(String::class.java)
            val respostaCorreta = perguntaSnap.child("respostaCorreta").getValue(String::class.java)
            val opcoes = perguntaSnap.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
            if (!pergunta.isNullOrBlank() && !respostaCorreta.isNullOrBlank() && opcoes.size == 4) {
                mapOf(
                    "pergunta" to pergunta,
                    "respostaCorreta" to respostaCorreta,
                    "opcoes" to opcoes
                )
            } else {
                null
            }
        }
    }

    private fun categoriaPublicaId(nomeUtilizador: String, categoria: String): String {
        val bruto = "${nomeUtilizador}_${categoria}".lowercase()
        return bruto.replace(Regex("[.#$\\[\\]/]"), "_").replace(Regex("\\s+"), "_")
    }
}
