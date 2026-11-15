package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetLimit
import com.example.data.model.MoneyTransaction
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoNumberStyle
import com.example.util.SouthAfricaFormatters

private val MoneyCategories = listOf(
    "Groceries", "Transport", "Electricity", "Water", "Rent",
    "Data & Airtime", "Salary", "Side Hustle", "Savings", "Other"
)

@Composable
fun MoneyScreen(
    transactions: List<MoneyTransaction>,
    budgets: List<BudgetLimit>,
    savingsTargetZar: Double,
    onAddTransaction: (String, Double, String, String, String, String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpsertBudget: (String, Double, String) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var filterType by remember { mutableStateOf("ALL") }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    val todayIso = remember { SouthAfricaFormatters.todayIso() }
    val monthPrefix = remember { SouthAfricaFormatters.currentMonthPrefix() }

    val totalIncome = remember(transactions, monthPrefix) {
        transactions.filter { it.type == "INCOME" && it.dateIso.startsWith(monthPrefix) }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions, monthPrefix) {
        transactions.filter { it.type == "EXPENSE" && it.dateIso.startsWith(monthPrefix) }.sumOf { it.amount }
    }
    val totalSavingsLogged = remember(transactions) {
        transactions.filter { it.category == "Savings" }.sumOf { it.amount }
    }
    val weeklyExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" && SouthAfricaFormatters.isWithinLast7Days(it.dateIso) }
            .sumOf { it.amount }
    }

    val filteredTx = remember(transactions, filterType) {
        when (filterType) {
            "INCOME" -> transactions.filter { it.type == "INCOME" }
            "EXPENSE" -> transactions.filter { it.type == "EXPENSE" }
            else -> transactions
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 1) showAddBudgetDialog = true else showAddTxDialog = true
                },
                modifier = Modifier.testTag("money_add_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTab == 1) "Add budget" else "Add transaction"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Money Manager (ZAR)", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Personal cashflow, daily & monthly budgets, and savings progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Monthly Income", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    SouthAfricaFormatters.formatZar(totalIncome),
                                    style = MonoNumberStyle,
                                    color = IncomeGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Monthly Expenses", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    SouthAfricaFormatters.formatZar(totalExpense),
                                    style = MonoNumberStyle,
                                    color = ExpenseRed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val target = savingsTargetZar.coerceAtLeast(1.0)
                        val savingsRatio = (totalSavingsLogged / target).toFloat().coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Savings Tracker", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${SouthAfricaFormatters.formatZarCompact(totalSavingsLogged)} / ${SouthAfricaFormatters.formatZarCompact(target)}",
                                style = MonoNumberStyle
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { savingsRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            item {
                val tabs = listOf("Ledger", "Budgets", "Reports")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL" to "All", "INCOME" to "Income", "EXPENSE" to "Expenses").forEach { (key, label) ->
                                FilterChip(
                                    selected = filterType == key,
                                    onClick = { filterType = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    if (filteredTx.isEmpty()) {
                        item {
                            EmptyMoneyCard("No transactions recorded yet. Tap + to record income or expenses in Rand (ZAR).")
                        }
                    } else {
                        items(filteredTx, key = { it.id }) { tx ->
                            val isIncome = tx.type == "INCOME"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isIncome) IncomeGreen else ExpenseRed
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${tx.category} • ${SouthAfricaFormatters.formatDisplayDate(tx.dateIso)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (tx.notes.isNotBlank()) {
                                            Text(
                                                tx.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = (if (isIncome) "+" else "-") + SouthAfricaFormatters.formatZar(tx.amount),
                                        style = MonoNumberStyle,
                                        color = if (isIncome) IncomeGreen else ExpenseRed
                                    )
                                    IconButton(onClick = { onDeleteTransaction(tx.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete transaction")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (budgets.isEmpty()) {
                        item {
                            EmptyMoneyCard("No daily or monthly budgets set yet. Tap + to create a category limit in ZAR.")
                        }
                    } else {
                        items(budgets, key = { it.id }) { budget ->
                            val spent = transactions
                                .filter {
                                    it.type == "EXPENSE" &&
                                        it.category == budget.category &&
                                        if (budget.period == "DAILY") it.dateIso == todayIso
                                        else it.dateIso.startsWith(monthPrefix)
                                }
                                .sumOf { it.amount }
                            val ratio = (spent / budget.limitAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                            val overBudget = spent > budget.limitAmount

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(budget.category, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${budget.period.lowercase().replaceFirstChar { it.uppercase() }} Budget",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDeleteBudget(budget.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove budget")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Spent: ${SouthAfricaFormatters.formatZar(spent)}",
                                            style = MonoNumberStyle,
                                            color = if (overBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Limit: ${SouthAfricaFormatters.formatZar(budget.limitAmount)}",
                                            style = MonoNumberStyle
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        color = if (overBudget) ExpenseRed else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    if (overBudget) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Over budget by ${SouthAfricaFormatters.formatZar(spent - budget.limitAmount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Spending Overview", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Last 7 Days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(SouthAfricaFormatters.formatZar(weeklyExpense), style = MonoNumberStyle)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("This Month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(SouthAfricaFormatters.formatZar(totalExpense), style = MonoNumberStyle)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        val breakdown = remember(transactions, monthPrefix) {
                            transactions
                                .filter { it.type == "EXPENSE" && it.dateIso.startsWith(monthPrefix) }
                                .groupBy { it.category }
                                .mapValues { (_, list) -> list.sumOf { it.amount } }
                                .entries
                                .sortedByDescending { it.value }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Where Your Money Goes (This Month)", style = MaterialTheme.typography.titleMedium)
                                if (breakdown.isEmpty()) {
                                    Text(
                                        "Log monthly expenses to see category breakdowns.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val maxVal = breakdown.maxOf { it.value }.coerceAtLeast(1.0)
                                    val barColor = MaterialTheme.colorScheme.primary
                                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    breakdown.forEach { (cat, sum) ->
                                        val pct = ((sum / totalExpense.coerceAtLeast(1.0)) * 100).toInt()
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("$cat ($pct%)", style = MaterialTheme.typography.bodyMedium)
                                                Text(SouthAfricaFormatters.formatZar(sum), style = MonoNumberStyle)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Canvas(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(10.dp)
                                            ) {
                                                drawRoundRect(
                                                    color = trackColor,
                                                    cornerRadius = CornerRadius(8f, 8f),
                                                    size = size
                                                )
                                                val fillW = (sum / maxVal).toFloat().coerceIn(0.03f, 1f) * size.width
                                                drawRoundRect(
                                                    color = barColor,
                                                    cornerRadius = CornerRadius(8f, 8f),
                                                    size = Size(fillW, size.height)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Private manual ZAR ledger. LIFEHUB SA never fakes bank connections or external account verification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTxDialog = false },
            onConfirm = { title, amount, type, category, dateIso, notes ->
                onAddTransaction(title, amount, type, category, dateIso, notes)
                showAddTxDialog = false
            }
        )
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { category, limit, period ->
                onUpsertBudget(category, limit, period)
                showAddBudgetDialog = false
            }
        )
    }
}

@Composable
private fun EmptyMoneyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf("Groceries") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Transaction (ZAR)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = {
                            type = "INCOME"
                            if (category == "Groceries") category = "Salary"
                        },
                        label = { Text("Income") }
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description (e.g., Checkers, Salary)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (R)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MoneyCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.replace(",", ".").toDoubleOrNull()
                    when {
                        title.isBlank() -> error = "Please enter a description."
                        amt == null || amt <= 0.0 -> error = "Enter a valid ZAR amount greater than 0."
                        else -> onConfirm(title, amt, type, category, SouthAfricaFormatters.todayIso(), notes)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var category by remember { mutableStateOf("Groceries") }
    var limitText by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("MONTHLY") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Category Budget (ZAR)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = period == "DAILY",
                        onClick = { period = "DAILY" },
                        label = { Text("Daily") }
                    )
                    FilterChip(
                        selected = period == "MONTHLY",
                        onClick = { period = "MONTHLY" },
                        label = { Text("Monthly") }
                    )
                }
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Budget Limit (R)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MoneyCategories.filter { it != "Salary" && it != "Side Hustle" }.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.replace(",", ".").toDoubleOrNull()
                    if (limit == null || limit <= 0.0) {
                        error = "Enter a valid limit above R 0."
                    } else {
                        onConfirm(category, limit, period)
                    }
                }
            ) { Text("Save Budget") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
