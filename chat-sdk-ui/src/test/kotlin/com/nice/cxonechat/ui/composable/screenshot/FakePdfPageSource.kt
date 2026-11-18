/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable.screenshot

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.nice.cxonechat.ui.util.PdfPageSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A4 at 72dpi, so a stubbed page has the proportions of a document rather than of a square. */
private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842

/**
 * A page of flat colour, standing in for one a decoder would have produced. Green, not the stubbed image loader's
 * cyan, so the two cannot be confused in a golden.
 */
internal fun stubbedPdfPage(): Bitmap = createBitmap(PAGE_WIDTH, PAGE_HEIGHT).apply { eraseColor(Color.GREEN) }

/**
 * A single-page document whose page is already in whatever state the caller wants, so a thumbnail can be drawn
 * without a decoder.
 *
 * @param renderedPage the rendered page, or `null` for one still rendering — the state that draws the file tile.
 */
internal class FakePdfPageSource(
    renderedPage: Bitmap?,
) : PdfPageSource {

    override val pageList: List<PdfPageSource.Page> = listOf(FakePage(renderedPage))

    private class FakePage(renderedPage: Bitmap?) : PdfPageSource.Page {

        override val pageContent: StateFlow<Bitmap?> = MutableStateFlow(renderedPage)

        override fun load() = Unit

        override fun recycle() = Unit
    }
}
