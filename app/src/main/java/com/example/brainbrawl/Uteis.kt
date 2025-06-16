package com.example.brainbrawl

import Pergunta
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest
import kotlin.random.Random

object Uteis {

    // Função utilizada para validar os campos
    fun validarCampos(nome: String, password: String? = null): String? {
        if (nome.isEmpty() || (password != null && password.isEmpty())) {
            return "Preencha todos os campos"
        }

        if (nome.length < 3 || nome.length > 20) {
            return "O nome deve ter entre 3 e 20 caracteres"
        }

        if (!nome.matches(Regex("^[\\p{L}0-9_]+$"))) {
            return "O nome só pode conter letras, números e underscores"
        }

        if (password != null) {
            if (password.length < 8 || password.length > 20) {
                return "A senha deve ter entre 8 e 20 caracteres"
            }
        }

        return null
    }

    // Encriptar senha usando SHA-256
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }


    // Função para abrir activity de categoria
    fun abrirEscolherCategoriaActivity(context: Context, modoJogo: String, nomeUtilizador: String?, nomeJogador: String?, admin: Boolean) {
        val intent = Intent(context, EscolherCategoriaActivity::class.java)
        modoJogo.let { intent.putExtra("modoJogo", it) }
        admin.let { intent.putExtra("admin", it) }
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
        context.startActivity(intent)
    }

    // Função para abrir a sala de espera em modos de grupo
    fun abrirSalaDeEsperaGrupo(
        context: Context,
        codigoSala: String,
        nomeUtilizador: String?,
        nomeJogador: String,
        nomeCategoria: String,
        admin: Boolean,
        modoJogo: String
    ) {
        val intent = Intent(context, SalaDeEsperaGrupoActivity::class.java)
        codigoSala.let { intent.putExtra("codigoSala", it) }
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        nomeJogador.let { intent.putExtra("nomeJogador", it) }
        nomeCategoria.let { intent.putExtra("nomeCategoria", it) }
        admin.let { intent.putExtra("admin", it) }
        modoJogo.let { intent.putExtra("modoJogo", it) }
        context.startActivity(intent)
    }

    // Função para enviar o jogador para a activity de pontuação correta
    fun enviarPontuacaoActivity(
        context: Context,
        codigoSala: String,
        modoJogo: String,
        nomeUtilizador: String,
        pontuacao: Double,
        nomeCategoria: String,
        nomeJogador: String,
        totalPerguntascertas: Int,
        numeroPerguntasCertas: Int,
        totalPerguntas: Int,
        equipa: String? = null
    ) {
        val destino = when (modoJogo) {
            "1x1" -> Pontuacao1x1Activity::class.java
            "2x2" -> Pontuacao2x2Activity::class.java
            else -> throw IllegalArgumentException("Modo de jogo desconhecido")
        }

        val intent = Intent(context, destino)
        codigoSala.let { intent.putExtra("codigoSala", it) }
        nomeUtilizador.let { intent.putExtra("nomeUtilizador", it) }
        nomeJogador.let { intent.putExtra("nomeJogador", it) }
        nomeCategoria.let { intent.putExtra("nomeCategoria", it) }
        modoJogo.let { intent.putExtra("modoJogo", it) }
        pontuacao.let { intent.putExtra("pontuacao", it) }
        numeroPerguntasCertas.let { intent.putExtra("numeroPerguntasCertas", it) }
        totalPerguntascertas.let { intent.putExtra("totalPerguntascertas", it) }
        totalPerguntas.let { intent.putExtra("totalPerguntas", it) }

        if (equipa != null) {
            intent.putExtra("equipa", equipa)
        }
        context.startActivity(intent)
    }

    // Função utilizada para gerar o código da sala
    fun gerarCodigoSala(): String {
        return Random.nextInt(1000, 9999).toString()
    }

    // Funçº utilizada para criar uma sala caótica e ir buscar todas as perguntas de todas as categorias
    fun criarSalaCaoticaEEntrar(context: Context,  nomeUtilizador: String?,  nomeJogador: String, onError: (String) -> Unit = {}) {
        val database = FirebaseDatabase.getInstance().reference
        val codigoSala = gerarCodigoSala()
        database.child("categorias").get().addOnSuccessListener { snapshot ->
            val todasPerguntas = mutableListOf<Map<String, Any>>()
            snapshot.children.forEach { catSnap ->
                val perguntasSnap = catSnap.child("perguntas")
                perguntasSnap.children.forEach { perguntaSnap ->
                    val pergunta = perguntaSnap.child("pergunta").getValue(String::class.java)
                    val respostaCorreta = perguntaSnap.child("respostaCorreta").getValue(String::class.java)
                    val opcoes = perguntaSnap.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
                    if (pergunta != null && respostaCorreta != null && opcoes.size == 4) {
                        todasPerguntas.add(mapOf("pergunta" to pergunta, "respostaCorreta" to respostaCorreta, "opcoes" to opcoes))
                    }
                }
            }
            val perguntasRandom = todasPerguntas.shuffled().take(15)
            val salaData = mapOf(
                "horaCriacao" to System.currentTimeMillis(),
                "admin" to nomeJogador,
                "estado" to "em_espera",
                "modoJogo" to "caotico",
                "jogadores" to mapOf<String, Any>(nomeJogador to mapOf("nome" to nomeJogador, "pontuacao" to 0.0)),
                "categoria" to "Todas as categorias",
                "perguntas" to perguntasRandom
            )
            database.child("salas").child(codigoSala).setValue(salaData).addOnSuccessListener {
                abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, nomeJogador, "Todas as categorias", true, "caotico")
            }.addOnFailureListener { onError(it.message ?: "Erro desconhecido") }
        }.addOnFailureListener { onError(it.message ?: "Erro ao buscar categorias") }
    }

    // Função utilizada para criar a sala com as perguntas de uma categoria específica
    fun criarSalaComCategoriaEEntrar(
        context: Context,
        codigoSala: String,
        nomeUtilizador: String?,
        nomeJogador: String,
        nomeCategoria: String,
        admin: Boolean,
        modoJogo: String,
        onError: (String) -> Unit = {}
    ) {
        val database = FirebaseDatabase.getInstance().reference
        database.child("categorias").child(nomeCategoria).child("perguntas").get().addOnSuccessListener { snapshot ->
            val perguntas = mutableListOf<Map<String, Any>>()
            snapshot.children.forEach { perguntaSnap ->
                val pergunta = perguntaSnap.child("pergunta").getValue(String::class.java)
                val respostaCorreta = perguntaSnap.child("respostaCorreta").getValue(String::class.java)
                val opcoes = perguntaSnap.child("opcoes").children.mapNotNull { it.getValue(String::class.java) }
                if (pergunta != null && respostaCorreta != null && opcoes.size == 4) {
                    perguntas.add(mapOf("pergunta" to pergunta, "respostaCorreta" to respostaCorreta, "opcoes" to opcoes))
                }
            }
            val perguntasRandom = perguntas.shuffled().take(15)
            val salaData = mapOf(
                "horaCriacao" to System.currentTimeMillis(),
                "admin" to nomeJogador,
                "estado" to "em_espera",
                "modoJogo" to modoJogo,
                "jogadores" to mapOf<String, Any>(nomeJogador to mapOf("nome" to nomeJogador, "pontuacao" to 0.0)),
                "categoria" to nomeCategoria,
                "perguntas" to perguntasRandom
            )
            database.child("salas").child(codigoSala).setValue(salaData).addOnSuccessListener {
                abrirSalaDeEsperaGrupo(context, codigoSala, nomeUtilizador, nomeJogador, nomeCategoria, admin, modoJogo)
            }.addOnFailureListener { onError(it.message ?: "Erro desconhecido") }
        }.addOnFailureListener { onError(it.message ?: "Erro ao buscar perguntas") }
    }

    // Função auxiliar para mudar a cor de um botão
    fun definirCorBotao(botao: android.widget.Button, cor: String) {
        botao.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(cor)
        )
    }

    // Função que devolve a lista de opções embaralhada
    fun obterOpcoesAleatorias(pergunta: Pergunta): List<String> {
        val opcoes = pergunta.opcoes.toMutableList()
        opcoes.shuffle()
        return opcoes
    }

    // Função para atualizar a pontuação dos jogadores
    fun atualizarPontuacao(context: Context, tempoRestante: Double, numeroPerguntasCertas: Int, bonus: Int): Int {
        val tempoUsado = (15 - tempoRestante).toInt()
        var pontuacao = (15 - tempoUsado) * 10

        // Bonus por sequência de respostas corretas
        if (numeroPerguntasCertas == 2) {
            pontuacao += bonus
            Toast.makeText(context, "Bónus de sequência! +$bonus pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas == 3) {
            pontuacao += bonus + 25
            Toast.makeText(context, "Bónus de sequência! +${bonus + 25} pontos", Toast.LENGTH_SHORT).show()
        } else if (numeroPerguntasCertas >= 4) {
            pontuacao += bonus + 100
            Toast.makeText(context, "Bónus de sequência! +${bonus + 50} pontos", Toast.LENGTH_SHORT).show()
        }
        return pontuacao
    }

    // Função utiçizada para tocar o somm (certo ou errado)
    fun tocarSom(context: Context, resourceId: Int) {
        val mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
        mediaPlayer?.start()
    }
}