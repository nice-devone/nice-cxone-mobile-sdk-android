/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A utility class for rendering PDF files into bitmaps.
 *
 * This class provides functionality to render individual pages of a PDF file
 * and manage their lifecycle, including loading and recycling bitmaps.
 *
 * @param fileDescriptor The file descriptor of the PDF file to be rendered.
 * @param maxPages Optional limit on the number of pages to render.
 */
internal class PdfRender private constructor(
    private val fileDescriptor: ParcelFileDescriptor,
    private val maxPages: Int? = null,
) {
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    private val mutex: Mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val pageCount get() = { maxPages?.let { minOf(it, pdfRenderer.pageCount) } ?: pdfRenderer.pageCount }

    val pageLists: List<Page> = List(pageCount()) {
        Page(
            mutex = mutex,
            index = it,
            pdfRenderer = pdfRenderer,
            coroutineScope = coroutineScope,
        )
    }

    /**
     * Closes the `PdfRender` instance, releasing all resources associated with it.
     * This includes recycling all pages and closing the file descriptor.
     */
    fun close() {
        pageLists.forEach(Page::recycle)
        pdfRenderer.close()
        fileDescriptor.close()
    }

    companion object {
        /**
         * Factory method to create a `PdfRender` instance asynchronously.
         *
         * @param fileDescriptor The file descriptor of the PDF file.
         * @param maxPages Optional limit on the number of pages to render.
         * @return A `Result` containing the created `PdfRender` instance or an error.
         */
        suspend fun create(
            fileDescriptor: ParcelFileDescriptor,
            maxPages: Int? = null,
        ): Result<PdfRender> = withContext(Dispatchers.IO) {
            runCatching { PdfRender(fileDescriptor, maxPages) }
        }
    }

    /**
     * Represents a single page of the PDF file.
     *
     * @property mutex A `Mutex` to ensure thread-safe operations on the page.
     * @property index The index of the page in the PDF file.
     * @property pdfRenderer The `PdfRenderer` instance used to render the page.
     * @property coroutineScope The `CoroutineScope` for managing asynchronous operations.
     */
    internal class Page(
        val mutex: Mutex,
        val index: Int,
        val pdfRenderer: PdfRenderer,
        val coroutineScope: CoroutineScope,
    ) {
        private var isLoaded = false

        private var job: Job? = null

        private val dimension = pdfRenderer.openPage(index).use { currentPage ->
            Dimension(
                width = currentPage.width,
                height = currentPage.height
            )
        }

        private val _pageContent = MutableStateFlow<Bitmap?>(null)

        /**
         * A [StateFlow] holding the bitmap content of the page.
         * Initially, this is `null` until the page is loaded.
         */
        val pageContent: StateFlow<Bitmap?>
            get() = _pageContent.asStateFlow()

        /**
         * Calculates the height of the page based on a given width, preserving the aspect ratio.
         *
         * @param width The desired width of the page.
         * @return The calculated height of the page.
         */
        fun heightByWidth(width: Int): Int {
            val ratio = dimension.width.toFloat() / dimension.height
            return (ratio * width).toInt()
        }

        /**
         * Loads the page content into a bitmap asynchronously.
         */
        fun load() {
            if (!isLoaded) {
                job = coroutineScope.launch {
                    mutex.withLock {
                        val newBitmap: Bitmap
                        pdfRenderer.openPage(index).use { currentPage ->
                            newBitmap = createBitmap(
                                width = currentPage.width,
                                height = currentPage.height
                            )
                            currentPage.render(
                                newBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                        }
                        isLoaded = true
                        _pageContent.emit(newBitmap)
                    }
                }
            }
        }

        /**
         * Recycles the page content, releasing the bitmap resources.
         * Sets the page content to `null` and marks the page as unloaded.
         */
        fun recycle() {
            isLoaded = false
            val oldBitmap = _pageContent.value
            _pageContent.tryEmit(null)
            oldBitmap?.recycle()
        }

        private fun createBitmap(
            width: Int,
            height: Int,
            color: Int = Color.WHITE,
        ): Bitmap = createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            canvas.drawColor(color)
            canvas.drawBitmap(this, 0f, 0f, null)
        }
    }

    data class Dimension(
        val width: Int,
        val height: Int,
    )
}
