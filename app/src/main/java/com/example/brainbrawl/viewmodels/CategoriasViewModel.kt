package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.CategoriaRepository

class CategoriasViewModel(
    private val categoriaRepository: CategoriaRepository = CategoriaRepository()
) : ViewModel() {

    private val _categorias = MutableLiveData<CategoriasUiState>()
    val categorias: LiveData<CategoriasUiState> = _categorias

    private val _evento = MutableLiveData<CategoriasEvent?>()
    val evento: LiveData<CategoriasEvent?> = _evento

    fun carregarCategoriasPersonalizadas(uid: String, nomeUtilizador: String) {
        categoriaRepository.carregarCategoriasPersonalizadas(uid, nomeUtilizador)
            .addOnSuccessListener { categorias ->
                categoriaRepository.carregarCategoriasPublicas()
                    .addOnSuccessListener { publicas ->
                        _categorias.value = CategoriasUiState(categorias, publicas)
                    }
                    .addOnFailureListener {
                        _categorias.value = CategoriasUiState(categorias, emptyList())
                    }
            }
    }

    fun criarCategoriaPersonalizada(uid: String, nomeUtilizador: String, nomeCategoria: String) {
        categoriaRepository.criarCategoriaPersonalizada(uid, nomeUtilizador, nomeCategoria)
            .addOnSuccessListener {
                _evento.value = CategoriasEvent.CategoriaCriada
            }
            .addOnFailureListener { error ->
                _evento.value = CategoriasEvent.Erro(error.message ?: "Erro ao criar categoria.")
            }
    }

    fun eliminarCategoria(uid: String, nomeUtilizador: String, nomeCategoria: String) {
        categoriaRepository.eliminarCategoria(uid, nomeUtilizador, nomeCategoria)
            .addOnSuccessListener {
                _evento.value = CategoriasEvent.CategoriaEliminada
            }
            .addOnFailureListener {
                _evento.value = CategoriasEvent.Erro("Erro ao eliminar categoria.")
            }
    }

    fun publicarCategoria(uid: String, nomeUtilizador: String, nomeJogador: String?, nomeCategoria: String) {
        categoriaRepository.publicarCategoria(uid, nomeUtilizador, nomeJogador, nomeCategoria)
            .addOnSuccessListener {
                _evento.value = CategoriasEvent.CategoriaPublicada
            }
            .addOnFailureListener { error ->
                _evento.value = CategoriasEvent.Erro(error.message ?: "Erro ao publicar categoria.")
            }
    }

    fun removerCategoriaPublica(uid: String, nomeUtilizador: String, nomeCategoria: String) {
        categoriaRepository.removerCategoriaPublica(uid, nomeUtilizador, nomeCategoria)
            .addOnSuccessListener {
                _evento.value = CategoriasEvent.CategoriaPublicaRemovida
            }
            .addOnFailureListener { error ->
                _evento.value = CategoriasEvent.Erro(error.message ?: "Erro ao remover categoria pública.")
            }
    }

    fun consumirEvento() {
        _evento.value = null
    }
}

data class CategoriasUiState(
    val personalizadas: List<CategoriaRepository.CategoriaPersonalizada>,
    val publicas: List<CategoriaRepository.CategoriaPublica>
)

sealed class CategoriasEvent {
    data object CategoriaCriada : CategoriasEvent()
    data object CategoriaEliminada : CategoriasEvent()
    data object CategoriaPublicada : CategoriasEvent()
    data object CategoriaPublicaRemovida : CategoriasEvent()
    data class Erro(val mensagem: String) : CategoriasEvent()
}
