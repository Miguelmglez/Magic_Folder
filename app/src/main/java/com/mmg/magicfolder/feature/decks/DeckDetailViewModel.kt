package com.mmg.magicfolder.feature.decks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmg.magicfolder.core.domain.model.Card
import com.mmg.magicfolder.core.domain.model.DataResult
import com.mmg.magicfolder.core.domain.model.Deck
import com.mmg.magicfolder.core.domain.repository.CardRepository
import com.mmg.magicfolder.core.domain.repository.DeckRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckCard(
    val scryfallId:  String,
    val quantity:    Int,
    val isSideboard: Boolean,
    val card:        Card?,
)

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    data class UiState(
        val deck:              Deck?             = null,
        val cards:             List<DeckCard>    = emptyList(),
        val isLoading:         Boolean           = true,
        val totalCards:        Int               = 0,
        val manaCurve:         Map<Int, Int>     = emptyMap(),
        val colorDistribution: Map<String, Int>  = emptyMap(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deckRepository.observeDeckWithCards(deckId).collect { deckWithCards ->
                if (deckWithCards == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@collect
                }
                _uiState.update {
                    it.copy(
                        deck       = deckWithCards.deck,
                        isLoading  = false,
                        totalCards = deckWithCards.totalCards,
                    )
                }
                val mainboard = deckWithCards.mainboard.map { slot ->
                    val card = when (val r = cardRepository.getCardById(slot.scryfallId)) {
                        is DataResult.Success -> r.data
                        else                  -> null
                    }
                    DeckCard(slot.scryfallId, slot.quantity, isSideboard = false, card = card)
                }
                _uiState.update {
                    it.copy(
                        cards             = mainboard,
                        manaCurve         = buildManaCurve(mainboard),
                        colorDistribution = buildColorDist(mainboard),
                    )
                }
            }
        }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch {
            deckRepository.removeCardFromDeck(deckId, cardId, isSideboard = false)
        }
    }

    private fun buildManaCurve(cards: List<DeckCard>): Map<Int, Int> =
        cards
            .mapNotNull { it.card }
            .groupBy { minOf(it.cmc.toInt(), 7) }
            .mapValues { it.value.size }

    private fun buildColorDist(cards: List<DeckCard>): Map<String, Int> =
        cards
            .mapNotNull { it.card }
            .flatMap { it.colors }
            .groupBy { it }
            .mapValues { it.value.size }
}
