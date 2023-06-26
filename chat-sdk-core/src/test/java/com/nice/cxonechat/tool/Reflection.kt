package com.nice.cxonechat.tool

import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility

internal fun Collection<KCallable<*>>.getPublicProperties() = filterIsInstance<KMutableProperty<*>>()
    .filter { kCallable -> kCallable.visibility == KVisibility.PUBLIC }
