dependencies {
    compileOnly(project(":extensions:shared:library"))
    compileOnly(libs.annotation)

    implementation(libs.hiddenapibypass)
}

android {
    defaultConfig {
        minSdk = 26
    }
}
