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

package com.nice.cxonechat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.navArgs
import coil.compose.AsyncImage
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TopBar
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Activity to preview images in chat transcript.
 */
class ImagePreviewActivity : ComponentActivity() {
    private val imageUrl: String by lazy { navArgs<ImagePreviewActivityArgs>().value.imageUrl }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatTheme {
                ChatTheme.Scaffold(
                    topBar = {
                        ChatTheme.TopBar(
                            title = stringResource(string.image_preview_title)
                        )
                    }
                ) { paddingValues ->
                    ZoomableImage(paddingValues)
                }
            }
        }
    }

    @Composable
    private fun ZoomableImage(paddingValues: PaddingValues) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .zoomable(rememberZoomState())
        )
    }
}
