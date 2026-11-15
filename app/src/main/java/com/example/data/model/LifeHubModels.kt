package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val currencyCode: String = "ZAR",
    val savingsTargetZar: Double = 5000.0,
    val currentStreak: Int = 0,
    val bestScore: Int = 0,
    val totalGamePoints: Int = 0,
    val lastPlayedDate: String = "",
    val isPremium: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MoneyTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE",
    val category: String = "Groceries",
    val dateIso: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BudgetLimit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "Groceries",
    val limitAmount: Double = 0.0,
    val period: String = "MONTHLY",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PersonalGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Saving Money",
    val targetValue: Double = 100.0,
    val currentValue: Double = 0.0,
    val unitLabel: String = "ZAR",
    val deadlineIso: String = "",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlannerTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val priority: String = "MEDIUM",
    val dueDateIso: String = "",
    val dueTime: String = "",
    val category: String = "Personal",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SmartReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Rent",
    val dueDateTimeIso: String = "",
    val recurrence: String = "ONCE",
    val amountZar: Double = 0.0,
    val completed: Boolean = false,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShoppingItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "x",
    val estimatedPrice: Double = 0.0,
    val shopName: String = "Checkers",
    val category: String = "Groceries",
    val purchased: Boolean = false,
    val isFrequent: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GameScoreRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameId: String = "",
    val difficulty: String = "EASY",
    val score: Int = 0,
    val dateIso: String = "",
    val durationSeconds: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedQuote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quoteId: String = "",
    val text: String = "",
    val author: String = "",
    val category: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LocalService(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val businessName: String = "",
    val category: String = "Mechanics",
    val city: String = "",
    val suburb: String = "",
    val businessPhone: String = "",
    val servicesOffered: String = "",
    @field:JvmField val isVerified: Boolean = false,
    @field:JvmField val isPublished: Boolean = true,
    val ratingSum: Int = 0,
    val reviewCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val averageRating: Double
        get() = if (reviewCount > 0) ratingSum.toDouble() / reviewCount else 0.0
}

@IgnoreExtraProperties
data class ServiceReview(
    @DocumentId val id: String = "",
    val serviceId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val bestScore: Int = 0,
    @field:JvmField val isPublic: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
