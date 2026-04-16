package com.game.beaters

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.game.beaters.audio.AudioEngine
import com.game.beaters.audio.SynthAudioEngine
import com.game.beaters.game.Difficulty
import com.game.beaters.game.GameEngine
import com.game.beaters.game.GameView
import com.game.beaters.level.ProceduralLevelLoader
import com.game.beaters.storage.ScoreStorage

/**
 * Activity de gameplay. Gestiona el ciclo de vida del motor de audio
 * (start/release) y del render loop de la [GameView].
 *
 * Al terminar la partida muestra un dialogo con el puntaje y permite
 * reintentar o volver al menu. El puntaje se guarda si clasifica al top 3.
 *
 * Pausa:
 *   - El boton de pausa en el HUD pausa el engine y muestra un dialogo
 *     modal con opciones Reanudar / Volver al menu.
 *   - Si la activity va al background (home, multitasking), tambien se
 *     auto-pausa y al volver al foreground se muestra el dialogo para
 *     que el jugador reanude explicitamente (evita que caigan notas
 *     apenas vuelve a ver la pantalla).
 */
class GameActivity : AppCompatActivity(), GameEngine.Listener {

    private lateinit var difficulty: Difficulty
    private lateinit var gameView: GameView
    private lateinit var audio: AudioEngine
    private lateinit var engine: GameEngine
    private lateinit var scoreStorage: ScoreStorage

    private lateinit var scoreText: TextView
    private lateinit var comboText: TextView
    private lateinit var progressText: TextView
    private lateinit var pauseButton: ImageButton

    private var targetHits: Int = ProceduralLevelLoader.DEFAULT_TARGET
    private var finished = false
    private var pauseDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        difficulty = Difficulty.fromKey(intent.getStringExtra(EXTRA_DIFFICULTY))
        targetHits = intent.getIntExtra(EXTRA_TARGET_HITS, ProceduralLevelLoader.DEFAULT_TARGET)
            .coerceIn(MIN_TARGET, MAX_TARGET)

        gameView = findViewById(R.id.gameView)
        scoreText = findViewById(R.id.hudScore)
        comboText = findViewById(R.id.hudCombo)
        progressText = findViewById(R.id.hudProgress)
        pauseButton = findViewById(R.id.btnPause)

        scoreText.text = getString(R.string.hud_score, 0)
        comboText.text = ""
        progressText.text = getString(R.string.hud_progress, 0, targetHits)

        pauseButton.setOnClickListener { showPauseDialog() }

        audio = SynthAudioEngine()
        scoreStorage = ScoreStorage(this)

        // En v1 el juego siempre usa el generador procedural. Cuando se
        // implemente MidiLevelLoader, este es el unico lugar donde hay
        // que elegir el loader adecuado (por extension del archivo, etc.).
        val level = ProceduralLevelLoader().generate(difficulty, targetHits)
        engine = GameEngine(level, difficulty, audio, this, targetHits)
        gameView.attachEngine(engine)
    }

    override fun onStart() {
        super.onStart()
        audio.start()
    }

    override fun onResume() {
        super.onResume()
        gameView.startLoop()
        // Si el engine quedo pausado (porque se regreso del background o
        // porque el dialogo de pausa esta abierto), mostrar el dialogo
        // para que el jugador reanude explicitamente.
        if (::engine.isInitialized && engine.isPaused() && !finished) {
            showPauseDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        gameView.stopLoop()
        // Pausar el engine cuando la activity va al background evita que
        // caigan notas en una pantalla que el usuario no esta mirando.
        if (::engine.isInitialized && !finished) {
            engine.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        audio.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        pauseDialog?.dismiss()
        pauseDialog = null
    }

    // ---- GameEngine.Listener ----

    override fun onScoreChanged(newScore: Int, combo: Int, greenHits: Int, target: Int) {
        runOnUiThread {
            scoreText.text = getString(R.string.hud_score, newScore)
            comboText.text = if (combo >= 2) getString(R.string.hud_combo, combo) else ""
            progressText.text = getString(R.string.hud_progress, greenHits, target)
        }
    }

    override fun onGameFinished(finalScore: Int) {
        if (finished) return
        finished = true
        runOnUiThread {
            pauseDialog?.dismiss()
            pauseDialog = null
            showGameOver(finalScore)
        }
    }

    // ---- Pausa ----

    private fun showPauseDialog() {
        if (finished) return
        if (pauseDialog?.isShowing == true) return
        engine.pause()
        pauseDialog = AlertDialog.Builder(this)
            .setTitle(R.string.game_pause_title)
            .setCancelable(false)
            .setPositiveButton(R.string.game_pause_resume) { _, _ -> resumeFromPause() }
            .setNegativeButton(R.string.game_pause_menu) { _, _ -> finish() }
            .show()
    }

    private fun resumeFromPause() {
        pauseDialog?.dismiss()
        pauseDialog = null
        if (finished) return
        engine.resume()
    }

    private fun showGameOver(finalScore: Int) {
        gameView.stopLoop()
        val rank = scoreStorage.submit(difficulty, finalScore)
        val message = buildString {
            append(getString(R.string.game_over_score, finalScore))
            if (rank in 1..ScoreStorage.CAPACITY) {
                append("\n")
                append(getString(R.string.game_over_new_record))
                append(" (#").append(rank).append(")")
            }
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.game_over_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.game_over_retry) { _, _ -> recreate() }
            .setNegativeButton(R.string.game_over_menu) { _, _ -> finish() }
            .show()
    }

    companion object {
        const val EXTRA_DIFFICULTY = "difficulty"
        const val EXTRA_TARGET_HITS = "target_hits"
        private const val MIN_TARGET = 5
        private const val MAX_TARGET = 200
    }
}
