package com.rigoni.citiesindex.utils;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class StreamUtils {
    private static final String TAG = "StreamUtils";

    /**
     * Closes an {@link InputStream} if needed, errors are ignored.
     * @param stream the stream to be closed.
     */
    public static void closeInputStreamNoThrow(@Nullable final InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (final IOException e) {
                Log.e(TAG, "Failed to close InputStream, ignoring");
            }
        }
    }
}
