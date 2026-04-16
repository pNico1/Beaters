package com.game.beaters

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.game.beaters.game.Difficulty
import com.game.beaters.storage.ScoreStorage

class ScoresActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scores)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scores_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val storage = ScoreStorage(this)
        bind(storage, Difficulty.EASY, findViewById(R.id.scoresEasy))
        bind(storage, Difficulty.MEDIUM, findViewById(R.id.scoresMedium))
        bind(storage, Difficulty.HARD, findViewById(R.id.scoresHard))

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun bind(storage: ScoreStorage, difficulty: Difficulty, target: TextView) {
        val scores = storage.topScores(difficulty)
        if (scores.isEmpty()) {
            target.text = getString(R.string.scores_empty)
        } else {
            target.text = scores
                .mapIndexed { idx, s -> getString(R.string.score_entry, idx + 1, s) }
                .joinToString("\n")
        }
    }
}
