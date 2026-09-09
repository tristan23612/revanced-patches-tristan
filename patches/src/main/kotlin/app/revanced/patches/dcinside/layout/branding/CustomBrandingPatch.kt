package app.revanced.patches.dcinside.layout.branding

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.*
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.util.*
import app.revanced.util.Utils.trimIndentMultiline
import org.w3c.dom.Element
import java.io.File

internal const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/layout/branding/DcinsideBrandingPatch;"

private val mipmapDirectories = mapOf(
    "mipmap-mdpi" to "108x108 px",
    "mipmap-hdpi" to "162x162 px",
    "mipmap-xhdpi" to "216x216 px",
    "mipmap-xxhdpi" to "324x324 px",
    "mipmap-xxxhdpi" to "432x432 px",
)

// original 포함, custom은 별도 처리
private val iconStyles = arrayOf("original", "rounded", "minimal", "scaled")
private const val PRESET_APP_NAMES = 5
private const val CUSTOM_STYLE = "custom"

private const val LAUNCHER_PREFIX = "revanced_launcher_"
private const val ADAPTIVE_BG_PREFIX = "revanced_adaptive_background_"
private const val ADAPTIVE_FG_PREFIX = "revanced_adaptive_foreground_"
private const val ADAPTIVE_MONO_PREFIX = "revanced_adaptive_monochrome_"

private val CUSTOM_ICON_FILES = arrayOf(
    "$ADAPTIVE_BG_PREFIX$CUSTOM_STYLE.png",
    "$ADAPTIVE_FG_PREFIX$CUSTOM_STYLE.png",
)

val customBrandingPatch = resourcePatch(
    name = "Custom branding",
    description = "디시인사이드 앱 이름과 아이콘을 변경하는 옵션을 추가합니다.",
) {
    compatibleWith("com.dcinside.app.android")
    dependsOn(
        sharedExtensionPatch,
        addResourcesPatch,
    )

    val customName by stringOption(
        name = "App name",
        description = "커스텀 앱 이름.",
    )

    val customIcon by stringOption(
        name = "Custom icon",
        description = """
            아이콘 파일이 담긴 폴더 경로.
            다음 폴더 중 필요한 해상도 폴더에 이미지가 있어야 합니다:
            ${mipmapDirectories.keys.joinToString("\n") { "- $it" }}

            각 폴더는 다음 파일을 모두 포함해야 합니다:
            ${CUSTOM_ICON_FILES.joinToString("\n")}

            이미지 크기:
            ${mipmapDirectories.map { (dpi, dim) -> "- $dpi: $dim" }.joinToString("\n")}
        """.trimIndentMultiline(),
    )

    dependsOn(
        bytecodePatch {
            dependsOn(sharedExtensionPatch)
            apply {
                mainActivityOnCreateMethodMatch.method.addInstruction(
                    0,
                    "invoke-static { }, $EXTENSION_CLASS->setBranding()V",
                )

                userProvidedCustomNameExtensionMethod.returnEarly(customName != null)
                userProvidedCustomIconExtensionMethod.returnEarly(customIcon != null)
                // 필요 시 알림 아이콘 후킹 로직 여기에 추가
            }
        },
    )

    afterDependents {
        val useCustomName = customName != null
        if (useCustomName) {
            document("AndroidManifest.xml").use { document ->
                val application = document.getElementsByTagName("application").item(0) as Element
                application.setAttribute("android:label", customName)
            }
        }
    }

    apply {
        val useCustomName = customName != null
        val useCustomIcon = customIcon != null

        // shared 쪽 아이콘 엔트리 배열/타이틀 문자열도 함께 로드
        addResources("dcinside", "layout.branding.customBrandingPatch")

        PreferenceScreen.GENERAL.addPreferences(
            if (useCustomName) {
                ListPreference(
                    key = "revanced_dcinside_custom_branding_name",
                    entriesKey = "revanced_dcinside_custom_branding_name_custom_entries",
                    entryValuesKey = "revanced_dcinside_custom_branding_name_custom_entry_values",
                )
            } else {
                ListPreference("revanced_dcinside_custom_branding_name")
            },
            if (useCustomIcon) {
                ListPreference(
                    key = "revanced_dcinside_custom_branding_icon",
                    entriesKey = "revanced_dcinside_custom_branding_icon_custom_entries",
                    entryValuesKey = "revanced_dcinside_custom_branding_icon_custom_entry_values",
                )
            } else {
                ListPreference("revanced_dcinside_custom_branding_icon")
            },
        )

        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val mainAlias = document.childNodes.findElementByAttributeValueOrThrow(
                "android:name",
                "com.dcinside.app.MainActivity",
            )
            val intents = mainAlias.childNodes

            if (useCustomIcon) {
                application.setAttribute("android:icon", "@mipmap/revanced_launcher_custom")
            }

            fun createAlias(name: String, icon: String, label: String, enabled: Boolean): Element {
                val alias = document.createElement("activity-alias")
                alias.setAttribute("android:name", name)
                alias.setAttribute("android:enabled", enabled.toString())
                alias.setAttribute("android:exported", "true")
                alias.setAttribute("android:icon", "@mipmap/$icon")
                alias.setAttribute("android:label", label)
                alias.setAttribute("android:targetActivity", "com.dcinside.app.main.HomeActivity")
                for (i in 0 until intents.length) {
                    alias.appendChild(intents.item(i).cloneNode(true))
                }
                return alias
            }

            val defaultNameIndex = if (useCustomName) PRESET_APP_NAMES else 1
            val defaultIconEnabledStyle = if (useCustomIcon) CUSTOM_STYLE else "original"

            for (appIndex in 1..PRESET_APP_NAMES) {
                // appIndex별 라벨 결정: 1번은 원본, 커스텀 슬롯(5번)은 customName, 나머지는 프리셋 entry_N
                val label = when {
                    appIndex == 1 -> "@string/app_name"
                    useCustomName && appIndex == PRESET_APP_NAMES -> customName ?: "Custom"
                    else -> "@string/revanced_dcinside_custom_branding_name_entry_$appIndex"
                }

                iconStyles.forEach { style ->
                    val iconRes = if (style == "original") "ic_launcher" else "$LAUNCHER_PREFIX$style"
                    application.appendChild(
                        createAlias(
                            ".revanced_${style}_$appIndex",
                            iconRes,
                            label,
                            enabled = (appIndex == defaultNameIndex && style == defaultIconEnabledStyle),
                        ),
                    )
                }

                application.appendChild(
                    createAlias(
                        ".revanced_${CUSTOM_STYLE}_$appIndex",
                        "$LAUNCHER_PREFIX$CUSTOM_STYLE",
                        label,
                        enabled = (appIndex == defaultNameIndex && useCustomIcon && CUSTOM_STYLE == defaultIconEnabledStyle),
                    ),
                )
            }

            intents.findElementByAttributeValueOrThrow(
                "android:name", "android.intent.action.MAIN",
            ).removeFromParent()
        }

        // 아이콘 스타일별 리소스 복사 (original 제외, 이미 앱에 있는 아이콘이므로)
        // monochrome 포함 — 안 넣으면 adaptive icon XML의 참조가 깨져서 링크 에러 발생
        iconStyles.filter { it != "original" }.forEach { style ->
            copyResources(
                "custom-branding",
                ResourceGroup(
                    "drawable",
                    "$ADAPTIVE_BG_PREFIX$style.xml",
                    "$ADAPTIVE_FG_PREFIX$style.xml",
                    "$ADAPTIVE_MONO_PREFIX$style.xml",
                ),
                ResourceGroup(
                    "mipmap-anydpi-v26",
                    "$LAUNCHER_PREFIX$style.xml",
                ),
            )
        }

        // 커스텀 아이콘 템플릿 리소스 — 옵션을 안 켜도 alias가 참조할 placeholder가 있어야 링크가 통과함
        copyResources(
            "custom-branding",
            ResourceGroup(
                "drawable",
                "$ADAPTIVE_MONO_PREFIX$CUSTOM_STYLE.xml",
            ),
            ResourceGroup(
                "mipmap-anydpi-v26",
                "$LAUNCHER_PREFIX$CUSTOM_STYLE.xml",
            ),
        )
        // custom 배경/전경 PNG placeholder를 각 dpi 폴더에 복사
        mipmapDirectories.keys.forEach { dpi ->
            copyResources(
                "custom-branding",
                ResourceGroup(
                    dpi,
                    "$ADAPTIVE_BG_PREFIX$CUSTOM_STYLE.png",
                    "$ADAPTIVE_FG_PREFIX$CUSTOM_STYLE.png",
                ),
            )
        }

        // 사용자가 지정한 커스텀 아이콘 파일 복사 (placeholder를 덮어씀)
        if (useCustomIcon) {
            val iconPathFile = File(customIcon!!.trim())

            if (!iconPathFile.exists()) {
                throw PatchException("커스텀 아이콘 경로를 찾을 수 없습니다: ${iconPathFile.absolutePath}")
            }
            if (!iconPathFile.isDirectory) {
                throw PatchException("커스텀 아이콘 경로는 폴더여야 합니다: ${iconPathFile.absolutePath}")
            }

            val resourceDirectory = get("res")
            var copiedFiles = false

            iconPathFile.listFiles { file -> file.isDirectory && file.name in mipmapDirectories }!!
                .forEach { dpiSourceFolder ->
                    val targetDpiFolder = resourceDirectory.resolve(dpiSourceFolder.name)
                    if (!targetDpiFolder.exists()) {
                        throw IllegalStateException("리소스 폴더 없음: $dpiSourceFolder")
                    }

                    val customFiles = dpiSourceFolder.listFiles { file ->
                        file.isFile && file.name in CUSTOM_ICON_FILES
                    }!!

                    if (customFiles.isNotEmpty() && customFiles.size != CUSTOM_ICON_FILES.size) {
                        throw PatchException(
                            "필요한 아이콘 파일이 모두 있어야 합니다. 현재: ${customFiles.map { it.name }}",
                        )
                    }

                    customFiles.forEach { source ->
                        val target = targetDpiFolder.resolve(source.name)
                        source.copyTo(target = target, overwrite = true)
                        copiedFiles = true
                    }
                }

            if (!copiedFiles) {
                throw PatchException(
                    "지정한 경로에서 필요한 파일을 찾지 못했습니다: ${CUSTOM_ICON_FILES.contentToString()}\n경로: ${iconPathFile.absolutePath}",
                )
            }
        }
    }
}