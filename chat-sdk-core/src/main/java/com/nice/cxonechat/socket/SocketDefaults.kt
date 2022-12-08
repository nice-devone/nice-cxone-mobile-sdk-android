package com.nice.cxonechat.socket

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonToken.NUMBER
import com.google.gson.stream.JsonWriter
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.util.DateTime
import com.nice.cxonechat.util.RuntimeTypeAdapterFactory
import com.nice.cxonechat.util.timestampToDate
import com.nice.cxonechat.util.toTimestamp
import java.util.Date
import kotlin.math.roundToLong

internal object SocketDefaults {

    private val messageContentAdapter = RuntimeTypeAdapterFactory.of(MessagePolyContent::class.java, "type")
        .registerSubtype(MessagePolyContent.Text::class.java, "TEXT")
        .registerSubtype(MessagePolyContent.Plugin::class.java, "PLUGIN")
        .registerDefault(MessagePolyContent.Noop)
    private val messageElementAdapter = RuntimeTypeAdapterFactory.of(MessagePolyElement::class.java, "type")
        .registerSubtype(MessagePolyElement.Menu::class.java, "MENU")
        .registerSubtype(MessagePolyElement.File::class.java, "FILE")
        .registerSubtype(MessagePolyElement.Title::class.java, "TITLE")
        .registerSubtype(MessagePolyElement.Subtitle::class.java, "SUBTITLE")
        .registerSubtype(MessagePolyElement.Text::class.java, "TEXT")
        .registerSubtype(MessagePolyElement.Button::class.java, "BUTTON")
        .registerSubtype(MessagePolyElement.TextAndButtons::class.java, "TEXT_AND_BUTTONS")
        .registerSubtype(MessagePolyElement.QuickReplies::class.java, "QUICK_REPLIES")
        .registerSubtype(MessagePolyElement.InactivityPopup::class.java, "INACTIVITY_POPUP")
        .registerSubtype(MessagePolyElement.Countdown::class.java, "COUNTDOWN")
        .registerSubtype(MessagePolyElement.Custom::class.java, "CUSTOM")
        .registerDefault(MessagePolyElement.Noop)

    val serializer: Gson = GsonBuilder()
        .registerTypeAdapterFactory(messageContentAdapter)
        .registerTypeAdapterFactory(messageElementAdapter)
        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .registerTypeAdapter(DateTime::class.java, DateTimeTypeAdapter())
        .create()

    private class DateTypeAdapter : TypeAdapter<Date>() {

        override fun write(out: JsonWriter, value: Date?) {
            if (value == null) {
                out.nullValue()
                return
            }
            out.value(value.toTimestamp())
        }

        override fun read(reader: JsonReader): Date? {
            if (reader.peek() == NULL) {
                reader.nextNull()
                return null
            }
            if (reader.peek() == NUMBER) {
                var time = reader.nextDouble()
                if (time < epochLimitSeconds) time *= 1000
                return Date(time.roundToLong())
            }
            return reader.nextString().timestampToDate()
        }

        companion object {

            // this will make the program malfunction on Sat Nov 20 2286 17:46:40 UTC (:
            private const val epochLimitSeconds = 10_000_000_000L

        }

    }

    private class DateTimeTypeAdapter : TypeAdapter<DateTime>() {

        private val fallback = DateTypeAdapter()

        override fun write(out: JsonWriter, value: DateTime?) {
            if (value == null) {
                out.nullValue()
                return
            }
            out.value(value.toTimestamp())
        }

        override fun read(reader: JsonReader): DateTime? {
            return fallback.read(reader)?.let(::DateTime)
        }

    }

}
