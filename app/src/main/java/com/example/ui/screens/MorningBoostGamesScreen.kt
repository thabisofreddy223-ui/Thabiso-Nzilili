package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BoostGameType
import com.example.data.model.GameDifficulty
import com.example.data.model.GameScoreRecord
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MorningBoostCatalog
import com.example.data.model.TriviaQuestion
import com.example.data.model.UserProfile
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoNumberStyle
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MorningBoostScreen(
    profile: UserProfile?,
    recentScores: List<GameScoreRecord>,
    leaderboard: List<LeaderboardEntry>,
    shareOnLeaderboard: Boolean,
    onBack: () -> Unit,
    onGameCompleted: (String, String, Int, Int) -> Unit,
    onNavigateToGoals: () -> Unit
) {
    var activeGame by remember { mutableStateOf<BoostGameType?>(null) }
    var difficulty by remember { mutableStateOf(GameDifficulty.EASY) }
    var completionSummary by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showLeaderboard by remember { mutableStateOf(false) }

    BackHandler {
        when {
            completionSummary != null -> {
                completionSummary = null
                activeGame = null
            }
            activeGame != null -> activeGame = null
            showLeaderboard -> showLeaderboard = false
            else -> onBack()
        }
    }

    if (completionSummary != null) {
        val (gameTitle, points) = completionSummary!!
        GameCompletionCelebration(
            gameTitle = gameTitle,
            pointsEarned = points,
            streak = (profile?.currentStreak ?: 0).coerceAtLeast(1),
            onCheckGoals = {
                completionSummary = null
                activeGame = null
                onNavigateToGoals()
            },
            onPlayAnother = {
                completionSummary = null
                activeGame = null
            }
        )
        return
    }

    val active = activeGame
    if (active != null) {
        val startMs = remember(active, difficulty) { System.currentTimeMillis() }
        val finishGame: (Int) -> Unit = { score ->
            val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).toInt().coerceAtLeast(1)
            onGameCompleted(active.id, difficulty.name, score, elapsedSec)
            completionSummary = active.title to score
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeGame = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to games")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(active.title, style = MaterialTheme.typography.titleLarge)
                    Text("Difficulty: ${difficulty.label}", style = MaterialTheme.typography.bodySmall)
                }
            }
            when (active) {
                BoostGameType.BRAIN_CHALLENGE -> QuizGameView(
                    questions = remember(difficulty) { MorningBoostCatalog.getBrainQuestions(difficulty) },
                    multiplier = difficulty.multiplier,
                    onComplete = finishGame
                )
                BoostGameType.DAILY_TRIVIA -> QuizGameView(
                    questions = remember(difficulty) { MorningBoostCatalog.getTriviaQuestions(difficulty) },
                    multiplier = difficulty.multiplier,
                    onComplete = finishGame
                )
                BoostGameType.WORD_OF_DAY -> WordOfDayGameView(
                    targetWord = remember(difficulty) { MorningBoostCatalog.getDailyWord(difficulty) },
                    multiplier = difficulty.multiplier,
                    onComplete = finishGame
                )
                BoostGameType.QUICK_MATHS -> QuickMathsGameView(
                    difficulty = difficulty,
                    onComplete = finishGame
                )
                BoostGameType.MEMORY_MATCH -> MemoryMatchGameView(
                    difficulty = difficulty,
                    onComplete = finishGame
                )
                BoostGameType.DAILY_PUZZLE -> SlidingPuzzleGameView(
                    difficulty = difficulty,
                    onComplete = finishGame
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text("Morning Boost", style = MaterialTheme.typography.headlineLarge)
                    Text("6 quick offline-ready brain warm-ups", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.height(145.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_morning_boost_banner),
                        contentDescription = "Morning Boost banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                            Text("${profile?.currentStreak ?: 0} Days", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary)
                            Text("Total: ${profile?.totalGamePoints ?: 0} pts • Best: ${profile?.bestScore ?: 0}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        OutlinedButton(
                            onClick = { showLeaderboard = !showLeaderboard },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showLeaderboard) "Games" else "Ranks")
                        }
                    }
                }
            }
        }

        if (showLeaderboard) {
            item {
                Text("Friend & Community Leaderboard", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (shareOnLeaderboard) "Your score is shared publicly (no email/PII shown)." else "You are currently hidden from public rankings (change in Settings).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (leaderboard.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Text("Play any Morning Boost game to post the first score!", modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(leaderboard.mapIndexed { i, e -> i + 1 to e }, key = { it.second.userId }) { (rank, entry) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#$rank", style = MonoNumberStyle, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(entry.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text("${entry.currentStreak} day streak • Best ${entry.bestScore}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("${entry.totalPoints} pts", style = MonoNumberStyle, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        } else {
            item {
                Text("Select Difficulty", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text("${diff.label} (${diff.multiplier}× pts)") }
                        )
                    }
                }
            }

            items(BoostGameType.entries) { game ->
                val icon = when (game) {
                    BoostGameType.BRAIN_CHALLENGE -> Icons.Default.Psychology
                    BoostGameType.WORD_OF_DAY -> Icons.Default.Spellcheck
                    BoostGameType.QUICK_MATHS -> Icons.Default.Calculate
                    BoostGameType.DAILY_TRIVIA -> Icons.Default.Public
                    BoostGameType.MEMORY_MATCH -> Icons.Default.Memory
                    BoostGameType.DAILY_PUZZLE -> Icons.Default.Extension
                }
                val gameBest = recentScores.filter { it.gameId == game.id }.maxOfOrNull { it.score } ?: 0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeGame = game }
                        .testTag("game_card_${game.id.lowercase()}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.title, style = MaterialTheme.typography.titleMedium)
                            Text(game.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${game.durationLabel} • Personal Best: $gameBest pts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { activeGame = game }) { Text("Play") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizGameView(
    questions: List<TriviaQuestion>,
    multiplier: Int,
    onComplete: (Int) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var selectedOpt by remember { mutableIntStateOf(-1) }
    var correctCount by remember { mutableIntStateOf(0) }
    val q = questions[index]

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Question ${index + 1} of ${questions.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Text(q.prompt, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(18.dp))
        }
        q.options.forEachIndexed { optIdx, text ->
            val isChosen = selectedOpt == optIdx
            val isCorrect = optIdx == q.correctIndex
            val container = when {
                selectedOpt == -1 -> MaterialTheme.colorScheme.surfaceContainer
                isCorrect -> MaterialTheme.colorScheme.primaryContainer
                isChosen -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = selectedOpt == -1) {
                        selectedOpt = optIdx
                        if (optIdx == q.correctIndex) correctCount++
                    },
                colors = CardDefaults.cardColors(containerColor = container),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
            }
        }
        if (selectedOpt != -1) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                Text(q.explanation, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
            }
            Button(
                onClick = {
                    if (index < questions.lastIndex) {
                        index++
                        selectedOpt = -1
                    } else {
                        onComplete((correctCount * 25 + 15) * multiplier)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (index < questions.lastIndex) "Next Question" else "Finish Challenge")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordOfDayGameView(
    targetWord: String,
    multiplier: Int,
    onComplete: (Int) -> Unit
) {
    val guesses = remember { mutableStateListOf<String>() }
    var currentGuess by remember { mutableStateOf("") }
    var showHint by remember { mutableStateOf(false) }
    val solved = guesses.lastOrNull() == targetWord
    val finished = solved || guesses.size >= 6

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Guess the 5-letter word (${guesses.size}/6)", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = { showHint = true }) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hint")
            }
        }
        if (showHint) {
            Text("First letter is '${targetWord.first()}'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        for (row in 0 until 6) {
            val word = when {
                row < guesses.size -> guesses[row]
                row == guesses.size -> currentGuess
                else -> ""
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 5) {
                    val ch = word.getOrNull(col)?.toString() ?: ""
                    val submitted = row < guesses.size
                    val bg = when {
                        !submitted -> MaterialTheme.colorScheme.surfaceContainer
                        ch == targetWord[col].toString() -> IncomeGreen
                        ch.isNotEmpty() && targetWord.contains(ch) -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val fg = if (submitted && (ch == targetWord[col].toString() || targetWord.contains(ch))) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ch, style = MaterialTheme.typography.titleLarge, color = fg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (finished) {
            val pts = if (solved) ((7 - guesses.size) * 20) * multiplier else 15 * multiplier
            Text(
                if (solved) "Brilliant! Word: $targetWord" else "Good try! Today's word was $targetWord",
                style = MaterialTheme.typography.titleMedium,
                color = if (solved) IncomeGreen else MaterialTheme.colorScheme.primary
            )
            Button(onClick = { onComplete(pts) }, modifier = Modifier.fillMaxWidth()) {
                Text("Collect $pts Points")
            }
        } else {
            val keys = "QWERTYUIOPASDFGHJKLZXCVBNM".toList()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                keys.forEach { letter ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(width = 34.dp, height = 42.dp)
                            .clickable {
                                if (currentGuess.length < 5) currentGuess += letter
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(letter.toString(), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { if (currentGuess.isNotEmpty()) currentGuess = currentGuess.dropLast(1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
                Button(
                    onClick = {
                        if (currentGuess.length == 5) {
                            guesses.add(currentGuess)
                            currentGuess = ""
                        }
                    },
                    enabled = currentGuess.length == 5,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
private fun QuickMathsGameView(
    difficulty: GameDifficulty,
    onComplete: (Int) -> Unit
) {
    val totalQ = 8
    var qIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(if (difficulty == GameDifficulty.EASY) 120 else 75) }

    val problem = remember(qIndex, difficulty) {
        val maxN = when (difficulty) {
            GameDifficulty.EASY -> 20
            GameDifficulty.MEDIUM -> 50
            GameDifficulty.HARD -> 95
        }
        val a = Random.nextInt(3, maxN)
        val b = Random.nextInt(2, maxN / 2 + 2)
        when (qIndex % 3) {
            0 -> "R $a + R $b = ?" to (a + b)
            1 -> "$a × ${(b % 9) + 2} = ?" to (a * ((b % 9) + 2))
            else -> "${a + b} - $b = ?" to a
        }
    }

    LaunchedEffect(Unit) {
        while (timeLeft > 0 && qIndex < totalQ) {
            delay(1000L)
            timeLeft--
        }
        if (timeLeft <= 0 && qIndex < totalQ) {
            onComplete((score + 10) * difficulty.multiplier)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question ${qIndex + 1}/$totalQ", style = MaterialTheme.typography.titleSmall)
            Text("Time: ${timeLeft}s", style = MonoNumberStyle, color = MaterialTheme.colorScheme.tertiary)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(problem.first, style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (input.isEmpty()) "?" else input,
                    style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        val pad = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("C", "0", "OK"))
        pad.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            when (key) {
                                "C" -> input = ""
                                "OK" -> {
                                    if (input.toIntOrNull() == problem.second) score += 15
                                    input = ""
                                    if (qIndex < totalQ - 1) qIndex++
                                    else onComplete((score + 20) * difficulty.multiplier)
                                }
                                else -> if (input.length < 5) input += key
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(key, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryMatchGameView(
    difficulty: GameDifficulty,
    onComplete: (Int) -> Unit
) {
    val pairCount = when (difficulty) {
        GameDifficulty.EASY -> 6
        GameDifficulty.MEDIUM -> 8
        GameDifficulty.HARD -> 10
    }
    val iconsPool = listOf(
        Icons.Default.WbSunny, Icons.Default.Home, Icons.Default.Star, Icons.Default.Favorite,
        Icons.Default.Bolt, Icons.Default.Coffee, Icons.Default.ShoppingCart, Icons.Default.LocalFlorist,
        Icons.Default.EmojiEvents, Icons.Default.Public
    )
    val deck = remember(difficulty) {
        (0 until pairCount).flatMap { listOf(it, it) }.shuffled()
    }
    val matched = remember { mutableStateListOf<Int>() }
    var firstIdx by remember { mutableIntStateOf(-1) }
    var secondIdx by remember { mutableIntStateOf(-1) }
    var moves by remember { mutableIntStateOf(0) }

    LaunchedEffect(secondIdx) {
        if (secondIdx != -1 && firstIdx != -1) {
            delay(550L)
            if (deck[firstIdx] == deck[secondIdx]) {
                matched.add(firstIdx)
                matched.add(secondIdx)
            }
            firstIdx = -1
            secondIdx = -1
        }
    }

    val cols = if (difficulty == GameDifficulty.EASY) 3 else 4
    val allMatched = matched.size == deck.size

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Matched: ${matched.size / 2} / $pairCount", style = MaterialTheme.typography.titleSmall)
            Text("Moves: $moves", style = MonoNumberStyle)
        }
        deck.chunked(cols).forEachIndexed { rowIdx, rowCards ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowCards.forEachIndexed { colIdx, iconId ->
                    val cardIdx = rowIdx * cols + colIdx
                    val isFaceUp = cardIdx in matched || cardIdx == firstIdx || cardIdx == secondIdx
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable(enabled = !isFaceUp && secondIdx == -1) {
                                if (firstIdx == -1) {
                                    firstIdx = cardIdx
                                } else {
                                    secondIdx = cardIdx
                                    moves++
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (cardIdx in matched) MaterialTheme.colorScheme.primaryContainer
                            else if (isFaceUp) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFaceUp) iconsPool[iconId] else Icons.Default.QuestionMark,
                                contentDescription = if (isFaceUp) "Matched symbol" else "Hidden card",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
        if (allMatched) {
            val pts = ((140 - moves * 3).coerceAtLeast(30)) * difficulty.multiplier
            Button(onClick = { onComplete(pts) }, modifier = Modifier.fillMaxWidth()) {
                Text("All Pairs Found! Claim $pts pts")
            }
        }
    }
}

@Composable
private fun SlidingPuzzleGameView(
    difficulty: GameDifficulty,
    onComplete: (Int) -> Unit
) {
    val gridSize = if (difficulty == GameDifficulty.HARD) 4 else 3
    val totalTiles = gridSize * gridSize
    val solvedBoard = remember(gridSize) { (1 until totalTiles).toList() + 0 }
    val board = remember(difficulty) {
        val list = solvedBoard.toMutableList()
        var emptyPos = list.indexOf(0)
        repeat(if (difficulty == GameDifficulty.EASY) 18 else 45) {
            val r = emptyPos / gridSize
            val c = emptyPos % gridSize
            val neighbors = buildList {
                if (r > 0) add(emptyPos - gridSize)
                if (r < gridSize - 1) add(emptyPos + gridSize)
                if (c > 0) add(emptyPos - 1)
                if (c < gridSize - 1) add(emptyPos + 1)
            }
            val swap = neighbors.random()
            list[emptyPos] = list[swap]
            list[swap] = 0
            emptyPos = swap
        }
        mutableStateListOf<Int>().apply { addAll(list) }
    }
    var moves by remember { mutableIntStateOf(0) }
    val isSolved = board.toList() == solvedBoard

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Arrange numbers 1 to ${totalTiles - 1}", style = MaterialTheme.typography.titleSmall)
            Text("Moves: $moves", style = MonoNumberStyle)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (r in 0 until gridSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until gridSize) {
                        val idx = r * gridSize + c
                        val value = board[idx]
                        val emptyIdx = board.indexOf(0)
                        val er = emptyIdx / gridSize
                        val ec = emptyIdx % gridSize
                        val canSlide = (kotlin.math.abs(r - er) + kotlin.math.abs(c - ec)) == 1
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable(enabled = value != 0 && canSlide && !isSolved) {
                                    board[emptyIdx] = value
                                    board[idx] = 0
                                    moves++
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (value == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                else if (value == idx + 1) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (value != 0) {
                                    Text(
                                        value.toString(),
                                        style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isSolved) {
            val pts = ((150 - moves * 2).coerceAtLeast(35)) * difficulty.multiplier
            Button(onClick = { onComplete(pts) }, modifier = Modifier.fillMaxWidth()) {
                Text("Puzzle Solved! Claim $pts pts")
            }
        }
    }
}

@Composable
private fun GameCompletionCelebration(
    gameTitle: String,
    pointsEarned: Int,
    streak: Int,
    onCheckGoals: () -> Unit,
    onPlayAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = IncomeGreen,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Great Start to Your Day!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You completed $gameTitle and earned +$pointsEarned points! Your Morning Boost streak is $streak day${if (streak == 1) "" else "s"}.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Your mind is sharp—take a moment to check your Daily Goals and top priorities for today!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCheckGoals,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Check My Daily Goals")
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onPlayAnother,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Back to Morning Boost")
        }
    }
}
