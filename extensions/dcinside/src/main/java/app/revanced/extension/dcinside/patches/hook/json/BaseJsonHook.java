package app.revanced.extension.dcinside.patches.hook.json;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public abstract class BaseJsonHook implements JsonHook {
    /**
     * Applies a custom transformation or modification to the given JSON object.
     *
     * @param json The JSON object to process. Must not be null.
     * @return The JSON object after applying the transformation. Must not be null.
     */
    public abstract JSONObject apply(@NotNull JSONObject json);

    @Override
    @NotNull
    public JSONObject transform(@NotNull JSONObject json) {
        return apply(json);
    }
}
