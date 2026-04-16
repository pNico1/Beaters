package com.game.beaters.audio

import com.game.beaters.game.MusicalNote

/**
 * Contrato del motor de audio del juego. Abstraído para permitir swap
 * entre implementación sintetizada (default) y una basada en samples WAV
 * en el futuro, sin tocar el resto del código.
 */
interface AudioEngine {
    /** Debe llamarse al inicio (por ej. onStart de la Activity de juego). */
    fun start()

    /** Libera recursos. onStop de la Activity. */
    fun release()

    /** Reproduce la nota musical dada (acierto). */
    fun playNote(note: MusicalNote)

    /** Ruido disonante para errores (nota roja tocada o verde a destiempo). */
    fun playMiss()
}
