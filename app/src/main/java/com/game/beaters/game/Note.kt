package com.game.beaters.game

/** Tipo de nota: verde (tapear) o roja (evitar). */
enum class NoteType { GREEN, RED }

/**
 * Nota que cae en pantalla.
 *
 * - [lane] 0..3 indica la columna.
 * - [musicalNote] pitch que se reproduce al golpearla correctamente.
 * - [spawnTimeMs] es el tiempo (en ms desde el inicio del juego) en que la
 *   nota debería aparecer. El engine mantiene las notas en una cola y las
 *   instancia visibles cuando su spawnTime entra en ventana.
 * - [yPx] posición vertical actual en píxeles. El engine la actualiza frame
 *   a frame según la velocidad de la dificultad.
 * - [state] permite marcar la nota como consumida (hit, miss, fuera).
 */
data class Note(
    val lane: Int,
    val type: NoteType,
    val musicalNote: MusicalNote,
    val spawnTimeMs: Long,
    var yPx: Float = 0f,
    var state: State = State.PENDING
) {
    enum class State {
        PENDING,   // todavía no entró en pantalla
        ACTIVE,    // cayendo
        HIT,       // tapeada correctamente
        MISSED,    // salió de la zona de hit sin ser tocada (si era verde) o fue tocada a destiempo
        IGNORED    // roja que bajó y salió (correcto por parte del jugador)
    }
}
