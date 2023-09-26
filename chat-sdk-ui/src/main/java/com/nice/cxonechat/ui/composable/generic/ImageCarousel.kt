/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * An image carousel to display a list of images (by URL) with automatic timed paging and page dot indicators.
 *
 * @param images list of image URLs to display.
 * @param modifier Compose Modifier to use.
 * @param pagerState [PagerState] used by included [HorizontalPager].
 * @param autoScrollDuration Time, in ms, to delay between automatically scrolling the pager.  Pass zero to
 * disable auto scrolling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCarousel(
    images: List<Pair<String, String?>>,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        images.size
    },
    autoScrollDuration: Long = 5_000,
) {
    Carousel(
        images,
        modifier = modifier,
        pagerState = pagerState,
        autoScrollDuration = autoScrollDuration,
    ) { (url, name), mod ->
        AsyncImage(
            model = url,
            contentDescription = name,
            modifier = mod,
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
        )
    }
}
