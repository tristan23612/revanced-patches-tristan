package app.revanced.patches.dcinside.management.userId

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.instructions
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
import app.revanced.patches.shared.misc.settings.preference.NonInteractivePreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.doRecursively
import app.revanced.util.getFreeRegisterProvider
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.registersUsed
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import org.w3c.dom.Element

private const val SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/management/userId/ShowUserIdPatch;"
internal const val POST_INFO_CLASS_DESCRIPTOR = "Lcom/dcinside/app/model/PostInfo;"
internal const val POST_ITEM_CLASS_DESCRIPTOR = "Lcom/dcinside/app/response/PostItem;"

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

        targetLeft.setAttribute("app:layout_constraintEnd_toStartOf", "@+id/revanced_user_id")
        targetRight.setAttribute("app:layout_constraintStart_toEndOf", "@+id/revanced_user_id")

        val userIdElement = document.createElement(viewClass).apply {
            setAttribute("android:textAppearance", "?attr/textTypeSub")
            setAttribute("android:textColor", textColorAttr)
            setAttribute("android:id", "@+id/revanced_user_id")
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

context(context: ResourcePatchContext)
private fun injectGalleryDataStore(layoutPath: String) {
    context.document(layoutPath).use { document ->
        val root = document.documentElement ?: return@use

        // 수정 및 추가된 부분: 갤러리 ID 및 Type을 함께 포괄하도록 식별자(ID) 및 위젯 명칭 변경
        val galleryDataElement = document.createElement("Space").apply {
            setAttribute("android:id", "@+id/revanced_gallery_data_space")
            setAttribute("android:layout_width", "0dp")
            setAttribute("android:layout_height", "0dp")
            setAttribute("android:visibility", "gone")
        }

        root.appendChild(galleryDataElement)
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
            injectGalleryDataStore(layoutPath)
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
        injectGalleryDataStore("res/layout/view_read_header.xml")
    }
}

private val replyShowUserIdResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        mapOf(
            "res/layout/view_reply_item_image.xml" to "31dp",
            "res/layout/view_reply_item_image_big.xml" to "31dp",
            "res/layout/view_reply_item_tcon.xml" to "31dp",
            "res/layout/view_reply_item_text.xml" to "26dp",
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
            injectGalleryDataStore(layoutPath)
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
        addResources("dcinside", "management.userId.showUserIdPatch")

        PreferenceScreen.MANAGEMENT.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_user_id_screen",
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = setOf(
                    NonInteractivePreference("revanced_show_user_id_guide"),
                    SwitchPreference("revanced_show_user_id"),
                    SwitchPreference("revanced_enable_user_id_gall_scope")
                )
            ),
        )

        fun ClassDef.getFieldBySerializedName(serializedName: String) = fields.first { field ->
            field.annotations.any { annotation ->
                annotation.elements.any { element -> element.name == "value" && (element.value as? StringEncodedValue)?.value == serializedName }
            }
        }

        val postItemClassDef = firstClassDef(POST_ITEM_CLASS_DESCRIPTOR)
        val postItemUserIdField = postItemClassDef.getFieldBySerializedName("user_id")
        val postItemUserIdGetterMethodReference = postItemClassDef.getStringGetterMethod(postItemUserIdField.name)

        val postInfoClassDef = firstClassDef(POST_INFO_CLASS_DESCRIPTOR)
        val postInfoUserIdField = postInfoClassDef.getFieldBySerializedName("user_id")
        val postInfoUserIdGetterMethodReference = postInfoClassDef.getStringGetterMethod(postInfoUserIdField.name)

        postSearchItemOnBindViewHolderMethodMatch(normalPostItemBindMethodMatch).let {
            it.method.apply {
                val insertIndex = it[2]
                val insertSmali = $$"""
                    invoke-virtual { p0, p2 }, $$definingClass->getItem(I)Lcom/dcinside/app/response/PostItem;
                    move-result-object v0
                    
                    invoke-virtual { v0 }, $$postItemUserIdGetterMethodReference
                    move-result-object v1
                    
                    iget-object v0, p1, Landroidx/recyclerview/widget/RecyclerView$ViewHolder;->itemView:Landroid/view/View;
                    
                    invoke-static { v0, v1 }, $$SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->setUserId(Landroid/view/View;Ljava/lang/String;)V
                """

                addInstructions(
                    insertIndex,
                    insertSmali,
                )
            }
        }

        postSearchItemBindMethodMatch.let {
            it.method.apply {
                val postItemIndex = it[1]
                val postItemRegister = getInstruction<OneRegisterInstruction>(postItemIndex).registerA

                val insertIndex = it[-1]
                val insertSmali = $$"""
                    move-object/from16 v0, p1
                    iget-object v0, v0, Landroidx/recyclerview/widget/RecyclerView$ViewHolder;->itemView:Landroid/view/View;
                    
                    invoke-virtual { v$$postItemRegister }, $$postItemUserIdGetterMethodReference
                    move-result-object v1
                    
                    invoke-static { v0, v1 }, $$SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->setUserId(Landroid/view/View;Ljava/lang/String;)V
                """

                addInstructions(
                    insertIndex,
                    insertSmali,
                )
            }
        }

        postHeaderSetupMethodMatch.let {
            it.method.apply {
                val insertIndex = it[-1]
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                val freeRegisterProvider = getFreeRegisterProvider(insertIndex + 1, 2, insertRegister)
                val userIdRegister = freeRegisterProvider.getFreeRegister()
                val viewRegister = freeRegisterProvider.getFreeRegister()

                val insertSmali = $$"""
                    move-object/from16 v$$viewRegister, p0
                    
                    move-object/from16 v$$userIdRegister, p1
                    invoke-virtual { v$$userIdRegister }, $$postInfoUserIdGetterMethodReference
                    move-result-object v$$userIdRegister
                    
                    invoke-static { v$$viewRegister, v$$userIdRegister }, $$SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->setUserId(Landroid/view/View;Ljava/lang/String;)V
                """

                addInstructions(
                    insertIndex,
                    insertSmali
                )
            }
        }

        postReplySetupMethodMatch.let {
            it.method.apply {
                val insertIndex = it[-1] + 1

                val freeRegisterProvider = getFreeRegisterProvider(insertIndex, 2)
                val userIdRegister = freeRegisterProvider.getFreeRegister()
                val viewRegister = freeRegisterProvider.getFreeRegister()

                val viewIndex = it[3]
                val viewReference = getInstruction<OneRegisterInstruction>(viewIndex).reference!!

                val postReplyClassDef = firstClassDef(it.method.parameters[2].type)
                val postReplyUserIdField = postReplyClassDef.getFieldBySerializedName("user_id")
                val postReplyUserIdGetterMethodReference = postReplyClassDef.getStringGetterMethod(postReplyUserIdField.name)

                val insertSmali = $$"""
                    move-object/from16 v$$viewRegister, p1
                    invoke-virtual { v$$viewRegister }, $$viewReference
                    move-result-object v$$viewRegister
                    
                    move-object/from16 v$$userIdRegister, p3
                    invoke-virtual { v$$userIdRegister }, $$postReplyUserIdGetterMethodReference
                    move-result-object v$$userIdRegister
                    
                    invoke-static { v$$viewRegister, v$$userIdRegister }, $$SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->setUserId(Landroid/view/View;Ljava/lang/String;)V
                """

                addInstructions(
                    insertIndex,
                    insertSmali
                )
            }
        }

        setOf(
            postHistoryRealmSetupMethodMatch,
            postHistoryRealmReSetupMethodMatch
        ).forEach { methodMatch ->
            methodMatch.let {
                it.method.apply {
                    val postInfoClassDef = firstClassDef(POST_INFO_CLASS_DESCRIPTOR)

                    val userIdField = postInfoClassDef.getFieldBySerializedName("user_id")
                    val userIpField = postInfoClassDef.getFieldBySerializedName("ip")
                    val userNameField = postInfoClassDef.getFieldBySerializedName("name")

                    val userIdGetterMethodReference = postInfoClassDef.getStringGetterMethod(userIdField.name)
                    val userIpGetterMethodReference = postInfoClassDef.getStringGetterMethod(userIpField.name)
                    val userNameGetterMethodReference = postInfoClassDef.getStringGetterMethod(userNameField.name, it)

                    val instructions = it.method.instructions
                    val lastIndex = instructions.lastIndex

                    instructions.reversed().forEachIndexed { reversedIndex, instruction ->
                        instruction.methodReference?.let { methodReference ->
                            if (methodReference == userNameGetterMethodReference) {
                                val index = lastIndex - reversedIndex

                                val postInfoIndex = index
                                val postInfoRegister = getInstruction<Instruction>(postInfoIndex).registersUsed.first()

                                val userNameIndex = index + 1
                                val userNameRegister = getInstruction<OneRegisterInstruction>(userNameIndex).registerA

                                val registerProvider = getFreeRegisterProvider(userNameIndex, 2, userNameRegister)
                                val userIdRegister = registerProvider.getFreeRegister()
                                val userIpRegister = registerProvider.getFreeRegister()

                                addInstructions(
                                    userNameIndex + 1,
                                    $$"""
                                        invoke-static {v$$userNameRegister, v$$userIdRegister, v$$userIpRegister }, $$SHOW_USER_ID_PATCH_EXTENSION_CLASS_DESCRIPTOR->addUserId(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                                        move-result-object v$$userNameRegister
                                    """
                                )

                                addInstructions(
                                    postInfoIndex,
                                    $$"""
                                        invoke-virtual { v$$postInfoRegister }, $$userIdGetterMethodReference
                                        move-result-object v$$userIdRegister
                                        invoke-virtual { v$$postInfoRegister }, $$userIpGetterMethodReference
                                        move-result-object v$$userIpRegister
                                    """
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}