package org.movzx.dumbai.viewmodel

import org.movzx.dumbai.model.CivitaiImage

interface BaseUiState {
    val images: List<CivitaiImage>
    val isLoading: Boolean
    val gridColumns: Int
    val scrollIndex: Int
    val scrollOffset: Int
    val downloadProgresses: Map<Long, Float>
}
