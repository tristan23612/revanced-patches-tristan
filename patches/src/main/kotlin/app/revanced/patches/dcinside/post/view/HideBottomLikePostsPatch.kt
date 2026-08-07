package app.revanced.patches.dcinside.post.view

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/post/view/HideBottomLikePostsPatch;"

@Suppress("unused")
val hideBottomLikePostsPatch = bytecodePatch(
    name = "Hide bottom like posts",
    description = "Hides recommended posts below the next/previous post.",
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "post.view.hideBottomLikePostsPatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_hide_bottom_like_posts"),
        )

        bottomLikePostsMethod.apply {
            addInstructions(
                0,
                $$"""
                invoke-static { }, $$EXTENSION_CLASS_DESCRIPTOR->shouldHideBottomLikePosts()Z
                move-result v0
                if-eqz v0, :show_bottom_like_posts
                
                return-void
                :show_bottom_like_posts
                """
            )
        }
    }
}