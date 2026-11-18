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

import android.content.Context
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.nice.cxonechat.message.TimeSlot
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message.TimePicker
import com.nice.cxonechat.ui.composable.generic.scrollbar.rememberScrollbarState
import com.nice.cxonechat.ui.composable.generic.scrollbar.scrollbar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ModalBottomSheet
import com.nice.cxonechat.ui.util.formatDuration
import com.nice.cxonechat.ui.util.formatHeader
import com.nice.cxonechat.ui.util.formatTime
import com.nice.cxonechat.ui.util.preview.message.UiSdkTimePicker
import com.nice.cxonechat.ui.util.toDateKey

/**
 * Built on the same [ChatTheme.ModalBottomSheet] the list picker and pre-chat survey share, rather than
 * a custom-made header/footer: that is what keeps its title padding, edge-to-edge divider and
 * Cancel/Submit row identical to theirs, and to the design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerBottomSheet(
    message: TimePicker,
    onDismiss: () -> Unit,
    onDone: (TimeSlot, String) -> Unit,
) {
    var selectedSlot: TimeSlot? by remember { mutableStateOf(null) }
    val groupedSlots = remember(message.timeSlots) {
        message.timeSlots.groupBy { it.startTime.toDateKey() }.toSortedMap()
    }
    val context = LocalContext.current
    val selectedSlotLocalizedText = selectedSlot?.let { slot ->
        stringResource(
            string.time_slot_localized_text,
            formatHeader(context, slot.startTime.toDateKey()),
            formatTime(context, slot.startTime),
            formatDuration(slot.duration),
        )
    }

    ChatTheme.ModalBottomSheet(
        onDismiss = onDismiss,
        onSubmit = {
            val slot = selectedSlot
            val localizedText = selectedSlotLocalizedText
            if (slot != null && localizedText != null) {
                onDone(slot, localizedText)
            }
        },
        canSubmit = selectedSlot != null,
        title = message.popupTitle,
        modifier = Modifier.testTag("time_picker_bottom_sheet"),
    ) { contentPadding ->
        TimePickerSlotGroups(
            groupedSlots = groupedSlots,
            contentPadding = contentPadding,
            selectedSlot = selectedSlot,
            onSelected = { slot -> selectedSlot = if (selectedSlot == slot) null else slot }
        )
    }
}

@Composable
private fun TimePickerSlotGroups(
    groupedSlots: Map<String, List<TimeSlot>>,
    contentPadding: PaddingValues,
    selectedSlot: TimeSlot?,
    onSelected: (TimeSlot) -> Unit,
) {
    val context = LocalContext.current
    val sections = remember(groupedSlots) {
        groupedSlots.entries.map { it.key to it.value }
    }
    val listState = rememberLazyListState()
    val scrollbarState = rememberScrollbarState()

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .scrollbar(listState, scrollbarState, Orientation.Vertical)
    ) {
        item {
            // The design's description line, on the same subtle surface as the date groups below it.
            Text(
                text = stringResource(string.time_picker_dialogue_description),
                style = chatTypography.listPickerBottomSheetSubtitleText,
                color = chatColors.token.content.secondary,
                modifier = Modifier
                    .padding(bottom = space.medium)
                    .semantics {
                        testTag = "time_picker_subtitle"
                    },
            )
        }
        items(
            items = sections,
            key = { (date, _) -> date }
        ) { (date, daySlots) ->
            TimePickerDateSection(
                context = context,
                date = date,
                daySlots = daySlots,
                selectedSlot = selectedSlot,
                onSelected = onSelected,
            )
        }
    }
}

@Composable
private fun TimePickerDateSection(
    context: Context,
    date: String,
    daySlots: List<TimeSlot>,
    selectedSlot: TimeSlot?,
    onSelected: (TimeSlot) -> Unit,
) {
    Text(
        text = formatHeader(context, date),
        style = chatTypography.dateHeader,
        color = chatColors.token.content.primary,
        modifier = Modifier
            .padding(bottom = space.medium, top = space.large)
            .semantics {
                heading()
                testTag = "time_picker_date_header_$date"
            },
    )

    // As many columns as fit at the slot's minimum width, stretched to fill the row: the design's three
    // in a phone's portrait window and six in its landscape one. A short last row keeps the column widths.
    BoxWithConstraints(Modifier.selectableGroup()) {
        val gap = space.medium
        val columns = ((maxWidth + gap) / (space.timeSlotMinWidth + gap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            daySlots.withIndex().chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { (index, slot) ->
                        TimeSlotItem(
                            slot = slot,
                            slotTag = uniqueSlotRowTag(slot = slot, dateKey = date, index = index),
                            modifier = Modifier
                                .weight(1f)
                                .height(space.timeSlotHeight),
                            selected = selectedSlot == slot,
                            onSelected = { onSelected(slot) }
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
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
    val dateKey = slot.startTime.toDateKey()
    val accessibilityText = stringResource(
        string.time_slot_accessibility_text,
        formatHeader(context, dateKey),
        formatTime(context, slot.startTime),
        formatDuration(slot.duration)
    )
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
                contentDescription = accessibilityText
            }
            .defaultMinSize(space.chipMinSize, space.chipMinSize)
    ) {
        TimeSlotContent(slot = slot, slotTag = slotTag, selected = selected)
    }
}

@Composable
private fun TimeSlotContent(
    slot: TimeSlot,
    slotTag: String,
    selected: Boolean,
) {
    val context = LocalContext.current
    val startTime = formatTime(context, slot.startTime)
    val duration = formatDuration(slot.duration)
    Column(
        modifier = Modifier
            .padding(space.timeSlotPadding)
            .semantics {
                testTag = slotTag
            }
    ) {
        Text(
            text = startTime,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .semantics {
                    testTag = "time_slot_$startTime"
                    hideFromAccessibility() // already included in contentDescription of the whole chip.
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
                    hideFromAccessibility() // already included in contentDescription of the whole chip.
                },
            color = if (selected) chatColors.token.brand.onPrimary else chatColors.token.content.tertiary,
            style = chatTypography.timePickerDuration,
        )
    }
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

internal fun uniqueSlotRowTag(slot: TimeSlot, dateKey: String, index: Int): String =
    "time_slot_row_${dateKey}_${slot.startTime.toEpochMilliseconds()}_${slot.duration}_$index"
