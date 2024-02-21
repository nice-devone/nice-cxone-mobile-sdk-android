package com.nice.cxonechat.log

import io.mockk.confirmVerified
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.PrintStream

internal class LoggerNoopTest {

    private lateinit var out: PrintStream

    @Before
    fun prepare() {
        out = mockk()
        System.setOut(out)
    }

    @After
    fun tearDown() {
        System.setOut(null)
    }

    @Test
    fun log_hasNoInteractions() {
        LoggerNoop.log(Level.Info, "")
        confirmVerified(out)
    }
}
