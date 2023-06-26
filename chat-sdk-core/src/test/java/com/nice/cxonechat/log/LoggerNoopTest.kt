package com.nice.cxonechat.log

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyZeroInteractions
import java.io.PrintStream

internal class LoggerNoopTest {

    private lateinit var out: PrintStream

    @Before
    fun prepare() {
        out = mock()
        System.setOut(out)
    }

    @After
    fun tearDown() {
        System.setOut(null)
    }

    @Test
    fun log_hasNoInteractions() {
        LoggerNoop.log(Level.Info, "")
        verifyZeroInteractions(out)
    }
}
