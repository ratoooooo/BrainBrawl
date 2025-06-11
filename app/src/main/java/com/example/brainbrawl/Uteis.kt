package com.example.brainbrawl

import Pergunta
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.security.MessageDigest
import kotlin.random.Random

object Uteis {

    // Login e Registo
    // Função para validar campos
    fun validarCampos(nome: String, senha: String? = null): String? {
        if (nome.isEmpty() || (senha != null && senha.isEmpty())) {
            return "Preencha todos os campos"
        }

        if (nome.length < 3 || nome.length > 20) {
            return "O nome deve ter entre 3 e 20 caracteres"
        }

        if (!nome.matches(Regex("^[\\p{L}0-9_]+$"))) {
            return "O nome só pode conter letras, números e underscores"
        }

        if (senha != null) {
            if (senha.length < 8 || senha.length > 20) {
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


    // CategoriaActivity
    // Função para abrir a MainActivity com os parâmetros necessários
    fun abrirMainActivity(context: Context, nomeCategoria: String?, modoJogo: String, nomeUtilizador: String?) {
        // Criar intent para MainActivity
        val intent = Intent(context, MainActivity::class.java)
        //Guardar o código da sala
        intent.putExtra("codigoSala", gerarCodigoSala())
        if (nomeCategoria != null)
        {
            intent.putExtra("nomeCategoria", nomeCategoria)
        }
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("modoJogo", modoJogo)
        context.startActivity(intent)
    }

    // ModoActivity
    // Função para abrir activity de categoria
     fun abrirEscolherCategoriaActivity(context: Context, modoJogo: String, nomeUtilizador: String?) {
        val intent = Intent(context, EscolherCategoriaActivity::class.java)
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        context.startActivity(intent)
    }

    //Gerar código da sala
    fun gerarCodigoSala(): String {
        return Random.nextInt(1000, 9999).toString()
    }


    // Jogo Activity
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

    fun enviarPontuacaoActivity(context: Context, modoJogo: String, nomeUtilizador: String, pontuacao: Double, nomeCategoria: String, nomeJogador: String,  numeroPerguntasCertas: Int,totalPerguntas: Int,equipa: String? = null) {
            val destino = when (modoJogo) {
            "1x1" -> Pontuacao1x1Activity::class.java
            "2x2" -> Pontuacao2x2Activity::class.java
            else -> throw IllegalArgumentException("Modo de jogo desconhecido")
        }

        val intent = Intent(context, destino::class.java)
        intent.putExtra("modoJogo", modoJogo)
        intent.putExtra("nomeUtilizador", nomeUtilizador)
        intent.putExtra("totalPontos", pontuacao)
        intent.putExtra("nomeCategoria", nomeCategoria)
        intent.putExtra("nomeJogador", nomeJogador)
        intent.putExtra("respostasCertas", numeroPerguntasCertas)
        intent.putExtra("totalPerguntas", totalPerguntas)
        if (equipa != null) {
            intent.putExtra("equipa", equipa)
        }

        context.startActivity(intent)
    }

    // Função para atualizar a pontuação dos jogadores
    fun atualizarPontuacao(context: Context, tempoRestante: Double, numeroPerguntasCertas: Int, bonus: Int): Int
    {
            val tempoUsado = (15 - tempoRestante).toInt()
            var pontuacao = (15 - tempoUsado) * 10

            // Bonus por sequência de respostas corretas
            if ( numeroPerguntasCertas== 2) {
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
}
