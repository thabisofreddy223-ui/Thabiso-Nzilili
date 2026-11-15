package com.example.data.repository

import com.example.data.model.MoneyTransaction
import com.example.data.model.PersonalGoal
import com.example.data.model.PlannerTask
import com.example.data.model.ShoppingItem
import com.example.data.model.SmartReminder
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val transactions: List<MoneyTransaction>,
    val tasks: List<PlannerTask>,
    val reminders: List<SmartReminder>,
    val shoppingItems: List<ShoppingItem>,
    val goals: List<PersonalGoal>
)

object BackupManager {
    fun exportToJson(
        transactions: List<MoneyTransaction>,
        tasks: List<PlannerTask>,
        reminders: List<SmartReminder>,
        shoppingItems: List<ShoppingItem>,
        goals: List<PersonalGoal>
    ): String {
        val root = JSONObject()
        root.put("app", "LIFEHUB SA")
        root.put("version", 1)

        val txArr = JSONArray()
        transactions.forEach { tx ->
            txArr.put(JSONObject().apply {
                put("title", tx.title)
                put("amount", tx.amount)
                put("type", tx.type)
                put("category", tx.category)
                put("dateIso", tx.dateIso)
                put("notes", tx.notes)
            })
        }
        root.put("transactions", txArr)

        val taskArr = JSONArray()
        tasks.forEach { t ->
            taskArr.put(JSONObject().apply {
                put("title", t.title)
                put("priority", t.priority)
                put("dueDateIso", t.dueDateIso)
                put("dueTime", t.dueTime)
                put("category", t.category)
                put("completed", t.completed)
            })
        }
        root.put("tasks", taskArr)

        val remArr = JSONArray()
        reminders.forEach { r ->
            remArr.put(JSONObject().apply {
                put("title", r.title)
                put("category", r.category)
                put("dueDateTimeIso", r.dueDateTimeIso)
                put("recurrence", r.recurrence)
                put("amountZar", r.amountZar)
                put("completed", r.completed)
                put("notificationsEnabled", r.notificationsEnabled)
            })
        }
        root.put("reminders", remArr)

        val shopArr = JSONArray()
        shoppingItems.forEach { s ->
            shopArr.put(JSONObject().apply {
                put("name", s.name)
                put("quantity", s.quantity)
                put("unit", s.unit)
                put("estimatedPrice", s.estimatedPrice)
                put("shopName", s.shopName)
                put("category", s.category)
                put("purchased", s.purchased)
                put("isFrequent", s.isFrequent)
            })
        }
        root.put("shoppingItems", shopArr)

        val goalArr = JSONArray()
        goals.forEach { g ->
            goalArr.put(JSONObject().apply {
                put("title", g.title)
                put("category", g.category)
                put("targetValue", g.targetValue)
                put("currentValue", g.currentValue)
                put("unitLabel", g.unitLabel)
                put("deadlineIso", g.deadlineIso)
                put("completed", g.completed)
            })
        }
        root.put("goals", goalArr)

        return root.toString(2)
    }

    fun parseFromJson(rawJson: String): BackupPayload {
        val root = JSONObject(rawJson)
        val transactions = mutableListOf<MoneyTransaction>()
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            val amount = o.optDouble("amount", 0.0)
            if (amount > 0.0) {
                transactions.add(
                    MoneyTransaction(
                        title = o.optString("title", "Imported Entry"),
                        amount = amount,
                        type = o.optString("type", "EXPENSE"),
                        category = o.optString("category", "Other"),
                        dateIso = o.optString("dateIso", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        val tasks = mutableListOf<PlannerTask>()
        val taskArr = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until taskArr.length()) {
            val o = taskArr.getJSONObject(i)
            tasks.add(
                PlannerTask(
                    title = o.optString("title", "Task"),
                    priority = o.optString("priority", "MEDIUM"),
                    dueDateIso = o.optString("dueDateIso", ""),
                    dueTime = o.optString("dueTime", ""),
                    category = o.optString("category", "Personal"),
                    completed = o.optBoolean("completed", false)
                )
            )
        }

        val reminders = mutableListOf<SmartReminder>()
        val remArr = root.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until remArr.length()) {
            val o = remArr.getJSONObject(i)
            reminders.add(
                SmartReminder(
                    title = o.optString("title", "Reminder"),
                    category = o.optString("category", "Personal"),
                    dueDateTimeIso = o.optString("dueDateTimeIso", ""),
                    recurrence = o.optString("recurrence", "ONCE"),
                    amountZar = o.optDouble("amountZar", 0.0),
                    completed = o.optBoolean("completed", false),
                    notificationsEnabled = o.optBoolean("notificationsEnabled", true)
                )
            )
        }

        val shopping = mutableListOf<ShoppingItem>()
        val shopArr = root.optJSONArray("shoppingItems") ?: JSONArray()
        for (i in 0 until shopArr.length()) {
            val o = shopArr.getJSONObject(i)
            shopping.add(
                ShoppingItem(
                    name = o.optString("name", "Item"),
                    quantity = o.optDouble("quantity", 1.0).coerceAtLeast(1.0),
                    unit = o.optString("unit", "x"),
                    estimatedPrice = o.optDouble("estimatedPrice", 0.0).coerceAtLeast(0.0),
                    shopName = o.optString("shopName", "General"),
                    category = o.optString("category", "Groceries"),
                    purchased = o.optBoolean("purchased", false),
                    isFrequent = o.optBoolean("isFrequent", false)
                )
            )
        }

        val goals = mutableListOf<PersonalGoal>()
        val goalArr = root.optJSONArray("goals") ?: JSONArray()
        for (i in 0 until goalArr.length()) {
            val o = goalArr.getJSONObject(i)
            goals.add(
                PersonalGoal(
                    title = o.optString("title", "Goal"),
                    category = o.optString("category", "Saving Money"),
                    targetValue = o.optDouble("targetValue", 100.0).coerceAtLeast(1.0),
                    currentValue = o.optDouble("currentValue", 0.0).coerceAtLeast(0.0),
                    unitLabel = o.optString("unitLabel", "ZAR"),
                    deadlineIso = o.optString("deadlineIso", ""),
                    completed = o.optBoolean("completed", false)
                )
            )
        }

        return BackupPayload(transactions, tasks, reminders, shopping, goals)
    }
}
