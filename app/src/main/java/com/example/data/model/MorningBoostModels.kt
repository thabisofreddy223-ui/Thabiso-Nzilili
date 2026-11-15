package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class BoostGameType(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationLabel: String
) {
    BRAIN_CHALLENGE("BRAIN_CHALLENGE", "Brain Challenge", "Daily logic & pattern teasers", "2–3 min"),
    WORD_OF_DAY("WORD_OF_DAY", "Word of the Day", "Guess the 5-letter hidden word", "2–4 min"),
    QUICK_MATHS("QUICK_MATHS", "Quick Maths", "Fast mental arithmetic & ZAR sums", "2 min"),
    DAILY_TRIVIA("DAILY_TRIVIA", "Daily Trivia", "South Africa & world knowledge", "2–3 min"),
    MEMORY_MATCH("MEMORY_MATCH", "Memory Match", "Pair identical symbols in few moves", "2–4 min"),
    DAILY_PUZZLE("DAILY_PUZZLE", "Daily Puzzle", "Slide tiles into ascending order", "2–5 min")
}

enum class GameDifficulty(val label: String, val multiplier: Int) {
    EASY("Easy", 1),
    MEDIUM("Medium", 2),
    HARD("Hard", 3)
}

data class TriviaQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class MotivationalQuote(
    val id: String,
    val text: String,
    val author: String,
    val category: String
)

object MorningBoostCatalog {

    private fun daySeed(): Int {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return abs(dateStr.hashCode())
    }

    val quotes: List<MotivationalQuote> = listOf(
        MotivationalQuote("q1", "It always seems impossible until it is done.", "Nelson Mandela", "Perseverance"),
        MotivationalQuote("q2", "Small disciplined steps every morning build an unshakeable life.", "LIFEHUB SA", "Daily Focus"),
        MotivationalQuote("q3", "Do not judge me by my successes, judge me by how many times I fell down and got back up.", "Nelson Mandela", "Resilience"),
        MotivationalQuote("q4", "A rand saved today is freedom earned tomorrow.", "SA Financial Wisdom", "Money"),
        MotivationalQuote("q5", "Umuntu ngumuntu ngabantu — we grow stronger when we uplift our community.", "Ubuntu Proverb", "Community"),
        MotivationalQuote("q6", "Start where you are. Use what you have. Do what you can.", "Arthur Ashe", "Action"),
        MotivationalQuote("q7", "Consistency beats intensity when building a business, a career, or a savings habit.", "LIFEHUB SA", "Growth"),
        MotivationalQuote("q8", "Every morning brings a fresh page—plan your top three priorities before the noise begins.", "LIFEHUB SA", "Productivity"),
        MotivationalQuote("q9", "Knowledge is the one investment nobody can ever tax or take away from you.", "Proverb", "Learning"),
        MotivationalQuote("q10", "Peace of mind begins when you know exactly where your money and time are going.", "LIFEHUB SA", "Clarity")
    )

    fun dailyQuote(): MotivationalQuote {
        return quotes[daySeed() % quotes.size]
    }

    fun dailyChallengePrompt(): String {
        val challenges = listOf(
            "Log every rand you spend today and review your top budget category tonight.",
            "Complete your highest-priority task before 11:00 and cross off 2 errands.",
            "Move even R 20 into your savings goal today—momentum matters more than size.",
            "Check your upcoming bills for the next 7 days so nothing catches you off guard.",
            "Plan tomorrow's shopping list by shop before leaving home to avoid impulse buys."
        )
        return challenges[daySeed() % challenges.size]
    }

    fun getBrainQuestions(difficulty: GameDifficulty): List<TriviaQuestion> {
        val pool = when (difficulty) {
            GameDifficulty.EASY -> listOf(
                TriviaQuestion("What comes next in the sequence: 3, 6, 9, 12, ...?", listOf("14", "15", "16", "18"), 1, "Add 3 each step: 12 + 3 = 15."),
                TriviaQuestion("If 1 loaf of bread costs R 18, how much do 3 loaves cost?", listOf("R 48", "R 54", "R 56", "R 60"), 1, "18 × 3 = 54."),
                TriviaQuestion("Which word is the odd one out?", listOf("Pretoria", "Durban", "Gqeberha", "Limpopo"), 3, "Limpopo is a province; the others are cities."),
                TriviaQuestion("A taxi leaves at 07:15 and takes 45 minutes. When does it arrive?", listOf("07:50", "08:00", "08:05", "08:15"), 1, "15 min + 45 min = 60 min -> 08:00.")
            )
            GameDifficulty.MEDIUM -> listOf(
                TriviaQuestion("Find the next number: 2, 6, 12, 20, 30, ...?", listOf("38", "40", "42", "44"), 2, "Differences increase by 2 (+4, +6, +8, +10, +12 -> 42)."),
                TriviaQuestion("You have R 500. You spend 30% on groceries and R 85 on electricity. What remains?", listOf("R 245", "R 265", "R 275", "R 290"), 1, "30% of 500 is 150. 500 - 150 - 85 = R 265."),
                TriviaQuestion("If 5 machines take 5 minutes to make 5 parts, how long do 50 machines take for 50 parts?", listOf("5 minutes", "10 minutes", "25 minutes", "50 minutes"), 0, "Each machine takes 5 minutes to make 1 part."),
                TriviaQuestion("Which letter comes next: B, D, G, K, P, ...?", listOf("U", "V", "W", "X"), 1, "Skips +2, +3, +4, +5, +6 -> V.")
            )
            GameDifficulty.HARD -> listOf(
                TriviaQuestion("Find the missing number: 4, 9, 19, 39, 79, ...?", listOf("149", "159", "160", "169"), 1, "Each term is (previous × 2) + 1 -> 79×2+1 = 159."),
                TriviaQuestion("A R 1,200 jacket is discounted 25%, then another 10% off the sale price. Final price?", listOf("R 780", "R 810", "R 840", "R 860"), 1, "1200 × 0.75 = 900; 900 × 0.90 = R 810."),
                TriviaQuestion("Sipho is older than Thabo. Lerato is younger than Thabo. Who is oldest?", listOf("Thabo", "Lerato", "Sipho", "Cannot tell"), 2, "Sipho > Thabo > Lerato."),
                TriviaQuestion("How many times does the digit 7 appear in whole numbers from 1 to 80?", listOf("16", "17", "18", "19"), 2, "Units: 7,17,27,37,47,57,67,77(1) = 8; Tens: 70..79 = 10 -> 18 times.")
            )
        }
        val offset = daySeed() % pool.size
        return pool.indices.map { i -> pool[(i + offset) % pool.size] }
    }

    fun getDailyWord(difficulty: GameDifficulty): String {
        val words = when (difficulty) {
            GameDifficulty.EASY -> listOf("RANDS", "GOALS", "SAVED", "HAPPY", "LIGHT", "SMART", "TRUST", "POWER")
            GameDifficulty.MEDIUM -> listOf("SHARP", "GRACE", "FOCUS", "BRISK", "CRAFT", "SPARK", "KAROO", "BLOOM")
            GameDifficulty.HARD -> listOf("ZEBRA", "VIGOR", "QUART", "PIXEL", "OXIDE", "FJORD", "GLYPH", "PRISM")
        }
        return words[daySeed() % words.size]
    }

    fun getTriviaQuestions(difficulty: GameDifficulty): List<TriviaQuestion> {
        val pool = when (difficulty) {
            GameDifficulty.EASY -> listOf(
                TriviaQuestion("What is the currency of South Africa?", listOf("Dollar", "Rand", "Pula", "Shilling"), 1, "The South African Rand (ZAR) is the official currency."),
                TriviaQuestion("Which city is famous for Table Mountain?", listOf("Durban", "Cape Town", "Bloemfontein", "Polokwane"), 1, "Table Mountain overlooks Cape Town."),
                TriviaQuestion("How many official provinces does South Africa have?", listOf("7", "8", "9", "10"), 2, "South Africa has 9 provinces."),
                TriviaQuestion("What is the national flower of South Africa?", listOf("King Protea", "Sunflower", "Jacaranda", "Aloe"), 0, "The King Protea (Protea cynaroides) is South Africa's national flower.")
            )
            GameDifficulty.MEDIUM -> listOf(
                TriviaQuestion("Which South African city is known as the 'City of Gold' (eGoli)?", listOf("Pretoria", "Johannesburg", "Kimberley", "Gqeberha"), 1, "Johannesburg is affectionately called eGoli."),
                TriviaQuestion("Which is the judicial capital of South Africa?", listOf("Cape Town", "Pretoria", "Bloemfontein", "Mbombela"), 2, "Bloemfontein is the judicial capital; Pretoria is administrative; Cape Town is legislative."),
                TriviaQuestion("In which ocean does Durban lie?", listOf("Atlantic Ocean", "Indian Ocean", "Southern Ocean", "Pacific Ocean"), 1, "Durban sits along the warm Indian Ocean coast."),
                TriviaQuestion("What is the world's largest terrestrial mammal found in Kruger National Park?", listOf("White Rhino", "African Elephant", "Hippopotamus", "Giraffe"), 1, "The African Bush Elephant is the largest land mammal.")
            )
            GameDifficulty.HARD -> listOf(
                TriviaQuestion("Which SA river is the longest river flowing entirely within or bordering South Africa?", listOf("Limpopo River", "Orange River", "Vaal River", "Tugela River"), 1, "The Orange (Gariep) River stretches about 2,200 km."),
                TriviaQuestion("What is the highest mountain peak in South Africa?", listOf("Thabana Ntlenyana", "Mafadi", "Champagne Castle", "Compassberg"), 1, "Mafadi in the Drakensberg reaches 3,450 m inside South Africa."),
                TriviaQuestion("In which year did the South African Rand replace the South African Pound?", listOf("1958", "1961", "1966", "1971"), 1, "The Rand was introduced on 14 February 1961."),
                TriviaQuestion("Which South African astronomer discovered Proxima Centauri in 1915?", listOf("Robert Innes", "Andrew Thackeray", "David Gill", "John Herschel"), 0, "Robert Innes discovered Proxima Centauri at the Union Observatory in Johannesburg.")
            )
        }
        val offset = daySeed() % pool.size
        return pool.indices.map { i -> pool[(i + offset) % pool.size] }
    }
}
