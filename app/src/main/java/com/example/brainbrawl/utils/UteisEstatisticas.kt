package com.example.brainbrawl.utils

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.google.firebase.database.DataSnapshot

object UteisEstatisticas {

    /**
     * Determina se uma categoria é competitiva com base na sua origem.
     * Apenas categorias explicitamente marcadas como oficiais são competitivas.
     */
    fun isCategoriaCompetitiva(origem: String?): Boolean {
        return origem == GameConstants.ORIGEM_CATEGORIA_OFICIAL
    }

    /**
     * Determina se uma sala (DataSnapshot) é competitiva.
     * Uma sala só é competitiva se tiver CATEGORIA_ORIGEM == ORIGEM_CATEGORIA_OFICIAL.
     * Qualquer outro valor, ou a ausência do campo, resulta em não competitiva (mais seguro).
     */
    fun isSalaCompetitiva(salaSnapshot: DataSnapshot): Boolean {
        val origem = salaSnapshot.child(FirebasePaths.CATEGORIA_ORIGEM).getValue(String::class.java)
        
        // Regra conservadora: apenas oficial é competitivo.
        if (origem != GameConstants.ORIGEM_CATEGORIA_OFICIAL) {
            return false
        }

        // Reforço de segurança: se existirem vestígios de categoria pública ou personalizada,
        // forçamos não competitivo mesmo que CATEGORIA_ORIGEM diga o contrário (evita manipulação).
        val temPublica = salaSnapshot.child("categoriaPublica").getValue(Boolean::class.java) == true ||
                salaSnapshot.child(FirebasePaths.CATEGORIA_PUBLICA_ID).exists()
        
        val temPersonalizada = salaSnapshot.child("categoriaPersonalizada").getValue(Boolean::class.java) == true ||
                salaSnapshot.child(FirebasePaths.DONO_UID).exists()

        return !temPublica && !temPersonalizada
    }
}
