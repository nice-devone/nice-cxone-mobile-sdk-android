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

package com.nice.cxonechat.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.google.gson.Gson
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.AudioRecordingUiState
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.conversation.model.Message.Attachment
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.databinding.CustomSnackBarBinding
import com.nice.cxonechat.ui.databinding.FragmentChatThreadBinding
import com.nice.cxonechat.ui.domain.AttachmentSharingRepository
import com.nice.cxonechat.ui.main.ChatThreadViewModel.OnPopupActionState.ReceivedOnPopupAction
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.FAILURE
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.SUCCESS
import com.nice.cxonechat.ui.util.checkPermissions
import com.nice.cxonechat.ui.util.openWithAndroid
import com.nice.cxonechat.ui.util.repeatOnViewOwnerLifecycle
import com.nice.cxonechat.ui.util.showRationale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import javax.inject.Inject
import com.nice.cxonechat.ui.composable.conversation.model.Message as UiMessage

/**
 * Fragment presenting UI of one concrete chat thread (conversation).
 */
@Suppress(
    "TooManyFunctions" // Legacy for now
)
@AndroidEntryPoint
class ChatThreadFragment : Fragment() {

    private val viewModel: ChatThreadViewModel by viewModels()

    private val audioViewModel: AudioRecordingViewModel by viewModels()

    private var fragmentBinding: FragmentChatThreadBinding? = null

    private val activityLauncher by lazy {
        ActivityLauncher(requireActivity().activityResultRegistry)
            .also(lifecycle::addObserver)
    }

    @Inject
    internal lateinit var attachmentSharingRepository: AttachmentSharingRepository

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(RequestPermission()) { isGranted ->
            if (!isGranted) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.no_notifications_title)
                    .setMessage(R.string.no_notifications_message)
                    .setNeutralButton(R.string.ok, null)
                    .show()
            }
        }

    private val audioRequestPermissionLauncher = registerForActivityResult(
        RequestMultiplePermissions()
    ) { requestResults: Map<String, Boolean>? ->
        if (requestResults.orEmpty().any { !it.value }) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.recording_audio_permission_denied_title)
                .setMessage(R.string.recording_audio_permission_denied_body)
                .setNeutralButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentChatThreadBinding.inflate(layoutInflater, container, false)
        fragmentBinding = binding
        registerOnPopupActionListener()
        registerChatMetadataListener()
        registerMessageListener()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermissions(
                Manifest.permission.POST_NOTIFICATIONS,
                R.string.notifications_rationale
            )
        }
        activityLauncher // activity launcher has to self-register before onStart
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Workaround for issue that the optionsMenu is not updated,
        // until activity is resumed or user navigates elsewhere.
        activity?.invalidateOptionsMenu()
    }

    private fun checkNotificationPermissions(permission: String, @StringRes rationale: Int) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> Unit

            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
                showRationale(rationale) {
                    requestPermissionLauncher.launch(permission)
                }

            else ->
                requestPermissionLauncher.launch(permission)
        }
    }

    private fun registerOnPopupActionListener() {
        repeatOnViewOwnerLifecycle {
            viewModel.actionState.filterIsInstance<ReceivedOnPopupAction>().collect {
                val rawVariables = it.variables
                try {
                    val variables = Gson().toJson(rawVariables)
                    val jsonObject = JSONTokener(variables).nextValue() as JSONObject
                    val headingText = jsonObject.getString("headingText")
                    val bodyText = jsonObject.getString("bodyText")
                    val action = jsonObject.getJSONObject("action")
                    val actionText = action.getString("text")
                    val actionUrl = action.getString("url")
                    val data = SnackbarSetupData(headingText, bodyText, actionText, actionUrl, it)
                    showSnackBar(data)
                } catch (expected: Exception) {
                    Toast.makeText(requireContext(), "Unable to decode ReceivedOnPopupAction", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun registerChatMetadataListener() {
        repeatOnViewOwnerLifecycle {
            viewModel.chatMetadata.collect { chatData ->
                activity?.title = chatData.threadName
            }
        }
    }

    private fun registerMessageListener() {
        repeatOnViewOwnerLifecycle {
            val threadNameFlow = viewModel.chatMetadata.map { it.threadName }
            fragmentBinding?.composeThreadView!!.setContent {
                LaunchedEffect(key1 = viewModel) {
                    viewModel.refresh()
                }

                ChatTheme {
                    ChatConversation(
                        conversationState = ConversationUiState(
                            threadName = threadNameFlow,
                            sdkMessages = viewModel.messages,
                            typingIndicator = viewModel.agentState,
                            sendMessage = viewModel::sendMessage,
                            onClick = ::onMessageClick,
                            onLongClick = ::onMessageLongClick,
                            loadMore = viewModel::loadMore,
                            canLoadMore = viewModel.canLoadMore,
                            onStartTyping = ::onStartTyping,
                            onStopTyping = ::onStopTyping,
                        ),
                        audioRecordingState = AudioRecordingUiState(
                            uriFlow = audioViewModel.recordedUriFlow,
                            isRecordingFlow = audioViewModel.recordingFlow,
                            onDismiss = ::onDismissRecording,
                            onApprove = viewModel::sendAttachment,
                            onAudioRecordToggle = { onTriggerRecording() }
                        ),
                        onAttachmentTypeSelection = { activityLauncher.getContent(it) }
                    )
                }
            }
        }
    }

    private fun onMessageClick(message: UiMessage) {
        if (message !is Attachment) return
        val url = message.originalUrl
        val mimeType = message.mimeType.orEmpty()
        val directions = when {
            mimeType.startsWith("image/") -> ChatThreadFragmentDirections.actionChatThreadFragmentToImagePreviewActivity(
                url
            )

            mimeType.startsWith("video/") -> ChatThreadFragmentDirections.actionChatThreadFragmentToVideoPreviewActivity(
                url
            )

            else -> {
                openWithAndroid(message)
                return
            }
        }
        findNavController().navigate(directions)
    }

    private fun onMessageLongClick(message: UiMessage) {
        if (message !is Attachment) return
        val context = context ?: return
        lifecycleScope.launch {
            val intent = attachmentSharingRepository.createSharingIntent(message, context)
            if (intent == null) {
                Toast.makeText(
                    requireContext(),
                    "Unable to store attachment for sharing, please try again later",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startActivity(Intent.createChooser(intent, null))
            }
        }
    }

    private fun openWithAndroid(message: Attachment) {
        val context = context ?: return

        if (!context.openWithAndroid(message.originalUrl, message.mimeType)) {
            AlertDialog.Builder(context)
                .setTitle(R.string.unsupported_type_title)
                .setMessage(getString(R.string.unsupported_type_message, message.mimeType))
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }

    // TODO implement menu handling

    private fun onStartTyping() {
        viewModel.reportThreadRead()
        viewModel.reportTypingStarted()
    }

    private fun onStopTyping() {
        viewModel.reportTypingEnd()
    }

    private fun showSnackBar(data: SnackbarSetupData) {
        val binding = fragmentBinding ?: return
        val parentLayout = binding.root
        val snackbar = Snackbar.make(parentLayout, "", Snackbar.LENGTH_INDEFINITE)
        val snackBinding = CustomSnackBarBinding.inflate(layoutInflater, null, false)
        snackbar.view.setBackgroundColor(Color.TRANSPARENT)

        val snackbarLayout = snackbar.view as SnackbarLayout
        snackbarLayout.setPadding(0, 0, 0, 0)

        val headingTextView: TextView = snackBinding.headingTextView
        val bodyTextView: TextView = snackBinding.bodyTextView
        val actionTextView: TextView = snackBinding.actionTextView
        val closeButton: ImageButton = snackBinding.closeButton

        headingTextView.text = data.headingText
        bodyTextView.text = data.bodyText
        actionTextView.text = data.actionText

        val action = data.action

        actionTextView.setOnClickListener {
            viewModel.reportOnPopupActionClicked(action)
            // TODO build intent for the actionUrl
            viewModel.reportOnPopupAction(SUCCESS, action)
            snackbar.dismiss()
        }

        closeButton.setOnClickListener {
            viewModel.reportOnPopupAction(FAILURE, action)
            snackbar.dismiss()
        }

        snackbarLayout.addView(snackBinding.root, 0)
        snackbar.show()

        viewModel.reportOnPopupActionDisplayed(action)
    }

    @SuppressLint(
        "MissingPermission" // permission state is checked by `checkPermissions()` method
    )
    private suspend fun onTriggerRecording(): Boolean {
        if (!checkPermissions(
                permissions = requiredRecordAudioPermissions,
                rationale = R.string.recording_audio_permission_rationale,
                onAcceptPermissionRequest = audioRequestPermissionLauncher::launch
            )
        ) {
            return false
            // Permissions will need to be sorted out first, user will have to click the button again after that
        }
        val context = requireContext()
        return if (audioViewModel.recordingFlow.value) {
            audioViewModel.stopRecording(context)
        } else {
            audioViewModel.startRecording(context).isSuccess
        }
    }

    @SuppressLint(
        "MissingPermission" // permission state is checked by `checkPermissions()` method
    )
    private fun onDismissRecording() {
        if (!checkPermissions(
                permissions = requiredRecordAudioPermissions,
                rationale = R.string.recording_audio_permission_rationale,
                onAcceptPermissionRequest = audioRequestPermissionLauncher::launch
            )
        ) {
            return
        }
        audioViewModel.deleteLastRecording(requireContext()) {
            Toast.makeText(requireContext(), R.string.record_audio_failed_cleanup, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        internal const val SENDER_ID = "1"
        internal val requiredRecordAudioPermissions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            setOf(Manifest.permission.RECORD_AUDIO)
        } else {
            setOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        private data class SnackbarSetupData(
            val headingText: String,
            val bodyText: String,
            val actionText: String,
            val actionUrl: String,
            val action: ReceivedOnPopupAction,
        )
    }

    /**
     * [LifecycleObserver][androidx.lifecycle.LifecycleObserver] intended to interface between the [ChatThreadFragment]
     * and document picker activities to pick attachments.  This is now the recommended method for calling the document
     * picker to fetch an image, video, or other document.
     *
     * At some point this could be expanded to support
     * [TakePicture][androidx.activity.result.contract.ActivityResultContracts.TakePicture] and friends.
     */
    inner class ActivityLauncher(
        private val registry: ActivityResultRegistry
    ) : DefaultLifecycleObserver {
        private var getContent: ActivityResultLauncher<String>? = null

        override fun onCreate(owner: LifecycleOwner) {
            getContent = registry.register("key", owner, GetContent()) { uri ->
                val safeUri = uri ?: return@register

                viewModel.sendAttachment(safeUri)
            }
        }

        /**
         * start a foreign activity to find an attachment with the indicated mime type
         *
         * [mimeType] is one of the strings contained in the string-array resource
         * attachment_type_mimetypes.
         *
         * Note that this will work for finding existing resources, but not for opening
         * the camera for photos or videos.
         *
         * @param mimeType attachment type to find.
         *
         */
        fun getContent(mimeType: String) = getContent?.launch(mimeType)
    }
}
