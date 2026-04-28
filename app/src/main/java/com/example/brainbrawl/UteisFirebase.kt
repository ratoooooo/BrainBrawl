package com.example.brainbrawl

import com.google.firebase.database.DataSnapshot

object UteisFirebase {
    fun DataSnapshot.longValue(default: Long = 0L): Long {
        return when (val value = value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            is String -> value.toLongOrNull() ?: default
            else -> default
        }
    }

    fun DataSnapshot.intValue(default: Int = 0): Int = longValue(default.toLong()).toInt()

    fun DataSnapshot.doubleValue(default: Double = 0.0): Double {
        return when (val value = value) {
            is Double -> value
            is Float -> value.toDouble()
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
            else -> default
        }
    }
}
