package com.example.brainbrawl.services

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthService(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun utilizadorAtual(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun entrar(email: String, password: String): Task<AuthResult> {
        return firebaseAuth.signInWithEmailAndPassword(email, password)
    }

    fun registar(email: String, password: String): Task<AuthResult> {
        return firebaseAuth.createUserWithEmailAndPassword(email, password)
    }

    fun terminarSessao() {
        firebaseAuth.signOut()
    }
}
