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

package com.nice.cxonechat.ui.domain

import com.nice.cxonechat.ui.AttachmentType
import com.nice.cxonechat.ui.data.source.AllowedFileType
import java.util.EnumMap

internal object GetAllowedAttachmentGroups {
    private const val ANY_IMAGE = "image/*"
    private const val CAMERA_IMAGE = "image/jpeg"
    private const val ANY_VIDEO = "video/*"
    private const val CAMERA_VIDEO = "video/mp4"

    private val FULL_MIMETYPE_REGEX = Regex(".+/.+")

    private val IMAGE = "image(/.+)?".toRegex()
    private val VIDEO = "video(/.+)?".toRegex()

    fun allowedAttachmentGroups(
        attachmentOptions: AttachmentOptions,
        allowedFileTypes: List<AllowedFileType>,
    ): List<MimeTypeGroup> {
        val mimeTypes = allowedFileTypes.map { it.mimeType.addSubtypeIfMissing() }.toSet()

        val canCaptureImage = mimeTypes.any { it == ANY_IMAGE || it == CAMERA_IMAGE }
        val canCaptureVideo = mimeTypes.any { it == ANY_VIDEO || it == CAMERA_VIDEO }

        val hasImageWildcard = mimeTypes.contains(ANY_IMAGE)
        val hasVideoWildcard = mimeTypes.contains(ANY_VIDEO)
        val hasMultiMedia = hasImageWildcard && hasVideoWildcard

        val imageTypes = if (hasMultiMedia) emptyList() else mimeTypes.filter { IMAGE.matches(it) && it != ANY_IMAGE }
        val videoTypes = if (hasMultiMedia) emptyList() else mimeTypes.filter { VIDEO.matches(it) && it != ANY_VIDEO }
        val preciseMediaTypes = imageTypes + videoTypes

        val result = mutableListOf<MimeTypeGroup>()

        attachmentOptions.forEach { (option, label) ->
            when (option) {
                AttachmentOption.CameraPhoto -> result.addCameraPhotoOption(label, canCaptureImage)
                AttachmentOption.CameraVideo -> result.addCameraVideoOption(label, canCaptureVideo)
                AttachmentOption.ImageAndVideo -> result.addImageAndVideoOption(
                    label = label,
                    hasMultiMedia = hasMultiMedia,
                    preciseMediaTypes = preciseMediaTypes,
                    hasImageWildcard = hasImageWildcard,
                    hasVideoWildcard = hasVideoWildcard,
                )

                AttachmentOption.File -> result.addFileOption(label, mimeTypes)
            }
        }
        return result
    }

    private fun MutableList<MimeTypeGroup>.addCameraPhotoOption(
        label: String,
        canCaptureImage: Boolean,
    ) {
        if (canCaptureImage) {
            add(MimeTypeGroup(label, AttachmentType.CameraPhoto))
        }
    }

    private fun MutableList<MimeTypeGroup>.addCameraVideoOption(
        label: String,
        canCaptureVideo: Boolean,
    ) {
        if (canCaptureVideo) {
            add(MimeTypeGroup(label, AttachmentType.CameraVideo))
        }
    }

    private fun MutableList<MimeTypeGroup>.addImageAndVideoOption(
        label: String,
        hasMultiMedia: Boolean,
        preciseMediaTypes: List<String>,
        hasImageWildcard: Boolean,
        hasVideoWildcard: Boolean,
    ) {
        when {
            hasMultiMedia -> add(MimeTypeGroup(label, AttachmentType.ImageAndVideo))
            preciseMediaTypes.isNotEmpty() -> {
                val mediaTypes = when {
                    hasImageWildcard -> preciseMediaTypes + ANY_IMAGE
                    hasVideoWildcard -> preciseMediaTypes + ANY_VIDEO
                    else -> preciseMediaTypes
                }.toTypedArray()
                add(MimeTypeGroup(label, AttachmentType.File(mediaTypes)))
            }

            hasImageWildcard -> add(MimeTypeGroup(label, AttachmentType.Image))
            hasVideoWildcard -> add(MimeTypeGroup(label, AttachmentType.Video))
        }
    }

    private fun MutableList<MimeTypeGroup>.addFileOption(
        label: String,
        mimeTypes: Set<String>,
    ) {
        if (mimeTypes.isNotEmpty()) {
            add(MimeTypeGroup(label, AttachmentType.File(mimeTypes.toTypedArray())))
        }
    }

    private fun String.addSubtypeIfMissing() = if (FULL_MIMETYPE_REGEX.matches(this)) this else "$this/*"
}

/**
 * Holds label for a group of mime types.
 */
internal data class MimeTypeGroup(
    val label: String,
    val options: AttachmentType,
)

/**
 * EnumMap alias for mapping attachment options to their string labels.
 */
typealias AttachmentOptions = EnumMap<AttachmentOption, String>

/**
 * Enum representing the available attachment options.
 */
enum class AttachmentOption {
    /** Capture a photo using the camera. */
    CameraPhoto,

    /** Capture a video using the camera. */
    CameraVideo,

    /** Select images and videos using the media picker. */
    ImageAndVideo,

    /** Select a file with specific MIME types. */
    File,
}
