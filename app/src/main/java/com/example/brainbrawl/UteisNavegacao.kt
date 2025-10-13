package com.example.brainbrawl

import android.content.Context
import android.content.Intent

object UteisNavegacao {
    // Função para abrir activity de categoria
    fun abrirEscolherCategoriaActivity(context: Context, modoJogo: String, nomeUtilizador: String?, nomeJogador: String?, admin: Boolean) {
        val intent = Intent(context, EscolherCategoriaActivity::class.java)
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("admin", admin)
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
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

    // Função para abrir a sala de espera em modos de grupo
    fun abrirSalaDeEsperaGrupo(context: Context, codigoSala: String, nomeUtilizador: String?, nomeJogador: String?, nomeCategoria: String, admin: Boolean, modoJogo: String) {
        val intent = Intent(context, SalaDeEsperaGrupoActivity::class.java)
        intent.putExtra("codigoSala", codigoSala)
        nomeUtilizador?.let { intent.putExtra("nomeUtilizador", it) }
        nomeJogador?.let { intent.putExtra("nomeJogador", it) }
        intent.putExtra("nomeCategoria", nomeCategoria)
        intent.putExtra("admin", admin)
        intent.putExtra("modoJogo", modoJogo)
        context.startActivity(intent)
    }
}