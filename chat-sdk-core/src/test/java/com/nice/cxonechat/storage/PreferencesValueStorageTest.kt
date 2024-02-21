package com.nice.cxonechat.storage

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.nice.cxonechat.tool.getPublicProperties
import com.nice.cxonechat.tool.nextString
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.test.assertEquals

/**
 * This test objective is to ensure that the [SharedPreferences] are used correctly.
 * Namely, each stored property should use different key for storage and also that the values returned by the implementation are not
 * modified.
 *
 * For now only [String] properties access from [ValueStorage] is tested.
 */
internal class PreferencesValueStorageTest {

    private val editor = mockk<Editor>()
    private val sharedPreferences = mockk<SharedPreferences> {
        every { edit() } returns editor
    }
    private val storage: ValueStorage = PreferencesValueStorage(sharedPreferences)
    private val stringProperties
        get() = storage::class
            .members
            .getPublicProperties()
            .filter { it.returnType.classifier == String::class }
            .map { it as KMutableProperty<String> }

    /**
     * Test that all supported property types from [ValueStorage] interface a properly stored to [SharedPreferences] using unique key for
     * each property.
     */
    @Test
    fun storageIsStoringValues() {
        val keys = mutableListOf<String>()

        every { editor.putString(capture(keys), any()) } returns editor
        justRun { editor.apply() }

        for (property in stringProperties) {
            val value = nextString()

            property.set(value)

            verify {
                editor.putString(any(), eq(value))
                editor.apply()
            }
        }

        confirmVerified(editor)

        // Insure that there are no duplicated keys
        assertEquals(keys, keys.toSet().toList(), "Duplicated key used in ValueStorage: $keys")
    }

    /**
     * Test that all supported property types from [ValueStorage] interface are retrieved from [SharedPreferences].
     */
    @Test
    fun storageIsRetrievingValues() {
        val testStringValue = nextString()

        every { sharedPreferences.getString(any(), any()) } returns testStringValue

        for (property in stringProperties) {
            assertEquals(testStringValue, property.get())
        }

        verify(exactly = stringProperties.size) {
            sharedPreferences.getString(any(), any())
        }
        confirmVerified(sharedPreferences)
        confirmVerified(editor)
    }

    private fun KProperty<*>.get() = getter.call(storage)

    private fun <T> KMutableProperty<T>.set(value: T) = setter.call(storage, value)
}
