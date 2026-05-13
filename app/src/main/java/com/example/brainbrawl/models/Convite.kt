package com.example.brainbrawl.models

data class Convite(
    val nomeAmigo: String = "",
    val codigoSala: String = "",
    val modo: String = "",
    val nomeCategoria: String = "",
    val amigoId: String = "",
    val chaveRemetente: String = "",
    val chaveDono: String = "",
    val remetenteUid: String = "",
    val remetenteChavePerfil: String = "",
    val remetenteNome: String = "",
    val destinatarioUid: String = "",
    val destinatarioChavePerfil: String = "",
    val destinatarioNome: String = ""
)
