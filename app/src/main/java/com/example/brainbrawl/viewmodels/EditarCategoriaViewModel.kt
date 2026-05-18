package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.CategoriaRepository

class EditarCategoriaViewModel(
    private val categoriaRepository: CategoriaRepository = CategoriaRepository()
) : ViewModel() {

    private val _perguntas = MutableLiveData<List<CategoriaRepository.PerguntaCategoria>>()
    val perguntas: LiveData<List<CategoriaRepository.PerguntaCategoria>> = _perguntas

    private val _evento = MutableLiveData<EditarCategoriaEvent?>()
    val evento: LiveData<EditarCategoriaEvent?> = _evento

    fun carregarPerguntasCategoria(uid: String, nomeUtilizador: String, nomeCategoria: String) {
        if (nomeCategoria.isBlank()) return

        categoriaRepository.carregarPerguntasEditaveis(uid, nomeUtilizador, nomeCategoria)
            .addOnSuccessListener { perguntas ->
                _perguntas.value = perguntas
            }
    }

    fun guardarPergunta(
        uid: String,
        nomeUtilizador: String,
        nomeCategoria: String,
        perguntaId: String?,
        pergunta: String,
        opcaoA: String,
        opcaoB: String,
        opcaoC: String,
        opcaoD: String,
        respostaCorreta: String,
        imagem: String,
        dificuldade: String?,
        categoriasReservadas: Set<String>
    ) {
        val validacao = validarPergunta(
            nomeCategoria,
            pergunta,
            opcaoA,
            opcaoB,
            opcaoC,
            opcaoD,
            respostaCorreta,
            categoriasReservadas
        )
        if (validacao != null) {
            _evento.value = EditarCategoriaEvent.ValidacaoFalhou(validacao)
            return
        }

        categoriaRepository.guardarPerguntaPersonalizada(
            uid,
            nomeUtilizador,
            nomeCategoria,
            perguntaId,
            CategoriaRepository.PerguntaCategoria(
                pergunta = pergunta,
                respostaCorreta = respostaCorreta,
                opcoes = listOf(opcaoA, opcaoB, opcaoC, opcaoD),
                imagem = imagem,
                dificuldade = dificuldade
            )
        )
            .addOnSuccessListener {
                _evento.value = EditarCategoriaEvent.PerguntaGuardada
                carregarPerguntasCategoria(uid, nomeUtilizador, nomeCategoria)
            }
            .addOnFailureListener { error ->
                _evento.value = EditarCategoriaEvent.ErroGuardar(error.message.orEmpty())
            }
    }

    fun eliminarPergunta(uid: String, nomeUtilizador: String, nomeCategoria: String, perguntaId: String) {
        categoriaRepository.eliminarPerguntaPersonalizada(uid, nomeUtilizador, nomeCategoria, perguntaId)
            .addOnSuccessListener {
                _evento.value = EditarCategoriaEvent.PerguntaEliminada(perguntaId)
                carregarPerguntasCategoria(uid, nomeUtilizador, nomeCategoria)
            }
    }

    fun criarCategoriaPersonalizada(uid: String, nomeUtilizador: String, nomeCategoria: String) {
        if (nomeCategoria.isBlank()) return

        categoriaRepository.criarCategoriaPersonalizada(uid, nomeUtilizador, nomeCategoria)
            .addOnSuccessListener {
                _evento.value = EditarCategoriaEvent.CategoriaCriada
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }

    private fun validarPergunta(
        nomeCategoria: String,
        pergunta: String,
        opcaoA: String,
        opcaoB: String,
        opcaoC: String,
        opcaoD: String,
        respostaCorreta: String,
        categoriasReservadas: Set<String>
    ): String? {
        if (categoriasReservadas.contains(nomeCategoria)) {
            return "Categoria não permitida"
        }

        if (
            nomeCategoria.length > 50 ||
            pergunta.length > 200 ||
            opcaoA.length > 100 ||
            opcaoB.length > 100 ||
            opcaoC.length > 100 ||
            opcaoD.length > 100
        ) {
            return "Campos excedem o tamanho máximo permitido"
        }

        if (
            nomeCategoria.isEmpty() ||
            pergunta.isEmpty() ||
            opcaoA.isEmpty() ||
            opcaoB.isEmpty() ||
            opcaoC.isEmpty() ||
            opcaoD.isEmpty() ||
            respostaCorreta.isEmpty()
        ) {
            return "Preencha todos os campos"
        }

        val opcoes = listOf(opcaoA, opcaoB, opcaoC, opcaoD)
        if (opcoes.map { it.trim().lowercase() }.distinct().size != 4) {
            return "As opções devem ser todas diferentes"
        }

        if (respostaCorreta !in opcoes) {
            return "Escolhe a resposta correta entre as quatro opções"
        }

        return null
    }
}

sealed class EditarCategoriaEvent {
    data object CategoriaCriada : EditarCategoriaEvent()
    data object PerguntaGuardada : EditarCategoriaEvent()
    data class PerguntaEliminada(val perguntaId: String) : EditarCategoriaEvent()
    data class ValidacaoFalhou(val mensagem: String) : EditarCategoriaEvent()
    data class ErroGuardar(val mensagem: String) : EditarCategoriaEvent()
}
