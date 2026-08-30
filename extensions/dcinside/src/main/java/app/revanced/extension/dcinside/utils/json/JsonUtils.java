package app.revanced.extension.dcinside.utils.json;

import app.revanced.extension.dcinside.utils.stream.StreamUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public final class JsonUtils {
    public static final JsonUtils INSTANCE = new JsonUtils();

    private JsonUtils() {
    }

    /**
     * Parses a JSON object from an input stream.
     *
     * @param jsonInputStream The input stream containing the JSON object to parse.
     * @return The parsed JSONObject.
     * @throws IOException If an I/O error occurs while reading the input stream.
     * @throws JSONException If the input stream content is not a valid JSON object.
     */
    @NotNull
    public static JSONObject parseJsonObject(@NotNull InputStream jsonInputStream) throws IOException, JSONException {
        return new JSONObject(StreamUtils.toString(jsonInputStream));
    }

    /**
     * Parses a JSON array from an input stream.
     *
     * @param jsonInputStream The input stream containing the JSON array to parse.
     * @return The parsed JSONArray.
     * @throws IOException If an I/O error occurs while reading the input stream.
     * @throws JSONException If the input stream content is not a valid JSON array.
     */
    public static JSONArray parseJsonArray(@NotNull InputStream jsonInputStream) throws IOException, JSONException {
        return new JSONArray(StreamUtils.toString(jsonInputStream));
    }
}
