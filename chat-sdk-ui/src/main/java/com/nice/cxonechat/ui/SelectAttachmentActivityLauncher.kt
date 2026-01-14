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

package com.nice.cxonechat.ui

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CaptureVideo
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.timedScope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.ui.storage.TemporaryFileProvider
import com.nice.cxonechat.ui.storage.TemporaryFileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * This class is responsible for launching a system activity which will report selected attachment(s)
 * via the provided [sendAttachments] callback.
 *
 * The [onCreate] method must be called from the owning activity's onCreate method (before the activity is in state Started).
 */
internal class SelectAttachmentActivityLauncher(
    private val context: Context,
    private val temporaryFileStorage: TemporaryFileStorage,
    private val sendAttachments: (uris: List<Uri>) -> Unit,
    private val registry: ActivityResultRegistry,
    logger: Logger,
) : DefaultLifecycleObserver, LoggerScope by LoggerScope<SelectAttachmentActivityLauncher>(logger) {
    private var getContent: ActivityResultLauncher<String>? = null
    private var getDocument: ActivityResultLauncher<Array<String>>? = null

    private var takePhoto: ActivityResultLauncher<Uri>? = null

    private var captureVideo: ActivityResultLauncher<Uri>? = null

    private var getMediaPicker: ActivityResultLauncher<PickVisualMediaRequest>? = null

    private var captureUri: Uri? = null
    private val captureUriGuard = Any()

    override fun onCreate(owner: LifecycleOwner) {
        timedScope("onCreate") {
            getContent = registry.register(
                key = "com.nice.cxonechat.ui.content",
                lifecycleOwner = owner,
                contract = GetMultipleContents(),
                callback = sendAttachments
            )
            getDocument = registry.register(
                key = "com.nice.cxonechat.ui.document",
                lifecycleOwner = owner,
                contract = OpenMultipleDocuments(),
                callback = sendAttachments
            )
            takePhoto = registry.register(
                key = "com.nice.cxonechat.ui.photo",
                lifecycleOwner = owner,
                contract = TakePicture(),
                callback = ::captureCallback
            )
            captureVideo = registry.register(
                key = "com.nice.cxonechat.ui.video",
                lifecycleOwner = owner,
                contract = CaptureVideo(),
                callback = ::captureCallback
            )
            getMediaPicker = registry.register(
                key = "com.nice.cxonechat.ui.media_picker",
                lifecycleOwner = owner,
                contract = ActivityResultContracts.PickMultipleVisualMedia(),
                callback = sendAttachments
            )
            Dispatchers.IO.asExecutor().execute(::restoreCaptureUri)
            debug("SelectAttachmentActivityLauncher initialized")
        }
    }

    private fun restoreCaptureUri() = scope("restoreCaptureUri") {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        synchronized(captureUriGuard) {
            captureUri = sharedPreferences.getString(CAPTURE_URI_KEY, null)?.let { uriString ->
                sharedPreferences.edit { remove(CAPTURE_URI_KEY) }
                runCatching {
                    uriString.toUri()
                }.onSuccess {
                    debug("CaptureUri restored")
                }.getOrNull()
            }
        }
    }

    private fun captureCallback(success: Boolean) = timedScope("captureCallback") {
        debug("Capture completed with success: $success")
        val uri = synchronized(captureUriGuard) {
            captureUri
        }
        when {
            success && uri != null -> sendAttachments(listOf(uri))
            else -> sendAttachments(emptyList())
        }
    }

    private fun createCaptureUri(prefix: String, suffix: String): Uri = scope("createCaptureUri") {
        val newFile = temporaryFileStorage.createCaptureFile(prefix, suffix)
        return if (newFile != null) {
            runCatching {
                TemporaryFileProvider.getUriForFile(newFile, context)
            }.onFailure {
                warning("Failed to get uri from file provider")
            }.getOrDefault(Uri.EMPTY)
        } else {
            warning("Failed to create temporary file for capture")
            Uri.EMPTY
        }
    }

    override fun onDestroy(owner: LifecycleOwner): Unit = timedScope("onDestroy") {
        getContent?.unregister()
        getContent = null
        getDocument?.unregister()
        getDocument = null
        takePhoto?.unregister()
        takePhoto = null
        captureVideo?.unregister()
        captureVideo = null
        getMediaPicker?.unregister()
        getMediaPicker = null
        synchronized(captureUriGuard) {
            captureUri?.let { uri ->
                Dispatchers.IO.asExecutor().execute { storeCaptureUri(uri) }
                captureUri = null
            }
        }
    }

    private fun storeCaptureUri(uri: Uri) = scope("storeCaptureUri") {
        context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(CAPTURE_URI_KEY, uri.toString())
        }
    }

    /**
     * start a foreign activity to find an attachment with the indicated mime types
     *
     * [mimeTypes] is one of the strings supplied by the chat instance.
     *
     * Note that this will work for finding existing resources, but not for opening
     * the camera for photos or videos.
     *
     * @param mimeTypes attachment types to find.
     *
     */
    fun getDocument(mimeTypes: Array<String>) = scope("getDocument") {
        if (mimeTypes.size == 1) {
            getContent?.launch(mimeTypes[0])
        } else {
            getDocument?.launch(mimeTypes)
        }
    }

    /**
     * Launches a media picker activity to select images, videos, or both.
     * This should be used only when MimeType is set to `image\*` or `video\*` or both.
     *
     * @param mediumType The type of media to be selected (image, video, or both).
     */
    fun pickMedia(mediumType: AttachmentType.MediaPicker) = scope("pickMedia") {
        when (mediumType) {
            AttachmentType.Image ->
                getMediaPicker?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

            AttachmentType.Video ->
                getMediaPicker?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))

            AttachmentType.ImageAndVideo -> getMediaPicker?.launch(PickVisualMediaRequest())
        }
    }

    /**
     * Launches an activity to capture or select an attachment based on the specified type.
     *
     * @param attachmentType The type of attachment to be captured or selected.
     */
    fun getAttachment(attachmentType: AttachmentType) = scope("getAttachment") {
        when (attachmentType) {
            AttachmentType.CameraPhoto -> synchronized(captureUriGuard) {
                captureUri = null
                Dispatchers.IO.asExecutor().execute {
                    val uri = createCaptureUri("photo_", ".jpeg")
                    synchronized(captureUriGuard) {
                        captureUri = uri
                    }
                    takePhoto?.launch(uri)
                }
            }

            AttachmentType.CameraVideo -> synchronized(captureUriGuard) {
                captureUri = null
                Dispatchers.IO.asExecutor().execute {
                    val uri = createCaptureUri("video_", ".mp4")
                    synchronized(captureUriGuard) {
                        captureUri = uri
                    }
                    captureVideo?.launch(uri)
                }
            }

            is AttachmentType.MediaPicker -> pickMedia(attachmentType)
            is AttachmentType.File -> getDocument(attachmentType.mimeTypes)
        }
    }

    private companion object {
        const val SHARED_PREF_NAME = "cxone_chat_attachment_prefs"
        const val CAPTURE_URI_KEY = "capture_uri"
    }
}

/**
 * Represents different types of attachments that can be sent.
 */
internal sealed interface AttachmentType {

    /**
     * Represents media picker types (image, video, or both).
     */
    sealed interface MediaPicker : AttachmentType

    /**
     * Represents media capture types (photo or video).
     */
    sealed interface CaptureMedia : AttachmentType

    /**
     * Represents capturing a photo using the camera.
     */
    data object CameraPhoto : CaptureMedia

    /**
     * Represents capturing a video using the camera.
     */
    data object CameraVideo : CaptureMedia

    /**
     * Represents selecting an image using the media picker.
     */
    data object Image : MediaPicker

    /**
     * Represents selecting a video using the media picker.
     */
    data object Video : MediaPicker

    /**
     * Represents selecting both images and videos using the media picker.
     */
    data object ImageAndVideo : MediaPicker

    /**
     * Represents selecting a file with specific MIME types.
     *
     * @property mimeTypes An array of MIME types to filter the selectable files.
     */
    data class File(val mimeTypes: Array<String>) : AttachmentType {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as File
            return mimeTypes.contentEquals(other.mimeTypes)
        }

        override fun hashCode(): Int = mimeTypes.contentHashCode()
    }
}
