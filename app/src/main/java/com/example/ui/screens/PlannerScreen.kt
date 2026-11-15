package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.PersonalGoal
import com.example.data.model.PlannerTask
import com.example.data.model.SmartReminder
import com.example.notifications.NotificationHelper
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoNumberStyle
import com.example.util.SouthAfricaFormatters

private val ReminderCategories = listOf(
    "Rent", "Electricity", "Water", "Appointment", "School", "Work", "Personal", "Deadline"
)
private val GoalCategories = listOf(
    "Saving Money", "Employment", "Learning Skills", "Starting Business", "Productivity", "Personal Project"
)

@Composable
fun PlannerScreen(
    tasks: List<PlannerTask>,
    reminders: List<SmartReminder>,
    goals: List<PersonalGoal>,
    onAddTask: (String, String, String, String, String) -> Unit,
    onToggleTask: (PlannerTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddReminder: (String, String, String, String, Double, Boolean) -> Unit,
    onToggleReminder: (SmartReminder) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onAddGoal: (String, String, Double, Double, String, String) -> Unit,
    onUpdateGoalProgress: (PersonalGoal, Double) -> Unit,
    onDeleteGoal: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var taskFilter by remember { mutableStateOf("ALL") }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val completedTasks = tasks.count { it.completed }
    val completionPct = if (tasks.isEmpty()) 0 else ((completedTasks * 100f) / tasks.size).toInt()
    val filteredTasks = remember(tasks, taskFilter) {
        val today = SouthAfricaFormatters.todayIso()
        when (taskFilter) {
            "TODAY" -> tasks.filter { it.dueDateIso == today }
            "UNFINISHED" -> tasks.filter { !it.completed }
            "DONE" -> tasks.filter { it.completed }
            else -> tasks
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showTaskDialog = true
                        1 -> showReminderDialog = true
                        else -> showGoalDialog = true
                    }
                },
                modifier = Modifier.testTag("planner_add_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add planner item")
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
                Text("Plan, Reminders & Goals", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Stay organised with daily tasks, utility & bill reminders, and personal goals",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                val tabs = listOf("Tasks", "Reminders", "Goals")
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Productivity Stats", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "$completedTasks of ${tasks.size} tasks completed",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    "$completionPct%",
                                    style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL" to "All", "TODAY" to "Today", "UNFINISHED" to "Pending", "DONE" to "Done")
                                .forEach { (key, label) ->
                                    FilterChip(
                                        selected = taskFilter == key,
                                        onClick = { taskFilter = key },
                                        label = { Text(label) }
                                    )
                                }
                        }
                    }
                    if (filteredTasks.isEmpty()) {
                        item {
                            EmptyPlannerCard("No tasks in this view. Tap + to plan your next priority.")
                        }
                    } else {
                        items(filteredTasks, key = { it.id }) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onToggleTask(task) }) {
                                        Icon(
                                            imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Complete task",
                                            tint = if (task.completed) IncomeGreen else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        Text(
                                            text = "${task.category} • ${SouthAfricaFormatters.formatDisplayDate(task.dueDateIso)}${if (task.dueTime.isNotBlank()) " at ${task.dueTime}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = when (task.priority) {
                                            "HIGH" -> MaterialTheme.colorScheme.errorContainer
                                            "MEDIUM" -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = task.priority,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(onClick = { onDeleteTask(task.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete task")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (!NotificationHelper.hasNotificationPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "Enable notifications to receive bill & appointment alerts.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                        Text("Enable")
                                    }
                                }
                            }
                        }
                    }
                    val activeReminders = reminders.filter { !it.completed }
                    val completedReminders = reminders.filter { it.completed }
                    if (reminders.isEmpty()) {
                        item {
                            EmptyPlannerCard("No reminders yet. Schedule rent, prepaid electricity, water bills, school, or work events.")
                        }
                    } else {
                        items(activeReminders, key = { it.id }) { rem ->
                            ReminderItemCard(rem, onToggle = { onToggleReminder(rem) }, onDelete = { onDeleteReminder(rem.id) })
                        }
                        if (completedReminders.isNotEmpty()) {
                            item {
                                Text(
                                    "Completed History (${completedReminders.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(completedReminders, key = { it.id }) { rem ->
                                ReminderItemCard(rem, onToggle = { onToggleReminder(rem) }, onDelete = { onDeleteReminder(rem.id) })
                            }
                        }
                    }
                }

                2 -> {
                    if (goals.isEmpty()) {
                        item {
                            EmptyPlannerCard("No personal goals yet. Track savings, job search steps, new skills, or business milestones.")
                        }
                    } else {
                        items(goals, key = { it.id }) { goal ->
                            val pct = (goal.currentValue / goal.targetValue.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (goal.completed) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (goal.completed) {
                                                    Icon(
                                                        Icons.Default.EmojiEvents,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                                            }
                                            Text(
                                                "${goal.category}${if (goal.deadlineIso.isNotBlank()) " • Target ${SouthAfricaFormatters.formatDisplayDate(goal.deadlineIso)}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete goal")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${goal.currentValue.toInt()} / ${goal.targetValue.toInt()} ${goal.unitLabel}",
                                            style = MonoNumberStyle
                                        )
                                        Text("${(pct * 100).toInt()}%", style = MonoNumberStyle)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (goal.completed) {
                                        Text(
                                            "Achievement unlocked! Well done on reaching your goal.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IncomeGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        val step = if (goal.unitLabel == "ZAR") 100.0 else 1.0
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { onUpdateGoalProgress(goal, goal.currentValue + step) }
                                            ) {
                                                Text("+${step.toInt()} ${goal.unitLabel}")
                                            }
                                            Button(
                                                onClick = { onUpdateGoalProgress(goal, goal.targetValue) }
                                            ) {
                                                Text("Mark Achieved")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        AddTaskDialog(
            onDismiss = { showTaskDialog = false },
            onConfirm = { title, priority, dueDate, dueTime, cat ->
                onAddTask(title, priority, dueDate, dueTime, cat)
                showTaskDialog = false
            }
        )
    }
    if (showReminderDialog) {
        AddReminderDialog(
            onDismiss = { showReminderDialog = false },
            onConfirm = { title, cat, dateTime, recurrence, amount, notif ->
                onAddReminder(title, cat, dateTime, recurrence, amount, notif)
                showReminderDialog = false
            }
        )
    }
    if (showGoalDialog) {
        AddGoalDialog(
            onDismiss = { showGoalDialog = false },
            onConfirm = { title, cat, target, current, unit, deadline ->
                onAddGoal(title, cat, target, current, unit, deadline)
                showGoalDialog = false
            }
        )
    }
}

@Composable
private fun ReminderItemCard(
    rem: SmartReminder,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (rem.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Mark reminder done",
                    tint = if (rem.completed) IncomeGreen else MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rem.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (rem.completed) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    "${rem.category} • ${rem.recurrence} • ${SouthAfricaFormatters.formatDisplayDateTime(rem.dueDateTimeIso)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (rem.amountZar > 0.0) {
                Text(
                    SouthAfricaFormatters.formatZarCompact(rem.amountZar),
                    style = MonoNumberStyle,
                    color = ExpenseRed
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete reminder")
            }
        }
    }
}

@Composable
private fun EmptyPlannerCard(msg: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            msg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var dueDate by remember { mutableStateOf(SouthAfricaFormatters.todayIso()) }
    var dueTime by remember { mutableStateOf("09:00") }
    var category by remember { mutableStateOf("Work") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Daily Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Time") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Work, Study, Home)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) error = "Task title is required."
                else onConfirm(title, priority, dueDate.ifBlank { SouthAfricaFormatters.todayIso() }, dueTime, category.ifBlank { "Personal" })
            }) { Text("Add Task") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Double, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Rent") }
    var dateTime by remember { mutableStateOf("${SouthAfricaFormatters.todayIso()}T18:00") }
    var recurrence by remember { mutableStateOf("MONTHLY") }
    var amountText by remember { mutableStateOf("") }
    var notifEnabled by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Smart Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder (e.g., Prepaid Electricity)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderCategories.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                OutlinedTextField(
                    value = dateTime,
                    onValueChange = { dateTime = it },
                    label = { Text("Date & Time (YYYY-MM-DDTHH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ONCE", "DAILY", "WEEKLY", "MONTHLY").forEach { rec ->
                        FilterChip(selected = recurrence == rec, onClick = { recurrence = rec }, label = { Text(rec) })
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Optional Bill Amount (ZAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Push Notification Alert", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifEnabled, onCheckedChange = { notifEnabled = it })
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) error = "Title is required."
                else onConfirm(title, category, dateTime, recurrence, amountText.toDoubleOrNull() ?: 0.0, notifEnabled)
            }) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Saving Money") }
    var targetText by remember { mutableStateOf("5000") }
    var currentText by remember { mutableStateOf("0") }
    var unit by remember { mutableStateOf("ZAR") }
    var deadline by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Personal Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g., Emergency Fund)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                unit = if (cat == "Saving Money") "ZAR" else "steps"
                            },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        label = { Text("Current") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Target Date (optional YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val target = targetText.toDoubleOrNull()
                val current = currentText.toDoubleOrNull() ?: 0.0
                when {
                    title.isBlank() -> error = "Goal title is required."
                    target == null || target <= 0.0 -> error = "Target must be greater than 0."
                    else -> onConfirm(title, category, target, current, unit.ifBlank { "steps" }, deadline)
                }
            }) { Text("Create Goal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
