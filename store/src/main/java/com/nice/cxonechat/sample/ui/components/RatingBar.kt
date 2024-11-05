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

package com.nice.cxonechat.sample.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.sample.R.drawable
import java.lang.Double.max
import java.lang.Double.min
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.nextDown
import kotlin.math.nextUp

/**
 * A more or less standard rating bar.
 *
 * @param rating Actual rating, will be bound to the range of 0..<[stars].
 * @param modifier Compose Modifiers to apply.
 * @param stars Maximum number of stars.
 * @param starsColor Color for stars.  Defaults to `MaterialTheme.colors.primary`.
 * @param filledStar Painter to draw filled stars.
 * @param halfStar Painter to draw half filled stars.
 * @param unfilledStar Painter to draw empty stars.
 */
@Composable
fun RatingBar(
    rating: Double,
    modifier: Modifier = Modifier,
    stars: Int = 5,
    starsColor: Color = colorScheme.primary,
    filledStar: Painter = painterResource(id = drawable.star),
    halfStar: Painter = painterResource(id = drawable.star_half),
    unfilledStar: Painter = painterResource(id = drawable.star_outline),
) {
    val bound = min(stars.toDouble(), max(0.0, rating))
    val filledStars = floor(bound).toInt()
    val unfilledStars = (stars - ceil(bound)).toInt()
    val showHalf = !bound.rem(1).equals(0.0)

    Row(modifier = modifier) {
        repeat(filledStars) {
            Icon(filledStar, contentDescription = null, tint = starsColor)
        }
        if (showHalf) {
            Icon(halfStar, contentDescription = null, tint = starsColor)
        }
        repeat(unfilledStars) {
            Icon(unfilledStar, contentDescription = null, tint = starsColor)
        }
    }
}

@Preview
@Composable
private fun PreviewRatingBar() {
    MaterialTheme {
        Column {
            RatingBar(rating = 0.0)
            RatingBar(rating = 2.0.nextUp())
            RatingBar(rating = 5.0.nextDown())
            RatingBar(rating = 5.5)
        }
    }
}
