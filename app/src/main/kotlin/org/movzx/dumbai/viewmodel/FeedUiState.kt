package org.movzx.dumbai.viewmodel

import org.movzx.dumbai.model.CivitaiImage

data class FeedUiState(
    override val images: List<CivitaiImage> = emptyList(),
    override val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    val nsfw: String = "None",
    val sort: String = "Most Reactions",
    val period: String = "AllTime",
    val type: String = "image",
    val tagIds: String? = null,
    val pageLimit: Int = 100,
    override val gridColumns: Int = 3,
    override val scrollIndex: Int = 0,
    override val scrollOffset: Int = 0,
    override val downloadProgresses: Map<Long, Float> = emptyMap(),
    val favoriteIds: Set<Long> = emptySet(),
) : BaseUiState
