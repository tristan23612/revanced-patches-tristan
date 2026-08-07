package app.revanced.patches.dcinside.misc.settings

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

private const val LICENSE_ACTIVITY_CLASS_DESCRIPTOR = "Lcom/dcinside/app/license/LicenseActivity;"

internal val BytecodePatchContext.licenseActivityOnCreateMethod by gettingFirstMethodDeclaratively {
    name("onCreate")
    definingClass(LICENSE_ACTIVITY_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PROTECTED)
    returnType("V")
    parameterTypes("Landroid/os/Bundle;")
}