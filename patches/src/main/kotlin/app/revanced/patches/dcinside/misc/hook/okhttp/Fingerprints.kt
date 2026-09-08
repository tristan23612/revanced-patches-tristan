package app.revanced.patches.dcinside.misc.hook.okhttp

import app.revanced.patcher.accessFlags
import app.revanced.patcher.definingClass
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.name
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.okhttpBuildMethod by gettingFirstMethodDeclaratively {
    definingClass($$"Lokhttp3/OkHttpClient$Builder;")
    name("build")
}

internal val BytecodePatchContext.addQueryParameterHookMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    definingClass($$"Lokhttp3/HttpUrl$Builder;")
    name("addQueryParameter")
}