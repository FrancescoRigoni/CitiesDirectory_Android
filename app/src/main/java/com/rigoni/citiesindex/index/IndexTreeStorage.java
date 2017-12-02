package com.rigoni.citiesindex.index;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Set;

public interface IndexTreeStorage<T extends IndexTreeEntry> {
    /**
     * Fetches the list of entries at the specified subpath.
     * @param subPath the subpath to fetch the list from.
     * @param start the position where to start reading cities.
     * @param count how many cities to read, the returned list might contain less items.
     * @return a sorted {@link Set} of {@link IndexTreeEntry},
     * an empty list if no entries are found at subPath.
     */
    Set<T> getEntriesListAtSubPath(@NonNull final String subPath, int start, int count);

    /**
     * Adds an {@link IndexTreeEntry} at the specified subPath.
     * @param subPath the path where the entry has to be added.
     * @param entry the entry to add.
     */
    void addEntryAtSubPath(@NonNull final String subPath, @NonNull final T entry);

    /**
     * Returns a {@link List} of subpaths that contain at least one entry, starting from the
     * specified subPath.
     * @param subPath the subPath where to start scanning for entries.
     * @returns a {@link List} of subPaths that can be used to fetch entries, an empty {@link List}
     * if none are found.
     */
    List<String> getSubPathsContainingEntriesFrom(@Nullable final String subPath);

    /**
     * Returns the number of items contained in the specified subPath.
     * @param subPath the subPath to scan.
     * @return the number of entries found.
     */
    int getEntriesCountAtSubPath(final String subPath);

    /**
     * Delete the index structure.
     */
    void deleteIndex();

    /**
     * Can be used to inform the storage that a lot of data is about to be inserted.
     * Implementors can use this method to initialize optimization.
     */
    void initiateBulkInsert();

    /**
     * Can be used to inform the storage that a lot of data is about to be inserted.
     * Implementors can use this method to finalize optimization.
     */
    void finalizeBulkInsert();
}
