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

package com.nice.cxonechat.ui.util

import android.graphics.Bitmap
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow

/**
 * The part of a PDF renderer a thumbnail draws from: the document's pages and each page's bitmap once rendered.
 *
 * [PdfRender] is the only implementation the chat uses. The interface lets a screenshot test draw from a stubbed
 * page instead, [android.graphics.pdf.PdfRenderer] being a native that decodes nothing on the JVM.
 *
 * Closing is deliberately not part of it, so that a thumbnail cannot close a document it did not open.
 */
internal interface PdfPageSource {

    /** The document's pages in order, empty when none could be opened. */
    val pageList: List<Page>

    /** A single page of the document. */
    interface Page {

        /** The rendered page, `null` until [load] has produced it. */
        val pageContent: StateFlow<Bitmap?>

        /** Starts rendering this page, unless it is already rendered or rendering. */
        fun load()

        /** Releases the rendered page, leaving it loadable again. */
        fun recycle()
    }
}

/**
 * The document every PDF thumbnail below this point draws, or `null` to have each open the attachment it was
 * given.
 *
 * Nothing in the chat provides it. A screenshot test does, reaching the thumbnail through a whole conversation:
 * a local rather than a parameter threaded through the eight composables in between, as a message's colours are.
 *
 * Every thumbnail below shares the one instance, and none of them owns it — a provided document outlives any one
 * of them and is closed by whoever provided it.
 */
internal val LocalPdfPageSource = staticCompositionLocalOf<PdfPageSource?> { null }
