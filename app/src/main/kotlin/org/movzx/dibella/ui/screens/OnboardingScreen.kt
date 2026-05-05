package org.movzx.dibella.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.movzx.dibella.R

data class OnboardingPage(
    val title: String,
    val description: String,
    val iconResId: Int,
    val gradientColors: List<Color>,
)

@Composable
fun OnboardingScreen(onSkip: () -> Unit, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pages =
        listOf(
            OnboardingPage(
                title = stringResource(R.string.onboarding_title_1),
                description = stringResource(R.string.onboarding_desc_1),
                iconResId = R.drawable.ic_app_logo,
                gradientColors =
                    listOf(colorResource(R.color.primary), colorResource(R.color.secondary)),
            ),
            OnboardingPage(
                title = stringResource(R.string.onboarding_title_2),
                description = stringResource(R.string.onboarding_desc_2),
                iconResId = R.drawable.ic_app_logo,
                gradientColors =
                    listOf(colorResource(R.color.secondary), colorResource(R.color.accent)),
            ),
            OnboardingPage(
                title = stringResource(R.string.onboarding_title_3),
                description = stringResource(R.string.onboarding_desc_3),
                iconResId = R.drawable.ic_app_logo,
                gradientColors =
                    listOf(colorResource(R.color.accent), colorResource(R.color.primary)),
            ),
        )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            OnboardingPageContent(page = pages[page])
        }

        IconButton(onClick = onSkip, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.btn_skip),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, _ ->
                    val isSelected = index == pagerState.currentPage
                    val indicatorAlpha = if (isSelected) 1f else 0.3f

                    Box(
                        modifier =
                            Modifier.size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(colors = pages[index].gradientColors)
                                )
                                .alpha(indicatorAlpha)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1)
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    else onFinish()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.primary),
                        contentColor = Color.White,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text =
                        if (pagerState.currentPage == pages.size - 1)
                            stringResource(R.string.btn_get_started)
                        else stringResource(R.string.btn_next)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors =
                            listOf(
                                page.gradientColors[0].copy(alpha = 0.3f),
                                page.gradientColors[1].copy(alpha = 0.15f),
                                colorResource(R.color.background),
                            )
                    )
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp),
        ) {
            Image(
                painter = painterResource(id = page.iconResId),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = page.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
