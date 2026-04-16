package com.game.beaters.game

/**
 * Notas musicales soportadas. El mapeo a columnas (4 teclas) es:
 *   Columna 0  →  DO, RE
 *   Columna 1  →  MI, FA
 *   Columna 2  →  SOL, LA
 *   Columna 3  →  SI, DO5 (octava arriba)
 *
 * De esta manera hay exactamente 2 notas por tecla, cubriendo una octava
 * completa. La nota específica que cae se sortea en runtime (o se define
 * en el nivel cargado), y se reproduce con su frecuencia exacta cuando
 * el jugador golpea correctamente.
 *
 * Frecuencias en A440 (La central = 440 Hz), cuarta octava salvo DO5.
 */
enum class MusicalNote(
    val solfege: String,
    val frequencyHz: Float,
    val lane: Int
) {
    DO4(solfege = "Do",  frequencyHz = 261.63f, lane = 0),
    RE4(solfege = "Re",  frequencyHz = 293.66f, lane = 0),
    MI4(solfege = "Mi",  frequencyHz = 329.63f, lane = 1),
    FA4(solfege = "Fa",  frequencyHz = 349.23f, lane = 1),
    SOL4(solfege = "Sol", frequencyHz = 392.00f, lane = 2),
    LA4(solfege = "La",  frequencyHz = 440.00f, lane = 2),
    SI4(solfege = "Si",  frequencyHz = 493.88f, lane = 3),
    DO5(solfege = "Do'", frequencyHz = 523.25f, lane = 3);

    companion object {
        /** Devuelve las 2 notas asignadas a esa columna. */
        fun notesForLane(lane: Int): List<MusicalNote> =
            values().filter { it.lane == lane }

        /** Sortea una nota para una columna específica. */
        fun randomForLane(lane: Int): MusicalNote {
            val options = notesForLane(lane)
            return options[(Math.random() * options.size).toInt().coerceIn(0, options.size - 1)]
        }
    }
}
