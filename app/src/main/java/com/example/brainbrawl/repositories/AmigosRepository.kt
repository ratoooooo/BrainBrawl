package com.example.brainbrawl.repositories

import com.example.brainbrawl.Convite1x1
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
                "jogadores/$nomeUtilizador/amigos/$nomeAmigo" to true,
                "jogadores/$nomeAmigo/amigos/$nomeUtilizador" to true
            )
        )
    }

    fun removerAmigo(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return database.updateChildren(
            hashMapOf<String, Any?>(
                "jogadores/$nomeUtilizador/amigos/$nomeAmigo" to null,
                "jogadores/$nomeAmigo/amigos/$nomeUtilizador" to null
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
        val pedido = mapOf("estado" to "pendente")
        return jogadorRef(nomeAmigo).child("pedidos_amizade").child(nomeUtilizador).setValue(pedido)
    }

    fun aceitarPedidoAmizade(nomeUtilizador: String, nomeOutro: String): Task<Void> {
        return database.updateChildren(
            hashMapOf<String, Any?>(
                "jogadores/$nomeUtilizador/amigos/$nomeOutro" to true,
                "jogadores/$nomeOutro/amigos/$nomeUtilizador" to true,
                "jogadores/$nomeUtilizador/pedidos_amizade/$nomeOutro" to null,
                "jogadores/$nomeOutro/pedidos_amizade/$nomeUtilizador" to null
            )
        )
    }

    fun recusarPedidoAmizade(nomeUtilizador: String, nomeOutro: String): Task<Void> {
        return database.updateChildren(
            hashMapOf<String, Any?>(
                "jogadores/$nomeUtilizador/pedidos_amizade/$nomeOutro" to null,
                "jogadores/$nomeOutro/pedidos_amizade/$nomeUtilizador" to null
            )
        )
    }

    fun carregarPedidosRecebidos(nomeUtilizador: String): Task<List<String>> {
        return jogadorRef(nomeUtilizador).child("pedidos_amizade").get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar pedidos.")
            task.result.toPedidosPendentes()
        }
    }

    fun carregarPedidosEnviados(nomeUtilizador: String): Task<List<String>> {
        return database.child("jogadores").get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("Erro ao carregar pedidos enviados.")
            task.result.children.mapNotNull { jogador ->
                val nomeOutro = jogador.key ?: return@mapNotNull null
                val estado = jogador.child("pedidos_amizade")
                    .child(nomeUtilizador)
                    .child("estado")
                    .getValue(String::class.java)
                    .orEmpty()
                if (estado == "pendente") nomeOutro else null
            }
        }
    }

    fun carregarConvitesRecebidos(
        nomeUtilizador: String,
        nomeCategoriaPadrao: String
    ): Task<List<Convite1x1>> {
        return jogadorRef(nomeUtilizador).child("convites_recebidos").get().continueWith { task ->
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
        val reference = jogadorRef(nomeUtilizador).child("pedidos_amizade")
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
        onConvitesAlterados: (List<Convite1x1>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val reference = jogadorRef(nomeUtilizador).child("convites_recebidos")
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
            "estado" to "pendente",
            "codigoSala" to codigoSala,
            "nomeCategoria" to nomeCategoria
        )
        return database.updateChildren(
            mapOf(
                "sala_1x1/$codigoSala" to mapOf(
                    "jogadores" to mapOf(nomeUtilizador to true, nomeAmigo to true),
                    "admin" to nomeUtilizador,
                    "estado" to "em_espera",
                    "nomeCategoria" to nomeCategoria
                ),
                "jogadores/$nomeAmigo/convites_recebidos/$nomeUtilizador" to conviteData,
                "jogadores/$nomeUtilizador/convites_enviados/$nomeAmigo" to conviteData
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
            "estado" to "pendente",
            "codigoSala" to codigoSala,
            "modo" to "2x2",
            "nomeCategoria" to nomeCategoria
        )
        val updates = hashMapOf<String, Any>(
            "sala_2x2/$codigoSala" to mapOf(
                "jogadores" to jogadores,
                "admin" to nomeUtilizador,
                "estado" to "em_espera",
                "nomeCategoria" to nomeCategoria
            )
        )
        for (amigo in amigosSelecionados) {
            updates["jogadores/$amigo/convites_recebidos/$nomeUtilizador"] = conviteData
            updates["jogadores/$nomeUtilizador/convites_enviados/$amigo"] = conviteData
        }
        return database.updateChildren(updates)
    }

    fun aceitarConvite(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return database.updateChildren(
            mapOf(
                "jogadores/$nomeUtilizador/convites_recebidos/$nomeAmigo/estado" to "aceite",
                "jogadores/$nomeAmigo/convites_enviados/$nomeUtilizador/estado" to "aceite"
            )
        )
    }

    fun recusarConvite(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return removerConvite(nomeUtilizador, nomeAmigo)
    }

    fun removerConvite(nomeUtilizador: String, nomeAmigo: String): Task<Void> {
        return database.updateChildren(
            hashMapOf<String, Any?>(
                "jogadores/$nomeUtilizador/convites_recebidos/$nomeAmigo" to null,
                "jogadores/$nomeAmigo/convites_enviados/$nomeUtilizador" to null
            )
        )
    }

    private fun jogadorRef(nomeJogador: String): DatabaseReference {
        return database.child("jogadores").child(nomeJogador)
    }

    private fun amigosRef(nomeJogador: String): DatabaseReference {
        return jogadorRef(nomeJogador).child("amigos")
    }

    private fun DataSnapshot.toPedidosPendentes(): List<String> {
        return children.mapNotNull { pedido ->
            val nomeOutro = pedido.key ?: return@mapNotNull null
            val estado = pedido.child("estado").getValue(String::class.java).orEmpty()
            if (estado == "pendente") nomeOutro else null
        }
    }

    private fun DataSnapshot.toConviteRecebido(nomeCategoriaPadrao: String): Convite1x1? {
        val nomeAmigo = key ?: return null
        val estado = child("estado").getValue(String::class.java).orEmpty()
        if (estado != "pendente") return null

        val codigoSala = child("codigoSala").getValue(String::class.java).orEmpty()
        val modo = child("modo").getValue(String::class.java) ?: "1x1"
        val nomeCategoria = child("nomeCategoria").getValue(String::class.java) ?: nomeCategoriaPadrao
        return Convite1x1(nomeAmigo, codigoSala, modo, nomeCategoria)
    }
}
