package com.xxyxxdmc.hoshisuki

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_NAME = "messages.HoshisukiBundle"

object HoshisukiBundle : AbstractBundle(BUNDLE_NAME) {
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}
        