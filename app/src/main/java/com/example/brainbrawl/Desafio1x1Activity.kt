package com.example.brainbrawl

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Desafio1x1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nomeAmigo = intent.getStringExtra("nomeAmigo") ?: "Desconhecido"
        val nomeUtilizador = intent.getStringExtra("nomeUtilizador") ?: "Eu"
        val txt = TextView(this)
        txt.text = "Desafio de $nomeUtilizador para $nomeAmigo"
        setContentView(txt)
    }
}