package com.rigoni.citiesindex.utils;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;

import com.google.common.base.Preconditions;

import java.io.File;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

/**
 * A few utility functionalities for the index tree storage.
 * This does not really belong to the IndexTreeStorageFs class.
 */
public class IndexStorageUtils {

    /**
     * We need around 150 MB to build the index completely.
     * These values are used to quickly check if the storage space on the device is enough.
     * This could be smarter, however is good enough for this sample app.
     */
    public static final int BYTES_NEEDED_FOR_INDEX = 155140672;
    public static final int MEGABYTES_NEEDED_FOR_INDEX = 155140672 / 1024 / 1024;

    private static final String INDEX_BASE_DIRECTORY = "cities_index";

    /**
     * Looks for an existing index. The external cache directory is checked first, if no index is
     * found then the internal cache dir is searched.
     * @param context
     * @return the File representing the directory of the index, or null if the index is not found.
     */
    @Nullable
    public static File findExistingIndex(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        if (context.getExternalCacheDir() != null) {
            final File predictedFileOnExternalStorage
                    = new File(context.getExternalCacheDir().getAbsolutePath() + File.separator + INDEX_BASE_DIRECTORY);
            if (predictedFileOnExternalStorage.exists()) {
                return predictedFileOnExternalStorage;
            }
        }

        final File predictedFileOnInternalStorage
                = new File(context.getCacheDir().getAbsolutePath() + File.separator + INDEX_BASE_DIRECTORY);
        if (predictedFileOnInternalStorage.exists()) {
            return predictedFileOnInternalStorage;
        }

        return null;
    }

    /**
     * Search for a suitable location with enough free space to contain the full index for 200k cities.
     * @param context
     * @return a File representing the location where the index can be built. This is located either on
     *         the internal or external storage. This method return null if no suitable location was found.
     */
    @Nullable
    public static File findIndexStorageLocation(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        // Priority is given to external storage for obvious reasons.
        final boolean isExternalStorageMounted = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());

        if (isExternalStorageMounted) {
            // It is writable, does the volume have enough free space?
            final StatFs stat = new StatFs(context.getExternalCacheDir().getPath());
            long freeSpaceOnExternalStorage = (long)stat.getBlockSize() * (long)stat.getBlockCount();
            if (freeSpaceOnExternalStorage > BYTES_NEEDED_FOR_INDEX) {
                return new File(context.getExternalCacheDir().getAbsolutePath() + File.separator + INDEX_BASE_DIRECTORY);
            }
        } else {
            final StatFs stat = new StatFs(context.getCacheDir().getPath());
            long freeSpaceOnInternalStorage = (long)stat.getBlockSize() * (long)stat.getBlockCount();
            if (freeSpaceOnInternalStorage > BYTES_NEEDED_FOR_INDEX) {
                return new File(context.getCacheDir().getAbsolutePath() + File.separator + INDEX_BASE_DIRECTORY);
            }
        }
        return null;
    }
}
