package com.example.brainbrawl.repositories

import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.models.Badge
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction

class BadgesRepository(
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
) {

    fun obterConquistas(uid: String, isGuest: Boolean): Task<Set<String>> {
        if (isGuest || uid.isBlank()) {
            return Tasks.forResult(emptySet())
        }

        val result = TaskCompletionSource<Set<String>>()
        conquistasRef(uid).get()
            .addOnSuccessListener { snapshot ->
                result.setResult(snapshot.children.mapNotNull { it.key }.toSet())
            }
            .addOnFailureListener { exception ->
                result.setException(exception)
            }
        return result.task
    }

    fun gravarConquistasDesbloqueadas(uid: String, isGuest: Boolean, badges: List<Badge>): Task<Void> {
        if (isGuest || uid.isBlank() || badges.isEmpty()) {
            return Tasks.forResult(null)
        }

        return Tasks.whenAll(badges.map { badge ->
            gravarBadgeDesbloqueada(uid, badge)
        })
    }

    private fun gravarBadgeDesbloqueada(uid: String, badge: Badge): Task<Void> {
        val result = TaskCompletionSource<Void>()
        conquistasRef(uid).child(badge.id).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value != null) {
                    return Transaction.abort()
                }

                currentData.value = mapOf(
                    FirebasePaths.ID to badge.id,
                    FirebasePaths.FAMILIA to badge.familia.codigo,
                    FirebasePaths.NOME to badge.nome,
                    FirebasePaths.DESCRICAO to badge.descricao,
                    FirebasePaths.OBJETIVO to badge.objetivo,
                    FirebasePaths.PROGRESSO_AO_DESBLOQUEAR to badge.progressoAtual,
                    FirebasePaths.DRAWABLE_NAME to badge.drawableName,
                    FirebasePaths.DESBLOQUEADA_EM to ServerValue.TIMESTAMP,
                    FirebasePaths.ORIGEM to ORIGEM_AUTO
                )
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    result.setException(error.toException())
                } else {
                    result.setResult(null)
                }
            }
        })
        return result.task
    }

    private fun conquistasRef(uid: String): DatabaseReference {
        return database.child(FirebasePaths.CONQUISTAS).child(uid)
    }

    private companion object {
        const val ORIGEM_AUTO = "auto"
    }
}
