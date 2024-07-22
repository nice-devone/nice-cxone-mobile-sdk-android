/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.material.math.MathUtils.lerp
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * An image carousel to display a list of images (by URL) with automatic timed paging and page dot indicators.
 *
 * @param Item type of data to be passed to each displayed page.
 * @param items list of items to display.
 * @param modifier Compose Modifier to use.
 * @param pagerState [PagerState] used by included [HorizontalPager].
 * @param autoScrollDuration Time, in ms, to delay between automatically scrolling the pager.  Pass 0 to disable
 * auto scrolling.
 * @param content @Composable to generate each page in the carousel.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <Item> Carousel(
    items: List<Item>,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = items::size
    ),
    autoScrollDuration: Long = 5_000,
    content: @Composable (Item, Modifier) -> Unit,
) {
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    if (!isDragged && autoScrollDuration > 0) {
        with(pagerState) {
            var currentPageKey by remember { mutableIntStateOf(0) }
            LaunchedEffect(key1 = currentPageKey) {
                launch {
                    delay(timeMillis = autoScrollDuration)
                    val nextPage = (currentPage + 1).mod(items.size)
                    animateScrollToPage(page = nextPage)
                    currentPageKey = nextPage
                }
            }
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            userScrollEnabled = true,
            contentPadding = PaddingValues(horizontal = 32.dp),
        ) { index ->
            content(
                items[index],
                Modifier
                    .fillMaxSize()
                    .carouselTransition(index, pagerState),
            )
        }

        if (items.size > 1) {
            DotIndicators(
                pageCount = items.size,
                pagerState = pagerState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.carouselTransition(page: Int, pagerState: PagerState) =
    graphicsLayer {
        val pageOffset = ((pagerState.currentPage - page).toFloat() + pagerState.currentPageOffsetFraction).absoluteValue
        val transformation = lerp(
            0.7f,
            1.0f,
            1f - pageOffset.coerceIn(0f, 1f)
        )
        alpha = transformation
        scaleY = transformation
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DotIndicators(
    pageCount: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedColor: Color = colors.primary,
    unselectedColor: Color = colors.secondary,
    dotSize: Dp = 6.dp,
    dotSpacing: Dp = 3.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing)
    ) {
        repeat(pageCount) { index ->
            DotIndicator(
                selected = index == pagerState.currentPage,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                dotSize = dotSize
            )
        }
    }
}

@Composable
private fun DotIndicator(
    selected: Boolean,
    selectedColor: Color = colors.primary,
    unselectedColor: Color = colors.secondary,
    dotSize: Dp = 6.dp,
) {
    val color = if (selected) {
        selectedColor
    } else {
        unselectedColor
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color)
            .size(dotSize)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun DotIndicatorsPreview() {
    ChatTheme {
        DotIndicators(
            pageCount = 4,
            pagerState = rememberPagerState(
                initialPage = 2,
                initialPageOffsetFraction = 0f
            ) {
                4
            }
        )
    }
}
