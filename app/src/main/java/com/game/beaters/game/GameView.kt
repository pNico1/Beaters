package com.game.beaters.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.game.beaters.R

/**
 * View personalizada que dibuja el campo de juego y reenvía los toques al
 * [GameEngine].
 *
 * Render loop: en cada onDraw pedimos otro frame con
 * postInvalidateOnAnimation(), lo que queda sincronizado con VSYNC
 * (~60 fps en la mayoría de dispositivos). El deltaMs se calcula con el
 * tiempo entre frames reales, no asumiendo 16.6 ms, así el gameplay no
 * se ralentiza si hay un hipo.
 *
 * Decisión de diseño: todo el juego se dibuja acá sobre un único Canvas.
 * Es más performante que tener 30+ Views hijos con animaciones —
 * justamente el problema del enfoque "View tradicional + ObjectAnimator".
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var engine: GameEngine? = null
    private var running: Boolean = false
    private var lastFrameTimeNs: Long = 0L

    // Paints pre-creados (nunca asignar en onDraw)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val laneLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lane_line)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val hitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hit_line)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val keyIdlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lane_key_idle)
    }
    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lane_key_pressed)
    }
    private val keyMissPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lane_key_miss)
    }
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(140, 255, 255, 255)
    }
    private val noteLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Colores de notas cacheados
    private val greenColor = ContextCompat.getColor(context, R.color.note_green)
    private val greenDark = ContextCompat.getColor(context, R.color.note_green_dark)
    private val redColor = ContextCompat.getColor(context, R.color.note_red)
    private val redDark = ContextCompat.getColor(context, R.color.note_red_dark)

    // Seguimiento de qué pointer está tocando qué lane, para soportar
    // multi-touch (4 teclas a la vez).
    private val pointerLane = SparseIntArrayCompat()

    fun attachEngine(engine: GameEngine) {
        this.engine = engine
        if (width > 0 && height > 0) {
            engine.setDimensions(width.toFloat(), height.toFloat(), resources.displayMetrics.density)
        }
    }

    fun startLoop() {
        running = true
        lastFrameTimeNs = 0L
        postInvalidateOnAnimation()
    }

    fun stopLoop() {
        running = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        engine?.setDimensions(w.toFloat(), h.toFloat(), resources.displayMetrics.density)
        // Background gradient una vez que sabemos el tamaño
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            ContextCompat.getColor(context, R.color.bg_top),
            ContextCompat.getColor(context, R.color.bg_bottom),
            Shader.TileMode.CLAMP
        )
        noteLabelPaint.textSize = (h * 0.025f).coerceAtLeast(18f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val eng = engine ?: return

        // Calcular deltaMs real
        val nowNs = System.nanoTime()
        val deltaMs = if (lastFrameTimeNs == 0L) 0L else (nowNs - lastFrameTimeNs) / 1_000_000L
        lastFrameTimeNs = nowNs

        // Avanzar lógica (clamp para que un pausado largo no tire 5s de juego)
        if (running) {
            eng.tick(deltaMs.coerceAtMost(50L))
        }

        // ---- render ----

        // Fondo
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Lane dividers
        val laneWidth = width / 4f
        for (i in 1..3) {
            val x = laneWidth * i
            canvas.drawLine(x, 0f, x, height.toFloat(), laneLinePaint)
        }

        // Línea de hit
        val hitY = eng.hitLineYPx()
        canvas.drawLine(0f, hitY, width.toFloat(), hitY, hitLinePaint)

        // Notas
        val noteH = eng.noteHeight()
        val padding = laneWidth * 0.08f
        val notes = eng.snapshotNotes()
        for (n in notes) {
            // Solo se renderizan notas ACTIVE. El engine remueve las demas
            // apenas cambian de estado, asi que esto es defensivo.
            if (n.state != Note.State.ACTIVE) continue
            val left = n.lane * laneWidth + padding
            val right = (n.lane + 1) * laneWidth - padding
            val top = n.yPx - noteH / 2f
            val bottom = n.yPx + noteH / 2f
            val rect = RectF(left, top, right, bottom)

            // Gradiente vertical segun color.
            val (top2, bot) = when (n.type) {
                NoteType.GREEN -> greenColor to greenDark
                NoteType.RED -> redColor to redDark
            }
            notePaint.shader = LinearGradient(
                left, top, left, bottom,
                top2, bot,
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rect, 14f, 14f, notePaint)
            canvas.drawRoundRect(rect, 14f, 14f, noteBorderPaint)

            if (n.type == NoteType.GREEN) {
                canvas.drawText(
                    n.musicalNote.solfege,
                    (left + right) / 2f,
                    n.yPx + noteLabelPaint.textSize / 3f,
                    noteLabelPaint
                )
            }
        }

        // Teclas de abajo
        val keyTop = hitY + 16f
        val keyBottom = height.toFloat() - 12f
        for (i in 0..3) {
            val pressedIntensity = eng.laneFlashValue(i)
            val missIntensity = eng.laneMissFlashValue(i)
            val paint = when {
                missIntensity > 0.05f -> keyMissPaint
                pressedIntensity > 0.05f -> keyPressedPaint
                else -> keyIdlePaint
            }
            val rect = RectF(
                i * laneWidth + 8f,
                keyTop,
                (i + 1) * laneWidth - 8f,
                keyBottom
            )
            canvas.drawRoundRect(rect, 18f, 18f, paint)
        }

        if (running) postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eng = engine ?: return false
        val laneWidth = width / 4f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val x = event.getX(idx)
                val y = event.getY(idx)
                // Solo registrar taps en la zona inferior (teclas + zona de hit).
                if (y < eng.hitLineYPx() - 30f * resources.displayMetrics.density) {
                    return true
                }
                val lane = (x / laneWidth).toInt().coerceIn(0, 3)
                pointerLane.put(event.getPointerId(idx), lane)
                eng.onTap(lane)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                pointerLane.remove(event.getPointerId(event.actionIndex))
            }
        }
        return true
    }
}

/**
 * Envoltura mínima sobre un Map<Int,Int> para no depender de
 * androidx.collection (que no está en el libs.versions.toml y no queremos
 * agregar sólo para esto).
 */
private class SparseIntArrayCompat {
    private val map = HashMap<Int, Int>()
    fun put(k: Int, v: Int) { map[k] = v }
    fun remove(k: Int) { map.remove(k) }
    @Suppress("unused")
    fun get(k: Int): Int? = map[k]
}
