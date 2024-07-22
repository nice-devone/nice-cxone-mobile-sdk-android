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
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.EditCustomValuesDialog
import com.nice.cxonechat.ui.EditThreadNameDialog
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.AudioPlayerDialog
import com.nice.cxonechat.ui.composable.conversation.AudioRecordingUiState
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.EndConversationDialog
import com.nice.cxonechat.ui.composable.conversation.SelectAttachmentsDialog
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.generic.ImageViewerDialogCard
import com.nice.cxonechat.ui.composable.generic.VideoViewerDialogCard
import com.nice.cxonechat.ui.composable.theme.Alert
import com.nice.cxonechat.ui.composable.theme.BusySpinner
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.customvalues.mergeWithCustomField
import com.nice.cxonechat.ui.databinding.CustomSnackBarBinding
import com.nice.cxonechat.ui.databinding.FragmentChatThreadBinding
import com.nice.cxonechat.ui.domain.AttachmentSharingRepository
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.AudioPlayer
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.CustomValues
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.EditThreadName
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.EndContact
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.ErrorAttachmentNotSupported
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.ErrorAttachmentTooLarge
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.ErrorUnableToReadAttachment
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.ImageViewer
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.SelectAttachments
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.VideoPlayer
import com.nice.cxonechat.ui.main.ChatThreadViewModel.OnPopupActionState.ReceivedOnPopupAction
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.Failure
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.Success
import com.nice.cxonechat.ui.model.EndConversationChoice.CLOSE_CHAT
import com.nice.cxonechat.ui.model.EndConversationChoice.NEW_CONVERSATION
import com.nice.cxonechat.ui.model.EndConversationChoice.SHOW_TRANSCRIPT
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.util.checkPermissions
import com.nice.cxonechat.ui.util.contentDescription
import com.nice.cxonechat.ui.util.openWithAndroid
import com.nice.cxonechat.ui.util.repeatOnViewOwnerLifecycle
import com.nice.cxonechat.ui.util.showRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.UUID

/**
 * Fragment presenting UI of one concrete chat thread (conversation).
 */
@Suppress(
    "TooManyFunctions", // Legacy for now
    "LargeClass",
)
class ChatThreadFragment : Fragment() {

    private val chatViewModel: ChatThreadViewModel by viewModel()
    private val chatModel: ChatViewModel by activityViewModel()

    private val audioViewModel: AudioRecordingViewModel by viewModel()

    private val activityLauncher by lazy {
        ActivityLauncher(requireActivity().activityResultRegistry)
            .also(lifecycle::addObserver)
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(RequestPermission()) { isGranted ->
            if (!isGranted) {
                AlertDialog.Builder(requireContext())
                    .setTitle(string.no_notifications_title)
                    .setMessage(string.no_notifications_message)
                    .setNeutralButton(string.ok, null)
                    .show()
            }
        }

    private val audioRequestPermissionLauncher = registerForActivityResult(
        RequestMultiplePermissions()
    ) { requestResults: Map<String, Boolean>? ->
        if (requestResults.orEmpty().any { !it.value }) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(string.recording_audio_permission_denied_title)
                .setMessage(string.recording_audio_permission_denied_body)
                .setNeutralButton(string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
        }
    }

    private val valueStorage: ValueStorage by inject()

    private val attachmentSharingRepository: AttachmentSharingRepository by inject()

    private var fragmentBinding: FragmentChatThreadBinding? = null

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
                string.notifications_rationale
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
            chatViewModel.actionState.filterIsInstance<ReceivedOnPopupAction>().collect {
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
                    Toast.makeText(
                        requireContext(),
                        "Unable to decode ReceivedOnPopupAction",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun registerChatMetadataListener() {
        repeatOnViewOwnerLifecycle {
            chatViewModel.chatMetadata.collect { chatData ->
                activity?.title = chatData.threadName
            }
        }
    }

    private fun registerMessageListener() {
        repeatOnViewOwnerLifecycle {
            val threadNameFlow = chatViewModel.chatMetadata.map { it.threadName }

            fragmentBinding?.composeThreadView!!.setContent {
                ContentView(threadNameFlow)
                DialogView()
            }
        }
    }

    @Composable
    private fun DialogView() {
        when (val dialog = chatViewModel.dialogShown.collectAsState(None).value) {
            None -> Unit
            CustomValues -> CustomValuesDialog()
            EditThreadName -> EditThreadNameDialog(
                threadName = chatViewModel.selectedThreadName.orEmpty(),
                onCancel = chatViewModel::dismissDialog,
                onAccept = chatViewModel::confirmEditThreadName
            )

            is AudioPlayer -> AudioPlayerDialog(
                url = dialog.url,
                title = dialog.title,
                onCancel = chatViewModel::dismissDialog,
            )

            is SelectAttachments -> SelectAttachmentsDialog(
                attachments = dialog.attachments,
                title = dialog.title.orEmpty(),
                onAttachmentTapped = ::onAttachmentClicked,
                onCancel = chatViewModel::dismissDialog,
                onShare = ::onShare,
            )

            is ImageViewer -> ImageViewerDialogCard(
                image = dialog.image,
                title = dialog.title,
                onDismiss = chatViewModel::dismissDialog,
            )

            is VideoPlayer -> VideoViewerDialogCard(
                uri = dialog.uri,
                title = dialog.title,
                onDismiss = chatViewModel::dismissDialog
            )

            ErrorAttachmentNotSupported -> ErrorDialog(
                title = stringResource(id = string.attachment_upload_failure),
                message = stringResource(id = string.attachment_not_supported),
            )

            ErrorAttachmentTooLarge -> ErrorDialog(
                title = stringResource(id = string.attachment_upload_failure),
                message = stringResource(id = string.attachment_too_large, chatViewModel.maxAttachmentSize),
            )

            ErrorUnableToReadAttachment -> ErrorDialog(
                title = stringResource(id = string.attachment_upload_failure),
                message = stringResource(id = string.attachment_read_error)
            )

            EndContact -> EndContactDialog()
        }

        if (chatViewModel.preparingToShare.collectAsState().value) {
            BusySpinner(message = stringResource(string.preparing))
        }
    }

    @Composable
    private fun EndContactDialog() {
        ChatTheme {
            ChatTheme.EndConversationDialog(
                assignedAgent = chatViewModel.chatMetadata.collectAsState(initial = null).value?.agent,
                onDismiss = chatViewModel::dismissDialog,
                onUserSelection = {
                    when (it) {
                        SHOW_TRANSCRIPT -> {
                            // no-op required
                        }

                        NEW_CONVERSATION -> chatModel.refreshThreadState(true)
                        CLOSE_CHAT -> requireActivity().finish()
                    }
                }
            )
        }
    }

    @Composable
    private fun ErrorDialog(
        title: String,
        message: String,
    ) {
        ChatTheme.Alert(
            title = title,
            message = message,
            dismissLabel = stringResource(id = string.ok),
            onDismiss = chatViewModel::dismissDialog
        )
    }

    @Composable
    private fun CustomValuesDialog() {
        EditCustomValuesDialog(
            title = stringResource(string.edit_custom_field_title),
            fields = chatViewModel
                .preChatSurvey
                ?.fields
                .orEmpty()
                .mergeWithCustomField(
                    chatViewModel.customValues
                ),
            onCancel = chatViewModel::cancelEditingCustomValues,
            onConfirm = chatViewModel::confirmEditingCustomValues
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ContentView(threadNameFlow: Flow<String?>) {
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

        /** Key to force refresh if the selected thread has changed. */
        val threadId by remember {
            chatViewModel.chatThreadHandler.map { it.get().id }
        }.collectAsState(NIL_UUID)
        if (lifecycleState === State.RESUMED) {
            LaunchedEffect(lifecycleState, threadId) {
                if (threadId !== NIL_UUID) {
                    chatViewModel.refresh()
                }
            }
        }

        ChatTheme {
            ChatConversation(
                conversationState = ConversationUiState(
                    threadName = threadNameFlow,
                    sdkMessages = chatViewModel.messages,
                    typingIndicator = chatViewModel.agentState,
                    positionInQueue = chatViewModel.positionInQueue,
                    sendMessage = chatViewModel::sendMessage,
                    loadMore = chatViewModel::loadMore,
                    canLoadMore = chatViewModel.canLoadMore,
                    onStartTyping = ::onStartTyping,
                    onStopTyping = ::onStopTyping,
                    onAttachmentClicked = ::onAttachmentClicked,
                    onMoreClicked = ::onMoreClicked,
                    onShare = ::onShare,
                    isMultiThreaded = chatViewModel.isMultiThreadEnabled,
                    isLiveChat = chatViewModel.isLiveChat,
                    hasQuestions = chatViewModel.hasQuestions,
                    isArchived = chatViewModel.isArchived,
                ),
                audioRecordingState = AudioRecordingUiState(
                    uriFlow = audioViewModel.recordedUriFlow,
                    isRecordingFlow = audioViewModel.recordingFlow,
                    onDismiss = ::onDismissRecording,
                    onApprove = chatViewModel::sendAttachment,
                    onAudioRecordToggle = ::onTriggerRecording
                ),
                onAttachmentTypeSelection = {
                    activityLauncher.getDocument(it.toTypedArray())
                },
                onEditThreadName = ::showEditThreadName,
                onEditThreadValues = ::showEditCustomValues,
                onEndContact = ::endContact,
                displayEndConversation = chatViewModel::showEndContactDialog,
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true // Enabled for UI test automation
                }
            )
        }
    }

    private fun showEditThreadName() {
        chatViewModel.editThreadName()
    }

    private fun showEditCustomValues() {
        chatViewModel.startEditingCustomValues()
    }

    private fun endContact() {
        chatViewModel.endContact()
    }

    private fun onMoreClicked(attachments: List<Attachment>, title: String) {
        chatViewModel.selectAttachments(attachments, title)
    }

    private fun onShare(attachments: Collection<Attachment>) {
        chatViewModel.beginPrepareAttachments()

        val context = context ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val intent = attachmentSharingRepository.createSharingIntent(attachments, context)
            chatViewModel.finishPrepareAttachments()
            lifecycleScope.launch(Dispatchers.Main) {
                if (intent == null) {
                    Toast.makeText(
                        requireContext(),
                        getString(string.prepare_attachments_failure),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    startActivity(Intent.createChooser(intent, null))
                }
            }
        }
    }

    private fun onAttachmentClicked(attachment: Attachment) {
        val url = attachment.url
        val mimeType = attachment.mimeType.orEmpty()
        val title by lazy { attachment.contentDescription }
        when {
            mimeType.startsWith("image/") ->
                chatViewModel.showImage(url, title ?: getString(string.image_preview_title))

            mimeType.startsWith("video/") ->
                chatViewModel.showVideo(url, title ?: getString(string.video_preview_title))

            mimeType.startsWith("audio/") -> chatViewModel.playAudio(url, title)
            else -> openWithAndroid(attachment)
        }
    }

    private fun openWithAndroid(attachment: Attachment) {
        val context = context ?: return

        if (!context.openWithAndroid(attachment.url, attachment.mimeType)) {
            AlertDialog.Builder(context)
                .setTitle(string.unsupported_type_title)
                .setMessage(getString(string.unsupported_type_message, attachment.mimeType))
                .setNegativeButton(string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }

    // TODO implement menu handling

    private fun onStartTyping() {
        chatViewModel.reportThreadRead()
        chatViewModel.reportTypingStarted()
    }

    private fun onStopTyping() {
        chatViewModel.reportTypingEnd()
    }

    private fun showSnackBar(data: SnackbarSetupData) {
        val binding = fragmentBinding ?: return
        val parentLayout = binding.root
        val snackbar = Snackbar.make(parentLayout, "", Snackbar.LENGTH_INDEFINITE)
        val snackBinding = CustomSnackBarBinding.inflate(layoutInflater, null, false)
        snackbar.view.setBackgroundColor(Color.TRANSPARENT)

        val snackbarLayout = snackbar.view as ViewGroup
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
            chatViewModel.reportOnPopupActionClicked(action)
            // TODO build intent for the actionUrl
            chatViewModel.reportOnPopupAction(Success, action)
            snackbar.dismiss()
        }

        closeButton.setOnClickListener {
            chatViewModel.reportOnPopupAction(Failure, action)
            snackbar.dismiss()
        }

        snackbarLayout.addView(snackBinding.root, 0)
        snackbar.show()

        chatViewModel.reportOnPopupActionDisplayed(action)
    }

    @SuppressLint(
        "MissingPermission" // permission state is checked by `checkPermissions()` method
    )
    private suspend fun onTriggerRecording(): Boolean {
        if (!checkPermissions(
                valueStorage = valueStorage,
                permissions = requiredRecordAudioPermissions,
                rationale = string.recording_audio_permission_rationale,
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
        lifecycleScope.launch {
            if (!checkPermissions(
                    valueStorage = valueStorage,
                    permissions = requiredRecordAudioPermissions,
                    rationale = string.recording_audio_permission_rationale,
                    onAcceptPermissionRequest = audioRequestPermissionLauncher::launch
                )
            ) {
                return@launch
            }
            audioViewModel.deleteLastRecording(requireContext()) {
                Toast.makeText(requireContext(), string.record_audio_failed_cleanup, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        internal const val SENDER_ID = "1"

        private val NIL_UUID = UUID(0, 0)

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
        private var getDocument: ActivityResultLauncher<Array<String>>? = null

        override fun onCreate(owner: LifecycleOwner) {
            getContent = registry.register("com.nice.cxonechat.ui.content", owner, GetContent()) { uri ->
                val safeUri = uri ?: return@register

                chatViewModel.sendAttachment(safeUri)
            }
            getDocument = registry.register("com.nice.cxonechat.ui.document", owner, OpenDocument()) { uri ->
                val safeUri = uri ?: return@register

                chatViewModel.sendAttachment(safeUri)
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
        fun getDocument(mimeTypes: Array<String>) {
            if (mimeTypes.size == 1) {
                getContent?.launch(mimeTypes[0])
            } else {
                getDocument?.launch(mimeTypes)
            }
        }
    }
}
