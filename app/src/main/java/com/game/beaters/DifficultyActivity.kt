package com.game.beaters

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.game.beaters.game.Difficulty

class DifficultyActivity : AppCompatActivity() {

    private lateinit var targetInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_difficulty)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.difficulty_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        targetInput = findViewById(R.id.targetInput)

        findViewById<Button>(R.id.btnEasy).setOnClickListener { launch(Difficulty.EASY) }
        findViewById<Button>(R.id.btnMedium).setOnClickListener { launch(Difficulty.MEDIUM) }
        findViewById<Button>(R.id.btnHard).setOnClickListener { launch(Difficulty.HARD) }
    }

    private fun launch(difficulty: Difficulty) {
        val target = parseTargetOrDefault()
        startActivity(Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.displayKey)
            putExtra(GameActivity.EXTRA_TARGET_HITS, target)
        })
    }

    /**
     * Lee el numero tipeado por el usuario. Si esta vacio, no es un
     * entero valido o esta fuera de rango, vuelve al default (20). Se
     * clampea a [MIN_TARGET]..[MAX_TARGET] para evitar partidas de 1 nota
     * o absurdamente largas.
     */
    private fun parseTargetOrDefault(): Int {
        val raw = targetInput.text?.toString()?.trim().orEmpty()
        val parsed = raw.toIntOrNull() ?: DEFAULT_TARGET
        return parsed.coerceIn(MIN_TARGET, MAX_TARGET)
    }

    companion object {
        private const val DEFAULT_TARGET = 20
        private const val MIN_TARGET = 5
        private const val MAX_TARGET = 200
    }
}
