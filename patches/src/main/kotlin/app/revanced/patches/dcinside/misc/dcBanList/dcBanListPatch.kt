package app.revanced.patches.dcinside.misc.dcBanList

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.hook.json.jsonHookPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.InputType
import app.revanced.patches.shared.misc.settings.preference.NonInteractivePreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.TextPreference
import app.revanced.util.asSequence
import app.revanced.util.findFreeRegister
import org.w3c.dom.Element

private const val DC_BAN_LIST_BUTTON_ID_NAME = "revanced_dcinside_dc_ban_list_button"

private const val DC_BAN_LIST_EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/misc/dcBanList/DcBanListPatch;"

private val dcBanListResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        document("res/values/ids.xml").use { document ->
            val resources = document.documentElement
            val exists = document.getElementsByTagName("id")
                .asSequence()
                .any { it.attributes?.getNamedItem("name")?.nodeValue == DC_BAN_LIST_BUTTON_ID_NAME }

            if (!exists) {
                resources.appendChild(
                    document.createElement("item").apply {
                        setAttribute("type", "id")
                        setAttribute("name", DC_BAN_LIST_BUTTON_ID_NAME)
                    }
                )
            }
        }

        document("res/layout/fragment_post_list.xml").use { document ->
            val quickWrite = document.getElementsByTagName("androidx.constraintlayout.widget.ConstraintLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull { it.getAttribute("android:id") == "@id/post_list_quick_write" }
                ?: error("Could not find post_list_quick_write in fragment_post_list.xml")

            val containerId = "revanced_dcinside_floating_button_container"
            var container = document.getElementsByTagName("LinearLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull {
                    val id = it.getAttribute("android:id")
                    id == "@id/$containerId" || id == "@+id/$containerId"
                }

            // 1. 컨테이너가 없는 경우 새로 생성 및 삽입
            if (container == null) {
                container = document.createElement("LinearLayout").apply {
                    setAttribute("android:id", "@+id/$containerId")
                    setAttribute("android:layout_width", "wrap_content")
                    setAttribute("android:layout_height", "wrap_content")
                    setAttribute("android:layout_gravity", "end|bottom")
                    setAttribute("android:layout_marginBottom", "134.0dp")
                    setAttribute("android:layout_marginEnd", "15.0dp")
                    setAttribute("android:orientation", "vertical")
                }
                quickWrite.parentNode.insertBefore(container, quickWrite.nextSibling)
            }

            // 2. 신규 버튼 생성 및 컨테이너 내부 첫 번째 자식으로 주입
            val exists = document.getElementsByTagName("androidx.constraintlayout.widget.ConstraintLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .any { it.getAttribute("android:id") == "@id/$DC_BAN_LIST_BUTTON_ID_NAME" }

            if (!exists) {
                val dcBanListButton = quickWrite.cloneNode(true) as Element
                dcBanListButton.apply {
                    setAttribute("android:id", "@+id/$DC_BAN_LIST_BUTTON_ID_NAME")
                    removeAttribute("android:layout_gravity")
                    removeAttribute("android:layout_marginBottom")
                    removeAttribute("android:layout_marginEnd")
                    setAttribute("android:layout_marginTop", "10.0dp")
                }

                dcBanListButton.childNodes.asSequence()
                    .mapNotNull { it as? Element }
                    .firstOrNull { it.tagName == "androidx.appcompat.widget.AppCompatImageView" }
                    ?.apply {
                        setAttribute("android:padding", "9.0dp")
                        setAttribute("android:src", "@drawable/ic_side_notification")
                    }

                // LinearLayout 내부 제일 상단(또는 하단)에 추가
                container.appendChild(dcBanListButton)
            }
        }
    }
}

@Suppress("unused")
val dcBanListPatch = bytecodePatch(
    name = "DC Ban List",
    description = "Adds DC Ban List to the app."
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        dcBanListResourcePatch,
        jsonHookPatch,
    )

    apply {
        addResources("dcinside", "misc.dcBanList.dcBanListPatch")

        PreferenceScreen.MISC.addPreferences(
            SwitchPreference("revanced_show_dc_ban_list_button"),
            NonInteractivePreference(
                key = "revanced_dc_ban_list_google_login_webview",
                tag = "app.revanced.extension.dcinside.settings.preference.GoogleLoginWebViewPreference",
                selectable = true,
            ),
            TextPreference(
                key = "revanced_dc_ban_list_sheet_id_map",
                inputType = InputType.TEXT_MULTI_LINE,
            ),
        )

        apply {
            quickWriteVisibilityMethodMatch.let {
                it.method.apply {
                    val insertIndex = it[0]

                    val dcBanListButtonIdNameRegister = findFreeRegister(insertIndex)

                    addInstructions(
                        insertIndex,
                        $$"""
                            const-string v$$dcBanListButtonIdNameRegister, "$$DC_BAN_LIST_BUTTON_ID_NAME"
                            invoke-static { v0, p1, v$$dcBanListButtonIdNameRegister }, $$DC_BAN_LIST_EXTENSION_CLASS_DESCRIPTOR->setDcBanListButtonVisibility(Landroid/view/View;ZLjava/lang/String;)V
                        """
                    )
                }
            }

            postListOnViewCreatedMethodMatch.let {
                it.method.apply {
                    val onViewCreatedIndex = it[0]

                    val dcBanListButtonIdNameRegister = findFreeRegister(onViewCreatedIndex)

                    addInstructions(
                        onViewCreatedIndex + 1,
                        $$"""
                            const-string v$$dcBanListButtonIdNameRegister, "$$DC_BAN_LIST_BUTTON_ID_NAME"
                            invoke-static { p1, v$$dcBanListButtonIdNameRegister }, $$DC_BAN_LIST_EXTENSION_CLASS_DESCRIPTOR->setupDcBanListButton(Landroid/view/View;Ljava/lang/String;)V
                        """
                    )
                }
            }
        }
    }
}