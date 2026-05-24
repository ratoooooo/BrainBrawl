package com.example.brainbrawl.config

object GameConstants {
    const val ESTADO_EM_JOGO = "em_jogo"
    const val ESTADO_EM_ESPERA = "em_espera"
    const val ESTADO_TERMINADO = "terminado"
    const val ESTADO_ELIMINADO = "eliminado"
    const val ESTADO_ON = "on"
    const val ESTADO_OFF = "off"
    const val ESTADO_PENDENTE = "pendente"
    const val ESTADO_ACEITE = "aceite"
    const val ESTADO_AGUARDANDO = "aguardando"
    const val ESTADO_ENCONTRADO = "encontrado"
    const val ESTADO_CRIANDO = "criando"
    const val ESTADO_PUBLICA = "publica"
    const val ESTADO_PRIVADA = "privada"

    const val MODO_CLASSICO = "classico"
    const val MODO_CAOTICO = "caotico"
    const val MODO_ELIMINATORIAS = "eliminatorias"
    const val MODO_1X1 = "1x1"
    const val MODO_2X2 = "2x2"

    const val EQUIPA_A = "A"
    const val EQUIPA_B = "B"
    const val JOGADOR_ADMIN = "admin"

    const val TIPO_JOGADOR_AUTH = "auth"
    const val TIPO_JOGADOR_GUEST = "guest"

    const val ORIGEM_MATCHMAKING = "matchmaking"
    const val ORIGEM_CONVITE = "convite"
    const val ORIGEM_MANUAL = "manual"

    const val MATCHMAKING_QUESTION_TIME_SECONDS = 20.0
    const val MATCHMAKING_QUESTION_TIME_MS = 20_000L
    const val COMPETITIVE_DEFAULT_QUESTION_TIME_SECONDS = 15.0
    const val COMPETITIVE_DEFAULT_QUESTION_TIME_MS = 15_000L
    const val CLASSIC_QUESTION_TIME_SECONDS = 15.0
    const val CHAOTIC_QUESTION_TIME_SECONDS = 10.0
    const val ELIMINATION_QUESTION_TIME_SECONDS = 15.0

    const val ORIGEM_CATEGORIA_OFICIAL = "oficial"
    const val ORIGEM_CATEGORIA_PERSONALIZADA = "personalizada"
    const val ORIGEM_CATEGORIA_PUBLICA = "publica"

    const val HISTORICO_RETENCAO_DIAS = 3
}
