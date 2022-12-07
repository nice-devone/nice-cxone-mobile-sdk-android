package com.nice.cxonechat

import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.nice.cxonechat.internal.model.EnvironmentInternal
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@SmallTest
class ChatBuilderIntegrationTest {

    @Test
    fun connectsToServer() {
        val context = InstrumentationRegistry.getContext()
        val environment = EnvironmentInternal(
            name = "",
            location = "",
            baseUrl = "https://channels-eu1-qa.brandembassy.com/",
            socketUrl = "wss://chat-gateway-eu1-qa.brandembassy.com",
            originHeader = "https://livechat-eu1-qa.brandembassy.com",
            chatUrl = "https://channels-eu1-qa.brandembassy.com/chat/"
        )
        val config = SocketFactoryConfiguration(environment, 6450, "chat_f62c9eaf-f030-4d0d-aa87-6e8a5aed3c55")
        val latch = CountDownLatch(1)
        ChatBuilder(context, config)
            .setDevelopmentMode(true)
            .setUserName("john", "doe")
            .build {
                it.close()
                latch.countDown()
            }
        latch.await()
    }

}
