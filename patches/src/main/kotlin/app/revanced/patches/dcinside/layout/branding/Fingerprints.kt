package app.revanced.patches.dcinside.layout.branding

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.userProvidedCustomNameExtensionMethod by gettingFirstMethodDeclaratively {
    definingClass(EXTENSION_CLASS)
    name("userProvidedCustomName")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes()
}

internal val BytecodePatchContext.userProvidedCustomIconExtensionMethod by gettingFirstMethodDeclaratively {
    definingClass(EXTENSION_CLASS)
    name("userProvidedCustomIcon")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes()
}

internal val BytecodePatchContext.mainActivityOnCreateMethodMatch by composingFirstMethod {
    definingClass("Lcom/dcinside/app/main/HomeActivity;")
    name("onCreate")
}