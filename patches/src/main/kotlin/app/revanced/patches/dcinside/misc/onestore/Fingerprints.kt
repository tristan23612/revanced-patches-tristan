package app.revanced.patches.dcinside.misc.onestore

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.storeConfigurationMethodMatch by composingFirstMethod {
    strings(
        "onestore://common/product/%s",
        "https://onesto.re/%s",
    )
}