package app.revanced.patches.dcinside.misc.floatingButton

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.hook.json.jsonHookPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.BasePreference
import app.revanced.patches.shared.misc.settings.preference.InputType
import app.revanced.patches.shared.misc.settings.preference.NonInteractivePreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.TextPreference
import app.revanced.util.ResourceGroup
import app.revanced.util.asSequence
import app.revanced.util.copyResources
import app.revanced.util.findFreeRegister
import org.w3c.dom.Element

private const val FLOATING_BUTTON_CONTAINER_ID_NAME = "revanced_floating_button_container"

private fun String.toPascalCase() = replaceFirstChar { it.uppercase() }

private fun extensionClassDescriptorFor(patchName: String) =
    "Lapp/revanced/extension/dcinside/patches/misc/floatingButton/$patchName/${patchName.toPascalCase()}Patch;"

private val floatingButtonContainerResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        document("res/layout/fragment_post_list.xml").use { document ->
            val quickWrite = document.getElementsByTagName("androidx.constraintlayout.widget.ConstraintLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull { it.getAttribute("android:id") == "@id/post_list_quick_write" }
                ?: error("Could not find post_list_quick_write in fragment_post_list.xml")

            val container = document.createElement("LinearLayout").apply {
                setAttribute("android:id", "@+id/$FLOATING_BUTTON_CONTAINER_ID_NAME")
                setAttribute("android:layout_width", "wrap_content")
                setAttribute("android:layout_height", "wrap_content")
                setAttribute("android:layout_gravity", "end|bottom")
                setAttribute("android:layout_marginBottom", "134.0dp")
                setAttribute("android:layout_marginEnd", "15.0dp")
                setAttribute("android:orientation", "vertical")
            }

            quickWrite.parentNode.insertBefore(container, quickWrite.nextSibling)
        }
    }
}

context(context: ResourcePatchContext)
private fun addFloatingButtonResource(
    patchName: String,
    settingId: String,
) {
    context.apply {
        copyResources(
            "dcinside/floatingButton/$patchName",
            ResourceGroup(
                "drawable",
                "revanced_${settingId}_button.png",
            )
        )

        document("res/values/ids.xml").use { document ->
            document.documentElement.appendChild(
                document.createElement("item").apply {
                    setAttribute("type", "id")
                    setAttribute("name", "revanced_${settingId}_button")
                }
            )
        }

        document("res/layout/fragment_post_list.xml").use { document ->
            val quickWrite = document.getElementsByTagName("androidx.constraintlayout.widget.ConstraintLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull { it.getAttribute("android:id") == "@id/post_list_quick_write" }
                ?: error("Could not find post_list_quick_write in fragment_post_list.xml")

            val container = document.getElementsByTagName("LinearLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull {
                    val id = it.getAttribute("android:id")
                    id == "@id/$FLOATING_BUTTON_CONTAINER_ID_NAME" || id == "@+id/$FLOATING_BUTTON_CONTAINER_ID_NAME"
                }

            val floatingButton = quickWrite.cloneNode(true) as Element
            floatingButton.apply {
                setAttribute("android:id", "@+id/revanced_${settingId}_button")
                removeAttribute("android:layout_gravity")
                removeAttribute("android:layout_marginBottom")
                removeAttribute("android:layout_marginEnd")
                setAttribute("android:layout_marginTop", "10.0dp")
            }

            floatingButton.childNodes.asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull { it.tagName == "androidx.appcompat.widget.AppCompatImageView" }
                ?.apply {
                    setAttribute("android:padding", "6.0dp")
                    setAttribute("android:src", "@drawable/revanced_${settingId}_button")
                    removeAttribute("android:tint")
                    removeAttribute("app:tint")
                }

            // LinearLayout 내부 제일 상단(또는 하단)에 추가
            container?.appendChild(floatingButton)
        }
    }
}

private data class FloatingButtonDefinition(
    val patchName: String,
    val settingId: String,
    val extraPreferences: (settingId: String) -> Set<BasePreference>,
)

private val floatingButtonDefinitions = listOf(
    FloatingButtonDefinition("dcBanList", "dc_ban_list") { settingId ->
        setOf(
            TextPreference(
                key = "revanced_${settingId}_sheet_id_map",
                inputType = InputType.TEXT_MULTI_LINE,
            ),
            NonInteractivePreference(
                key = "revanced_${settingId}_gas_authorization_webview",
                tag = "app.revanced.extension.dcinside.settings.preference.GasAuthorizationWebViewPreference",
                selectable = true,
            ),
            NonInteractivePreference(
                key = "revanced_${settingId}_google_account_manage_webview",
                tag = "app.revanced.extension.dcinside.settings.preference.GoogleAccountManageWebViewPreference",
                selectable = true,
            ),
        )
    },
    FloatingButtonDefinition("gallScope", "gall_scope") { emptySet() },
)

private val floatingButtonResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        floatingButtonDefinitions.forEach { (patchName, settingId) ->
            addFloatingButtonResource(patchName, settingId)
        }
    }
}

@Suppress("unused")
val floatingButtonPatch = bytecodePatch(
    name = "Floating button patch",
    description = "Adds a options to add additional floating buttons upper the quick write button.",
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        jsonHookPatch,
        floatingButtonContainerResourcePatch,
        floatingButtonResourcePatch,
        gallScopePostHeaderPatch,
    )

    apply {
        addResources("dcinside", "misc.floatingButton.floatingButtonPatch")

        PreferenceScreen.MISC.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_floating_button_screen",
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = floatingButtonDefinitions.map { def ->
                    PreferenceScreenPreference(
                        key = "revanced_${def.settingId}_screen",
                        sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                        preferences = setOf(
                            SwitchPreference("revanced_show_${def.settingId}_button"),
                        ) + def.extraPreferences(def.settingId)
                    )
                }.toSet(),
            )
        )

        apply {
            floatingButtonVisibilityMethodMatch.let {
                it.method.apply {
                    val insertIndex = it[0]
                    val register = findFreeRegister(insertIndex)

                    var insertSmali = ""
                    floatingButtonDefinitions.forEach { (patchName, settingId) ->
                        val pascalName = patchName.toPascalCase()
                        val descriptor = extensionClassDescriptorFor(patchName)

                        insertSmali += $$"""
                            const-string v$$register, "revanced_$${settingId}_button"
                            invoke-static { v0, p1, v$$register }, $$descriptor->set$${pascalName}ButtonVisibility(Landroid/view/View;ZLjava/lang/String;)V
                        """
                    }

                    addInstructions(insertIndex, insertSmali)
                }
            }

            postListOnViewCreatedMethodMatch.let {
                it.method.apply {
                    val onViewCreatedIndex = it[0]
                    val register = findFreeRegister(onViewCreatedIndex)

                    var insertSmali = ""
                    floatingButtonDefinitions.forEach { (patchName, settingId) ->
                        val pascalName = patchName.toPascalCase()
                        val descriptor = extensionClassDescriptorFor(patchName)

                        insertSmali += $$"""
                            const-string v$$register, "revanced_$${settingId}_button"
                            invoke-static { p1, v$$register }, $$descriptor->setup$${pascalName}Button(Landroid/view/View;Ljava/lang/String;)V
                        """
                    }

                    addInstructions(onViewCreatedIndex + 1, insertSmali)
                }
            }
        }
    }
}