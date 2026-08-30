package app.revanced.extension.dcinside.patches.hook.json;

import app.revanced.extension.dcinside.patches.hook.patch.Hook;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public interface JsonHook extends Hook<JSONObject> {
    /**
     * Transforms the given JSON object using a custom implementation.
     *
     * @param json The JSON object to transform. Must not be null.
     * @return The transformed JSON object. Must not be null.
     */
    @NotNull
    JSONObject transform(@NotNull JSONObject json);

    @Override
    @NotNull
    default JSONObject hook(@NotNull JSONObject type) {
        return transform(type);
    }
}