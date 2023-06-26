package com.nice.cxonechat.storage

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.nice.cxonechat.tool.getPublicProperties
import com.nice.cxonechat.tool.nextString
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import kotlin.reflect.KMutableProperty
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * This test objective is to ensure that the [SharedPreferences] are used correctly.
 * Namely, each stored property should use different key for storage and also that the values returned by the implementation are not
 * modified.
 *
 * For now only [String] properties access from [ValueStorage] is tested.
 */
internal class PreferencesValueStorageTest {

    private lateinit var storage: ValueStorage
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: Editor

    @Before
    fun setup() {
        sharedPreferences = mock()
        editor = mock()
        whenever(sharedPreferences.edit()).thenReturn(editor)
        storage = PreferencesValueStorage(sharedPreferences)
    }

    /**
     * Test that all supported property types from [ValueStorage] interface a properly stored to [SharedPreferences] using unique key for
     * each property.
     */
    @Test
    fun storageIsStoringValues() {
        val properties = storage::class.members.getPublicProperties()
        val usedKeys = ArgumentCaptor.forClass(String::class.java)
        val uniqueUsedKeys = mutableSetOf<String>()
        val filledProperties = properties.fillStorageWithRandomValues()
        filledProperties.forEach { value ->
                when (value) {
                    is String -> verify(editor).putString(usedKeys.capture(), eq(value))
                }
                assertTrue(uniqueUsedKeys.add(usedKeys.value), "Key ${usedKeys.value} was already used to store different property")
            }
        val testedProperties = filledProperties.size
        verify(sharedPreferences, times(testedProperties)).edit()
        verify(editor, times(testedProperties)).apply()
        verifyNoMoreInteractions(editor)
        verifyNoMoreInteractions(sharedPreferences)
    }

    /**
     * Test that all supported property types from [ValueStorage] interface are retrieved from [SharedPreferences].
     */
    @Test
    fun storageIsRetrievingValues() {
        val testStringValue = nextString()
        whenever(sharedPreferences.getString(any(), anyOrNull())).thenReturn(testStringValue)
        val properties = storage::class.members.getPublicProperties()
        var stringCount = 0
        properties.forEach {
            when (it.returnType.classifier) {
                String::class -> {
                    assertEquals(testStringValue, getStoredProperty(it))
                    stringCount++
                }
            }
        }
        verify(sharedPreferences, times(stringCount)).getString(any(), anyOrNull())
        verifyNoMoreInteractions(sharedPreferences)
        verifyZeroInteractions(editor)
    }

    private fun getStoredProperty(property: KMutableProperty<*>) = property.getter.call(storage)

    private fun List<KMutableProperty<*>>.fillStorageWithRandomValues(): List<Any> = mapNotNull { property ->
        when (property.returnType.classifier) {
            String::class -> nextString()
            else -> null // not implemented
        }?.also { property.setter.call(storage, it) }
    }
}
