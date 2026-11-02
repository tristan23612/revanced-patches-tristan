package app.revanced.patches.xodo.pro

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.buildUserInfoMethodMatch2 by composingFirstMethod {
    instructions(
        "syncUserInfo userStatus: "(),
        "syncUserInfo userData: "(),
        Opcode.IF_EQZ(),
        Opcode.IF_EQZ(),
        Opcode.NEW_INSTANCE(),
        Opcode.INVOKE_DIRECT_RANGE(),
        Opcode.INVOKE_DIRECT(),
        after(
            Opcode.MOVE_RESULT_OBJECT(),
        ),
        after(
            Opcode.INVOKE_INTERFACE(),
        ),
        Opcode.RETURN_OBJECT(),
    )
}