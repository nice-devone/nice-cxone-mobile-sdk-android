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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.ui.util.PdfRender.Companion.create
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Utility class for rendering PDF files into bitmaps with lifecycle management.
 * Use [create] to instantiate.
 */
internal class PdfRender private constructor(
    private val fileDescriptor: ParcelFileDescriptor,
    private val pdfRenderer: PdfRenderer,
    private val maxPages: Int? = null,
    logger: Logger = LoggerNoop,
) : AutoCloseable, LoggerScope by LoggerScope("PdfRender", logger) {
    private val mutex: Mutex = Mutex()
    private val isClosed = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val pageCount: Int = maxPages?.let { minOf(it, pdfRenderer.pageCount) } ?: pdfRenderer.pageCount

    val pageList: List<Page> = List(pageCount) {
        Page(
            mutex = mutex,
            index = it,
            pdfRenderer = pdfRenderer,
            coroutineScope = coroutineScope,
            parentScope = this,
        )
    }

    /**
     * Closes the `PdfRender` instance, releasing all resources associated with it.
     * This includes closing all pages and closing the file descriptor.
     * This method is idempotent and can be called multiple times safely.
     *
     * Note: This performs best-effort cleanup without blocking. In-flight render
     * operations will be canceled when the coroutineScope is canceled, which
     * happens automatically as pages check state before emitting results. This
     * approach avoids blocking the calling thread (which will be the UI thread
     * during Compose disposal, preventing ANR).
     */
    override fun close(): Unit = scope("close") {
        // Set closed flag FIRST to prevent new operations
        if (!isClosed.compareAndSet(false, true)) {
            return // Already closed
        }

        // Close all pages (best-effort, non-blocking)
        pageList.forEach { page ->
            runCatching { page.close() }.onFailure {
                warning("Failed to close page ${page.index}", it)
            }
        }

        // Cancel coroutines - this cancels all in-flight render jobs
        runCatching { coroutineScope.cancel("Closed PdfRender") }.onFailure {
            warning("Failed to close coroutineScope", it)
        }

        // Close resources on a background dispatcher under the same mutex used for rendering
        // This ensures no render operation is in progress when we close the renderer
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                runCatching { pdfRenderer.close() }.onFailure {
                    warning("Failed to close PdfRenderer", it)
                }
                runCatching { fileDescriptor.close() }.onFailure {
                    warning("Failed to close file descriptor", it)
                }
            }
        }
    }

    companion object {
        /**
         * Creates PdfRender instance. Closes PdfRenderer and FD on failure to prevent leaks.
         */
        suspend fun create(
            fileDescriptor: ParcelFileDescriptor,
            maxPages: Int? = null,
            logger: Logger = LoggerNoop,
        ): Result<PdfRender> = withContext(Dispatchers.IO) {
            val scope = LoggerScope("PdfRender.Companion", logger)
            scope.scope("create") {
                var pdfRenderer: PdfRenderer? = null
                runCatching {
                    pdfRenderer = PdfRenderer(fileDescriptor)
                    PdfRender(fileDescriptor, pdfRenderer, maxPages, logger)
                }.onFailure {
                    pdfRenderer?.runCatching { close() }?.onFailure { closeError ->
                        warning("Failed to close PdfRenderer after creation failure", closeError)
                    }
                    fileDescriptor.runCatching { close() }.onFailure { closeError ->
                        warning("Failed to close FD after creation failure", closeError)
                    }
                }
            }
        }
    }

    /**
     * Represents a single page of the PDF file.
     *
     * @property mutex A `Mutex` to ensure thread-safe operations on the page.
     * @property index The index of the page in the PDF file.
     * @property pdfRenderer The `PdfRenderer` instance used to render the page.
     * @property coroutineScope The `CoroutineScope` for managing asynchronous operations.
     * @param parentScope The parent `LoggerScope` for delegated logging operations.
     */
    internal class Page(
        val mutex: Mutex,
        val index: Int,
        val pdfRenderer: PdfRenderer,
        val coroutineScope: CoroutineScope,
        parentScope: LoggerScope,
    ) : AutoCloseable, LoggerScope by LoggerScope("Page_$index", parentScope) {

        /**
         * State machine for page lifecycle.
         * Valid transitions:
         * INITIAL → LOADING → LOADED → (recycle back to) INITIAL → ... → CLOSED
         *
         * Note: recycle() transitions from LOADED back to INITIAL, allowing reuse.
         */
        private enum class PageState {
            INITIAL, // Created/recycled, dimensions loaded, ready to load bitmap
            LOADING, // Bitmap loading in progress
            LOADED, // Bitmap loaded and available
            CLOSED // Permanently closed, cannot be used
        }

        private val state = AtomicReference(PageState.INITIAL)

        @Volatile
        private var loadingJob: Job? = null

        private val dimensionResult: Result<Dimension> = scope("dimensionResult") {
            runCatching {
                pdfRenderer.openPage(index).use { currentPage ->
                    Dimension(
                        width = currentPage.width,
                        height = currentPage.height
                    )
                }
            }.onFailure { exception ->
                warning("Failed to initialize page dimensions for page $index: ${exception.message}", exception)
            }
        }

        private val dimension: Dimension
            get() = dimensionResult.getOrElse { Dimension.ZERO }

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
         * @return The calculated height of the page, or 0 if page is closed or has invalid dimensions.
         */
        fun heightByWidth(width: Int): Int {
            // Early validation - return 0 for any invalid state
            if (state.get() == PageState.CLOSED || dimensionResult.isFailure) {
                return 0
            }

            val dim = dimension
            return when {
                dim.width == 0 || dim.height == 0 -> 0
                else -> {
                    val ratio = dim.width.toFloat() / dim.height
                    (width / ratio).toInt()
                }
            }
        }

        /**
         * Loads the page content into a bitmap asynchronously.
         * This method is safe to call multiple times and will not start
         * a new load operation if one is already in progress.
         *
         * Valid state transitions:
         * - INITIAL/RECYCLED → LOADING (allowed, start render)
         * - LOADED → no-op (already loaded)
         * - LOADING → no-op (already in progress)
         * - CLOSED → throws IllegalStateException (invalid usage)
         *
         * @throws IllegalStateException if called after close()
         */
        fun load() {
            // Synchronized check-and-transition for state machine
            synchronized(this) {
                when (val currentState = state.get()) {
                    PageState.INITIAL -> if (state.compareAndSet(currentState, PageState.LOADING)) {
                        loadingJob = coroutineScope.launch {
                            mutex.withLock {
                                // Check if cancelled or closed before rendering
                                if (!isActive || state.get() == PageState.CLOSED) {
                                    // Revert to previous state if cancelled before rendering
                                    state.compareAndSet(PageState.LOADING, currentState)
                                    return@withLock
                                }
                                renderPageToBitmap()
                            }
                        }
                    }

                    PageState.LOADED, PageState.LOADING -> Ignored // Already loaded or loading, no action needed
                    PageState.CLOSED -> error("Cannot load closed page $index") // Invalid usage - throw exception
                }
            }
        }

        private suspend fun CoroutineScope.renderPageToBitmap() = scope("renderPageToBitmap") {
            var bitmapToCleanup: Bitmap? = null
            try {
                val newBitmap = pdfRenderer.openPage(index).use { currentPage ->
                    createBitmap(
                        width = currentPage.width,
                        height = currentPage.height
                    ).also { bitmap ->
                        currentPage.render(
                            bitmap,
                            null,
                            null,
                            RENDER_MODE_FOR_DISPLAY
                        )
                    }
                }

                bitmapToCleanup = newBitmap

                // Only emit if still in LOADING state and transition to LOADED
                if (state.compareAndSet(PageState.LOADING, PageState.LOADED) && isActive) {
                    _pageContent.emit(newBitmap)
                    bitmapToCleanup = null // Successfully emitted, don't clean up
                }
            } catch (e: CancellationException) {
                // Revert to INITIAL/RECYCLED if cancelled during render
                if (!state.compareAndSet(PageState.LOADING, PageState.INITIAL) && state.get() == PageState.LOADED) {
                    recycle()
                }
                throw e // Don't log cancellation as error
            } catch (expected: Exception) {
                // Revert to CLOSED on error
                state.set(PageState.CLOSED)
                this@scope.warning("Failed to render page $index", expected)
            } finally {
                // Clean up bitmap if created but not emitted
                // Handles: cancellation, close during render, recycle during render
                bitmapToCleanup?.recycle()
            }
        }

        /**
         * Recycles the page content, releasing bitmap resources for memory management.
         * Unlike close(), this allows the page to be loaded again later.
         * This is useful for temporary cleanup in scenarios like Compose recomposition.
         *
         * Valid state transition:
         * - LOADED → INITIAL (allowed, clear bitmap)
         * - Other states → no-op (nothing to recycle)
         */
        fun recycle() {
            // Cancel any in-flight load operation
            loadingJob?.cancel("Recycling page for memory management")

            // Reset transition from LOADED to INITIAL
            if (state.compareAndSet(PageState.LOADED, PageState.INITIAL)) {
                cleanup()
            }
        }

        /**
         * Closes this Page instance permanently, releasing all resources.
         * This includes canceling any pending load operations and recycling
         * the bitmap. This method is idempotent and can be called multiple times safely.
         * After calling close(), the page cannot be reused.
         *
         * Note: This performs best-effort cleanup without waiting for in-flight
         * operations to complete, to avoid blocking the calling thread. In-flight
         * render jobs will be cancelled by the parent coroutineScope cancellation
         * and will not emit results due to the CLOSED state check.
         */
        override fun close() {
            // Transition to CLOSED state (any state → CLOSED is valid)
            val previousState = state.getAndSet(PageState.CLOSED)
            if (previousState == PageState.CLOSED) {
                return // Already closed
            }

            // Cancel any in-flight job - final cleanup happens via parent scope cancellation
            loadingJob?.cancel("Closing page")

            cleanup()
        }

        /** Cleanup bitmap. */
        private fun cleanup() {
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
    ) {
        companion object {
            val ZERO = Dimension(0, 0)
        }
    }
}
