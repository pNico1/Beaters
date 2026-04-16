package com.game.beaters.level

import com.game.beaters.game.Difficulty
import java.io.InputStream

interface LevelLoader {
    fun canHandle(sourceName: String): Boolean
    fun load(source: InputStream, difficulty: Difficulty): Level
}