package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.models.Convite
import com.example.brainbrawl.models.PedidoAmizade
import com.example.brainbrawl.models.UtilizadorSocial
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AmigosRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    data class ListenerHandle internal constructor(
        private val listeners: List<ListenerRegistado>
    ) {
        internal fun remover() {
            listeners.forEach { it.reference.removeEventListener(it.listener) }
        }
    }

    data class ListenerRegistado internal constructor(
        val reference: DatabaseReference,
        val listener: ValueEventListener
    )

    private data class ConvitePendente(
        val chaveDono: String,
        val chaveRemetente: String,
        val codigoSala: String,
        val modo: String,
        val nomeCategoria: String
    )

    fun resolverUtilizador(identificador: String, nomeFallback: String = ""): Task<UtilizadorSocial?> {
        val result = TaskCompletionSource<UtilizadorSocial?>()
        val identificadorLimpo = identificador.trim()
        val fallbackLimpo = nomeFallback.trim()
        val pesquisa = identificadorLimpo.ifBlank { fallbackLimpo }

        if (pesquisa.isBlank()) {
            result.setResult(null)
            return result.task
        }

        jogadorRef(pesquisa).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists() && snapshot.isPerfilJogador()) {
                    result.setResult(snapshot.toUtilizadorSocial(pesquisa))
                } else {
                    procurarUtilizadorPorNome(pesquisa, fallbackLimpo, result)
                }
            }
            .addOnFailureListener { exception ->
                result.setException(exception)
            }

        return result.task
    }

    fun carregarListaAmigos(utilizador: UtilizadorSocial): Task<List<UtilizadorSocial>> {
        val result = TaskCompletionSource<List<UtilizadorSocial>>()
        carregarChavesSociais(utilizador, FirebasePaths.AMIGOS)
            .addOnSuccessListener { chaves ->
                resolverUtilizadores(chaves.map { it.second })
                    .addOnSuccessListener { amigos ->
                        result.setResult(amigos.filterNot { it.corresponde(utilizador) })
                    }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun adicionarAmigo(utilizador: UtilizadorSocial, amigo: UtilizadorSocial): Task<Void> {
        val updates = hashMapOf<String, Any?>()
        updates["${FirebasePaths.JOGADORES}/${utilizador.chavePerfil}/${FirebasePaths.AMIGOS}/${amigo.chavePrimaria}"] = true
        updates["${FirebasePaths.JOGADORES}/${amigo.chavePerfil}/${FirebasePaths.AMIGOS}/${utilizador.chavePrimaria}"] = true
        return database.updateChildren(updates)
    }

    fun removerAmigo(utilizador: UtilizadorSocial, amigo: UtilizadorSocial): Task<Void> {
        val updates = hashMapOf<String, Any?>()
        utilizador.chavesDonoSocial().forEach { chaveDono ->
            amigo.chavesCompatibilidade.forEach { chaveAmigo ->
                updates["${FirebasePaths.JOGADORES}/$chaveDono/${FirebasePaths.AMIGOS}/$chaveAmigo"] = null
            }
        }
        amigo.chavesDonoSocial().forEach { chaveDono ->
            utilizador.chavesCompatibilidade.forEach { chaveUtilizador ->
                updates["${FirebasePaths.JOGADORES}/$chaveDono/${FirebasePaths.AMIGOS}/$chaveUtilizador"] = null
            }
        }
        return database.updateChildren(updates)
    }

    fun verificarSeJaSaoAmigos(utilizador: UtilizadorSocial, amigo: UtilizadorSocial): Task<Boolean> {
        val result = TaskCompletionSource<Boolean>()
        carregarListaAmigos(utilizador)
            .addOnSuccessListener { amigos ->
                result.setResult(amigos.any { it.corresponde(amigo) })
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun pesquisarJogadorParaAdicionar(nomeJogador: String): Task<UtilizadorSocial?> {
        return resolverUtilizador(nomeJogador, nomeJogador)
    }

    fun enviarPedidoAmizade(utilizador: UtilizadorSocial, amigo: UtilizadorSocial): Task<Void> {
        val pedido = mapOf(FirebasePaths.ESTADO to GameConstants.ESTADO_PENDENTE)
        return jogadorRef(amigo.chavePerfil)
            .child(FirebasePaths.PEDIDOS_AMIZADE)
            .child(utilizador.chavePrimaria)
            .setValue(pedido)
    }

    fun aceitarPedidoAmizade(utilizador: UtilizadorSocial, pedido: PedidoAmizade): Task<Void> {
        val outro = pedido.utilizador
        val updates = hashMapOf<String, Any?>()
        val donosParaLimparPedido = (listOf(utilizador.chavePerfil) + pedido.chaveDono)
            .filter { it.isNotBlank() }
            .distinct()

        updates["${FirebasePaths.JOGADORES}/${utilizador.chavePerfil}/${FirebasePaths.AMIGOS}/${outro.chavePrimaria}"] = true

        donosParaLimparPedido.forEach { chaveDono ->
            outro.chavesCompatibilidade.forEach { chaveOutro ->
                updates["${FirebasePaths.JOGADORES}/$chaveDono/${FirebasePaths.PEDIDOS_AMIZADE}/$chaveOutro"] = null
            }
        }

        updates["${FirebasePaths.JOGADORES}/${outro.chavePerfil}/${FirebasePaths.AMIGOS}/${utilizador.chavePrimaria}"] = true
        utilizador.chavesCompatibilidade.forEach { chaveUtilizador ->
            updates["${FirebasePaths.JOGADORES}/${outro.chavePerfil}/${FirebasePaths.PEDIDOS_AMIZADE}/$chaveUtilizador"] = null
        }

        return database.updateChildren(updates)
    }

    fun recusarPedidoAmizade(utilizador: UtilizadorSocial, pedido: PedidoAmizade): Task<Void> {
        val outro = pedido.utilizador
        val updates = hashMapOf<String, Any?>()
        val donosUtilizador = (utilizador.chavesDonoSocial() + pedido.chaveDono)
            .filter { it.isNotBlank() }
            .distinct()

        donosUtilizador.forEach { chaveDono ->
            outro.chavesCompatibilidade.forEach { chaveOutro ->
                updates["${FirebasePaths.JOGADORES}/$chaveDono/${FirebasePaths.PEDIDOS_AMIZADE}/$chaveOutro"] = null
            }
        }
        utilizador.chavesCompatibilidade.forEach { chaveUtilizador ->
            updates["${FirebasePaths.JOGADORES}/${outro.chavePerfil}/${FirebasePaths.PEDIDOS_AMIZADE}/$chaveUtilizador"] = null
        }

        return database.updateChildren(updates)
    }

    fun carregarPedidosRecebidos(utilizador: UtilizadorSocial): Task<List<PedidoAmizade>> {
        val result = TaskCompletionSource<List<PedidoAmizade>>()
        carregarChavesSociais(utilizador, FirebasePaths.PEDIDOS_AMIZADE) { it.toPedidosPendentes() }
            .addOnSuccessListener { entradas ->
                resolverPedidos(entradas)
                    .addOnSuccessListener { result.setResult(it) }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun carregarPedidosEnviados(utilizador: UtilizadorSocial): Task<List<UtilizadorSocial>> {
        val result = TaskCompletionSource<List<UtilizadorSocial>>()
        database.child(FirebasePaths.JOGADORES).get()
            .addOnSuccessListener { snapshot ->
                val chavesUtilizador = utilizador.chavesCompatibilidade.toSet()
                val destinatarios = snapshot.children.mapNotNull { jogador ->
                    val chaveOutro = jogador.key ?: return@mapNotNull null
                    val pedidos = jogador.child(FirebasePaths.PEDIDOS_AMIZADE)
                    val temPedidoPendente = chavesUtilizador.any { chave ->
                        pedidos.child(chave).child(FirebasePaths.ESTADO).getValue(String::class.java) == GameConstants.ESTADO_PENDENTE
                    }
                    if (temPedidoPendente) chaveOutro else null
                }
                resolverUtilizadores(destinatarios)
                    .addOnSuccessListener { result.setResult(it) }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun carregarConvitesRecebidos(
        utilizador: UtilizadorSocial,
        nomeCategoriaPadrao: String
    ): Task<List<Convite>> {
        val result = TaskCompletionSource<List<Convite>>()
        carregarConvitesPendentes(utilizador, nomeCategoriaPadrao)
            .addOnSuccessListener { pendentes ->
                resolverConvites(pendentes)
                    .addOnSuccessListener { result.setResult(it) }
                    .addOnFailureListener { result.setException(it) }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun observarAmigos(
        utilizador: UtilizadorSocial,
        onAmigosAlterados: (List<UtilizadorSocial>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val snapshots = mutableMapOf<String, List<String>>()
        val listeners = utilizador.chavesDonoSocial().map { chaveDono ->
            val reference = amigosRef(chaveDono)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshots[chaveDono] = snapshot.children.mapNotNull { it.key }
                    resolverUtilizadores(snapshots.values.flatten())
                        .addOnSuccessListener { amigos ->
                            onAmigosAlterados(amigos.filterNot { it.corresponde(utilizador) })
                        }
                        .addOnFailureListener { onErro() }
                }

                override fun onCancelled(error: DatabaseError) {
                    onErro()
                }
            }
            reference.addValueEventListener(listener)
            ListenerRegistado(reference, listener)
        }
        return ListenerHandle(listeners)
    }

    fun observarPedidosRecebidos(
        utilizador: UtilizadorSocial,
        onPedidosAlterados: (List<PedidoAmizade>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val snapshots = mutableMapOf<String, List<Pair<String, String>>>()
        val listeners = utilizador.chavesDonoSocial().map { chaveDono ->
            val reference = jogadorRef(chaveDono).child(FirebasePaths.PEDIDOS_AMIZADE)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshots[chaveDono] = snapshot.toPedidosPendentes().map { chaveOutro -> chaveDono to chaveOutro }
                    resolverPedidos(snapshots.values.flatten())
                        .addOnSuccessListener { onPedidosAlterados(it) }
                        .addOnFailureListener { onErro() }
                }

                override fun onCancelled(error: DatabaseError) {
                    onErro()
                }
            }
            reference.addValueEventListener(listener)
            ListenerRegistado(reference, listener)
        }
        return ListenerHandle(listeners)
    }

    fun observarConvitesRecebidos(
        utilizador: UtilizadorSocial,
        nomeCategoriaPadrao: String,
        onConvitesAlterados: (List<Convite>) -> Unit,
        onErro: () -> Unit = {}
    ): ListenerHandle {
        val snapshots = mutableMapOf<String, List<ConvitePendente>>()
        val listeners = utilizador.chavesDonoSocial().map { chaveDono ->
            val reference = jogadorRef(chaveDono).child(FirebasePaths.CONVITES_RECEBIDOS)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshots[chaveDono] = snapshot.children.mapNotNull {
                        it.toConvitePendente(chaveDono, nomeCategoriaPadrao)
                    }
                    resolverConvites(snapshots.values.flatten())
                        .addOnSuccessListener { onConvitesAlterados(it) }
                        .addOnFailureListener { onErro() }
                }

                override fun onCancelled(error: DatabaseError) {
                    onErro()
                }
            }
            reference.addValueEventListener(listener)
            ListenerRegistado(reference, listener)
        }
        return ListenerHandle(listeners)
    }

    fun removerListener(handle: ListenerHandle?) {
        handle?.remover()
    }

    fun enviarConvite1x1(
        utilizador: UtilizadorSocial,
        amigo: UtilizadorSocial,
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
                    FirebasePaths.JOGADORES to mapOf(
                        utilizador.chavePrimaria to utilizador.toJogadorCompetitivoData(),
                        amigo.chavePrimaria to amigo.toJogadorCompetitivoData()
                    ),
                    FirebasePaths.ADMIN to utilizador.nomeDisplay,
                    FirebasePaths.ADMIN_ID to utilizador.uid.ifBlank { utilizador.chavePrimaria },
                    FirebasePaths.ADMIN_UID to utilizador.uid,
                    FirebasePaths.ESTADO to GameConstants.ESTADO_EM_ESPERA,
                    FirebasePaths.NOME_CATEGORIA to nomeCategoria
                ),
                "${FirebasePaths.JOGADORES}/${amigo.chavePerfil}/${FirebasePaths.CONVITES_RECEBIDOS}/${utilizador.chavePrimaria}" to conviteData,
                "${FirebasePaths.JOGADORES}/${utilizador.chavePerfil}/${FirebasePaths.CONVITES_ENVIADOS}/${amigo.chavePrimaria}" to conviteData
            )
        )
    }

    fun enviarConvite2x2(
        utilizador: UtilizadorSocial,
        amigosSelecionados: List<UtilizadorSocial>,
        codigoSala: String,
        nomeCategoria: String
    ): Task<Void> {
        val jogadores = hashMapOf<String, Any>(
            utilizador.chavePrimaria to utilizador.toJogadorCompetitivoData()
        )
        amigosSelecionados.forEach { amigo ->
            jogadores[amigo.chavePrimaria] = amigo.toJogadorCompetitivoData()
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
                FirebasePaths.ADMIN to utilizador.nomeDisplay,
                FirebasePaths.ADMIN_ID to utilizador.uid.ifBlank { utilizador.chavePrimaria },
                FirebasePaths.ADMIN_UID to utilizador.uid,
                FirebasePaths.ESTADO to GameConstants.ESTADO_EM_ESPERA,
                FirebasePaths.NOME_CATEGORIA to nomeCategoria
            )
        )
        amigosSelecionados.forEach { amigo ->
            updates["${FirebasePaths.JOGADORES}/${amigo.chavePerfil}/${FirebasePaths.CONVITES_RECEBIDOS}/${utilizador.chavePrimaria}"] = conviteData
            updates["${FirebasePaths.JOGADORES}/${utilizador.chavePerfil}/${FirebasePaths.CONVITES_ENVIADOS}/${amigo.chavePrimaria}"] = conviteData
        }
        return database.updateChildren(updates)
    }

    fun aceitarConvite(utilizador: UtilizadorSocial, convite: Convite): Task<Void> {
        val chaveRemetente = convite.amigoId.ifBlank { convite.nomeAmigo }
        val chaveConviteRecebido = convite.chaveRemetente.ifBlank { chaveRemetente }
        val chaveDono = convite.chaveDono.ifBlank { utilizador.chavePrimaria }
        val result = TaskCompletionSource<Void>()
        resolverUtilizador(chaveRemetente, convite.nomeAmigo)
            .addOnSuccessListener { remetente ->
                if (remetente == null) {
                    result.setException(IllegalStateException("Remetente do convite nao encontrado."))
                    return@addOnSuccessListener
                }
                database.updateChildren(
                    mapOf(
                        "${FirebasePaths.JOGADORES}/$chaveDono/${FirebasePaths.CONVITES_RECEBIDOS}/$chaveConviteRecebido/${FirebasePaths.ESTADO}" to GameConstants.ESTADO_ACEITE,
                        "${FirebasePaths.JOGADORES}/${remetente.chavePerfil}/${FirebasePaths.CONVITES_ENVIADOS}/$chaveDono/${FirebasePaths.ESTADO}" to GameConstants.ESTADO_ACEITE
                    )
                ).addOnSuccessListener {
                    result.setResult(null)
                }.addOnFailureListener {
                    result.setException(it)
                }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    fun recusarConvite(utilizador: UtilizadorSocial, convite: Convite): Task<Void> {
        return removerConvite(utilizador, convite)
    }

    fun removerConvite(utilizador: UtilizadorSocial, convite: Convite): Task<Void> {
        val chaveRemetente = convite.amigoId.ifBlank { convite.nomeAmigo }
        val chaveConviteRecebido = convite.chaveRemetente.ifBlank { chaveRemetente }
        val chaveDono = convite.chaveDono.ifBlank { utilizador.chavePrimaria }
        val result = TaskCompletionSource<Void>()
        resolverUtilizador(chaveRemetente, convite.nomeAmigo)
            .addOnSuccessListener { remetente ->
                if (remetente == null) {
                    result.setException(IllegalStateException("Remetente do convite nao encontrado."))
                    return@addOnSuccessListener
                }
                database.updateChildren(
                    hashMapOf<String, Any?>(
                        "${FirebasePaths.JOGADORES}/$chaveDono/${FirebasePaths.CONVITES_RECEBIDOS}/$chaveConviteRecebido" to null,
                        "${FirebasePaths.JOGADORES}/${remetente.chavePerfil}/${FirebasePaths.CONVITES_ENVIADOS}/$chaveDono" to null
                    )
                ).addOnSuccessListener {
                    result.setResult(null)
                }.addOnFailureListener {
                    result.setException(it)
                }
            }
            .addOnFailureListener { result.setException(it) }
        return result.task
    }

    private fun procurarUtilizadorPorNome(
        nomePesquisa: String,
        nomeFallback: String,
        result: TaskCompletionSource<UtilizadorSocial?>
    ) {
        jogadoresRef()
            .orderByChild(FirebasePaths.NOME_UTILIZADOR)
            .equalTo(nomePesquisa)
            .limitToFirst(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val perfil = querySnapshot.children.firstOrNull { it.isPerfilJogador() }
                result.setResult(perfil?.toUtilizadorSocial(nomeFallback.ifBlank { nomePesquisa }))
            }
            .addOnFailureListener { exception ->
                result.setException(exception)
            }
    }

    private fun carregarChavesSociais(
        utilizador: UtilizadorSocial,
        childPath: String,
        extractor: (DataSnapshot) -> List<String> = { snapshot -> snapshot.children.mapNotNull { it.key } }
    ): Task<List<Pair<String, String>>> {
        val result = TaskCompletionSource<List<Pair<String, String>>>()
        val donos = utilizador.chavesDonoSocial()
        if (donos.isEmpty()) {
            result.setResult(emptyList())
            return result.task
        }

        val entradas = mutableListOf<Pair<String, String>>()
        var pending = donos.size
        var falhou = false

        donos.forEach { chaveDono ->
            jogadorRef(chaveDono).child(childPath).get()
                .addOnSuccessListener { snapshot ->
                    if (falhou) return@addOnSuccessListener
                    entradas.addAll(extractor(snapshot).map { chaveDono to it })
                    pending--
                    if (pending == 0) result.setResult(entradas.distinct())
                }
                .addOnFailureListener { exception ->
                    if (!falhou) {
                        falhou = true
                        result.setException(exception)
                    }
                }
        }
        return result.task
    }

    private fun carregarConvitesPendentes(
        utilizador: UtilizadorSocial,
        nomeCategoriaPadrao: String
    ): Task<List<ConvitePendente>> {
        val result = TaskCompletionSource<List<ConvitePendente>>()
        val donos = utilizador.chavesDonoSocial()
        if (donos.isEmpty()) {
            result.setResult(emptyList())
            return result.task
        }

        val convites = mutableListOf<ConvitePendente>()
        var pending = donos.size
        var falhou = false

        donos.forEach { chaveDono ->
            jogadorRef(chaveDono).child(FirebasePaths.CONVITES_RECEBIDOS).get()
                .addOnSuccessListener { snapshot ->
                    if (falhou) return@addOnSuccessListener
                    convites.addAll(snapshot.children.mapNotNull { it.toConvitePendente(chaveDono, nomeCategoriaPadrao) })
                    pending--
                    if (pending == 0) result.setResult(convites.distinctBy { "${it.codigoSala}:${it.chaveRemetente}" })
                }
                .addOnFailureListener { exception ->
                    if (!falhou) {
                        falhou = true
                        result.setException(exception)
                    }
                }
        }
        return result.task
    }

    private fun resolverPedidos(entradas: List<Pair<String, String>>): Task<List<PedidoAmizade>> {
        val result = TaskCompletionSource<List<PedidoAmizade>>()
        if (entradas.isEmpty()) {
            result.setResult(emptyList())
            return result.task
        }

        val pedidos = mutableListOf<PedidoAmizade>()
        val vistos = mutableSetOf<String>()
        var pending = entradas.size

        entradas.forEach { (chaveDono, chaveOutro) ->
            resolverUtilizador(chaveOutro, chaveOutro)
                .addOnCompleteListener { task ->
                    val utilizador = if (task.isSuccessful) task.result else null
                    if (task.isSuccessful && utilizador != null && vistos.add(utilizador.chaveDedupe)) {
                        pedidos.add(PedidoAmizade(utilizador, chaveDono))
                    }
                    pending--
                    if (pending == 0) result.setResult(pedidos)
                }
        }
        return result.task
    }

    private fun resolverConvites(pendentes: List<ConvitePendente>): Task<List<Convite>> {
        val result = TaskCompletionSource<List<Convite>>()
        if (pendentes.isEmpty()) {
            result.setResult(emptyList())
            return result.task
        }

        val convites = mutableListOf<Convite>()
        val vistos = mutableSetOf<String>()
        var pending = pendentes.size

        pendentes.forEach { pendente ->
            resolverUtilizador(pendente.chaveRemetente, pendente.chaveRemetente)
                .addOnCompleteListener { task ->
                    val remetente = if (task.isSuccessful) task.result else null
                    if (task.isSuccessful && remetente != null) {
                        val chaveDedupe = "${pendente.codigoSala}:${remetente.chaveDedupe}"
                        if (vistos.add(chaveDedupe)) {
                            convites.add(
                                Convite(
                                    nomeAmigo = remetente.nomeDisplay,
                                    codigoSala = pendente.codigoSala,
                                    modo = pendente.modo,
                                    nomeCategoria = pendente.nomeCategoria,
                                    amigoId = remetente.chavePrimaria,
                                    chaveRemetente = pendente.chaveRemetente,
                                    chaveDono = pendente.chaveDono
                                )
                            )
                        }
                    }
                    pending--
                    if (pending == 0) result.setResult(convites)
                }
        }
        return result.task
    }

    private fun resolverUtilizadores(chaves: Collection<String>): Task<List<UtilizadorSocial>> {
        val result = TaskCompletionSource<List<UtilizadorSocial>>()
        val chavesUnicas = chaves.filter { it.isNotBlank() }.distinct()
        if (chavesUnicas.isEmpty()) {
            result.setResult(emptyList())
            return result.task
        }

        val utilizadores = mutableListOf<UtilizadorSocial>()
        val vistos = mutableSetOf<String>()
        var pending = chavesUnicas.size

        chavesUnicas.forEach { chave ->
            resolverUtilizador(chave, chave)
                .addOnCompleteListener { task ->
                    val utilizador = if (task.isSuccessful) task.result else null
                    if (task.isSuccessful && utilizador != null && vistos.add(utilizador.chaveDedupe)) {
                        utilizadores.add(utilizador)
                    }
                    pending--
                    if (pending == 0) result.setResult(utilizadores)
                }
        }
        return result.task
    }

    private fun jogadorRef(chaveJogador: String): DatabaseReference {
        return jogadoresRef().child(chaveJogador)
    }

    private fun amigosRef(chaveJogador: String): DatabaseReference {
        return jogadorRef(chaveJogador).child(FirebasePaths.AMIGOS)
    }

    private fun jogadoresRef(): DatabaseReference {
        return database.child(FirebasePaths.JOGADORES)
    }

    private fun UtilizadorSocial.chavesDonoSocial(): List<String> {
        return listOf(uid, chavePerfil, nomeUtilizador)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun UtilizadorSocial.toJogadorCompetitivoData(): Map<String, Any> {
        val dados = linkedMapOf<String, Any>(
            FirebasePaths.NOME to nomeDisplay,
            FirebasePaths.NOME_DISPLAY to nomeDisplay
        )
        if (uid.isNotBlank()) dados[FirebasePaths.UID] = uid
        if (nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = nomeUtilizador
        return dados
    }

    private fun DataSnapshot.toPedidosPendentes(): List<String> {
        return children.mapNotNull { pedido ->
            val nomeOutro = pedido.key ?: return@mapNotNull null
            val estado = pedido.child(FirebasePaths.ESTADO).getValue(String::class.java).orEmpty()
            if (estado == GameConstants.ESTADO_PENDENTE) nomeOutro else null
        }
    }

    private fun DataSnapshot.toConvitePendente(chaveDono: String, nomeCategoriaPadrao: String): ConvitePendente? {
        val chaveRemetente = key ?: return null
        val estado = child(FirebasePaths.ESTADO).getValue(String::class.java).orEmpty()
        if (estado != GameConstants.ESTADO_PENDENTE) return null

        val codigoSala = child(FirebasePaths.CODIGO_SALA).getValue(String::class.java).orEmpty()
        val modo = child(FirebasePaths.MODO).getValue(String::class.java) ?: GameConstants.MODO_1X1
        val nomeCategoria = child(FirebasePaths.NOME_CATEGORIA).getValue(String::class.java) ?: nomeCategoriaPadrao
        return ConvitePendente(chaveDono, chaveRemetente, codigoSala, modo, nomeCategoria)
    }

    private fun DataSnapshot.isPerfilJogador(): Boolean {
        return child(FirebasePaths.NOME_UTILIZADOR).exists() ||
            child(FirebasePaths.UID).exists() ||
            child(FirebasePaths.PASSWORD).exists() ||
            child(FirebasePaths.AVATAR).exists()
    }

    private fun DataSnapshot.toUtilizadorSocial(chaveOrigem: String): UtilizadorSocial {
        val chavePerfil = key.orEmpty()
        val nomeUtilizador = child(FirebasePaths.NOME_UTILIZADOR).getValue(String::class.java)
            ?: child(FirebasePaths.NOME).getValue(String::class.java)
            ?: chaveOrigem.ifBlank { chavePerfil }
        val uid = child(FirebasePaths.UID).getValue(String::class.java).orEmpty()

        return UtilizadorSocial(
            uid = uid,
            nomeUtilizador = nomeUtilizador,
            chavePerfil = chavePerfil,
            chaveOrigem = chaveOrigem.ifBlank { chavePerfil }
        )
    }
}
