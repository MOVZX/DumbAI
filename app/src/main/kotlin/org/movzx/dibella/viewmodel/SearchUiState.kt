package org.movzx.dibella.viewmodel

import org.movzx.dibella.model.CivitaiSearchResult

data class SearchUiState(
    val query: String = "",
    val results: List<CivitaiSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    val totalHits: Int = 0,
    val type: String = "image",
    val sort: String = "Relevancy",
    val gridColumns: Int = 3,
    val error: String? = null,
    val isRestored: Boolean = false,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val currentOffset: Int = 0,
    val currentPageStartOffset: Int = 0,
)
