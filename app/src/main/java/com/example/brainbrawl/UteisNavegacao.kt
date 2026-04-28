package com.example.brainbrawl

import android.content.Context
import android.content.Intent

object UteisNavegacao {
    fun adicionarDadosJogador(intent: Intent, nomeUtilizador: String?, nomeJogador: String?) {
        if (!nomeUtilizador.isNullOrBlank()) {
            intent.putExtra("nomeUtilizador", nomeUtilizador)
        }
        if (!nomeJogador.isNullOrBlank()) {
            intent.putExtra("nomeJogador", nomeJogador)
        }
    }

    fun abrirMainActivity(context: Context, nomeUtilizador: String?, nomeJogador: String?) {
        val intent = Intent(context, MainActivity::class.java)
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador)
        context.startActivity(intent)
    }

    fun abrirEntradaSalaActivity(context: Context, nomeUtilizador: String?, nomeJogador: String?) {
        val intent = Intent(context, SalaDeEsperaActivity::class.java)
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador)
        context.startActivity(intent)
    }

    // Função para abrir activity de categoria
    fun abrirEscolherCategoriaActivity(context: Context, modoJogo: String, nomeUtilizador: String?, nomeJogador: String?, admin: Boolean) {
        val intent = Intent(context, EscolherCategoriaActivity::class.java)
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("admin", admin)
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador)
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
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador)
        nomeCategoria.let { intent.putExtra("nomeCategoria", it) }
        modoJogo.let { intent.putExtra("modoJogo", it) }
        pontuacao.let { intent.putExtra("pontuacao", it) }
        pontuacao.let { intent.putExtra("totalPontos", it) }
        numeroPerguntasCertas.let { intent.putExtra("numeroPerguntasCertas", it) }
        totalPerguntascertas.let { intent.putExtra("totalPerguntascertas", it) }
        totalPerguntascertas.let { intent.putExtra("totalRespostasCertas", it) }
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
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador)
        intent.putExtra("nomeCategoria", nomeCategoria)
        intent.putExtra("admin", admin)
        intent.putExtra("modoJogo", modoJogo)
        context.startActivity(intent)
    }
}
