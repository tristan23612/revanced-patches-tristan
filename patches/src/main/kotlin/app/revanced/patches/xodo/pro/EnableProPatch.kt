package app.revanced.patches.xodo.pro

import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.xodo.misc.signature.spoofSignaturePatch

@Suppress("unused")
val enableProPatch = bytecodePatch(
    name = "Enable Pro",
    description = "Enables Pro features."
) {
    compatibleWith("com.xodo.pdf.reader")

    dependsOn(
        spoofSignaturePatch
    )

    apply {
        buildUserInfoMethodMatch2.let {
            it.method.apply {
                val insertIndex = it[-1]

                val userInfoInstanceFieldReference = getInstruction(it[4]).reference!!.toString()
                val buildUserInfoFieldReference = getInstruction(it[5]).reference!!.toString()

                val userDaoFieldReference = getInstruction(it[6]).reference!!.toString()
                val insertUserDaoFieldReference = getInstruction(it[8]).reference!!.toString()

                val insertSmali = $$"""
                    new-instance v6, $$userInfoInstanceFieldReference
                    const-wide/16 v7, 0x1
                    const-string v9, "ReVanced"
                    const-string v10, "ReVanced"
                    const-string v11, "ReVanced"
                    const-string v12, "ReVanced"
                    const-string v13, "ReVanced"
                    const/4 v14, 0x1
                    const-string v15, "Monthly"
                    const-string v16, "ReVanced"
                    
                    invoke-direct/range { v6 .. v16 }, $$buildUserInfoFieldReference
                    
                    invoke-direct { v1 }, $$userDaoFieldReference
                    move-result-object v5
                    invoke-interface { v5, v6 }, $$insertUserDaoFieldReference
                """

                addInstructions(
                    insertIndex,
                    insertSmali
                )
            }
        }
    }
}