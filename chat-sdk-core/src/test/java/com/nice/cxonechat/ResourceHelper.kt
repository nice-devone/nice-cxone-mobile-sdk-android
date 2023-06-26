package com.nice.cxonechat

import java.io.InputStream

internal object ResourceHelper {
    internal fun loadString(
        name: String,
        loader: ClassLoader? = ResourceHelper::class.java.classLoader
    ) = loadBytes(name, loader)?.toString(Charsets.UTF_8)

    internal fun loadBytes(
        name: String,
        loader: ClassLoader? = ResourceHelper::class.java.classLoader
    ) = loader?.getResourceAsStream(name)?.use(InputStream::readAllBytes)
}
