package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonGrid(columnCount: Int) {
    val itemCount =
        when (columnCount) {
            1 -> 2
            2 -> 8
            3 -> 16
            4 -> 30
            else -> 20
        }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnCount),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        userScrollEnabled = false,
    ) {
        items(itemCount) { index ->
            val aspectRatios = listOf(0.8f, 1.0f, 1.2f, 1.4f, 0.75f, 1.33f, 0.67f, 1.5f)
            val aspectRatio = aspectRatios[index % aspectRatios.size]

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerBackground()
            )
        }
    }
}
