dependencies {
    compileOnly(project(":extensions:shared:library"))
    compileOnly(libs.annotation)
    compileOnly(libs.okhttp)

    implementation(libs.hiddenapibypass)
    implementation(libs.jsoup)
}

android {
    defaultConfig {
        minSdk = 26
    }
}
