package app.revanced.patches.dcinside.management.post.duplicatePost

import app.revanced.patcher.extensions.addInstructions
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
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/management/post/duplicatePost/DuplicatePostSearchPatch;"

context(context: ResourcePatchContext)
private fun injectDuplicatePostSearchButton(layoutPath: String) {
    context.document(layoutPath).use { document ->
        val root = document.documentElement ?: return@use

        var enablePushElement: Element? = null
        var replyCountWrapElement: Element? = null

        root.doRecursively { node ->
            if (node is Element) {
                when (node.getAttribute("android:id")) {
                    "@+id/read_header_enable_push", "@id/read_header_enable_push" -> enablePushElement = node
                    "@+id/read_header_reply_count_wrap", "@id/read_header_reply_count_wrap" -> replyCountWrapElement = node
                }
            }
        }

        val targetLeft = enablePushElement ?: return@use
        val targetRight = replyCountWrapElement ?: return@use

        val searchButtonElement = document.createElement("TextView").apply {
            setAttribute("android:textAppearance", "?attr/textTypePostExt")
            setAttribute("android:textSize", "13sp")
            setAttribute("android:textColor", "?attr/colorAccent")
            setAttribute("android:gravity", "center")
            setAttribute("android:id", "@+id/revanced_duplicate_post_search_button")
            setAttribute("android:text", "순회검사")
            setAttribute("android:background", "@drawable/rounded_border_reply")
            setAttribute("android:paddingTop", "6dp")
            setAttribute("android:paddingBottom", "6dp")
            setAttribute("android:paddingStart", "3dp")
            setAttribute("android:paddingEnd", "4dp")
            setAttribute("android:layout_width", "wrap_content")
            setAttribute("android:layout_height", "wrap_content")
            setAttribute("android:layout_marginEnd", "6dp")
            setAttribute("android:singleLine", "true")
            setAttribute("android:includeFontPadding", "false")
            setAttribute("android:visibility", "gone")

            // push는 빼고 reply 기준으로만 정렬
            setAttribute("app:layout_constraintEnd_toStartOf", "@+id/read_header_reply_count_wrap")
            setAttribute("app:layout_constraintTop_toTopOf", "@+id/read_header_reply_count_wrap")
            setAttribute("app:layout_constraintBottom_toBottomOf", "@+id/read_header_reply_count_wrap")
        }

        targetRight.parentNode?.insertBefore(searchButtonElement, targetRight)
    }
}

internal val duplicatePostSearchResourcePatch = resourcePatch {
    compatibleWith("com.dcinside.app.android")

    apply {
        injectDuplicatePostSearchButton("res/layout/view_read_header.xml")
    }
}

@Suppress("unused")
val duplicatePostSearchPatch = bytecodePatch(
    name = "Duplicate post search",
    description = "Adds option to search duplicate posts.",
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        duplicatePostSearchResourcePatch,
    )

    apply {
        addResources("dcinside", "management.post.duplicatePost.duplicatePostSearchPatch")

        PreferenceScreen.MANAGEMENT.addPreferences(
            SwitchPreference("revanced_show_duplicate_post_search_button"),
        )

        postHeaderSetupMethodMatch.let {
            it.method.apply {
                addInstructions(
                    0,
                    $$"""
                        move-object/from16 v0, p0
                        invoke-static { v0 }, $$EXTENSION_CLASS_DESCRIPTOR->setupSearchButton(Landroid/view/View;)V
                    """
                )
            }
        }
    }
}