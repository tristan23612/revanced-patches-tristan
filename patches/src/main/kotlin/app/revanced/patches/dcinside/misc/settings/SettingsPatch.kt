package app.revanced.patches.dcinside.misc.settings

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.*
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.util.*
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val BASE_ACTIVITY_HOOK_CLASS_DESCRIPTOR = "Lapp/revanced/extension/shared/settings/BaseActivityHook;"
private const val DCINSIDE_ACTIVITY_HOOK_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/settings/DcinsideActivityHook;"

private val preferences = mutableSetOf<BasePreference>()

private val settingsResourcePatch = resourcePatch {
    dependsOn(
        resourceMappingPatch,
        settingsPatch(
            rootPreferences = null,
            preferences = preferences,
        ),
    )

    apply {
        copyResources(
            "settings",
            ResourceGroup(
                "drawable",
                "revanced_settings_icon_dynamic.xml",
                "revanced_settings_icon.xml",
                "revanced_settings_icon_bold.xml",
                "revanced_settings_screen_00_about.xml",
                "revanced_settings_screen_00_about_bold.xml",
                "revanced_settings_screen_01_ads.xml",
                "revanced_settings_screen_01_ads_bold.xml",
                "revanced_settings_screen_02_alt_thumbnails.xml",
                "revanced_settings_screen_02_alt_thumbnails_bold.xml",
                "revanced_settings_screen_03_feed.xml",
                "revanced_settings_screen_03_feed_bold.xml",
                "revanced_settings_screen_04_general.xml",
                "revanced_settings_screen_04_general_bold.xml",
                "revanced_settings_screen_05_player.xml",
                "revanced_settings_screen_05_player_bold.xml",
                "revanced_settings_screen_06_shorts.xml",
                "revanced_settings_screen_06_shorts_bold.xml",
                "revanced_settings_screen_07_seekbar.xml",
                "revanced_settings_screen_07_seekbar_bold.xml",
                "revanced_settings_screen_08_swipe_controls.xml",
                "revanced_settings_screen_08_swipe_controls_bold.xml",
                "revanced_settings_screen_09_return_youtube_dislike.xml",
                "revanced_settings_screen_09_return_youtube_dislike_bold.xml",
                "revanced_settings_screen_10_sponsorblock.xml",
                "revanced_settings_screen_10_sponsorblock_bold.xml",
                "revanced_settings_screen_11_misc.xml",
                "revanced_settings_screen_11_misc_bold.xml",
                "revanced_settings_screen_12_video.xml",
                "revanced_settings_screen_12_video_bold.xml",
            ),
        )

        copyResources(
            "dcinside/settings",
            ResourceGroup(
                "layout",
                "preference_with_icon.xml"
            )
        )

        document("AndroidManifest.xml").use { document ->
            val licenseElement = document.childNodes.findElementByAttributeValueOrThrow(
                "android:name",
                "com.dcinside.app.license.LicenseActivity",
            )

            licenseElement.setAttribute(
                "android:configChanges",
                "orientation|screenSize|keyboardHidden",
            )
        }

        document("res/layout/fragment_settings.xml").use { document ->
            val childNodes = document.childNodes

            // 기존 "오픈소스 라이선스" 항목 노드를 찾아서 원래 부모(앱 정보 카테고리 그룹)에서 떼어냄.
            val licenseElement = childNodes.findElementByAttributeValueOrThrow(
                "android:id",
                "@id/setting_license",
            )
            licenseElement.parentNode.removeChild(licenseElement)

            // 최상위 세로 LinearLayout (각 카테고리 헤더/그룹이 나열된 컨테이너)을 찾음.
            // ScrollView 바로 아래에 있는 그 LinearLayout.
            val rootLinearLayout = childNodes.findElementByAttributeValueOrThrow(
                "android:text",
                "@string/setting_app_info",
            ).parentNode

            // 맨 위에 삽입하기 위한 기준점 (현재 최상위 LinearLayout의 첫 번째 자식).
            val firstChild = rootLinearLayout.firstChild

            // "ReVanced" 섹션 헤더 (다른 카테고리 헤더 TextView와 동일한 스타일로 생성).
            val header = document.createElement("TextView")
            header.setAttribute("android:textAppearance", "?attr/textTypeSub")
            header.setAttribute("android:textColor", "?attr/colorPrimaryText")
            header.setAttribute("android:gravity", "center_vertical")
            header.setAttribute("android:background", "?attr/windowBackgroundInverse")
            header.setAttribute("android:paddingTop", "5dp")
            header.setAttribute("android:paddingBottom", "5dp")
            header.setAttribute("android:layout_width", "match_parent")
            header.setAttribute("android:layout_height", "wrap_content")
            header.setAttribute("android:text", "@string/revanced_settings_title")
            header.setAttribute("android:paddingStart", "10dp")
            header.setAttribute("android:paddingEnd", "10dp")

            // 헤더 아래에 항목 하나만 담을 그룹 LinearLayout (기존 카테고리 그룹과 동일한 패턴).
            val group = document.createElement("LinearLayout")
            group.setAttribute("android:orientation", "vertical")
            group.setAttribute("android:layout_width", "match_parent")
            group.setAttribute("android:layout_height", "wrap_content")
            group.setAttribute("android:divider", "?attr/divide_n")
            group.setAttribute("android:showDividers", "middle")
            group.appendChild(licenseElement)

            // 최상위 LinearLayout 맨 앞에 새 섹션(헤더 + 그룹)을 순서대로 삽입.
            rootLinearLayout.insertBefore(header, firstChild)
            rootLinearLayout.insertBefore(group, firstChild)
        }

        document("res/values/strings.xml").use { document ->
            document.documentElement.childNodes.findElementByAttributeValueOrThrow(
                "name",
                "setting_license"
            ).textContent = "ReVanced 설정"
        }
    }
}

val settingsPatch = bytecodePatch(
    description = "Adds settings for ReVanced to DCInside.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsResourcePatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "misc.settings.settingsPatch")

        modifyActivityForSettingsInjection(
            licenseActivityOnCreateMethod.classDef,
            licenseActivityOnCreateMethod,
            DCINSIDE_ACTIVITY_HOOK_CLASS_DESCRIPTOR,
        )
    }

    afterDependents {
        PreferenceScreen.close()
    }
}

/**
 * Modifies the activity to show ReVanced settings instead of its original purpose.
 */
internal fun modifyActivityForSettingsInjection(
    activityOnCreateClass: MutableClassDef,
    activityOnCreateMethod: MutableMethod,
    extensionClassType: String,
) {
    // Modify Activity and remove all existing layout code.
    // Must modify an existing activity and cannot add a new activity to the manifest,
    // as that fails for root installations.
    activityOnCreateMethod.addInstructions(
        0,
        """
            invoke-super { p0, p1 }, ${activityOnCreateClass.superclass}->onCreate(Landroid/os/Bundle;)V
            
            invoke-virtual { p0 }, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
            move-result-object v0
            const-string v1, "revanced_settings_intent"
            invoke-static { v1 }, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
            move-result-object v1
            invoke-virtual { v0, v1 }, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;
            
            invoke-static { p0 }, $extensionClassType->initialize(Landroid/app/Activity;)V
            return-void
        """,
    )

    // Remove other methods as they will break as the onCreate method is modified above.
    activityOnCreateClass.apply {
        methods.removeIf { it != activityOnCreateMethod && !MethodUtil.isConstructor(it) }
    }

    // Override base context to allow using ReVanced specific settings.
    ImmutableMethod(
        activityOnCreateClass.type,
        "attachBaseContext",
        listOf(ImmutableMethodParameter("Landroid/content/Context;", null, null)),
        "V",
        AccessFlags.PROTECTED.value,
        null,
        null,
        MutableMethodImplementation(3),
    ).toMutable().apply {
        addInstructions(
            """
                invoke-static { p1 }, $BASE_ACTIVITY_HOOK_CLASS_DESCRIPTOR->getAttachBaseContext(Landroid/content/Context;)Landroid/content/Context;
                move-result-object p1
                invoke-super { p0, p1 }, ${activityOnCreateClass.superclass}->attachBaseContext(Landroid/content/Context;)V
                return-void
            """,
        )
    }.let(activityOnCreateClass.methods::add)

    ImmutableMethod(
        activityOnCreateClass.type,
        "onBackPressed",
        emptyList(),
        "V",
        AccessFlags.PUBLIC.value,
        null,
        null,
        MutableMethodImplementation(3),
    ).toMutable().apply {
        addInstructions(
            """
                invoke-static {}, $extensionClassType->handleBackPress()Z
                move-result v0
                if-nez v0, :search_handled
                invoke-virtual { p0 }, Landroid/app/Activity;->finish()V
                :search_handled
                return-void
            """,
        )
    }.let(activityOnCreateClass.methods::add)
}

object PreferenceScreen : BasePreferenceScreen() {
    val ADS = Screen(
        key = "revanced_settings_screen_01_ads",
        summaryKey = null,
        icon = "@drawable/revanced_settings_screen_01_ads",
        iconBold = "@drawable/revanced_settings_screen_01_ads_bold",
        layout = "@layout/preference_with_icon",
    )
    val FEED = Screen(
        key = "revanced_settings_screen_03_feed",
        summaryKey = null,
        icon = "@drawable/revanced_settings_screen_03_feed",
        iconBold = "@drawable/revanced_settings_screen_03_feed_bold",
        layout = "@layout/preference_with_icon",
    )
    val GENERAL = Screen(
        key = "revanced_settings_screen_04_general",
        summaryKey = null,
        icon = "@drawable/revanced_settings_screen_04_general",
        iconBold = "@drawable/revanced_settings_screen_04_general_bold",
        layout = "@layout/preference_with_icon",
    )
    val MISC = Screen(
        key = "revanced_settings_screen_11_misc",
        summaryKey = null,
        icon = "@drawable/revanced_settings_screen_11_misc",
        iconBold = "@drawable/revanced_settings_screen_11_misc_bold",
        layout = "@layout/preference_with_icon",
    )

    override fun commit(screen: PreferenceScreenPreference) {
        preferences += screen
    }
}