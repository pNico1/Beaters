package com.game.beaters.game

import com.game.beaters.audio.AudioEngine
import com.game.beaters.level.Level

/**
 * Logica del juego, separada del render. El [GameEngine] no sabe nada de
 * Canvas ni de Views. Recibe el tamano de pantalla, un Level, una
 * dificultad, un AudioEngine y un callback para avisar cuando termina.
 *
 * La funcion tick() avanza el estado una cantidad de milisegundos, y
 * la funcion onTap(lane) registra un toque del jugador en la columna dada.
 *
 * Criterio de fin de partida:
 *   - Se alcanzo [targetGreenHits] notas verdes acertadas, O
 *   - Se consumieron todos los eventos del nivel (fallback).
 *
 * Scoring:
 *   - Hit perfecto (+/-50 ms):    +100 puntos + bonus combo
 *   - Hit bueno (+/-100 ms):      +50 puntos
 *   - Hit lejos (+/-180 ms):      +20 puntos pero ROMPE combo
 *   - Miss (verde no tocada):    -30 puntos, combo a 0
 *   - Roja tocada:               -60 puntos, combo a 0
 *   - Tap en vacio:              sin efecto
 */
class GameEngine(
    private val level: Level,
    private val difficulty: Difficulty,
    private val audio: AudioEngine,
    private val listener: Listener,
    private val targetGreenHits: Int
) {

    interface Listener {
        fun onScoreChanged(newScore: Int, combo: Int, greenHits: Int, target: Int)
        fun onGameFinished(finalScore: Int)
    }

    // Dimensiones
    private var heightPx: Float = 0f
    private var density: Float = 1f
    private var hitLineY: Float = 0f
    private var noteHeightPx: Float = 0f
    private var noteSpeedPxPerMs: Float = 0f

    // Estado
    private val activeNotes: MutableList<Note> = ArrayList(32)
    private var nextEventIndex = 0
    private var elapsedMs: Long = 0L
    private var finished: Boolean = false
    private var paused: Boolean = false

    // Scoring
    var score: Int = 0
        private set
    var combo: Int = 0
        private set
    var greenHits: Int = 0
        private set
    private var bestCombo: Int = 0

    // Feedback temporal para que la View pinte flashes de tecla
    private val laneFlash = FloatArray(4)
    private val laneMissFlash = FloatArray(4)

    fun setDimensions(widthPx: Float, heightPx: Float, density: Float) {
        this.heightPx = heightPx
        this.density = density

        // La linea de hit esta a ~82% de la altura (sobre las teclas).
        this.hitLineY = heightPx * 0.82f
        this.noteHeightPx = 54f * density
        this.noteSpeedPxPerMs = difficulty.noteSpeedDpPerSec * density / 1000f
    }

    fun pause() { paused = true }
    fun resume() { paused = false }
    fun isPaused(): Boolean = paused
    fun isFinished(): Boolean = finished

    fun tick(deltaMs: Long) {
        if (finished || paused) return
        elapsedMs += deltaMs

        // Decaimiento de flashes
        for (i in 0..3) {
            laneFlash[i] = (laneFlash[i] - deltaMs / 180f).coerceAtLeast(0f)
            laneMissFlash[i] = (laneMissFlash[i] - deltaMs / 220f).coerceAtLeast(0f)
        }

        // Spawn de notas cuyo tiempo ya llego (teniendo en cuenta la
        // distancia que tienen que recorrer). Queremos que lleguen a la
        // linea de hit exactamente en event.timeMs. Entonces spawneamos
        // cuando elapsed >= timeMs - travelTimeMs.
        val travelTimeMs = if (noteSpeedPxPerMs > 0f) (hitLineY / noteSpeedPxPerMs).toLong() else 0L

        while (nextEventIndex < level.events.size) {
            val ev = level.events[nextEventIndex]
            if (elapsedMs < ev.timeMs - travelTimeMs) break
            val lateMs = elapsedMs - (ev.timeMs - travelTimeMs)
            val initialY = lateMs * noteSpeedPxPerMs
            activeNotes += Note(
                lane = ev.lane,
                type = ev.type,
                musicalNote = ev.pitch,
                spawnTimeMs = elapsedMs,
                yPx = initialY,
                state = Note.State.ACTIVE
            )
            nextEventIndex++
        }

        // Mover notas activas y marcar las que salieron de la ventana de hit.
        // Las notas no-ACTIVE (HIT/MISSED/IGNORED) ya fueron removidas de la
        // lista en el momento en que cambiaron de estado, asi que todo lo
        // que queda aca es ACTIVE.
        val iter = activeNotes.iterator()
        while (iter.hasNext()) {
            val n = iter.next()
            n.yPx += deltaMs * noteSpeedPxPerMs

            if (n.yPx > hitLineY + missTolerancePx()) {
                when (n.type) {
                    NoteType.GREEN -> {
                        applyScore(delta = -30, breakCombo = true)
                        laneMissFlash[n.lane] = 1f
                    }
                    NoteType.RED -> {
                        // Dejar pasar una roja es correcto, sin cambios.
                    }
                }
                // En ambos casos la nota sale de pantalla: se remueve ya.
                iter.remove()
            }
        }

        // Fin por agotamiento de eventos (fallback, no deberia ocurrir si el
        // loader generó suficientes notas para el target).
        if (nextEventIndex >= level.events.size && activeNotes.isEmpty()) {
            finish()
        }
    }

    private fun missTolerancePx(): Float = 32f * density

    fun onTap(lane: Int) {
        if (finished || paused) return
        if (lane !in 0..3) return

        laneFlash[lane] = 1f

        // Buscar la nota activa mas cercana a la linea de hit en esa lane.
        var best: Note? = null
        var bestDist = Float.MAX_VALUE
        for (n in activeNotes) {
            if (n.lane != lane) continue
            val dist = kotlin.math.abs(n.yPx - hitLineY)
            if (dist < bestDist) {
                bestDist = dist
                best = n
            }
        }

        val hitWindow = 180f * density
        val note = best
        if (note == null || bestDist > hitWindow) {
            // Tap en vacio: sin penalizacion.
            return
        }

        if (note.type == NoteType.RED) {
            applyScore(delta = -60, breakCombo = true)
            laneMissFlash[lane] = 1f
            audio.playMiss()
            activeNotes.remove(note)
            return
        }

        // Verde: scoring escalonado segun precision
        val msOff = bestDist / noteSpeedPxPerMs
        val points = when {
            msOff <= 50f -> 100
            msOff <= 100f -> 50
            msOff <= 180f -> 20
            else -> 0
        }
        if (points == 0) {
            applyScore(delta = -30, breakCombo = true)
            laneMissFlash[lane] = 1f
            audio.playMiss()
            activeNotes.remove(note)
        } else {
            val isPerfect = msOff <= 50f
            val gained = points + if (isPerfect) combo else 0
            applyScore(delta = gained, breakCombo = !isPerfect && msOff > 100f)
            audio.playNote(note.musicalNote)
            activeNotes.remove(note)

            // Incrementar el contador de verdes acertadas y chequear fin.
            greenHits++
            if (greenHits >= targetGreenHits) {
                finish()
            }
        }
    }

    private fun applyScore(delta: Int, breakCombo: Boolean) {
        score = (score + delta).coerceAtLeast(0)
        if (breakCombo) combo = 0
        else if (delta > 0) {
            combo += 1
            if (combo > bestCombo) bestCombo = combo
        }
        listener.onScoreChanged(score, combo, greenHits, targetGreenHits)
    }

    private fun finish() {
        if (finished) return
        finished = true
        listener.onGameFinished(score)
    }

    // -------- acceso de solo lectura para el render --------

    fun snapshotNotes(): List<Note> = activeNotes
    fun laneFlashValue(lane: Int): Float = laneFlash[lane]
    fun laneMissFlashValue(lane: Int): Float = laneMissFlash[lane]
    fun hitLineYPx(): Float = hitLineY
    fun noteHeight(): Float = noteHeightPx
}
