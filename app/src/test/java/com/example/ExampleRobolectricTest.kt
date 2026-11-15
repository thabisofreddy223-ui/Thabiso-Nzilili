package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MoneyTransaction
import com.example.data.model.PersonalGoal
import com.example.data.model.PlannerTask
import com.example.data.model.ShoppingItem
import com.example.data.model.SmartReminder
import com.example.data.repository.BackupManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LIFEHUB SA", appName)
    }

    @Test
    fun `backupManager round-trips all personal records`() {
        val tx = listOf(MoneyTransaction(title = "Rent", amount = 6500.0, type = "EXPENSE", category = "Rent", dateIso = "2026-11-15"))
        val tasks = listOf(PlannerTask(title = "Check budget", priority = "HIGH", dueDateIso = "2026-11-15", category = "Finance"))
        val rems = listOf(SmartReminder(title = "City Power", category = "Electricity", dueDateTimeIso = "2026-11-20T09:00", recurrence = "MONTHLY", amountZar = 500.0))
        val shop = listOf(ShoppingItem(name = "Milk 2L", quantity = 2.0, unit = "x", estimatedPrice = 34.99, shopName = "Checkers", category = "Groceries"))
        val goals = listOf(PersonalGoal(title = "Savings", category = "Saving Money", targetValue = 5000.0, currentValue = 1200.0, unitLabel = "ZAR"))

        val json = BackupManager.exportToJson(tx, tasks, rems, shop, goals)
        val restored = BackupManager.parseFromJson(json)

        assertEquals(1, restored.transactions.size)
        assertEquals(6500.0, restored.transactions.first().amount, 0.01)
        assertEquals(1, restored.tasks.size)
        assertEquals(1, restored.reminders.size)
        assertEquals(1, restored.shoppingItems.size)
        assertEquals(1, restored.goals.size)
    }
}
