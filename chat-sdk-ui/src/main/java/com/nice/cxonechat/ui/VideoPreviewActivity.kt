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

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.navArgs
import com.nice.cxonechat.ui.databinding.ActivityVideoPreviewBinding

/**
 * Activity to preview videos from chat transcript.
 */
class VideoPreviewActivity : AppCompatActivity() {
    @Suppress("LateinitUsage")
    private lateinit var binding: ActivityVideoPreviewBinding
    private val videoUrl: String by lazy { navArgs<VideoPreviewActivityArgs>().value.videoUrl }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            val mediaController = MediaController(this@VideoPreviewActivity)
            mediaController.setAnchorView(videoView)

            videoView.setVideoURI(Uri.parse(videoUrl))
            videoView.setMediaController(mediaController)
            videoView.start()
        }
    }
}
