package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MoneyTransaction
import com.example.data.model.MorningBoostCatalog
import com.example.data.model.PersonalGoal
import com.example.data.model.PlannerTask
import com.example.data.model.SavedQuote
import com.example.data.model.ShoppingItem
import com.example.data.model.SmartReminder
import com.example.data.model.UserProfile
import com.example.data.repository.DashboardConfig
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoNumberStyle
import com.example.util.SouthAfricaFormatters
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    displayName: String,
    profile: UserProfile?,
    dashboardConfig: DashboardConfig,
    transactions: List<MoneyTransaction>,
    tasks: List<PlannerTask>,
    reminders: List<SmartReminder>,
    shoppingItems: List<ShoppingItem>,
    goals: List<PersonalGoal>,
    savedQuotes: List<SavedQuote>,
    onUpdateDashboardConfig: (DashboardConfig) -> Unit,
    onToggleTask: (PlannerTask) -> Unit,
    onToggleSaveQuote: (String, String, String, String, Boolean) -> Unit,
    onNavigateToMoney: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToMorningBoost: () -> Unit,
    onNavigateToMotivation: () -> Unit
) {
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var currentDateTimeText by remember { mutableStateOf(SouthAfricaFormatters.currentDateTimeDisplay()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentDateTimeText = SouthAfricaFormatters.currentDateTimeDisplay()
            delay(30_000L)
        }
    }

    val todayIso = remember { SouthAfricaFormatters.todayIso() }
    val monthPrefix = remember { SouthAfricaFormatters.currentMonthPrefix() }
    val todaySpent = remember(transactions, todayIso) {
        transactions.filter { it.type == "EXPENSE" && it.dateIso == todayIso }.sumOf { it.amount }
    }
    val monthIncome = remember(transactions, monthPrefix) {
        transactions.filter { it.type == "INCOME" && it.dateIso.startsWith(monthPrefix) }.sumOf { it.amount }
    }
    val monthExpense = remember(transactions, monthPrefix) {
        transactions.filter { it.type == "EXPENSE" && it.dateIso.startsWith(monthPrefix) }.sumOf { it.amount }
    }

    val pendingTasks = remember(tasks) { tasks.filter { !it.completed } }
    val upcomingReminders = remember(reminders) { reminders.filter { !it.completed } }
    val pendingShopping = remember(shoppingItems) { shoppingItems.filter { !it.purchased } }
    val shoppingTotal = remember(pendingShopping) {
        pendingShopping.sumOf { it.estimatedPrice * it.quantity }
    }
    val dailyQuote = remember { MorningBoostCatalog.dailyQuote() }
    val isQuoteSaved = savedQuotes.any { it.quoteId == dailyQuote.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${SouthAfricaFormatters.formatGreeting()}, ${displayName.substringBefore(" ")}",
                        style = MaterialTheme.typography.headlineLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentDateTimeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    IconButton(
                        onClick = { showCustomizeDialog = true },
                        modifier = Modifier.testTag("customize_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize dashboard widgets",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Important Notifications Banner
        if (upcomingReminders.isNotEmpty() || pendingTasks.any { it.priority == "HIGH" }) {
            item {
                val alertCount = upcomingReminders.size + pendingTasks.count { it.priority == "HIGH" }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPlan() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$alertCount Important Item${if (alertCount == 1) "" else "s"} Need Attention",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val firstItem = upcomingReminders.firstOrNull()?.title
                                ?: pendingTasks.firstOrNull { it.priority == "HIGH" }?.title
                                ?: ""
                            Text(
                                text = "Next: $firstItem",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Planner",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        if (dashboardConfig.showMoney) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMoney() }
                        .testTag("dashboard_money_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Money Summary (ZAR)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                text = "Today: ${SouthAfricaFormatters.formatZar(todaySpent)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = SouthAfricaFormatters.formatZar(monthIncome - monthExpense),
                            style = MonoNumberStyle.copy(
                                fontSize = MaterialTheme.typography.displaySmall.fontSize
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Monthly Net Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = SouthAfricaFormatters.formatZarCompact(monthIncome),
                                    style = MonoNumberStyle,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = SouthAfricaFormatters.formatZarCompact(monthExpense),
                                    style = MonoNumberStyle,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dashboardConfig.showMorningBoost) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMorningBoost() }
                        .testTag("dashboard_morning_boost_card"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Box(modifier = Modifier.height(165.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_morning_boost_banner),
                            contentDescription = "Morning Boost daily mini-games",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "MORNING BOOST • ${profile?.currentStreak ?: 0} DAY STREAK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Best: ${profile?.bestScore ?: 0} pts",
                                    style = MonoNumberStyle,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "6 Quick Brain Games",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = "Word, Maths, Trivia, Memory & Puzzles (2–5 min)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onNavigateToMorningBoost,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dashboardConfig.showTasks) {
            item {
                DashboardSectionCard(
                    title = "Today's Tasks (${pendingTasks.size} pending)",
                    actionLabel = "Planner",
                    onActionClick = onNavigateToPlan
                ) {
                    if (pendingTasks.isEmpty()) {
                        Text(
                            text = "All caught up! Add tasks in your Daily Planner to stay focused.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        pendingTasks.take(3).forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleTask(task) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Toggle task",
                                    tint = if (task.completed) IncomeGreen else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${task.category} • ${task.priority}${if (task.dueTime.isNotBlank()) " • ${task.dueTime}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dashboardConfig.showReminders) {
            item {
                DashboardSectionCard(
                    title = "Upcoming Bills & Reminders",
                    actionLabel = "View all",
                    onActionClick = onNavigateToPlan
                ) {
                    if (upcomingReminders.isEmpty()) {
                        Text(
                            text = "No upcoming bills or deadlines. Schedule rent, electricity, or water reminders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        upcomingReminders.take(3).forEach { rem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(rem.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${rem.category} • ${SouthAfricaFormatters.formatDisplayDateTime(rem.dueDateTimeIso)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (rem.amountZar > 0.0) {
                                    Text(
                                        text = SouthAfricaFormatters.formatZarCompact(rem.amountZar),
                                        style = MonoNumberStyle,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dashboardConfig.showShopping) {
            item {
                DashboardSectionCard(
                    title = "Shopping Basket",
                    actionLabel = "Open List",
                    onActionClick = onNavigateToShop
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${pendingShopping.size} item${if (pendingShopping.size == 1) "" else "s"} to buy",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                val topShops = pendingShopping.map { it.shopName }.distinct().take(3).joinToString(", ")
                                Text(
                                    text = if (topShops.isBlank()) "Ready for your next shop" else topShops,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = SouthAfricaFormatters.formatZar(shoppingTotal),
                            style = MonoNumberStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (dashboardConfig.showGoals) {
            item {
                DashboardSectionCard(
                    title = "Personal Goals",
                    actionLabel = "Goals",
                    onActionClick = onNavigateToPlan
                ) {
                    if (goals.isEmpty()) {
                        Text(
                            text = "Set a savings, learning, career, or business goal to track daily progress.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        goals.take(2).forEach { goal ->
                            val pct = (goal.currentValue / goal.targetValue).toFloat().coerceIn(0f, 1f)
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(goal.title, style = MaterialTheme.typography.titleSmall)
                                    }
                                    Text(
                                        text = "${(pct * 100).toInt()}%",
                                        style = MonoNumberStyle,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dashboardConfig.showMotivation) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMotivation() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAILY MOTIVATION",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    onToggleSaveQuote(
                                        dailyQuote.id,
                                        dailyQuote.text,
                                        dailyQuote.author,
                                        dailyQuote.category,
                                        isQuoteSaved
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (isQuoteSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Save motivational quote",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Text(
                            text = "\"${dailyQuote.text}\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "— ${dailyQuote.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Today's Challenge: ${MorningBoostCatalog.dailyChallengePrompt()}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomizeDialog) {
        CustomizeDashboardDialog(
            current = dashboardConfig,
            onDismiss = { showCustomizeDialog = false },
            onSave = {
                onUpdateDashboardConfig(it)
                showCustomizeDialog = false
            }
        )
    }
}

@Composable
private fun DashboardSectionCard(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onActionClick) {
                    Text(actionLabel)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun CustomizeDashboardDialog(
    current: DashboardConfig,
    onDismiss: () -> Unit,
    onSave: (DashboardConfig) -> Unit
) {
    var draft by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Dashboard") },
        text = {
            Column {
                WidgetToggleRow("Money Summary (ZAR)", draft.showMoney) {
                    draft = draft.copy(showMoney = it)
                }
                WidgetToggleRow("Morning Boost Games", draft.showMorningBoost) {
                    draft = draft.copy(showMorningBoost = it)
                }
                WidgetToggleRow("Today's Tasks", draft.showTasks) {
                    draft = draft.copy(showTasks = it)
                }
                WidgetToggleRow("Upcoming Bills & Reminders", draft.showReminders) {
                    draft = draft.copy(showReminders = it)
                }
                WidgetToggleRow("Shopping Basket", draft.showShopping) {
                    draft = draft.copy(showShopping = it)
                }
                WidgetToggleRow("Personal Goals", draft.showGoals) {
                    draft = draft.copy(showGoals = it)
                }
                WidgetToggleRow("Daily Motivation", draft.showMotivation) {
                    draft = draft.copy(showMotivation = it)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(draft) }) { Text("Save Layout") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun WidgetToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
