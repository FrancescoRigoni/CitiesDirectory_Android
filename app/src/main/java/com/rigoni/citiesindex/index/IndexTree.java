package com.rigoni.citiesindex.index;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


public class IndexTree {
    /**
     * A convenience constant to be used when all the items are to be fetched
     * via filterForward(), not the best approach but we know that the limit
     * is 200k.
     */
    private static final int COUNT_ALL = 400000;

    /**
     * Maximum depth of the index tree.
     */
    private static final int MAX_DEPTH = 3;

    /**
     * The storage backing the tree.
     */
    @NonNull
    private final IndexTreeStorage mStorage;

    /**
     * Constructor.
     * @param storage a mandatory {@link IndexTreeStorage} implementation.
     */
    public IndexTree(@NonNull final IndexTreeStorage storage) {
        Preconditions.checkNotNull(storage);
        mStorage = storage;
    }

    /**
     * Filters forward over the tree for the list of entries matching the filter string.
     * @param filter the filter string.
     * @param lastPreviouslyReadNormalizedName the normalizedName of the last entry read with this method on a previous call.
     * @param count the amount of entries to fetch.
     * @return a {@link List} of {@link IndexTreeEntry} containing count or less items,
     * an empty list if no entries are found matching the filterForward.
     */
    public List<IndexTreeEntry> filterForward(@NonNull final String filter,
                                              @NonNull String lastPreviouslyReadNormalizedName,
                                              int count) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(lastPreviouslyReadNormalizedName);

        // Start walking from where we left off, if we can figure it out.
        final String subPath = createRelativePathFromFilter(filter);

        final List<String> subPathsToWalk = mStorage.getSubPathsContainingEntriesFrom(subPath);

        boolean finished = false;
        boolean isReading = false;
        final List<IndexTreeEntry> entries = new ArrayList<>();
        for (String subSubPath : subPathsToWalk) {
            final List<IndexTreeEntry> subEntries = new ArrayList<>();
            subEntries.addAll(mStorage.getEntriesListAtSubPath(subSubPath, 0, COUNT_ALL));
            final ListIterator<IndexTreeEntry> iterator = subEntries.listIterator();

            if (!isReading) {
                // Iterate until we find the starting point.
                while (iterator.hasNext()) {
                    final IndexTreeEntry entry = iterator.next();
                    if (!TextUtils.isEmpty(lastPreviouslyReadNormalizedName)) {
                        // We have a previously read name, so we need to start from the one after it.
                        isReading = entry.getIndexTreeKey().equals(lastPreviouslyReadNormalizedName);
                    } else {
                        // We don't have a previous name, but filter matches.
                        isReading = entry.getIndexTreeKey().startsWith(filter);
                        if (isReading) iterator.previous();
                    }

                    if (isReading) {
                        // Found the starting point.
                        break;
                    }
                }
            }

            if (isReading) {
                while (iterator.hasNext()) {
                    final IndexTreeEntry entry = iterator.next();
                    if (entry.getIndexTreeKey().startsWith(filter)) {
                        entries.add(entry);
                        finished = entries.size() >= count;
                        if (finished) break;
                    } else {
                        // Matching entries finished.
                        finished = true;
                        break;
                    }
                }
                if (finished) break;
            }
        }
        return entries;
    }

    /**
     * Filters backwards over the tree for the list of entries matching the filter string.
     * @param filter the filter string.
     * @param start the start point of the returned list, will be zero for initial filtering and will change when displaying paginated data.
     * @param count the amount of entries to fetch.
     * @return a {@link List} of {@link IndexTreeEntry} containing count or less items,
     * an empty list if no entries are found matching the filterForward.
     */
    public List<IndexTreeEntry> filterBackwards(@NonNull final String filter, int start, int count) {
        Preconditions.checkNotNull(filter);
        // Not implemented, algorithm is similar to filterForwards.
        return new ArrayList<>();
    }

    /**
     * Adds an entry into the index tree.
     * @param entry the entry to be added.
     */
    public void addEntry(@NonNull final IndexTreeEntry entry) {
        Preconditions.checkNotNull(entry);
        mStorage.addEntryAtSubPath(createRelativePathFromFilter(entry.getIndexTreeKey()), entry);
    }

    public static String createRelativePathFromFilter(@NonNull final String filter) {
        Preconditions.checkNotNull(filter);
        StringBuilder relativePath = new StringBuilder();
        int pathComponentsCount = Math.min(MAX_DEPTH, filter.length());
        for (int i = 0; i < pathComponentsCount; i++) {
            relativePath.append(filter.charAt(i)).append(File.separator);
        }
        return relativePath.toString();
    }

    public void initiateBulkInsert() {
        mStorage.initiateBulkInsert();
    }

    public void finalizeBulkInsert() {
        mStorage.finalizeBulkInsert();
    }

    public void delete() {
        mStorage.deleteIndex();
    }
}
