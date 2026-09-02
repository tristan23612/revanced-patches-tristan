package app.revanced.patches.dcinside.post.write.pum

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.writeConfigNotAllowedPumSharedPreferenceMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes()
    returnType("Z")
    instructions(
        Opcode.CONST_4(),
        "writeConfigNotAllowedPum"(),
    )
}