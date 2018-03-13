package com.rigoni.citiesindex.index;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.rigoni.citiesindex.utils.FsUtils;
import com.rigoni.citiesindex.utils.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * An alphabetically sorted, file system based storage implementation for the IndexTreeStorage.
 */
public class IndexTreeStorageFs<T extends IndexTreeEntry> implements IndexTreeStorage<T> {
    private static final String TAG = IndexTreeStorageFs.class.getSimpleName();

    /**
     * Entries are split inside bins residing at specific paths inside the index tree.
     */
    private static final String ENTRIES_BIN_FILE_NAME = "entries.json";

    /**
     * Each entries bin file has a count file associated, this is useful to quickly lookup
     * how many entries are in the bin without looping over it.
     */
    private static final String ENTRIES_COUNT_FILE_NAME = "count.json";

    /**
     * Cache specific parameters.
     */
    private static final int CACHED_ENTRIES_QUEUE_MAX_LIST_ITEM_SIZE = 1000;
    private static final int CACHED_ENTRIES_QUEUE_MAX_ITEMS = 300;
    private static final boolean DEBUG = false;

    /**
     * The base index path. The directory tree will start from here.
     */
    private final String mBasePath;

    /**
     * The cache is used to speed up the index creation by keeping entries in a Map
     * inside mEntriesListsPerSubPathCache.
     */
    private boolean mUseCache;
    private Map<String, Set<T>> mEntriesListsPerSubPathCache = new HashMap<>();

    /**
     * This is needed when serializing and de serializing entries, damn type erasure.
     */
    private Class mEntryClass;

    /**
     * Constructor.
     * @param entryClass the class representing the entry, must be the same as T
     * @param basePath the base path for the storage, absolute.
     * @param wipeExistingTree if the index already exists, delete it.
     */
    public IndexTreeStorageFs(@NonNull Class entryClass, @NonNull final String basePath, final boolean wipeExistingTree) {
        Preconditions.checkNotNull(basePath);
        Preconditions.checkNotNull(entryClass);
        mBasePath = basePath;
        mEntryClass = entryClass;
        final File basePathDirectory = new File(mBasePath);
        if (!basePathDirectory.exists()) {
            Log.i(TAG, "Creating directory " + basePath);
            if (!basePathDirectory.mkdirs()) {
                // This is a very bad condition. And there is no easy way to recover.
                throw new RuntimeException("Index tree directory cannot not be created");
            }
        } else if (wipeExistingTree) {
            deleteIndex();
        }
    }

    @Override
    public void deleteIndex() {
        FsUtils.deleteDirectory(new File(mBasePath));
    }

    @Override
    public void initiateBulkInsert() {
        mUseCache = true;
        mEntriesListsPerSubPathCache.clear();
    }

    @Override
    public void finalizeBulkInsert() {
        storeCachedItems();
        mUseCache = false;
    }

    @Override
    public Set<T> getEntriesListAtSubPath(@NonNull final String subPath, int start, int count) {
        Preconditions.checkNotNull(subPath);
        return getEntriesAtPath(subPath, start, count);
    }

    @Override
    public void addEntryAtSubPath(@NonNull String subPath, @NonNull T entry) {
        Preconditions.checkNotNull(subPath);
        Preconditions.checkNotNull(entry);

        final int entriesCountAtPath = getEntriesCountNotRecursive(subPath);
        final Set<T> entries = getEntriesAtPath(subPath, 0, entriesCountAtPath);
        if (DEBUG) {
            Log.i(TAG, "Adding entry: " + entry.getIndexTreeKey());
            Log.i(TAG, "  At path: " + getEntriesFileAbsolutePath(subPath));
            Log.i(TAG, "  Which already contains: " + entriesCountAtPath + " entries");
        }
        entries.add(entry);
        storeEntriesAtPath(entries, subPath);
    }

    @Override
    public List<String> getSubPathsContainingEntriesFrom(@Nullable String subPath) {
        Preconditions.checkNotNull(subPath);

        final List<String> subPathsContainingEntries = new ArrayList<>();
        final File entriesCountFile = new File(getEntriesFileAbsolutePath(subPath));
        if (getEntriesCountNotRecursive(subPath) > 0) {
            subPathsContainingEntries.add(subPath);
        }

        final File currentDir = entriesCountFile.getParentFile();
        final File[] files = currentDir.listFiles();
        if (files != null && files.length > 0) {
            for (final File f : files) {
                if (f.isDirectory()) {
                    subPathsContainingEntries.addAll(getSubPathsContainingEntriesFrom(subPath + File.separator + f.getName()));
                }
            }
        }
        return subPathsContainingEntries;
    }

    @Override
    public int getEntriesCountAtSubPath(@NonNull String subPath) {
        Preconditions.checkNotNull(subPath);
        return getEntriesCountNotRecursive(subPath);
    }

    /**
     * Non-recursive means that this method only returns the entries count at the
     * specified path, no looping through subdirectories is done.
     */
    private int getEntriesCountNotRecursive(@NonNull final String subPath) {
        Preconditions.checkNotNull(subPath);
        // Try cache first
        if (mUseCache && mEntriesListsPerSubPathCache.containsKey(subPath)) {
            return mEntriesListsPerSubPathCache.get(subPath).size();
        }

        final File entriesCountFile = new File(getEntriesCountFileAbsolutePath(subPath));
        if (!entriesCountFile.exists()) {
            // No file, no entries.
            return 0;
        }

        // Everything failed so far, read the count file.
        return readIntFromJsonFile(entriesCountFile.getAbsolutePath());
    }

    private Set<T> getEntriesAtPath(@NonNull final String subPath, int start, int count) {
        Preconditions.checkNotNull(subPath);

        // Check the cache first.
        if (mUseCache && mEntriesListsPerSubPathCache.containsKey(subPath)) {
            final Set<T> entries = mEntriesListsPerSubPathCache.get(subPath);
            final Set<T> toReturn = new TreeSet<>();
            final Iterator<T> iterator = entries.iterator();
            int position = 0;
            while (iterator.hasNext()) {
                if (position >= start && position < start + count) toReturn.add(iterator.next());
                else iterator.next();
                position++;
            }
            return toReturn;
        }

        // No cache hit, too bad..
        final File entriesBinFile = new File(getEntriesFileAbsolutePath(subPath));

        Set<T> entries = new TreeSet<>();
        // If the file does not exist we abstract this problem by creating an empty file
        // and returning an empty queue.
        if (!entriesBinFile.exists()) {
            try {
                if (!entriesBinFile.getParentFile().mkdirs()) throw new RuntimeException("Unable to create directory for bin file");
                if (!entriesBinFile.createNewFile()) throw new RuntimeException("Unable to create bin file");
            } catch (final IOException e) {
                throw new RuntimeException("Cannot create entries bin file at " + entriesBinFile.getAbsolutePath() + " (" + e.getMessage()+ ")");
            }
        } else {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(entriesBinFile);
                final JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
                final Gson gson = new GsonBuilder().create();
                if (reader.peek().equals(JsonToken.BEGIN_ARRAY)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        if (start == 0 && count > 0) {
                            // Read data
                            final T entry = gson.fromJson(reader, mEntryClass);
                            entries.add(entry);
                            count--;
                        } else {
                            // When start reaches zero we start reading values.
                            start--;
                            reader.skipValue();
                        }
                    }
                    reader.close();
                }
            } catch (final Exception e) {
                throw new RuntimeException("Cannot read entries file at " + entriesBinFile.getAbsolutePath() + " " + e.getMessage());
            } finally {
                StreamUtils.closeInputStreamNoThrow(inputStream);
            }
        }
        return entries;
    }

    private void storeEntriesAtPath(@NonNull final Set<T> entries, @NonNull final String subPath) {
        // Cache the entries if possible.
        boolean needsStorage = true;
        if (mUseCache) {
            if (entries.size() < CACHED_ENTRIES_QUEUE_MAX_LIST_ITEM_SIZE &&
                mEntriesListsPerSubPathCache.size() < CACHED_ENTRIES_QUEUE_MAX_ITEMS) {

                mEntriesListsPerSubPathCache.put(subPath, entries);
                needsStorage = false;
                if (DEBUG) {
                    Log.d(TAG, "Cached entries list for subPath " + subPath + " (" + entries.size() + " entries)");
                }
            } else {
                // Remove old cached entry.
                if (mEntriesListsPerSubPathCache.containsKey(subPath)) {
                    mEntriesListsPerSubPathCache.remove(subPath);
                    // Mark it for persistent storage.
                    if (DEBUG) {
                        Log.d(TAG, "Removed cached entries list for subPath " + subPath + " (" + entries.size() + " entries)");
                    }
                }
            }
        }
        if (needsStorage) storeOnFileSystem(subPath, entries);
        if (mUseCache) performCacheMaintenance();
    }

    private void performCacheMaintenance() {
        // The cache works a lot better if the main entries json is sorted alphabetically by name.
        // That's why it's sorted.
        if (mEntriesListsPerSubPathCache.size() >= CACHED_ENTRIES_QUEUE_MAX_ITEMS) {
            storeCachedItems();
            Log.i(TAG, "Entries list cache was full, flushed");
        }
    }

    /**
     * This method is invoked when finalizing the bulk insert or when, during a bulk insert, the in memory
     * cache becomes too big.
     */
    private void storeCachedItems() {
        // Store the lists and cleanup the cache.
        final List<String> toRemove = new ArrayList<>();
        // Let's store everything and clear the cache.
        for (final String sp : mEntriesListsPerSubPathCache.keySet()) {
            storeOnFileSystem(sp, mEntriesListsPerSubPathCache.get(sp));
            toRemove.add(sp);
        }
        for (final String sp : toRemove) {
            mEntriesListsPerSubPathCache.remove(sp);
        }
    }

    private void storeOnFileSystem(@NonNull final String subPath, @NonNull final Set<T> entries) {
        Preconditions.checkNotNull(subPath);
        Preconditions.checkNotNull(entries);

        final String entriesBinFilePath = getEntriesFileAbsolutePath(subPath);
        final String entriesCountFilePath = getEntriesCountFileAbsolutePath(subPath);

        // Save the entries count
        saveIntToJsonFile(entriesCountFilePath, entries.size());

        JsonWriter writer = null;
        try {
            writer = new JsonWriter(new FileWriter(new File(entriesBinFilePath), false));
            final Gson gson = new Gson();

            writer.beginArray();
            for (final T entry : entries) {
                gson.toJson(entry, mEntryClass, writer);
            }
            writer.endArray();

        } catch (Exception e) {
            throw new RuntimeException("Cannot store entries file at " + entriesBinFilePath + " " + e.getMessage());
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (final Exception e) {}
            }
        }
    }

    /**
     * @param path
     * @return the path of the entries bin file at the specified path
     */
    @NonNull
    private String getEntriesFileAbsolutePath(@NonNull final String path) {
        return mBasePath + File.separator + path + File.separator + ENTRIES_BIN_FILE_NAME;
    }

    /**
     * @param path
     * @return the path of the entries count file at the specified path
     */
    @NonNull
    private String getEntriesCountFileAbsolutePath(@NonNull final String path) {
        return mBasePath + File.separator + path + File.separator + ENTRIES_COUNT_FILE_NAME;
    }

    /**
     * Read a single int value from a json file.
     * @param filePath
     */
    private int readIntFromJsonFile(@NonNull final String filePath) {
        Preconditions.checkNotNull(filePath);

        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(filePath));
            reader.beginObject();
            reader.nextName();
            int value = reader.nextInt();
            reader.endObject();
            return value;
        } catch (final Exception e) {
            throw new RuntimeException("Cannot read entries count file " + filePath + " " + e.getMessage());
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (final Exception e) {}
            }
        }
    }

    /**
     * Saves a single int value to a json file.
     * @param filePath
     */
    private boolean saveIntToJsonFile(@NonNull final String filePath, int value) {
        Preconditions.checkNotNull(filePath);

        JsonWriter writer = null;
        try {
            writer = new JsonWriter(new FileWriter(filePath, false));
            writer.beginObject();
            writer.name("count").value(value);
            writer.endObject();
            return true;
        } catch (final Exception e) {
            throw new RuntimeException("Cannot write entries count file " + filePath + " " + e.getMessage());
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (final Exception e) {}
            }
        }
    }
}
