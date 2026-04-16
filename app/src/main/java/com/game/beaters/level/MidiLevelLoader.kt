package com.game.beaters.level

import com.game.beaters.game.Difficulty
import com.game.beaters.game.MusicalNote
import java.io.InputStream
/**
 * Loader para archivos MIDI (.mid).
 *
 * STUB - NO IMPLEMENTADO AUN.
 *
 * La idea, cuando se implemente, es:
 *
 *  1. Parsear el archivo SMF (Standard MIDI File): header chunk "MThk",
 *     tracks "MTrk", delta-times variables, eventos NoteOn/NoteOff.
 *  2. Para cada NoteOn:
 *       a. Convertir el numero MIDI de nota a [MusicalNote] mas cercana
 *          dentro de la octava soportada (Do4..Do5). Fuera de rango se
 *          transpone por octavas.
 *       b. Asignar columna segun [MusicalNote.lane].
 *       c. Calcular el tiempo absoluto en ms usando el tempo track
 *          (meta event 0x51).
 *  3. Opcionalmente inyectar notas ROJAS como "trampas" distribuidas
 *     segun [Difficulty.redProbability]. Si el MIDI original no tiene
 *     canal reservado para rojas, se generan entre beats para hacer
 *     el nivel mas desafiante.
 *  4. Devolver la lista ordenada por timeMs.
 *
 * Alternativas a evaluar cuando se implemente:
 *   - Usar una libreria (ej. midi-parser-kt) vs. parser manual (no hay
 *     tantas lineas, pero hay que manejar el formato variable-length
 *     cuidadosamente).
 *   - Soportar solo formato 0 (una pista) inicialmente.
 *
 * El resto del juego esta disenado para funcionar apenas este loader
 * devuelva un [Level] valido. No hace falta tocar el GameEngine ni el
 * GameView cuando se implemente.
 */
class MidiLevelLoader : LevelLoader {

    override fun canHandle(sourceName: String): Boolean =
        sourceName.lowercase().endsWith(".mid") ||
        sourceName.lowercase().endsWith(".midi")

    override fun load(source: InputStream, difficulty: Difficulty): Level {
        // TODO: implementar parser SMF completo. Por ahora devolvemos un
        //   nivel vacio para evitar crashes si alguien lo invoca por error.
        val events = emptyList<NoteEvent>()

        // Ejemplo de como deberia quedar una vez implementado:
        //
        // val rawNotes = SmfParser.parse(source)
        // val events = rawNotes
        //     .map { raw ->
        //         val pitch = midiNoteToSolfege(raw.midiNumber)
        //         NoteEvent(
        //             timeMs = raw.absoluteTimeMs,
        //             lane = pitch.lane,
        //             type = NoteType.GREEN,
        //             pitch = pitch
        //         )
        //     }
        //     .let { sprinkleRedTraps(it, difficulty) }
        //     .sortedBy { it.timeMs }

        return Level(
            id = "midi_unimplemented",
            title = "MIDI (pendiente de implementar)",
            bpm = 120f,
            events = events
        )
    }

    @Suppress("unused")
    private fun midiNoteToSolfege(midiNumber: Int): MusicalNote {
        // Placeholder. midiNumber 60 = C4 (Do4).
        val normalized = ((midiNumber - 60) % 12 + 12) % 12
        return when (normalized) {
            0 -> MusicalNote.DO4
            2 -> MusicalNote.RE4
            4 -> MusicalNote.MI4
            5 -> MusicalNote.FA4
            7 -> MusicalNote.SOL4
            9 -> MusicalNote.LA4
            11 -> MusicalNote.SI4
            else -> MusicalNote.DO5 // accidentales se redondean al Do superior por ahora
        }
    }

    @Suppress("unused")
    private fun sprinkleRedTraps(
        events: List<NoteEvent>,
        difficulty: Difficulty
    ): List<NoteEvent> {
        // TODO: insertar trampas rojas entre las verdes segun difficulty.redProbability.
        //   Evitar colocarlas <150ms de una verde en la misma lane.
        return events
    }
}
