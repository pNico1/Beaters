package com.game.beaters.level

import com.game.beaters.game.Difficulty
import com.game.beaters.game.MusicalNote
import com.game.beaters.game.NoteType
import java.io.InputStream
import kotlin.random.Random

/**
 * Generador procedural de niveles. Se usa cuando no hay un archivo de nivel
 * cargado — o sea, en la v1 del juego.
 *
 * Construye una secuencia de NoteEvents respetando [Difficulty.spawnIntervalMs]
 * y [Difficulty.redProbability]. La longitud del nivel se determina por la
 * cantidad de notas VERDES que queremos que el jugador tenga para acertar
 * (targetGreenHits): seguimos spawneando hasta llegar a ese numero, con un
 * pequeno buffer de seguridad por si el jugador falla muchas al final.
 *
 * Si se llama via la interfaz [LevelLoader.load], se usa un target por
 * defecto (DEFAULT_TARGET) porque el loader generico no conoce la
 * preferencia del usuario.
 */
class ProceduralLevelLoader(
    private val seed: Long = System.currentTimeMillis()
) : LevelLoader {

    override fun canHandle(sourceName: String): Boolean = true

    override fun load(source: InputStream, difficulty: Difficulty): Level {
        // source no se consume — el loader es procedural.
        return generate(difficulty, DEFAULT_TARGET)
    }

    fun generate(difficulty: Difficulty, targetGreenHits: Int): Level {
        val random = Random(seed)
        val events = mutableListOf<NoteEvent>()

        var t = 1500L  // un respiro inicial de 1.5s antes de la primera nota

        // Jitter: variar el intervalo +/-25% para que no quede mecanico.
        val baseInterval = difficulty.spawnIntervalMs

        // Evitamos 3 rojas seguidas (frustrante) y 2 notas simultaneas en la
        // misma lane (imposibles de tapear bien).
        var consecutiveReds = 0
        var lastLane = -1

        // Spawneamos hasta tener target + buffer de notas verdes. El buffer
        // absorbe la posibilidad de que el jugador falle algunas; si no las
        // necesita el nivel termina antes y las sobrantes no se usan.
        val greensNeeded = targetGreenHits + GREEN_SAFETY_BUFFER
        var greensPlaced = 0

        while (greensPlaced < greensNeeded) {
            val lane = pickLane(random, lastLane)
            val isRed = random.nextFloat() < difficulty.redProbability && consecutiveReds < 2
            val type = if (isRed) NoteType.RED else NoteType.GREEN
            val pitch = if (type == NoteType.GREEN) {
                MusicalNote.notesForLane(lane).let { it[random.nextInt(it.size)] }
            } else {
                // Las rojas no deben sonar, pero igual les ponemos un pitch
                // (por consistencia del modelo). Nunca se reproduce.
                MusicalNote.notesForLane(lane)[0]
            }

            events += NoteEvent(timeMs = t, lane = lane, type = type, pitch = pitch)

            if (type == NoteType.GREEN) greensPlaced++
            consecutiveReds = if (isRed) consecutiveReds + 1 else 0
            lastLane = lane

            // Proximo tiempo con jitter.
            val jitter = (baseInterval * 0.25f).toLong()
            val delta = baseInterval + random.nextLong(-jitter, jitter + 1)
            t += delta.coerceAtLeast(150L)
        }

        return Level(
            id = "procedural_${difficulty.displayKey}_$targetGreenHits",
            title = "Modo ${difficulty.displayKey}",
            bpm = 60_000f / baseInterval,
            events = events
        )
    }

    private fun pickLane(random: Random, lastLane: Int): Int {
        // Evitar repetir demasiado la misma columna para que no sea monotono.
        var lane = random.nextInt(4)
        if (lane == lastLane && random.nextFloat() < 0.6f) {
            lane = (lane + 1 + random.nextInt(3)) % 4
        }
        return lane
    }

    companion object {
        const val DEFAULT_TARGET = 20
        private const val GREEN_SAFETY_BUFFER = 8
    }
}
