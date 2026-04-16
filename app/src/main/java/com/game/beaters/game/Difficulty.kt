package com.game.beaters.game

/**
 * Dificultades del juego. Controlan velocidad de descenso, ritmo de spawn
 * y proporción de notas rojas (trampa).
 *
 * Parámetros pensados para 60 fps. "noteSpeedDpPerSec" es la velocidad a la
 * que las notas bajan por la pantalla. "spawnIntervalMs" es el tiempo
 * promedio entre notas. "redProbability" es la probabilidad de que una
 * nota generada proceduralmente sea roja (trampa).
 */
enum class Difficulty(
    val displayKey: String,
    val noteSpeedDpPerSec: Float,
    val spawnIntervalMs: Long,
    val redProbability: Float,
    val durationSeconds: Int
) {
    EASY(
        displayKey = "easy",
        noteSpeedDpPerSec = 260f,
        spawnIntervalMs = 900L,
        redProbability = 0.08f,
        durationSeconds = 60
    ),
    MEDIUM(
        displayKey = "medium",
        noteSpeedDpPerSec = 380f,
        spawnIntervalMs = 650L,
        redProbability = 0.18f,
        durationSeconds = 75
    ),
    HARD(
        displayKey = "hard",
        noteSpeedDpPerSec = 520f,
        spawnIntervalMs = 430L,
        redProbability = 0.28f,
        durationSeconds = 90
    );

    companion object {
        fun fromKey(key: String?): Difficulty =
            values().firstOrNull { it.displayKey == key } ?: MEDIUM
    }
}
