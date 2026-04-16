package com.game.beaters.level

import com.game.beaters.game.MusicalNote
import com.game.beaters.game.NoteType

/**
 * Representación en memoria de un nivel.
 *
 * Un nivel es metadata + una lista ordenada de eventos. Cada NoteEvent es
 * independiente del tiempo de render: dice "en el ms X del nivel, en la
 * columna Y debe aparecer una nota del tipo T con el pitch P".
 *
 * El [GameEngine] traduce estos eventos a posiciones Y concretas según
 * la velocidad de la dificultad elegida. Esto permite que un mismo nivel
 * se juegue en fácil/media/difícil: más lento/rápido pero con el mismo
 * patrón musical.
 *
 * Por el momento no se carga ningún nivel desde disco. El juego usa un
 * nivel generado proceduralmente (ver [ProceduralLevelLoader]). Dejamos
 * toda la estructura preparada para que cuando se implemente el parser
 * MIDI (ver [MidiLevelLoader]) solo haya que rellenar esa función.
 */
data class Level(
    val id: String,
    val title: String,
    val bpm: Float,
    /** Eventos ORDENADOS por timeMs ascendente. */
    val events: List<NoteEvent>
)

data class NoteEvent(
    val timeMs: Long,
    val lane: Int,
    val type: NoteType,
    val pitch: MusicalNote
)
