package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GameScoreRecord
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LocalService
import com.example.data.model.MorningBoostCatalog
import com.example.data.model.SavedQuote
import com.example.data.model.ServiceReview
import com.example.data.model.UserProfile
import com.example.data.repository.AppPreferences
import com.example.data.repository.NotificationFrequency
import com.example.data.repository.NotificationPreferences
import com.example.data.repository.ThemeMode
import com.example.ui.theme.MonoNumberStyle

enum class ExploreSection { OVERVIEW, MORNING_BOOST, MOTIVATION, SERVICES, SETTINGS }

private val ServiceCategories = listOf(
    "All", "Mechanics", "Plumbers", "Electricians", "Hairdressers",
    "Cleaning services", "Computer repairs", "Tutors", "Other"
)

@Composable
fun ExploreHubScreen(
    section: ExploreSection,
    onSectionChange: (ExploreSection) -> Unit,
    currentUserId: String,
    profile: UserProfile?,
    preferences: AppPreferences,
    gameScores: List<GameScoreRecord>,
    leaderboard: List<LeaderboardEntry>,
    savedQuotes: List<SavedQuote>,
    services: List<LocalService>,
    selectedServiceReviews: List<ServiceReview>,
    onSelectServiceForReviews: (String?) -> Unit,
    onGameCompleted: (String, String, Int, Int) -> Unit,
    onToggleSaveQuote: (String, String, String, String, Boolean) -> Unit,
    onCreateService: (String, String, String, String, String, String) -> Unit,
    onSubmitReview: (String, Int, String) -> Unit,
    onDeleteService: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onUpdateNotificationPrefs: (NotificationPreferences) -> Unit,
    onSetShareOnLeaderboard: (Boolean) -> Unit,
    onUpdateProfile: (String, Double) -> Unit,
    onBuildBackupJson: () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onSendTestNotification: () -> Unit,
    onShowMessage: (String) -> Unit,
    onNavigateToGoals: () -> Unit,
    onSignOut: () -> Unit
) {
    if (section != ExploreSection.OVERVIEW) {
        BackHandler { onSectionChange(ExploreSection.OVERVIEW) }
    }

    when (section) {
        ExploreSection.MORNING_BOOST -> MorningBoostScreen(
            profile = profile,
            recentScores = gameScores,
            leaderboard = leaderboard,
            shareOnLeaderboard = preferences.shareOnLeaderboard,
            onBack = { onSectionChange(ExploreSection.OVERVIEW) },
            onGameCompleted = onGameCompleted,
            onNavigateToGoals = onNavigateToGoals
        )
        ExploreSection.MOTIVATION -> DailyMotivationScreen(
            savedQuotes = savedQuotes,
            onToggleSaveQuote = onToggleSaveQuote,
            onBack = { onSectionChange(ExploreSection.OVERVIEW) }
        )
        ExploreSection.SERVICES -> LocalServicesScreen(
            currentUserId = currentUserId,
            services = services,
            selectedReviews = selectedServiceReviews,
            onSelectServiceForReviews = onSelectServiceForReviews,
            onCreateService = onCreateService,
            onSubmitReview = onSubmitReview,
            onDeleteService = onDeleteService,
            onBack = { onSectionChange(ExploreSection.OVERVIEW) }
        )
        ExploreSection.SETTINGS -> SettingsAndBackupScreen(
            profile = profile,
            preferences = preferences,
            onThemeModeChange = onThemeModeChange,
            onUpdateNotificationPrefs = onUpdateNotificationPrefs,
            onSetShareOnLeaderboard = onSetShareOnLeaderboard,
            onUpdateProfile = onUpdateProfile,
            onBuildBackupJson = onBuildBackupJson,
            onRestoreBackupJson = onRestoreBackupJson,
            onSendTestNotification = onSendTestNotification,
            onShowMessage = onShowMessage,
            onSignOut = onSignOut,
            onBack = { onSectionChange(ExploreSection.OVERVIEW) }
        )
        ExploreSection.OVERVIEW -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Explore & Community", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Morning Boost, Daily Motivation, Local Services, and Account Controls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ExploreTileCard(
                    title = "Morning Boost Mini-Games",
                    subtitle = "6 daily offline brain, word, maths, trivia & puzzle challenges",
                    badge = "${profile?.currentStreak ?: 0}d streak",
                    icon = Icons.Default.Psychology,
                    tag = "explore_morning_boost",
                    onClick = { onSectionChange(ExploreSection.MORNING_BOOST) }
                )
            }
            item {
                ExploreTileCard(
                    title = "Daily Motivation",
                    subtitle = "Daily quotes, encouragement & your ${savedQuotes.size} saved favourites",
                    badge = "Daily quote",
                    icon = Icons.Default.FormatQuote,
                    tag = "explore_motivation",
                    onClick = { onSectionChange(ExploreSection.MOTIVATION) }
                )
            }
            item {
                ExploreTileCard(
                    title = "Local Services Directory",
                    subtitle = "Discover nearby mechanics, plumbers, electricians, tutors & more",
                    badge = "${services.size} listed",
                    icon = Icons.Default.Handyman,
                    tag = "explore_services",
                    onClick = { onSectionChange(ExploreSection.SERVICES) }
                )
            }
            item {
                ExploreTileCard(
                    title = "Settings, Backup & Privacy",
                    subtitle = "Light/Dark mode, notification controls, JSON backup & sign-out",
                    badge = preferences.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Settings,
                    tag = "explore_settings",
                    onClick = { onSectionChange(ExploreSection.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun ExploreTileCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(999.dp)) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyMotivationScreen(
    savedQuotes: List<SavedQuote>,
    onToggleSaveQuote: (String, String, String, String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showSavedOnly by remember { mutableStateOf(false) }
    val savedIds = remember(savedQuotes) { savedQuotes.map { it.quoteId }.toSet() }
    val quotes = remember(showSavedOnly, savedIds) {
        if (showSavedOnly) MorningBoostCatalog.quotes.filter { it.id in savedIds }
        else MorningBoostCatalog.quotes
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Daily Motivation", style = MaterialTheme.typography.headlineLarge)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Today's Daily Challenge", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(MorningBoostCatalog.dailyChallengePrompt(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showSavedOnly, onClick = { showSavedOnly = false }, label = { Text("All Quotes") })
                FilterChip(selected = showSavedOnly, onClick = { showSavedOnly = true }, label = { Text("Saved (${savedQuotes.size})") })
            }
        }
        if (quotes.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Text("Tap the bookmark icon on any quote to save your personal favourites.", modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            items(quotes, key = { it.id }) { q ->
                val isSaved = q.id in savedIds
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("\"${q.text}\"", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("— ${q.author} • ${q.category}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onToggleSaveQuote(q.id, q.text, q.author, q.category, isSaved) }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save quote"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalServicesScreen(
    currentUserId: String,
    services: List<LocalService>,
    selectedReviews: List<ServiceReview>,
    onSelectServiceForReviews: (String?) -> Unit,
    onCreateService: (String, String, String, String, String, String) -> Unit,
    onSubmitReview: (String, Int, String) -> Unit,
    onDeleteService: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var inspectingService by remember { mutableStateOf<LocalService?>(null) }

    val filtered = remember(services, selectedCategory, searchQuery) {
        services.filter { svc ->
            val catOk = selectedCategory == "All" || svc.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val queryOk = q.isBlank() || svc.businessName.lowercase().contains(q) ||
                svc.city.lowercase().contains(q) || svc.suburb.lowercase().contains(q) ||
                svc.servicesOffered.lowercase().contains(q)
            catOk && queryOk
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = "Register local business")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    Text("Local Services", style = MaterialTheme.typography.headlineLarge)
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(18.dp))) {
                    Image(
                        painter = painterResource(id = R.drawable.img_services_banner),
                        contentDescription = "Local South African services",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)))
                        )
                    )
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Text("Trusted Community Pros", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Real business profiles & genuine customer reviews only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by name, city, suburb, or service") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ServiceCategories.forEach { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No businesses listed in this filter yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "LIFEHUB SA never invents fake businesses or reviews. Own a local business? Tap the + button below to publish your real profile.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { showCreateDialog = true }) { Text("Create Business Profile") }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { svc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(svc.businessName, style = MaterialTheme.typography.titleMedium)
                                    if (svc.isVerified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Verified, contentDescription = "Verified business", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (svc.ownerId == currentUserId) {
                                    IconButton(onClick = { onDeleteService(svc.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete listing")
                                    }
                                }
                            }
                            Text(
                                "${svc.category} • ${svc.suburb}, ${svc.city}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(svc.servicesOffered, style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (svc.reviewCount > 0) "${"%.1f".format(svc.averageRating)} (${svc.reviewCount} reviews)" else "No reviews yet",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${svc.businessPhone}")))
                                        }
                                    }) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call")
                                    }
                                    Button(onClick = {
                                        onSelectServiceForReviews(svc.id)
                                        inspectingService = svc
                                    }) {
                                        Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reviews")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateServiceDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, cat, city, suburb, phone, offered ->
                onCreateService(name, cat, city, suburb, phone, offered)
                showCreateDialog = false
            }
        )
    }

    val svc = inspectingService
    if (svc != null) {
        ServiceReviewsDialog(
            service = svc,
            reviews = selectedReviews,
            onDismiss = {
                inspectingService = null
                onSelectServiceForReviews(null)
            },
            onSubmitReview = { rating, comment ->
                onSubmitReview(svc.id, rating, comment)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Mechanics") }
    var city by remember { mutableStateOf("") }
    var suburb by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var offered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Business Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ServiceCategories.filter { it != "All" }.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = suburb, onValueChange = { suburb = it }, label = { Text("Suburb") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = offered, onValueChange = { offered = it }, label = { Text("Services Offered") }, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    name.trim().length < 2 -> error = "Business name must be at least 2 characters."
                    city.isBlank() -> error = "City is required."
                    phone.trim().length < 5 -> error = "Valid phone number is required."
                    offered.trim().length < 3 -> error = "Describe your services."
                    else -> onConfirm(name, category, city, suburb, phone, offered)
                }
            }) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ServiceReviewsDialog(
    service: LocalService,
    reviews: List<ServiceReview>,
    onDismiss: () -> Unit,
    onSubmitReview: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${service.businessName} Reviews") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Leave a genuine customer review", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        FilterChip(selected = rating == star, onClick = { rating = star }, label = { Text("$star★") })
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Your honest experience") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (comment.isNotBlank()) {
                            onSubmitReview(rating, comment)
                            comment = ""
                        }
                    },
                    enabled = comment.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Post Review") }
                Spacer(modifier = Modifier.height(6.dp))
                if (reviews.isEmpty()) {
                    Text("No customer reviews posted yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    reviews.take(4).forEach { r ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${r.authorName} • ${r.rating}★", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(r.comment, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsAndBackupScreen(
    profile: UserProfile?,
    preferences: AppPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onUpdateNotificationPrefs: (NotificationPreferences) -> Unit,
    onSetShareOnLeaderboard: (Boolean) -> Unit,
    onUpdateProfile: (String, Double) -> Unit,
    onBuildBackupJson: () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onSendTestNotification: () -> Unit,
    onShowMessage: (String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var nameDraft by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
    var targetDraft by remember(profile) { mutableStateOf((profile?.savingsTargetZar ?: 5000.0).toInt().toString()) }

    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onShowMessage(if (granted) "Notification permission granted!" else "Notification permission denied.")
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(onBuildBackupJson().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess { onShowMessage("Encrypted account backup exported to JSON.") }
                .onFailure { onShowMessage("Export failed: ${it.localizedMessage}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }.onSuccess { json ->
                if (json.isNotBlank()) onRestoreBackupJson(json)
            }.onFailure { onShowMessage("Restore failed: ${it.localizedMessage}") }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Settings & Privacy", style = MaterialTheme.typography.headlineLarge)
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Account & Savings Target", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = nameDraft, onValueChange = { nameDraft = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = targetDraft, onValueChange = { targetDraft = it }, label = { Text("Monthly Savings Goal (ZAR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onUpdateProfile(nameDraft.ifBlank { "LIFEHUB Member" }, targetDraft.toDoubleOrNull() ?: 5000.0) }) {
                        Text("Save Profile")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        }
        item {
            val n = preferences.notifications
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notifications & Frequency", style = MaterialTheme.typography.titleMedium)
                    SettingSwitchRow("Enable Notifications", n.masterEnabled) { onUpdateNotificationPrefs(n.copy(masterEnabled = it)) }
                    SettingSwitchRow("Upcoming Bills & Rent", n.billReminders) { onUpdateNotificationPrefs(n.copy(billReminders = it)) }
                    SettingSwitchRow("Unfinished Tasks & Deadlines", n.taskDeadlines) { onUpdateNotificationPrefs(n.copy(taskDeadlines = it)) }
                    SettingSwitchRow("Shopping & Daily Challenges", n.dailyBoostChallenges) { onUpdateNotificationPrefs(n.copy(dailyBoostChallenges = it)) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NotificationFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = n.frequency == freq,
                                onClick = { onUpdateNotificationPrefs(n.copy(frequency = freq)) },
                                label = { Text(freq.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Permission") }
                        }
                        Button(onClick = onSendTestNotification) { Text("Test Alert") }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup, Recovery & Privacy", style = MaterialTheme.typography.titleMedium)
                    SettingSwitchRow("Appear on Morning Boost Leaderboard", preferences.shareOnLeaderboard, onSetShareOnLeaderboard)
                    if (preferences.lastBackupIso.isNotBlank()) {
                        Text("Last backup: ${preferences.lastBackupIso}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("lifehub_sa_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("LIFEHUB SA Free Companion", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "All core features—Money Manager, Smart Reminders, Shopping Assistant, Daily Planner, Goals, Morning Boost & Local Directory—are free without intrusive ads.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
