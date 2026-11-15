package com.example

import com.example.data.model.GameDifficulty
import com.example.data.model.MorningBoostCatalog
import com.example.util.SouthAfricaFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun zarFormatting_formatsPositiveAndNegative() {
        val pos = SouthAfricaFormatters.formatZar(1250.5)
        val neg = SouthAfricaFormatters.formatZar(-45.0)
        assertTrue(pos.startsWith("R "))
        assertTrue(neg.startsWith("-R "))
    }

    @Test
    fun morningBoostCatalog_providesAllGameDatasets() {
        assertEquals(5, MorningBoostCatalog.getDailyWord(GameDifficulty.EASY).length)
        assertTrue(MorningBoostCatalog.getBrainQuestions(GameDifficulty.MEDIUM).size >= 4)
        assertTrue(MorningBoostCatalog.getTriviaQuestions(GameDifficulty.HARD).size >= 4)
        assertTrue(MorningBoostCatalog.quotes.size >= 10)
    }
}
