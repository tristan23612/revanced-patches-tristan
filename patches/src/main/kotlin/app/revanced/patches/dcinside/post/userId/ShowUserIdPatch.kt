package app.revanced.patches.dcinside.post.userId

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.extensions.reference
import app.revanced.patcher.firstClassDef
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.doRecursively
import app.revanced.util.getFreeRegisterProvider
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import org.w3c.dom.Element

private const val SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/post/userId/ShowUserIdPatch;"
private const val POST_ITEM_CLASS_DESCRIPTOR = "Lcom/dcinside/app/model/PostInfo;"

context(context: ResourcePatchContext)
private fun injectUserIdTextView(
    layoutPath: String,
    leftAnchorId: String,
    rightAnchorId: String,
    verticalAnchorId: String,
    viewClass: String,
    textColorAttr: String,
    height: String,
    extraAttributes: Map<String, String> = emptyMap(),
) {
    context.document(layoutPath).use { document ->
        val root = document.documentElement ?: return@use

        var leftElement: Element? = null
        var rightElement: Element? = null

        root.doRecursively { node ->
            if (node is Element) {
                when (node.getAttribute("android:id")) {
                    "@+id/$leftAnchorId", "@id/$leftAnchorId" -> leftElement = node
                    "@+id/$rightAnchorId", "@id/$rightAnchorId" -> rightElement = node
                }
            }
        }

        val targetLeft = leftElement ?: return@use
        val targetRight = rightElement ?: return@use

        targetLeft.setAttribute("app:layout_constraintEnd_toStartOf", "@+id/custom_user_id")
        targetRight.setAttribute("app:layout_constraintStart_toEndOf", "@+id/custom_user_id")

        val userIdElement = document.createElement(viewClass).apply {
            setAttribute("android:textAppearance", "?attr/textTypeSub")
            setAttribute("android:textColor", textColorAttr)
            setAttribute("android:id", "@+id/custom_user_id")
            setAttribute("android:layout_width", "wrap_content")
            setAttribute("android:layout_height", height)
            setAttribute("android:singleLine", "true")
            setAttribute("android:includeFontPadding", "false")
            setAttribute("android:visibility", "gone")

            setAttribute("app:layout_constraintBottom_toBottomOf", "@+id/$verticalAnchorId")
            setAttribute("app:layout_constraintStart_toEndOf", "@+id/$leftAnchorId")
            setAttribute("app:layout_constraintEnd_toStartOf", "@+id/$rightAnchorId")

            extraAttributes.forEach { (key, value) -> setAttribute(key, value) }
        }

        targetLeft.parentNode?.insertBefore(userIdElement, targetRight)
    }
}

private val postListShowUserIdResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        listOf(
            "res/layout/view_post_list_item_basic.xml",
            "res/layout/view_post_list_item_split.xml",
        ).forEach { layoutPath ->
            injectUserIdTextView(
                layoutPath = layoutPath,
                leftAnchorId = "post_list_item_member_ic",
                rightAnchorId = "post_list_item_counts",
                verticalAnchorId = "post_list_item_nic",
                viewClass = "com.dcinside.app.view.ResizeTextView",
                textColorAttr = "?attr/dcPostReadSubColor",
                height = "wrap_content",
                extraAttributes = mapOf(
                    "android:gravity" to "center_vertical",
                    "app:layout_constraintTop_toTopOf" to "@+id/post_list_item_nic",
                ),
            )
        }
    }
}

private val postHeaderShowUserIdResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        injectUserIdTextView(
            layoutPath = "res/layout/view_read_header.xml",
            leftAnchorId = "read_header_member_ic",
            rightAnchorId = "read_header_gallog",
            verticalAnchorId = "read_header_name",
            viewClass = "android.widget.TextView",
            textColorAttr = "?attr/colorPostExt",
            height = "wrap_content",
            extraAttributes = mapOf(
                "android:layout_marginStart" to "1dp",
                "app:layout_constraintBaseline_toBaselineOf" to "@+id/read_header_name",
            ),
        )
    }
}

private val replyShowUserIdResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        mapOf(
            "res/layout/view_reply_item_text.xml" to "26dp",
            "res/layout/view_reply_item_image.xml" to "31dp",
            "res/layout/view_reply_item_image_big.xml" to "31dp",
            "res/layout/view_reply_item_voice.xml" to "26dp",
            "res/layout/view_reply_item_voice2.xml" to "26dp",
        ).forEach { (layoutPath, height) ->
            injectUserIdTextView(
                layoutPath = layoutPath,
                leftAnchorId = "reply_member_ic",
                rightAnchorId = "reply_user_memo",
                verticalAnchorId = "reply_name",
                viewClass = "com.dcinside.app.view.ResizeTextView",
                textColorAttr = "?attr/dcPostReadSubColor",
                height = height,
                extraAttributes = mapOf(
                    "android:gravity" to "center_vertical",
                    "app:layout_constraintTop_toTopOf" to "@+id/reply_name",
                ),
            )
        }
    }
}

@Suppress("unused")
val showUserIdPatch = bytecodePatch(
    name = "Show user ID",
    description = "Adds option to show the user ID.",
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        postListShowUserIdResourcePatch,
        postHeaderShowUserIdResourcePatch,
        replyShowUserIdResourcePatch,
    )

    apply {
        addResources("dcinside", "post.userId.showUserIdPatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_show_user_id"),
        )

        postItemBindMethodMatch.let {
            it.method.apply {
                val postItemIndex = it[1]
                val postItemRegister = getInstruction<OneRegisterInstruction>(postItemIndex).registerA

                val userIdMethodReference = getInstruction(it[3]).methodReference

                val spannableIndex = it[-1]
                val spannableRegister = getInstruction<OneRegisterInstruction>(spannableIndex).registerA

                val registerProvider = getFreeRegisterProvider(spannableIndex, 2, spannableRegister)
                val viewRegister = registerProvider.getFreeRegister()
                val userIdRegister = registerProvider.getFreeRegister()

                val insertIndex = spannableIndex + 1
                var insertSmali = $$"""
                    move-object/from16 v$$viewRegister, p1
                    iget-object v$$viewRegister, v$$viewRegister, Landroidx/recyclerview/widget/RecyclerView$ViewHolder;->itemView:Landroid/view/View;
                    
                    invoke-virtual {v$$postItemRegister}, $$userIdMethodReference
                    move-result-object v$$userIdRegister
                    
                    invoke-static {v$$viewRegister, v$$userIdRegister, v$$spannableRegister}, $$SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->setUserId(Landroid/view/View;Ljava/lang/String;Ljava/lang/CharSequence;)V
                """

                addInstructions(insertIndex, insertSmali)
            }
        }

        postSearchItemBindMethodMatch.let {
            it.method.apply {
                val postItemIndex = it[1]
                val postItemRegister = getInstruction<OneRegisterInstruction>(postItemIndex).registerA

                val userIdMethodReference = getInstruction(it[6]).methodReference

                val spannableIndex = it[-1]
                val spannableRegister = getInstruction<OneRegisterInstruction>(spannableIndex).registerA

                val registerProvider = getFreeRegisterProvider(postItemIndex, 2, postItemRegister)
                val viewRegister = registerProvider.getFreeRegister()
                val userIdRegister = registerProvider.getFreeRegister()

                val insertIndex = spannableIndex + 1
                addInstructions(
                    insertIndex,
                    $$"""
                        move-object/from16 v$$viewRegister, p1
                        iget-object v$$viewRegister, v$$viewRegister, Landroidx/recyclerview/widget/RecyclerView$ViewHolder;->itemView:Landroid/view/View;
                        
                        invoke-virtual { v$$postItemRegister }, $$userIdMethodReference
                        move-result-object v$$userIdRegister
                        
                        invoke-static { v$$viewRegister, v$$userIdRegister, v$$spannableRegister }, $$SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->setUserId(Landroid/view/View;Ljava/lang/String;Ljava/lang/CharSequence;)V
                    """
                )
            }
        }


        postHeaderSetupMethodMatch.let {
            it.method.apply {
                val userIdIndex = it[2]
                val userIdReference = getInstruction<OneRegisterInstruction>(userIdIndex).reference!!

                val charSequenceIndex = it[6]
                val charSequenceRegister = getInstruction<OneRegisterInstruction>(charSequenceIndex).registerA

                val registerProvider = getFreeRegisterProvider(charSequenceIndex, 2, charSequenceRegister)
                val viewRegister = registerProvider.getFreeRegister()
                val userIdRegister = registerProvider.getFreeRegister()

                addInstructions(
                    charSequenceIndex + 1,
                    """
                        move-object/from16 v$viewRegister, p0
                        
                        move-object/from16 v$userIdRegister, p1
                        invoke-virtual { v$userIdRegister }, $userIdReference
                        move-result-object v$userIdRegister
                        
                        invoke-static { v$viewRegister , v$userIdRegister, v$charSequenceRegister }, $SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->setUserId(Landroid/view/View;Ljava/lang/String;Ljava/lang/CharSequence;)V
                    """
                )
            }
        }

        postReplySetupMethodMatch.let {
            it.method.apply {
                val userIdIndex = it[10]
                val userIdReference = getInstruction<OneRegisterInstruction>(userIdIndex).reference!!

                val charSequenceIndex = it[-1]
                val charSequenceRegister = getInstruction<OneRegisterInstruction>(charSequenceIndex).registerA

                val viewRegister = getInstruction<FiveRegisterInstruction>(charSequenceIndex - 1).registerD
                val userIdRegister = getInstruction<FiveRegisterInstruction>(charSequenceIndex - 1).registerE

                val viewIndex = it[3]
                val viewReference = getInstruction<OneRegisterInstruction>(viewIndex).reference!!

                addInstructions(
                    charSequenceIndex + 1,
                    """
                        move-object/from16 v$viewRegister, p1
                        invoke-virtual { v$viewRegister }, $viewReference
                        move-result-object v$viewRegister
                        
                        move-object/from16 v$userIdRegister, p3
                        invoke-virtual { v$userIdRegister }, $userIdReference
                        move-result-object v$userIdRegister
                        
                        invoke-static { v$viewRegister , v$userIdRegister, v$charSequenceRegister }, $SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->setUserId(Landroid/view/View;Ljava/lang/String;Ljava/lang/CharSequence;)V
                    """
                )
            }
        }

        postHistoryRealmSetupMethodMatch.let {
            it.method.apply {
                fun ClassDef.getFieldBySerializedName(serializedName: String) = fields.first { field ->
                    field.annotations.any { annotation ->
                        annotation.elements.any { element -> element.name == "value" && (element.value as? StringEncodedValue)?.value == serializedName }
                    }
                }
                val postItemClassDef = firstClassDef(POST_ITEM_CLASS_DESCRIPTOR)

                val userIdField = postItemClassDef.getFieldBySerializedName("user_id")
                val userIpField = postItemClassDef.getFieldBySerializedName("ip")
                val userNameField = postItemClassDef.getFieldBySerializedName("name")

                val userIdGetterMethodReference = postItemClassDef.getStringGetterMethod(userIdField.name)
                val userIpGetterMethodReference = postItemClassDef.getStringGetterMethod(userIpField.name)
                val userNameGetterMethodReference = postItemClassDef.getStringGetterMethod(userNameField.name, it)

                val userNameIndex = indexOfFirstInstructionOrThrow {methodReference == userNameGetterMethodReference} + 1
                val userNameRegister = getInstruction<OneRegisterInstruction>(userNameIndex).registerA

                val registerProvider = getFreeRegisterProvider(userNameIndex, 2, userNameRegister)
                val userIdRegister = registerProvider.getFreeRegister()
                val userIpRegister = registerProvider.getFreeRegister()

                addInstructions(
                    userNameIndex + 1,
                    """
                        invoke-virtual {p2}, $userIdGetterMethodReference
                        move-result-object v$userIdRegister
                        invoke-virtual {p2}, $userIpGetterMethodReference
                        move-result-object v$userIpRegister
                        invoke-static {v$userNameRegister, v$userIdRegister, v$userIpRegister }, $SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->addUserId(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$userNameRegister
                    """
                )
            }
        }
    }
}