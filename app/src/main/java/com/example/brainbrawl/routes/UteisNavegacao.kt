package com.example.brainbrawl.routes

import android.content.Context
import android.content.Intent
import com.example.brainbrawl.EscolherCategoriaActivity
import com.example.brainbrawl.MainActivity
import com.example.brainbrawl.Pontuacao1x1Activity
import com.example.brainbrawl.Pontuacao2x2Activity
import com.example.brainbrawl.SalaDeEsperaActivity
import com.example.brainbrawl.SalaDeEsperaGrupoActivity
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.services.AuthService

object UteisNavegacao {
    private val authService = AuthService()

    fun adicionarDadosJogador(intent: Intent, nomeUtilizador: String?, nomeJogador: String?, uid: String? = null) {
        val uidEfetivo = uid?.takeIf { it.isNotBlank() } ?: authService.utilizadorAtual()?.uid
        if (!uidEfetivo.isNullOrBlank()) {
            intent.putExtra(IntentExtras.UID, uidEfetivo)
        }
        if (!nomeUtilizador.isNullOrBlank()) {
            intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
        }
        if (!nomeJogador.isNullOrBlank()) {
            intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
        }
    }

    fun abrirMainActivity(context: Context, nomeUtilizador: String?, nomeJogador: String?, uid: String? = null) {
        val intent = Intent(context, MainActivity::class.java)
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador, uid)
        context.startActivity(intent)
    }

    fun abrirEntradaSalaActivity(context: Context, nomeUtilizador: String?, nomeJogador: String?, uid: String? = null) {
        val intent = Intent(context, SalaDeEsperaActivity::class.java)
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador, uid)
        context.startActivity(intent)
    }

    // Função para abrir activity de categoria
    fun abrirEscolherCategoriaActivity(context: Context, modoJogo: String, nomeUtilizador: String?, nomeJogador: String?, admin: Boolean, uid: String? = null) {
        val intent = Intent(context, EscolherCategoriaActivity::class.java)
        intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
        intent.putExtra(IntentExtras.ADMIN, admin)
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador, uid)
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
        equipa: String? = null,
        uid: String? = null
    ) {
        val destino = when (modoJogo) {
            GameConstants.MODO_1X1 -> Pontuacao1x1Activity::class.java
            GameConstants.MODO_2X2 -> Pontuacao2x2Activity::class.java
            else -> throw IllegalArgumentException("Modo de jogo desconhecido")
        }

        val intent = Intent(context, destino)
        codigoSala.let { intent.putExtra(IntentExtras.CODIGO_SALA, it) }
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador, uid)
        nomeCategoria.let { intent.putExtra(IntentExtras.NOME_CATEGORIA, it) }
        modoJogo.let { intent.putExtra(IntentExtras.MODO_JOGO, it) }
        pontuacao.let { intent.putExtra(IntentExtras.PONTUACAO, it) }
        pontuacao.let { intent.putExtra(IntentExtras.TOTAL_PONTOS, it) }
        numeroPerguntasCertas.let { intent.putExtra(IntentExtras.NUMERO_PERGUNTAS_CERTAS, it) }
        totalPerguntascertas.let { intent.putExtra(IntentExtras.TOTAL_PERGUNTAS_CERTAS_LEGACY, it) }
        totalPerguntascertas.let { intent.putExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, it) }
        totalPerguntas.let { intent.putExtra(IntentExtras.TOTAL_PERGUNTAS, it) }

        if (equipa != null) {
            intent.putExtra(IntentExtras.EQUIPA, equipa)
        }
        context.startActivity(intent)
    }

    // Função para abrir a sala de espera em modos de grupo
    fun abrirSalaDeEsperaGrupo(context: Context, codigoSala: String, nomeUtilizador: String?, nomeJogador: String?, nomeCategoria: String, admin: Boolean, modoJogo: String, uid: String? = null) {
        val intent = Intent(context, SalaDeEsperaGrupoActivity::class.java)
        intent.putExtra(IntentExtras.CODIGO_SALA, codigoSala)
        adicionarDadosJogador(intent, nomeUtilizador, nomeJogador, uid)
        intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
        intent.putExtra(IntentExtras.ADMIN, admin)
        intent.putExtra(IntentExtras.MODO_JOGO, modoJogo)
        context.startActivity(intent)
    }
}
