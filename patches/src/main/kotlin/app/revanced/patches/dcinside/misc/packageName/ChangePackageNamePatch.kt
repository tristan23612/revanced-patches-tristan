package app.revanced.patches.dcinside.misc.packageName

import app.revanced.patcher.patch.*
import app.revanced.util.asSequence
import app.revanced.util.getNode
import org.w3c.dom.Element

@Suppress("unused")
val changePackageNamePatch = resourcePatch(
    name = "Change dcinside package name",
    description = "Appends \".revanced\" to the package name. " +
            "Changing the package name of the app can lead to unexpected issues.",
    use = false,
) {
    compatibleWith("com.dcinside.app.android")

    val updatePermissions by booleanOption(
        default = true,
        name = "Update permissions",
        description = "Update compatibility receiver permissions. " +
                "Enabling this can fix installation errors, but this can also break features in certain apps.",
    )

    val updateProviders by booleanOption(
        default = true,
        name = "Update providers",
        description = "Update provider names declared by the app. " +
                "Enabling this can fix installation errors, but this can also break features in certain apps.",
    )

    afterDependents {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getNode("manifest") as Element
            val packageName = manifest.getAttribute("package")

            val newPackageName = "$packageName.revanced"

            manifest.setAttribute("package", newPackageName)

            if (updatePermissions == true) {
                val permissions = manifest.getElementsByTagName("permission").asSequence()
                val usesPermissions = manifest.getElementsByTagName("uses-permission").asSequence()

                (permissions + usesPermissions)
                    .map { it as Element }
                    .forEach {
                        val name = it.getAttribute("android:name")
                        if (name.startsWith("$packageName.")) {
                            it.setAttribute("android:name", name.replace(packageName, newPackageName))
                        }
                    }
            }

            if (updateProviders == true) {
                val providers = manifest.getElementsByTagName("provider").asSequence()

                for (node in providers) {
                    val provider = node as Element

                    val authorities = provider.getAttribute("android:authorities")
                    if (!authorities.startsWith("$packageName.")) continue

                    provider.setAttribute("android:authorities", authorities.replace(packageName, newPackageName))
                }

                runCatching {
                    document("res/values/strings.xml").use { stringsDocument ->
                        val stringNodes = stringsDocument.getElementsByTagName("string").asSequence()
                        for (node in stringNodes) {
                            val element = node as Element
                            val content = element.textContent
                            if (content.contains(packageName)) {
                                element.textContent = content.replace(packageName, newPackageName)
                            }
                        }
                    }
                }
            }
        }
    }
}
