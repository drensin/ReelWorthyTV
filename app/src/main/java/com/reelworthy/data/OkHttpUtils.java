package com.reelworthy.data;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Utility class to interact with OkHttp 4.x static methods from Java.
 * <p>
 * <b>Architectural Note:</b> This class exists to bypass a Kotlin compiler
 * issue where calling
 * deprecated OkHttp static methods (like {@code MediaType.parse} or
 * {@code RequestBody.create})
 * causes an error ("moved to extension function") that cannot be suppressed.
 * calling them from
 * Java avoids this restriction, allowing us to maintain compatibility with the
 * transitively
 * included OkHttp version.
 */
public class OkHttpUtils {

    /**
     * Creates a {@link MediaType} from a string.
     * 
     * @param type The media type string (e.g. "application/json").
     * @return The parsed MediaType, or null if invalid.
     */
    public static MediaType createMediaType(String type) {
        return MediaType.parse(type);
    }

    /**
     * Creates a {@link RequestBody} from a string content.
     * 
     * @param mediaType The MediaType of the content.
     * @param content   The string content to include in the body.
     * @return A new RequestBody instance.
     */
    public static RequestBody createRequestBody(MediaType mediaType, String content) {
        return RequestBody.create(mediaType, content);
    }
}
