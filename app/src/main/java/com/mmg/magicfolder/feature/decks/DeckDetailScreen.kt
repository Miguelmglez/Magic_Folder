package com.mmg.magicfolder.feature.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmg.magicfolder.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mmg.magicfolder.core.domain.model.Deck
import com.mmg.magicfolder.core.ui.theme.magicColors
import com.mmg.magicfolder.core.ui.theme.magicTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId:    Long,
    onBack:    () -> Unit,
    viewModel: DeckDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mc = MaterialTheme.magicColors
    val ty = MaterialTheme.magicTypography
    var showAddCardsSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = mc.background,
        topBar = {
            Surface(
                color = mc.backgroundSecondary,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint               = mc.textSecondary,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text  = uiState.deck?.name ?: "Deck",
                            style = ty.titleMedium,
                            color = mc.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        uiState.deck?.format?.let { fmt ->
                            Surface(
                                shape    = RoundedCornerShape(4.dp),
                                color    = mc.goldMtg.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text     = fmt.uppercase(),
                                    style    = ty.labelSmall,
                                    color    = mc.goldMtg,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = {
                    viewModel.showCollectionCards()
                    showAddCardsSheet = true
                },
                containerColor = mc.primaryAccent,
                contentColor   = mc.background,
                shape          = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.deckbuilder_add_cards))
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = mc.primaryAccent)
            }

            uiState.cards.isEmpty() -> Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = stringResource(R.string.deckbuilder_deck_empty),
                    style = ty.titleMedium,
                    color = mc.textSecondary,
                )
            }

            else -> DeckContent(
                uiState  = uiState,
                onRemove = viewModel::removeCard,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showAddCardsSheet) {
        AddCardsSheet(
            uiState       = uiState,
            format         = viewModel.deckFormat,
            onQueryChange = viewModel::onAddCardsQueryChange,
            onAdd         = viewModel::addCardToDeck,
            onRemove      = viewModel::removeCardFromDeck,
            onDismiss     = {
                showAddCardsSheet = false
                viewModel.clearAddCardsState()
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Add cards ModalBottomSheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardsSheet(
    uiState:       DeckDetailViewModel.UiState,
    format:        com.mmg.magicfolder.core.domain.model.DeckFormat?,
    onQueryChange: (String) -> Unit,
    onAdd:         (String) -> Unit,
    onRemove:      (String) -> Unit,
    onDismiss:     () -> Unit,
) {
    val mc         = MaterialTheme.magicColors
    val ty         = MaterialTheme.magicTypography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = mc.backgroundSecondary,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = mc.textDisabled) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = 16.dp),
        ) {
            // Title
            Text(
                text     = stringResource(R.string.deckbuilder_add_cards),
                style    = ty.titleMedium,
                color    = mc.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Search bar
            OutlinedTextField(
                value         = uiState.addCardsQuery,
                onValueChange = onQueryChange,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder   = { Text(stringResource(R.string.deckbuilder_add_cards_search_hint), color = mc.textDisabled) },
                leadingIcon   = {
                    if (uiState.isSearchingCards) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = mc.primaryAccent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = mc.textSecondary)
                    }
                },
                trailingIcon = if (uiState.addCardsQuery.isNotEmpty()) {{
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_close), tint = mc.textSecondary)
                    }
                }} else null,
                singleLine = true,
                shape      = MaterialTheme.shapes.medium,
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = mc.primaryAccent,
                    unfocusedBorderColor = mc.surfaceVariant,
                    focusedTextColor     = mc.textPrimary,
                    unfocusedTextColor   = mc.textPrimary,
                    cursorColor          = mc.primaryAccent,
                ),
            )

            // Card list
            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(uiState.addCardsResults, key = { it.card.scryfallId }) { row ->
                    AddCardSheetRow(
                        row       = row,
                        format    = format,
                        onAdd     = { onAdd(row.card.scryfallId) },
                        onRemove  = { onRemove(row.card.scryfallId) },
                    )
                }
                if (uiState.addCardsResults.isEmpty() && !uiState.isSearchingCards) {
                    item {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = if (uiState.addCardsQuery.isBlank()) stringResource(R.string.deckbuilder_no_cards)
                                        else stringResource(R.string.deckbuilder_no_cards),
                                style = ty.bodyMedium,
                                color = mc.textDisabled,
                            )
                        }
                    }
                }
            }

            // Done button
            Button(
                onClick  = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(48.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = mc.primaryAccent),
                shape    = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text  = stringResource(R.string.deckbuilder_add_cards_done),
                    style = ty.titleMedium,
                    color = mc.background,
                )
            }
        }
    }
}

@Composable
private fun AddCardSheetRow(
    row:      DeckDetailViewModel.AddCardRow,
    format:   com.mmg.magicfolder.core.domain.model.DeckFormat?,
    onAdd:    () -> Unit,
    onRemove: () -> Unit,
) {
    val mc   = MaterialTheme.magicColors
    val ty   = MaterialTheme.magicTypography
    val card = row.card

    // Determine if add button should be disabled
    val isCommander = format?.uniqueCards == true
    val addEnabled = if (isCommander) row.quantityInDeck < 1 else true

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = mc.surface,
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model              = card.imageArtCrop,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(width = 52.dp, height = 38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(mc.surfaceVariant),
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text     = card.name,
                        style    = ty.bodyMedium,
                        color    = mc.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!row.isOwned) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = mc.surfaceVariant,
                        ) {
                            Text(
                                text     = "Scryfall",
                                style    = ty.labelSmall,
                                color    = mc.textDisabled,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text     = card.typeLine,
                    style    = ty.bodySmall,
                    color    = mc.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Quantity controls
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (row.quantityInDeck > 0) {
                    IconButton(
                        onClick  = onRemove,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.action_remove),
                            tint               = mc.textSecondary,
                            modifier           = Modifier.size(16.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = mc.primaryAccent.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text     = "${row.quantityInDeck}",
                            style    = ty.labelMedium,
                            color    = mc.primaryAccent,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .widthIn(min = 24.dp),
                        )
                    }
                }
                IconButton(
                    onClick  = onAdd,
                    enabled  = addEnabled,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Default.Add,
                        contentDescription = stringResource(R.string.action_add),
                        tint               = if (addEnabled) mc.primaryAccent else mc.textDisabled,
                        modifier           = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Deck content (summary header + grouped card list)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeckContent(
    uiState:  DeckDetailViewModel.UiState,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deck       = uiState.deck ?: return
    val cardGroups = groupCardsByType(uiState.cards)
    val maxInCurve = uiState.manaCurve.values.maxOrNull() ?: 1

    LazyColumn(
        modifier            = modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DeckSummaryCard(
                deck       = deck,
                totalCards = uiState.totalCards,
                manaCurve  = uiState.manaCurve,
                maxInCurve = maxInCurve,
            )
        }

        cardGroups.forEach { (typeName, cards) ->
            item(key = "header_$typeName") {
                TypeGroupHeader(
                    typeName = typeName,
                    count    = cards.sumOf { it.quantity },
                )
            }
            items(cards, key = { it.scryfallId }) { deckCard ->
                CardRow(
                    deckCard = deckCard,
                    onRemove = { onRemove(deckCard.scryfallId) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Deck summary card (totals + mana curve)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeckSummaryCard(
    deck:       Deck,
    totalCards: Int,
    manaCurve:  Map<Int, Int>,
    maxInCurve: Int,
) {
    val mc          = MaterialTheme.magicColors
    val ty          = MaterialTheme.magicTypography
    val isCommander = deck.format.lowercase() == "commander"
    val targetCount = if (isCommander) 100 else 60

    Surface(shape = RoundedCornerShape(12.dp), color = mc.surface) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text  = "Total Cards",
                    style = ty.labelMedium,
                    color = mc.textSecondary,
                )
                Text(
                    text  = "$totalCards / $targetCount",
                    style = ty.titleMedium,
                    color = if (totalCards >= targetCount) mc.lifePositive else mc.textPrimary,
                )
            }
            if (totalCards < targetCount) {
                LinearProgressIndicator(
                    progress  = { totalCards.toFloat() / targetCount },
                    modifier  = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color     = mc.primaryAccent,
                    trackColor = mc.surfaceVariant,
                )
            }
            if (manaCurve.isNotEmpty()) {
                Text(
                    text  = "MANA CURVE",
                    style = ty.labelSmall,
                    color = mc.textSecondary,
                )
                ManaCurveBar(manaCurve = manaCurve, maxInCurve = maxInCurve)
            }
        }
    }
}

@Composable
private fun ManaCurveBar(manaCurve: Map<Int, Int>, maxInCurve: Int) {
    val mc = MaterialTheme.magicColors
    val ty = MaterialTheme.magicTypography
    Row(
        modifier              = Modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.Bottom,
    ) {
        (0..7).forEach { cmc ->
            val count    = manaCurve[cmc] ?: 0
            val fraction = if (maxInCurve > 0) count.toFloat() / maxInCurve else 0f
            Column(
                modifier            = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(mc.primaryAccent.copy(alpha = 0.5f + 0.5f * fraction)),
                    )
                }
                Text(
                    text  = if (cmc == 7) "7+" else cmc.toString(),
                    style = ty.labelSmall,
                    color = mc.textDisabled,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Type group header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypeGroupHeader(typeName: String, count: Int) {
    val mc = MaterialTheme.magicColors
    val ty = MaterialTheme.magicTypography
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = typeName,
            style = ty.titleMedium,
            color = mc.goldMtg,
        )
        Text(
            text  = "($count)",
            style = ty.bodyMedium,
            color = mc.textSecondary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Card row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardRow(
    deckCard: DeckCard,
    onRemove: () -> Unit,
) {
    val mc = MaterialTheme.magicColors
    val ty = MaterialTheme.magicTypography
    Surface(shape = RoundedCornerShape(8.dp), color = mc.surface) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model              = deckCard.card?.imageNormal,
                contentDescription = deckCard.card?.name ?: deckCard.scryfallId,
                modifier           = Modifier
                    .size(width = 44.dp, height = 60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(mc.surfaceVariant),
                contentScale       = ContentScale.Crop,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = deckCard.card?.name ?: deckCard.scryfallId,
                    style    = ty.bodyMedium,
                    color    = mc.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                deckCard.card?.typeLine?.let { type ->
                    Text(
                        text     = type,
                        style    = ty.bodySmall,
                        color    = mc.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                deckCard.card?.manaCost?.let { cost ->
                    Text(
                        text  = cost,
                        style = ty.labelSmall,
                        color = mc.textSecondary,
                    )
                }
            }

            if (deckCard.quantity > 1) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = mc.primaryAccent.copy(alpha = 0.2f),
                ) {
                    Text(
                        text     = "\u00d7${deckCard.quantity}",
                        style    = ty.labelMedium,
                        color    = mc.primaryAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            IconButton(
                onClick  = onRemove,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_remove),
                    tint               = mc.textDisabled,
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun groupCardsByType(cards: List<DeckCard>): List<Pair<String, List<DeckCard>>> {
    val typeOrder = listOf(
        "Creatures", "Instants", "Sorceries",
        "Enchantments", "Artifacts", "Planeswalkers", "Lands", "Other",
    )
    val groups = mutableMapOf<String, MutableList<DeckCard>>()

    cards.forEach { deckCard ->
        val typeLine = deckCard.card?.typeLine ?: ""
        val group = when {
            typeLine.contains("Creature",     ignoreCase = true) -> "Creatures"
            typeLine.contains("Instant",      ignoreCase = true) -> "Instants"
            typeLine.contains("Sorcery",      ignoreCase = true) -> "Sorceries"
            typeLine.contains("Enchantment",  ignoreCase = true) -> "Enchantments"
            typeLine.contains("Artifact",     ignoreCase = true) -> "Artifacts"
            typeLine.contains("Planeswalker", ignoreCase = true) -> "Planeswalkers"
            typeLine.contains("Land",         ignoreCase = true) -> "Lands"
            else                                                   -> "Other"
        }
        groups.getOrPut(group) { mutableListOf() }.add(deckCard)
    }

    return typeOrder
        .filter { it in groups }
        .map { it to (groups[it]!! as List<DeckCard>) }
}
