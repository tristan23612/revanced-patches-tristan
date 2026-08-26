package app.revanced.patches.dcinside.misc.hook.okhttp

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch

private const val CUSTOM_NETWORK_INTERCEPTOR_EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/hook/okhttp/CustomNetworkInterceptorPatch;"

@Suppress("unused")
val customNetworkInterceptorPatch = bytecodePatch {
    compatibleWith("com.dcinside.app.android")

    dependsOn(sharedExtensionPatch)

    apply {
        okhttpBuildMethod.addInstructions(
            0,
            $$"""
                new-instance v0, $$CUSTOM_NETWORK_INTERCEPTOR_EXTENSION_CLASS_DESCRIPTOR
                invoke-direct { v0 }, $$CUSTOM_NETWORK_INTERCEPTOR_EXTENSION_CLASS_DESCRIPTOR-><init>()V
                invoke-virtual { p0, v0 }, Lokhttp3/OkHttpClient$Builder;->addNetworkInterceptor(Lokhttp3/Interceptor;)Lokhttp3/OkHttpClient$Builder;
            """
        )

        addQueryParameterHookMethod.addInstructions(
            0,
            $$"""
                move-object/from16 v0, p1
                move-object/from16 v1, p2
                invoke-static {v0, v1}, $$CUSTOM_NETWORK_INTERCEPTOR_EXTENSION_CLASS_DESCRIPTOR->hookParam(Ljava/lang/String;Ljava/lang/String;)V
            """
        )
    }
}