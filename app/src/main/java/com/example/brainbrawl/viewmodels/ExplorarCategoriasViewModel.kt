package com.example.brainbrawl.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.brainbrawl.repositories.CategoriaRepository

class ExplorarCategoriasViewModel(
    private val categoriaRepository: CategoriaRepository = CategoriaRepository()
) : ViewModel() {

    private val _categorias = MutableLiveData<ExplorarCategoriasUiState>()
    val categorias: LiveData<ExplorarCategoriasUiState> = _categorias

    private val _evento = MutableLiveData<ExplorarCategoriasEvent?>()
    val evento: LiveData<ExplorarCategoriasEvent?> = _evento

    private var categoriasListener: CategoriaRepository.ListenerHandle? = null

    fun carregarCategorias() {
        removerListener()
        _categorias.value = ExplorarCategoriasUiState(carregando = true)
        categoriasListener = categoriaRepository.escutarCategoriasPublicas(
            onCategoriasAlteradas = { categorias ->
                _categorias.value = ExplorarCategoriasUiState(categorias = categorias)
            },
            onErro = {
                _categorias.value = ExplorarCategoriasUiState(erro = true)
            }
        )
    }

    fun guardarCategoria(uid: String, nomeUtilizador: String, categoria: CategoriaRepository.CategoriaPublica) {
        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            _evento.value = ExplorarCategoriasEvent.LoginNecessarioGuardar
            return
        }

        categoriaRepository.guardarCopiaCategoriaPublica(uid, nomeUtilizador, categoria.id)
            .addOnSuccessListener {
                _evento.value = ExplorarCategoriasEvent.CategoriaGuardada
            }
            .addOnFailureListener { error ->
                _evento.value = ExplorarCategoriasEvent.Erro(error.message ?: "Erro ao guardar categoria.")
            }
    }

    fun avaliarCategoria(categoriaId: String, uid: String, nomeUtilizador: String, valor: Int) {
        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            _evento.value = ExplorarCategoriasEvent.LoginNecessarioAvaliar
            return
        }

        categoriaRepository.avaliarCategoria(categoriaId, uid, nomeUtilizador, valor)
            .addOnSuccessListener { resultado ->
                _evento.value = when (resultado) {
                    CategoriaRepository.ResultadoAvaliacao.GUARDADA -> ExplorarCategoriasEvent.AvaliacaoGuardada
                    CategoriaRepository.ResultadoAvaliacao.JA_AVALIADA -> ExplorarCategoriasEvent.CategoriaJaAvaliada
                    else -> ExplorarCategoriasEvent.Erro("Erro ao avaliar.")
                }
            }
            .addOnFailureListener {
                _evento.value = ExplorarCategoriasEvent.Erro("Erro ao avaliar.")
            }
    }

    fun removerListener() {
        categoriaRepository.removerListener(categoriasListener)
        categoriasListener = null
    }

    fun consumirEvento() {
        _evento.value = null
    }

    override fun onCleared() {
        removerListener()
        super.onCleared()
    }
}

data class ExplorarCategoriasUiState(
    val categorias: List<CategoriaRepository.CategoriaPublica> = emptyList(),
    val carregando: Boolean = false,
    val erro: Boolean = false
)

sealed class ExplorarCategoriasEvent {
    data object LoginNecessarioGuardar : ExplorarCategoriasEvent()
    data object LoginNecessarioAvaliar : ExplorarCategoriasEvent()
    data object CategoriaGuardada : ExplorarCategoriasEvent()
    data object AvaliacaoGuardada : ExplorarCategoriasEvent()
    data object CategoriaJaAvaliada : ExplorarCategoriasEvent()
    data class Erro(val mensagem: String) : ExplorarCategoriasEvent()
}
