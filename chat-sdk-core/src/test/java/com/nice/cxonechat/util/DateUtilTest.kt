package com.nice.cxonechat.util

import org.junit.Assert
import org.junit.Test
import java.util.Date

internal class DateUtilTest {

    @Test
    fun testDateFormatting() {
        val date = Date(0L)
        val stringDate = date.toTimestamp()
        Assert.assertEquals("1970-01-01T00:00:00.000z", stringDate)
    }
}
