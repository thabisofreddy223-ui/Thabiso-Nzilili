package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.ShoppingItem
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoNumberStyle
import com.example.util.SouthAfricaFormatters

private val PopularSaShops = listOf("Checkers", "Pick n Pay", "Shoprite", "Woolworths", "Local Spaza", "General")
private val ShopCategories = listOf("Groceries", "Fresh Produce", "Household", "Toiletries", "Baby & School", "Hardware")

@Composable
fun ShoppingScreen(
    items: List<ShoppingItem>,
    onAddItem: (String, Double, String, Double, String, String, Boolean) -> Unit,
    onTogglePurchased: (ShoppingItem) -> Unit,
    onToggleFrequent: (ShoppingItem) -> Unit,
    onReAddFrequent: (ShoppingItem) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var groupMode by remember { mutableIntStateOf(0) } // 0 = By Shop, 1 = By Category
    var showAddDialog by remember { mutableStateOf(false) }

    val pendingTotal = remember(items) {
        items.filter { !it.purchased }.sumOf { it.estimatedPrice * it.quantity }
    }
    val purchasedTotal = remember(items) {
        items.filter { it.purchased }.sumOf { it.estimatedPrice * it.quantity }
    }
    val frequentPurchased = remember(items) {
        items.filter { it.isFrequent && it.purchased }
    }
    val grouped = remember(items, groupMode) {
        items.groupBy { if (groupMode == 0) it.shopName else it.category }
    }
    // Price comparison strictly from the user's own recorded entries (never invented prices!)
    val priceComparisons = remember(items) {
        items.filter { it.estimatedPrice > 0.0 }
            .groupBy { it.name.trim().lowercase() }
            .filter { (_, list) -> list.map { it.shopName }.distinct().size > 1 }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("shopping_add_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add shopping item")
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
                Text("Shopping Assistant", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Organise baskets by shop or category with live ZAR totals",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Remaining Basket", style = MaterialTheme.typography.labelMedium)
                            Text(
                                SouthAfricaFormatters.formatZar(pendingTotal),
                                style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("In Trolley / Paid", style = MaterialTheme.typography.labelMedium)
                            Text(
                                SouthAfricaFormatters.formatZar(purchasedTotal),
                                style = MonoNumberStyle,
                                color = IncomeGreen
                            )
                            Text(
                                "Est. Total: ${SouthAfricaFormatters.formatZarCompact(pendingTotal + purchasedTotal)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (frequentPurchased.isNotEmpty()) {
                item {
                    Column {
                        Text("Frequent Items (1-tap re-add)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(frequentPurchased, key = { "freq_${it.id}" }) { freq ->
                                AssistChip(
                                    onClick = { onReAddFrequent(freq) },
                                    label = { Text("+ ${freq.name} (${SouthAfricaFormatters.formatZarCompact(freq.estimatedPrice)})") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                val modes = listOf("Group by Shop", "Group by Category")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = groupMode == idx,
                            onClick = { groupMode = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            if (priceComparisons.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CompareArrows, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Your Price Comparisons", style = MaterialTheme.typography.titleSmall)
                            }
                            priceComparisons.entries.take(3).forEach { (_, entries) ->
                                val cheapest = entries.minByOrNull { it.estimatedPrice }!!
                                Text(
                                    "${cheapest.name}: lowest at ${cheapest.shopName} (${SouthAfricaFormatters.formatZar(cheapest.estimatedPrice)})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Your shopping list is empty. Tap + to add groceries, quantities, estimated ZAR prices, and save frequent items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                grouped.forEach { (groupTitle, groupItems) ->
                    item(key = "header_$groupTitle") {
                        val groupSubtotal = groupItems.filter { !it.purchased }.sumOf { it.estimatedPrice * it.quantity }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(groupTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                SouthAfricaFormatters.formatZar(groupSubtotal),
                                style = MonoNumberStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(groupItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onTogglePurchased(item) }) {
                                    Icon(
                                        imageVector = if (item.purchased) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Mark purchased",
                                        tint = if (item.purchased) IncomeGreen else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Text(
                                        text = "${item.quantity} ${item.unit} • ${item.shopName} • ${item.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = SouthAfricaFormatters.formatZar(item.estimatedPrice * item.quantity),
                                    style = MonoNumberStyle
                                )
                                IconButton(onClick = { onToggleFrequent(item) }) {
                                    Icon(
                                        imageVector = if (item.isFrequent) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Toggle frequent item",
                                        tint = if (item.isFrequent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteItem(item.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete shopping item")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddShoppingItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, unit, price, shop, cat, freq ->
                onAddItem(name, qty, unit, price, shop, cat, freq)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddShoppingItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Double, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("x") }
    var priceText by remember { mutableStateOf("") }
    var shop by remember { mutableStateOf("Checkers") }
    var category by remember { mutableStateOf("Groceries") }
    var isFrequent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shopping Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name (e.g., Maize Meal 5kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Unit Price (R)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Shop", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PopularSaShops.forEach { s ->
                        FilterChip(selected = shop == s, onClick = { shop = s }, label = { Text(s) })
                    }
                }
                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShopCategories.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = isFrequent,
                        onClick = { isFrequent = !isFrequent },
                        label = { Text("Save as Frequent Item") }
                    )
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val qty = qtyText.toDoubleOrNull()
                val price = priceText.toDoubleOrNull() ?: 0.0
                when {
                    name.isBlank() -> error = "Please enter a product name."
                    qty == null || qty <= 0.0 -> error = "Quantity must be greater than 0."
                    else -> onConfirm(name, qty, unit.ifBlank { "x" }, price.coerceAtLeast(0.0), shop, category, isFrequent)
                }
            }) { Text("Add Item") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
