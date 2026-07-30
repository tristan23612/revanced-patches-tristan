group = "app.tristan"

patches {
    about {
        name = "ReVanced Patches Tristan"
        description = "Patches for ReVanced by Tristan"
        source = "git@github.com:tristan23612/revanced-patches-tristan.git"
        author = "Tristan"
        contact = "https://github.com/tristan23612/revanced-patches-tristan"
        website = "https://github.com/tristan23612/revanced-patches-tristan"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    implementation(libs.apksig)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters"
        )
    }
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/tristan23612/revanced-patches-tristan")
            credentials(PasswordCredentials::class)
        }
    }
}