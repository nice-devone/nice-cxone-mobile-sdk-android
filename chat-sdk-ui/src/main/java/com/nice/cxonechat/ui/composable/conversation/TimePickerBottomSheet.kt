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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.TimeSlot
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message.TimePicker
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.DateProvider
import com.nice.cxonechat.ui.util.formatDuration
import com.nice.cxonechat.ui.util.formatHeader
import com.nice.cxonechat.ui.util.formatTime
import com.nice.cxonechat.ui.util.preview.message.TimePickerSlot
import com.nice.cxonechat.ui.util.preview.message.UiSdkTimePicker
import com.nice.cxonechat.ui.util.toDateKey
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerBottomSheet(
    message: TimePicker,
    onDismiss: () -> Unit,
    onDone: (TimeSlot, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) {
        it === Expanded
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets() },
        containerColor = chatColors.token.background.default,
        contentColor = chatColors.token.content.primary,
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(chatColors.token.background.surface.subtle)
            ) {
                Surface(
                    modifier =
                        Modifier
                            .padding(vertical = space.large)
                            .align(Alignment.Center),
                    color = chatColors.token.content.tertiary,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp))
                }
            }
        },
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .semantics {
                testTag = "time_picker_bottom_sheet"
            }
    ) {
        TimePickerBottomSheetContent(message = message, onDismiss = onDismiss, onDone = onDone)
    }
}

@Composable
internal fun TimePickerBottomSheetContent(
    message: TimePicker,
    onDismiss: () -> Unit,
    onDone: (TimeSlot, String) -> Unit,
) {
    var selectedSlot: TimeSlot? by remember { mutableStateOf(null) }
    val groupedSlots = remember(message.timeSlots) {
        groupSlotsByDate(message.timeSlots)
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(chatColors.token.background.surface.subtle)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = space.medium)
        ) {
            TimePickerHeader(message.popupTitle)
            TimePickerSlotGroups(
                groupedSlots = groupedSlots,
                selectedSlot = selectedSlot,
                onSelected = { slot -> selectedSlot = if (selectedSlot == slot) null else slot }
            )
        }
        HorizontalDivider(color = chatColors.token.border.default)
        BottomSheetButtonSection(
            selectedSlot = selectedSlot,
            onDismiss = onDismiss,
            onDone = onDone,
        )
    }
}

@Composable
private fun TimePickerHeader(title: String) {
    val subtitle = stringResource(string.time_picker_dialogue_description)
    Column(
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                testTag = "time_picker_header"
            }
    ) {
        Text(
            text = title,
            style = chatTypography.bottomSheetTitleText,
            modifier = Modifier.padding(start = space.large),
            color = chatColors.token.content.primary
        )

        Text(
            text = subtitle,
            color = chatColors.token.content.secondary,
            style = chatTypography.listPickerBottomSheetSubtitleText,
            modifier = Modifier.padding(start = space.large)
        )
    }

    HorizontalDivider(
        thickness = space.bottomSheetBorderWidth,
        color = chatColors.token.border.default,
        modifier = Modifier.padding(start = space.large, end = space.large, top = space.xl, bottom = space.medium)
    )
}

@Composable
private fun TimePickerSlotGroups(
    groupedSlots: Map<String, List<TimeSlot>>,
    selectedSlot: TimeSlot?,
    onSelected: (TimeSlot) -> Unit,
) {
    val context = LocalContext.current
    val sections = remember(groupedSlots) {
        groupedSlots.entries.map { it.key to it.value }
    }

    LazyColumn(modifier = Modifier.padding(start = space.large, bottom = space.large, end = space.large)) {
        items(
            items = sections,
            key = { (date, _) -> date }
        ) { (date, daySlots) ->
            Text(
                text = formatHeader(context, date),
                style = chatTypography.dateHeader,
                modifier = Modifier
                    .padding(bottom = space.medium, top = space.large)
                    .semantics {
                        testTag = "time_picker_date_header_$date"
                    },
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(space.medium),
                verticalArrangement = Arrangement.spacedBy(space.medium)
            ) {
                daySlots.forEachIndexed { index, slot ->
                    TimeSlotItem(
                        slot = slot,
                        slotTag = uniqueSlotRowTag(slot = slot, dateKey = date, index = index),
                        modifier = Modifier.size(space.timeSlotSize),
                        selected = selectedSlot == slot,
                        onSelected = { onSelected(slot) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSheetButtonSection(selectedSlot: TimeSlot?, onDismiss: () -> Unit, onDone: (TimeSlot, String) -> Unit) {
    val context = LocalContext.current
    val selectedSlotLocalizedText = selectedSlot?.let { slot ->
        stringResource(
            string.time_slot_localized_text,
            formatHeader(context, slot.startTime.toDateKey()),
            formatTime(context, slot.startTime),
            formatDuration(slot.duration),
        )
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(chatColors.token.background.default)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        BottomSheetButtonText(
            text = stringResource(string.cancel),
            enabled = true,
            onClick = { onDismiss() }
        )
        BottomSheetButtonText(
            text = stringResource(string.submit),
            enabled = selectedSlot != null,
            onClick = {
                if (selectedSlot != null && selectedSlotLocalizedText != null) {
                    onDone(selectedSlot, selectedSlotLocalizedText)
                }
            }
        )
    }
}

@Composable
private fun TimeSlotItem(
    slot: TimeSlot,
    slotTag: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onSelected: () -> Unit,
) {
    val context = LocalContext.current
    val selectedState = stringResource(string.time_slot_state_selected)
    val unselectedState = stringResource(string.time_slot_state_not_selected)
    Surface(
        color = if (selected) chatColors.token.brand.primary else chatColors.token.background.surface.emphasis,
        shape = chatShapes.chip,
        modifier = modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelected,
            )
            .semantics {
                stateDescription = if (selected) selectedState else unselectedState
            }
            .defaultMinSize(space.chipMinSize, space.chipMinSize)
    ) {
        Column(
            modifier = Modifier
                .padding(space.chipPadding)
                .semantics {
                    testTag = slotTag
                }
        ) {
            val startTime = formatTime(context, slot.startTime)
            val duration = formatDuration(slot.duration)
            Text(
                text = startTime,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics {
                        testTag = "time_slot_$startTime"
                    },
                color = if (selected) chatColors.token.brand.onPrimary else chatColors.token.brand.primary,
                style = chatTypography.chipText,
            )
            Text(
                text = duration,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics {
                        testTag = "duration_$duration"
                    },
                color = if (selected) chatColors.token.brand.onPrimary else chatColors.token.content.tertiary,
                style = chatTypography.timePickerDuration,
            )
        }
    }
}

private fun groupSlotsByDate(slots: Iterable<TimeSlot>): Map<String, List<TimeSlot>> {
    return slots.groupBy { it.startTime.toDateKey() }
        .toSortedMap()
}

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun TimePickerBottomSheetPreview() {
    ChatTheme {
        TimePickerBottomSheet(
            message = TimePicker(
                message = UiSdkTimePicker(),
            ),
            onDismiss = {},
            onDone = { _, _ -> }
        )
    }
}

@PreviewLightDark
@Composable
private fun TimeslotPreview() {
    ChatTheme {
        val previewSlot = TimePickerSlot(
            startTime = Date(DateProvider.now().time - 24 * 60 * 60 * 1000L)
        )
        TimeSlotItem(
            slot = previewSlot,
            slotTag = uniqueSlotRowTag(
                slot = previewSlot,
                dateKey = previewSlot.startTime.toDateKey(),
                index = 0
            ),
            modifier = Modifier,
            selected = true
        ) {}
    }
}

internal fun uniqueSlotRowTag(slot: TimeSlot, dateKey: String, index: Int): String =
    "time_slot_row_${dateKey}_${slot.startTime.time}_${slot.duration}_$index"
