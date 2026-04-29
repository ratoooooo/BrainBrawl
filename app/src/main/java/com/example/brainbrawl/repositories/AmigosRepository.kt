package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.Convite
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AmigosRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class ListenerHandle internal constructor(
        private val reference: DatabaseReference,
        private val listener: ValueEventListener
    ) {
        internal fun remover() {
            reference.removeEventListener(listener)
        }
    }

    fun carregarListaAmigos(nomeUtilizador: String): Task<List<String>> {
        return amigosRef(nomeUtilizador).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar amigos.")
            task.result.children.mapNotNull { it.key }
        }
    }

    fun adicionarAmigo(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return database.updateChildren(
            mapOf(
                "${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.AMIGOS}/$nomeAmigo" to true,
                "${FirebasePaths.JOGADORES}/$nomeAmigo/${FirebasePaths.AMIGOS}/$nomeUtilizador" to true
            )
        )
    }

    fun removerAmigo(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return database.updateChildren(
            hashMapOf<String, Any?>(
                "${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.AMIGOS}/$nomeAmigo" to null,
                "${FirebasePaths.JOGADORES}/$nomeAmigo/${FirebasePaths.AMIGOS}/$nomeUtilizador" to null
            )
        )
    }

    fun verificarSeJaSaoAmigos(nomeUtilizador: String, nomeAmigo: String): Task<Boolean> {
        return amigosRef(nomeUtilizador).child(nomeAmigo).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao verificar amizade.")
            task.result.exists()
        }
    }

    fun pesquisarJogadorParaAdicionar(nomeJogador: String): Task<Boolean> {
        return jogadorRef(nomeJogador).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao pesquisar jogador.")
            task.result.exists()
        }
    }

    fun enviarPedidoAmizade(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        val pedido = mapOf(FirebasePaths.ESTADO to GameConstants.ESTADO_PENDENTE)
        return jogadorRef(nomeAmigo).child(FirebasePaths.PEDIDOS_AMIZADE).child(nomeUtilizador).setValue(pedido)
    }

    fun aceitarPedidoAmizade(nomeUtilizador: String, nomeOutro: String): Task<Void> {
        return database.updateChildren(
            hashMapOf<String, Any?>(
                "${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.AMIGOS}/$nomeOutro" to true,
                "${FirebasePaths.JOGADORES}/$nomeOutro/${FirebasePaths.AMIGOS}/$nomeUtilizador" to true,
                "${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.PEDIDOS_AMIZADE}/$nomeOutro" to null,
                "${FirebasePaths.JOGADORES}/$nomeOutro/${FirebasePaths.PEDIDOS_AMIZADE}/$nomeUtilizador" to null
            )
        )
    }

    fun recusarPedidoAmizade(nomeUtilizador: String, nomeOutro: String): Task<Void> {
        return database.updateChildren(
            hashMapOf<String, Any?>(
                "${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.PEDIDOS_AMIZADE}/$nomeOutro" to null,
                "${FirebasePaths.JOGADORES}/$nomeOutro/${FirebasePaths.PEDIDOS_AMIZADE}/$nomeUtilizador" to null
            )
        )
    }

    fun carregarPedidosRecebidos(nomeUtilizador: String): Task<List<String>> {
        return jogadorRef(nomeUtilizador).child(FirebasePaths.PEDIDOS_AMIZADE).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar pedidos.")
            task.result.toPedidosPendentes()
        }
    }

    fun carregarPedidosEnviados(nomeUtilizador: String): Task<List<String>> {
        return database.child(FirebasePaths.JOGADORES).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar pedidos enviados.")
            task.result.children.mapNotNull { jogador ->
                val nomeOutro = jogador.key ?: return@mapNotNull null
                val estado = jogador.child(FirebasePaths.PEDIDOS_AMIZADE)
                    .child(nomeUtilizador)
                    .child(FirebasePaths.ESTADO)
                    .getValue(String::class.java)
                    .orEmpty()
                if (estado == GameConstants.ESTADO_PENDENTE) nomeOutro else null
            }
        }
    }

    fun carregarConvitesRecebidos(
        nomeUtilizador: String,
        nomeCategoriaPadrao: String
    ): Task<List<Convite>> {
        return jogadorRef(nomeUtilizador).child(FirebasePaths.CONVITES_RECEBIDOS).get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar convites.")
            task.result.children.mapNotNull { it.toConviteRecebido(nomeCategoriaPadrao) }
        }
    }

    fun observarAmigos(
        nomeUtilizador: String,
        onAmigosAlterados: (List<String>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = amigosRef(nomeUtilizador)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onAmigosAlterados(snapshot.children.mapNotNull { it.key })
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun observarPedidosRecebidos(
        nomeUtilizador: String,
        onPedidosAlterados: (List<String>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = jogadorRef(nomeUtilizador).child(FirebasePaths.PEDIDOS_AMIZADE)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onPedidosAlterados(snapshot.toPedidosPendentes())
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun observarConvitesRecebidos(
        nomeUtilizador: String,
        nomeCategoriaPadrao: String,
        onConvitesAlterados: (List<Convite>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = jogadorRef(nomeUtilizador).child(FirebasePaths.CONVITES_RECEBIDOS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onConvitesAlterados(snapshot.children.mapNotNull { it.toConviteRecebido(nomeCategoriaPadrao) })
            }

            override fun onCancelled(error: DatabaseError) {
                onErro()
            }
        }
        reference.addValueEventListener(listener)
        return ListenerHandle(reference, listener)
    }

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    fun enviarConvite1x1(
        nomeUtilizador: String,
        nomeAmigo: String,
        codigoSala: String,
        nomeCategoria: String
    ): Task<Void> {
        val conviteData = mapOf(
            FirebasePaths.ESTADO to GameConstants.ESTADO_PENDENTE,
            FirebasePaths.CODIGO_SALA to codigoSala,
            FirebasePaths.NOME_CATEGORIA to nomeCategoria
        )
        return database.updateChildren(
            mapOf(
                "${FirebasePaths.SALA_1X1}/$codigoSala" to mapOf(
                    FirebasePaths.JOGADORES to mapOf(nomeUtilizador to true, nomeAmigo to true),
                    FirebasePaths.ADMIN to nomeUtilizador,
                    FirebasePaths.ESTADO to GameConstants.ESTADO_EM_ESPERA,
                    FirebasePaths.NOME_CATEGORIA to nomeCategoria
                ),
                "${FirebasePaths.JOGADORES}/$nomeAmigo/${FirebasePaths.CONVITES_RECEBIDOS}/$nomeUtilizador" to conviteData,
                "${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.CONVITES_ENVIADOS}/$nomeAmigo" to conviteData
            )
        )
    }

    fun enviarConvite2x2(
        nomeUtilizador: String,
        amigosSelecionados: List<String>,
        codigoSala: String,
        nomeCategoria: String
    ): Task<Void> {
        val jogadores = hashMapOf<String, Any>(nomeUtilizador to true)
        for (amigo in amigosSelecionados) {
            jogadores[amigo] = true
        }

        val conviteData = mapOf(
            FirebasePaths.ESTADO to GameConstants.ESTADO_PENDENTE,
            FirebasePaths.CODIGO_SALA to codigoSala,
            FirebasePaths.MODO to GameConstants.MODO_2X2,
            FirebasePaths.NOME_CATEGORIA to nomeCategoria
        )
        val updates = hashMapOf<String, Any>(
            "${FirebasePaths.SALA_2X2}/$codigoSala" to mapOf(
                FirebasePaths.JOGADORES to jogadores,
                FirebasePaths.ADMIN to nomeUtilizador,
                FirebasePaths.ESTADO to GameConstants.ESTADO_EM_ESPERA,
                FirebasePaths.NOME_CATEGORIA to nomeCategoria
            )
        )
        for (amigo in amigosSelecionados) {
            updates["${FirebasePaths.JOGADORES}/$amigo/${FirebasePaths.CONVITES_RECEBIDOS}/$nomeUtilizador"] = conviteData
            updates["${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.CONVITES_ENVIADOS}/$amigo"] = conviteData
        }
        return database.updateChildren(updates)
    }

    fun aceitarConvite(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return database.updateChildren(
            mapOf(
                "${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.CONVITES_RECEBIDOS}/$nomeAmigo/${FirebasePaths.ESTADO}" to GameConstants.ESTADO_ACEITE,
                "${FirebasePaths.JOGADORES}/$nomeAmigo/${FirebasePaths.CONVITES_ENVIADOS}/$nomeUtilizador/${FirebasePaths.ESTADO}" to GameConstants.ESTADO_ACEITE
            )
        )
    }

    fun recusarConvite(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return removerConvite(nomeUtilizador, nomeAmigo)
    }

    fun removerConvite(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return database.updateChildren(
            hashMapOf<String, Any?>(
                "${FirebasePaths.JOGADORES}/$nomeUtilizador/${FirebasePaths.CONVITES_RECEBIDOS}/$nomeAmigo" to null,
                "${FirebasePaths.JOGADORES}/$nomeAmigo/${FirebasePaths.CONVITES_ENVIADOS}/$nomeUtilizador" to null
            )
        )
    }

    private fun jogadorRef(nomeJogador: String): DatabaseReference {
        return database.child(FirebasePaths.JOGADORES).child(nomeJogador)
    }

    private fun amigosRef(nomeJogador: String): DatabaseReference {
        return jogadorRef(nomeJogador).child(FirebasePaths.AMIGOS)
    }

    private fun DataSnapshot.toPedidosPendentes(): List<String> {
        return children.mapNotNull { pedido ->
            val nomeOutro = pedido.key ?: return@mapNotNull null
            val estado = pedido.child(FirebasePaths.ESTADO).getValue(String::class.java).orEmpty()
            if (estado == GameConstants.ESTADO_PENDENTE) nomeOutro else null
        }
    }

    private fun DataSnapshot.toConviteRecebido(nomeCategoriaPadrao: String): Convite? {
        val nomeAmigo = key ?: return null
        val estado = child(FirebasePaths.ESTADO).getValue(String::class.java).orEmpty()
        if (estado != GameConstants.ESTADO_PENDENTE) return null

        val codigoSala = child(FirebasePaths.CODIGO_SALA).getValue(String::class.java).orEmpty()
        val modo = child(FirebasePaths.MODO).getValue(String::class.java) ?: GameConstants.MODO_1X1
        val nomeCategoria = child(FirebasePaths.NOME_CATEGORIA).getValue(String::class.java) ?: nomeCategoriaPadrao
        return Convite(nomeAmigo, codigoSala, modo, nomeCategoria)
    }
}
