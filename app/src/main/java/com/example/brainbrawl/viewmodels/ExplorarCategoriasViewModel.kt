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
    private var uidAtual: String = ""
    private var nomeUtilizadorAtual: String = ""

    fun carregarCategorias(uid: String = "", nomeUtilizador: String = "") {
        uidAtual = uid
        nomeUtilizadorAtual = nomeUtilizador
        removerListener()
        _categorias.value = ExplorarCategoriasUiState(carregando = true)
        categoriasListener = categoriaRepository.escutarCategoriasPublicas(
            onCategoriasAlteradas = { categorias ->
                carregarMinhasCategorias(categorias)
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

    fun eliminarCategoria(uid: String, nomeUtilizador: String, categoria: CategoriaRepository.CategoriaPersonalizada) {
        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            _evento.value = ExplorarCategoriasEvent.LoginNecessarioGerir
            return
        }

        categoriaRepository.eliminarCategoria(uid, nomeUtilizador, categoria.nome)
            .addOnSuccessListener {
                _evento.value = ExplorarCategoriasEvent.CategoriaEliminada
                carregarMinhasCategorias(_categorias.value?.categoriasPublicas.orEmpty())
            }
            .addOnFailureListener { error ->
                _evento.value = ExplorarCategoriasEvent.Erro(error.message ?: "Erro ao eliminar categoria.")
            }
    }

    fun publicarCategoria(uid: String, nomeUtilizador: String, nomeJogador: String?, nomeCategoria: String) {
        if (uid.isBlank() && nomeUtilizador.isBlank()) {
            _evento.value = ExplorarCategoriasEvent.LoginNecessarioGerir
            return
        }

        categoriaRepository.publicarCategoria(uid, nomeUtilizador, nomeJogador, nomeCategoria)
            .addOnSuccessListener {
                _evento.value = ExplorarCategoriasEvent.CategoriaPublicada
                carregarMinhasCategorias(_categorias.value?.categoriasPublicas.orEmpty())
            }
            .addOnFailureListener { error ->
                _evento.value = ExplorarCategoriasEvent.Erro(error.message ?: "Erro ao publicar categoria.")
            }
    }

    fun removerCategoriaPublica(uid: String, nomeUtilizador: String, nomeCategoria: String) {
        categoriaRepository.removerCategoriaPublica(uid, nomeUtilizador, nomeCategoria)
            .addOnSuccessListener {
                _evento.value = ExplorarCategoriasEvent.CategoriaPublicaRemovida
                carregarMinhasCategorias(_categorias.value?.categoriasPublicas.orEmpty())
            }
            .addOnFailureListener { error ->
                _evento.value = ExplorarCategoriasEvent.Erro(error.message ?: "Erro ao remover categoria pública.")
            }
    }

    private fun carregarMinhasCategorias(publicas: List<CategoriaRepository.CategoriaPublica>) {
        if (uidAtual.isBlank() && nomeUtilizadorAtual.isBlank()) {
            _categorias.value = ExplorarCategoriasUiState(categoriasPublicas = publicas)
            return
        }

        categoriaRepository.carregarCategoriasPersonalizadas(uidAtual, nomeUtilizadorAtual)
            .addOnSuccessListener { minhas ->
                _categorias.value = ExplorarCategoriasUiState(categoriasPublicas = publicas, minhasCategorias = minhas)
            }
            .addOnFailureListener {
                _categorias.value = ExplorarCategoriasUiState(categoriasPublicas = publicas)
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
    val categoriasPublicas: List<CategoriaRepository.CategoriaPublica> = emptyList(),
    val minhasCategorias: List<CategoriaRepository.CategoriaPersonalizada> = emptyList(),
    val carregando: Boolean = false,
    val erro: Boolean = false
)

sealed class ExplorarCategoriasEvent {
    data object LoginNecessarioGuardar : ExplorarCategoriasEvent()
    data object LoginNecessarioAvaliar : ExplorarCategoriasEvent()
    data object LoginNecessarioGerir : ExplorarCategoriasEvent()
    data object CategoriaGuardada : ExplorarCategoriasEvent()
    data object CategoriaEliminada : ExplorarCategoriasEvent()
    data object CategoriaPublicada : ExplorarCategoriasEvent()
    data object CategoriaPublicaRemovida : ExplorarCategoriasEvent()
    data object AvaliacaoGuardada : ExplorarCategoriasEvent()
    data object CategoriaJaAvaliada : ExplorarCategoriasEvent()
    data class Erro(val mensagem: String) : ExplorarCategoriasEvent()
}
