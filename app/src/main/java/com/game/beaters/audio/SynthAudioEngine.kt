package com.game.beaters.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.game.beaters.game.MusicalNote
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Motor de audio sintetizado. Cada nota se genera en memoria como un buffer
 * PCM 16-bit mono a 44.1 kHz, aplicando una onda senoidal con envolvente
 * ADSR sencilla para evitar el "click" inicial y darle carácter de nota.
 *
 * Cada sonido se reproduce en su propio AudioTrack en MODO STATIC. El
 * worker thread llena el buffer, llama a play() y luego duerme lo justo
 * para liberar el track cuando termina — así evitamos depender de
 * callbacks que requerirían Looper.
 *
 * El pool de 4 threads permite hasta 4 sonidos simultáneos, lo que
 * cubre combos y miss overlapeados cómodamente.
 */
class SynthAudioEngine : AudioEngine {

    private val sampleRate = 44_100
    private val noteDurationMs = 320
    private val missDurationMs = 220

    private val executor = Executors.newFixedThreadPool(4)

    @Volatile
    private var running = false

    override fun start() {
        running = true
    }

    override fun release() {
        running = false
        executor.shutdownNow()
    }

    override fun playNote(note: MusicalNote) {
        if (!running) return
        executor.submit {
            playFrequency(note.frequencyHz, noteDurationMs)
        }
    }

    override fun playMiss() {
        if (!running) return
        executor.submit { playDissonant() }
    }

    // ------------------------------------------------------------------
    // Síntesis
    // ------------------------------------------------------------------

    private fun playFrequency(frequencyHz: Float, durationMs: Int) {
        val samples = generateNoteSamples(frequencyHz, durationMs)
        writeAndPlay(samples, durationMs)
    }

    private fun playDissonant() {
        // Frecuencias disonantes: batimiento (175 vs 185 Hz) + Si3 (247 Hz)
        val samples = generateDissonantSamples(
            floatArrayOf(175f, 185f, 247f),
            missDurationMs
        )
        writeAndPlay(samples, missDurationMs)
    }

    /** Genera muestras PCM 16-bit para una nota con envolvente ADSR. */
    private fun generateNoteSamples(frequencyHz: Float, durationMs: Int): ShortArray {
        val numSamples = sampleRate * durationMs / 1000
        val out = ShortArray(numSamples)
        val angular = 2.0 * PI * frequencyHz / sampleRate

        val attack = (numSamples * 0.04f).toInt().coerceAtLeast(1)
        val decay = (numSamples * 0.12f).toInt().coerceAtLeast(1)
        val release = (numSamples * 0.30f).toInt().coerceAtLeast(1)
        val sustainLevel = 0.55f

        for (i in 0 until numSamples) {
            val t = i.toDouble()
            // Fundamental + octava tenue para un timbre menos puro
            val sineVal = sin(angular * t) * 0.85 + sin(angular * 2.0 * t) * 0.15

            val env = when {
                i < attack -> i.toFloat() / attack
                i < attack + decay -> {
                    val d = (i - attack).toFloat() / decay
                    1f - (1f - sustainLevel) * d
                }
                i > numSamples - release -> {
                    val r = (numSamples - i).toFloat() / release
                    sustainLevel * r.coerceAtLeast(0f)
                }
                else -> sustainLevel
            }

            val value = sineVal * env * Short.MAX_VALUE * 0.6
            out[i] = value.toInt().toShort()
        }
        return out
    }

    private fun generateDissonantSamples(frequencies: FloatArray, durationMs: Int): ShortArray {
        val numSamples = sampleRate * durationMs / 1000
        val out = ShortArray(numSamples)
        val angulars = frequencies.map { 2.0 * PI * it / sampleRate }

        for (i in 0 until numSamples) {
            val t = i.toDouble()
            var s = 0.0
            for (a in angulars) s += sin(a * t)
            s /= angulars.size
            val env = exp(-3.0 * i / numSamples)
            val noise = (Math.random() - 0.5) * 0.25
            val value = (s + noise) * env * Short.MAX_VALUE * 0.55
            out[i] = value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun writeAndPlay(samples: ShortArray, durationMs: Int) {
        val bufferBytes = samples.size * 2

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (t: Throwable) {
            return
        }

        try {
            track.write(samples, 0, samples.size)
            track.play()
            // Esperar el tiempo que dura el audio + margen, y luego liberar.
            // Hacemos esto síncronamente en el worker thread del executor
            // para no necesitar un Handler con Looper.
            try {
                Thread.sleep((durationMs + 80).toLong())
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            try { track.stop() } catch (_: Throwable) {}
        } catch (_: Throwable) {
            // swallow
        } finally {
            try { track.release() } catch (_: Throwable) {}
        }
    }
}
