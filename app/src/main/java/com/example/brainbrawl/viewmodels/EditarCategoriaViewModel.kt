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

    fun carregarPerguntasCategoria(nomeUtilizador: String, nomeCategoria: String) {
        if (nomeCategoria.isBlank()) return

        categoriaRepository.carregarPerguntasEditaveis(nomeUtilizador, nomeCategoria)
            .addOnSuccessListener { perguntas ->
                _perguntas.value = perguntas
            }
    }

    fun guardarPergunta(
        nomeUtilizador: String,
        nomeCategoria: String,
        perguntaId: String?,
        pergunta: String,
        opcaoA: String,
        opcaoB: String,
        opcaoC: String,
        opcaoD: String,
        respostaCorreta: String,
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
            nomeUtilizador,
            nomeCategoria,
            perguntaId,
            CategoriaRepository.PerguntaCategoria(
                pergunta = pergunta,
                respostaCorreta = respostaCorreta,
                opcoes = listOf(opcaoA, opcaoB, opcaoC, opcaoD)
            )
        )
            .addOnSuccessListener {
                _evento.value = EditarCategoriaEvent.PerguntaGuardada
                carregarPerguntasCategoria(nomeUtilizador, nomeCategoria)
            }
            .addOnFailureListener { error ->
                _evento.value = EditarCategoriaEvent.ErroGuardar(error.message.orEmpty())
            }
    }

    fun eliminarPergunta(nomeUtilizador: String, nomeCategoria: String, perguntaId: String) {
        categoriaRepository.eliminarPerguntaPersonalizada(nomeUtilizador, nomeCategoria, perguntaId)
            .addOnSuccessListener {
                _evento.value = EditarCategoriaEvent.PerguntaEliminada(perguntaId)
                carregarPerguntasCategoria(nomeUtilizador, nomeCategoria)
            }
    }

    fun criarCategoriaPersonalizada(nomeUtilizador: String, nomeCategoria: String) {
        if (nomeCategoria.isBlank()) return

        categoriaRepository.criarCategoriaPersonalizada(nomeUtilizador, nomeCategoria)
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

        if (listOf(opcaoA, opcaoB, opcaoC, opcaoD).distinct().size != 4) {
            return "As opções devem ser todas diferentes"
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
