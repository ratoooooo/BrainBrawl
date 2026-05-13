package com.example.brainbrawl.utils

import android.content.Context
import androidx.annotation.DrawableRes
import com.example.brainbrawl.R

object AvatarUtils {
    private const val FALLBACK_NAME = "avatar_1_playstore"

    @DrawableRes
    fun resolverAvatar(context: Context, avatarFirebase: String?): Int {
        val nomes = nomesCandidatos(avatarFirebase)
        nomes.forEach { nome ->
            val drawableId = context.resources.getIdentifier(nome, "drawable", context.packageName)
            if (drawableId != 0) return drawableId
            val mipmapId = context.resources.getIdentifier(nome, "mipmap", context.packageName)
            if (mipmapId != 0) return mipmapId
        }
        return R.drawable.avatar_1_playstore
    }

    fun nomeAvatarPorIndex(index: Int): String {
        return "avatar_${index + 1}_playstore"
    }

    private fun nomesCandidatos(avatarFirebase: String?): List<String> {
        val normalizado = normalizarNome(avatarFirebase)
        val base = normalizado.ifBlank { FALLBACK_NAME }
        val candidatos = mutableListOf(base)
        val avatarNumero = Regex("^avatar_(\\d+)$").matchEntire(base)?.groupValues?.getOrNull(1)
        if (avatarNumero != null) {
            candidatos += "avatar_${avatarNumero}_playstore"
        }
        if (!base.endsWith("_playstore") && Regex("^avatar_\\d+_playstore$").matches("${base}_playstore")) {
            candidatos += "${base}_playstore"
        }
        candidatos += FALLBACK_NAME
        return candidatos.distinct()
    }

    private fun normalizarNome(avatarFirebase: String?): String {
        return avatarFirebase.orEmpty()
            .trim()
            .lowercase()
            .removePrefix("@drawable/")
            .removePrefix("@mipmap/")
            .substringBeforeLast(".png")
            .substringBeforeLast(".webp")
            .substringBeforeLast(".jpg")
            .substringBeforeLast(".jpeg")
            .substringBeforeLast(".xml")
            .trim()
    }
}
