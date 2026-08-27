package app.revanced.patches.dcinside.management.floatingButton

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

private fun String.toPascalCase() = replaceFirstChar { it.uppercase() }

private fun extensionClassDescriptorFor(patchName: String) =
    "Lapp/revanced/extension/dcinside/patches/management/floatingButton/$patchName/${patchName.toPascalCase()}Patch;"

private const val FLOATING_BUTTON_TOGGLE_PATCH_EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/dcinside/patches/management/floatingButton/FloatingButtonTogglePatch;"

private const val FLOATING_BUTTON_CONTAINER_ID_PREFIX= "revanced_floating_button"

private val floatingButtonContainerResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        copyResources(
            "dcinside/floatingButton",
            ResourceGroup(
                "drawable",
                "revanced_floating_button_toggle.xml",
            )
        )


        document("res/values/ids.xml").use { document ->
            listOf(
                "${FLOATING_BUTTON_CONTAINER_ID_PREFIX}_container",
                "${FLOATING_BUTTON_CONTAINER_ID_PREFIX}_sub_container",
                "${FLOATING_BUTTON_CONTAINER_ID_PREFIX}_toggle",
            ).forEach { idName ->
                document.documentElement.appendChild(
                    document.createElement("item").apply {
                        setAttribute("type", "id")
                        setAttribute("name", idName)
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

            val container = document.createElement("LinearLayout").apply {
                setAttribute("android:id", "@+id/${FLOATING_BUTTON_CONTAINER_ID_PREFIX}_container")
                setAttribute("android:layout_width", "wrap_content")
                setAttribute("android:layout_height", "wrap_content")
                setAttribute("android:layout_gravity", "end|bottom")
                setAttribute("android:layout_marginBottom", "134.0dp")
                setAttribute("android:layout_marginEnd", "15.0dp")
                setAttribute("android:orientation", "vertical")
            }

            val subContainer = document.createElement("LinearLayout").apply {
                setAttribute("android:id", "@+id/${FLOATING_BUTTON_CONTAINER_ID_PREFIX}_sub_container")
                setAttribute("android:layout_width", "wrap_content")
                setAttribute("android:layout_height", "wrap_content")
                setAttribute("android:orientation", "vertical")
                setAttribute("android:visibility", "gone")
            }
            container.appendChild(subContainer)

            val toggleButton = quickWrite.cloneNode(true) as Element
            toggleButton.apply {
                setAttribute("android:id", "@+id/${FLOATING_BUTTON_CONTAINER_ID_PREFIX}_toggle")
                removeAttribute("android:layout_gravity")
                removeAttribute("android:layout_marginBottom")
                removeAttribute("android:layout_marginEnd")
                setAttribute("android:layout_marginTop", "10.0dp")
            }

            toggleButton.childNodes.asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull { it.tagName == "androidx.appcompat.widget.AppCompatImageView" }
                ?.apply {
                    setAttribute("android:padding", "6.0dp")
                    setAttribute("android:src", "@drawable/revanced_floating_button_toggle")
                    removeAttribute("android:tint")
                    removeAttribute("app:tint")
                }

            container.appendChild(toggleButton)

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
                    id == "@id/${FLOATING_BUTTON_CONTAINER_ID_PREFIX}_sub_container" || id == "@+id/${FLOATING_BUTTON_CONTAINER_ID_PREFIX}_sub_container"
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
    FloatingButtonDefinition("gallScope", "gall_scope") { settingId ->
        setOf(
            NonInteractivePreference(
                key = "revanced_${settingId}_post_header_interaction_guide"
            )
        )
    },
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
    )

    apply {
        addResources("dcinside", "management.floatingButton.floatingButtonPatch")

        val floatingButtonGuidePreference = NonInteractivePreference(
            key = "revanced_floating_button_guide"
        )

        val floatingButtonScreens = floatingButtonDefinitions.map { def ->
            PreferenceScreenPreference(
                key = "revanced_${def.settingId}_screen",
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("revanced_show_${def.settingId}_button"),
                ) + def.extraPreferences(def.settingId)
            )
        }.toSet()

        PreferenceScreen.MANAGEMENT.addPreferences(
            PreferenceScreenPreference(
                key = "revanced_floating_button_screen",
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = setOf(floatingButtonGuidePreference) + floatingButtonScreens,
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

                    insertSmali += $$"""
                        const-string v$$register, "$$FLOATING_BUTTON_CONTAINER_ID_PREFIX"
                        invoke-static { v0, p1, v$$register }, $$FLOATING_BUTTON_TOGGLE_PATCH_EXTENSION_CLASS_DESCRIPTOR->setFloatingButtonToggleVisibility(Landroid/view/View;ZLjava/lang/String;)V
                    """

                    addInstructions(insertIndex, insertSmali)
                }
            }
        }
    }
}