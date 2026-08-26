package app.revanced.extension.dcinside.utils.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;

public final class StreamUtils {
    public static final StreamUtils INSTANCE = new StreamUtils();

    private StreamUtils() {
    }

    /**
     * Reads an InputStream into a String.
     *
     * @param inputStream The input stream to read.
     * @return The string content of the stream.
     * @throws IOException If an I/O error occurs.
     */
    @NotNull
    public static String toString(@NotNull InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString();
        }
    }

    /**
     * Converts a String into an InputStream.
     *
     * @param string The string to convert.
     * @return An InputStream containing the string bytes.
     */
    @NotNull
    public static InputStream fromString(@NotNull String string) {
        return new ByteArrayInputStream(string.getBytes());
    }
}