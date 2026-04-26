package org.movzx.dumbai.viewmodel

import org.movzx.dumbai.model.CivitaiImage

data class GalleryUiState(
    override val images: List<CivitaiImage> = emptyList(),
    override val isLoading: Boolean = false,
    val type: String = "all",
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    override val gridColumns: Int = 3,
    override val scrollIndex: Int = 0,
    override val scrollOffset: Int = 0,
    val downloadPath: String? = null,
    override val downloadProgresses: Map<Long, Float> = emptyMap(),
    val downloadedIds: Set<Long> = emptySet(),
    val isRestored: Boolean = false,
) : BaseUiState
