dependencies {
    compileOnly(project(":extensions:shared:library"))
    compileOnly(libs.annotation)
    compileOnly(libs.okhttp)

    implementation(libs.hiddenapibypass)
}

android {
    defaultConfig {
        minSdk = 26
    }
}
