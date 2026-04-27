package org.movzx.dibella.viewmodel

import org.movzx.dibella.model.CivitaiImage

interface BaseUiState {
    val images: List<CivitaiImage>
    val isLoading: Boolean
    val gridColumns: Int
    val scrollIndex: Int
    val scrollOffset: Int
    val downloadProgresses: Map<Long, Float>
}
