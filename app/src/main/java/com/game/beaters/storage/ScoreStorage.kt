package com.game.beaters.storage

import android.content.Context
import com.game.beaters.game.Difficulty

/**
 * Guarda los 3 puntajes más altos por dificultad en SharedPreferences.
 *
 * Las claves son "top_{difficulty}_{index}" con index 0..2. Se mantiene
 * la lista ordenada descendente. Si un nuevo score supera alguno de los
 * top 3, lo inserta en la posición correspondiente y desplaza los demás.
 *
 * SharedPreferences es suficiente para este volumen — no vale la pena
 * montar una Room DB.
 */
class ScoreStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun topScores(difficulty: Difficulty): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until CAPACITY) {
            val v = prefs.getInt(keyFor(difficulty, i), -1)
            if (v >= 0) result.add(v)
        }
        return result
    }

    /**
     * Intenta registrar un nuevo score. Devuelve la posición (1-indexed)
     * si entró al top, o 0 si no calificó.
     *
     * En caso de empate con un score existente, el score nuevo toma la
     * posición más alta disponible (es decir, se considera "tan bueno como"
     * el que ya estaba).
     */
    fun submit(difficulty: Difficulty, score: Int): Int {
        val previous = topScores(difficulty)
        val combined = (previous + score).sortedDescending().take(CAPACITY)

        val editor = prefs.edit()
        for (i in 0 until CAPACITY) {
            if (i < combined.size) editor.putInt(keyFor(difficulty, i), combined[i])
            else editor.remove(keyFor(difficulty, i))
        }
        editor.apply()

        // Si el score no aparece en el top final, no clasificó.
        val idx = combined.indexOf(score)
        return if (idx >= 0) idx + 1 else 0
    }

    private fun keyFor(difficulty: Difficulty, index: Int) =
        "top_${difficulty.displayKey}_$index"

    companion object {
        private const val PREFS = "beaters_scores"
        const val CAPACITY = 3
    }
}
