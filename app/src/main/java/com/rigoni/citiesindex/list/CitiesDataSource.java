package com.rigoni.citiesindex.list;

import android.arch.paging.ItemKeyedDataSource;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.rigoni.citiesindex.index.IndexTree;
import com.rigoni.citiesindex.index.IndexTreeEntry;

import java.util.ArrayList;
import java.util.List;

public class CitiesDataSource extends ItemKeyedDataSource<String, IndexTreeEntry> {
    public static final int LIST_PAGE_SIZE = 200;

    private final IndexTree mIndexTree;
    private String mFilter;

    /**
     * Constructor
     * @param indexTree the {@link IndexTree} where the cities are stored.
     */
    public CitiesDataSource(@NonNull final IndexTree indexTree) {
        Preconditions.checkNotNull(indexTree);
        mIndexTree = indexTree;
    }

    /**
     * Sets the filterForward string to restrict the loading.
     */
    public void setFilter(@NonNull final String filter) {
        Preconditions.checkNotNull(filter);
        mFilter = filter;
    }

    /**
     * @return the current filter.
     */
    @Nullable
    public String getFilter() {
        return mFilter;
    }

    @NonNull
    @Override
    public String getKey(@NonNull IndexTreeEntry item) {
        Preconditions.checkNotNull(item);
        return item.getIndexTreeKey();
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<String> params, @NonNull LoadInitialCallback<IndexTreeEntry> callback) {
        Preconditions.checkNotNull(params);
        Preconditions.checkNotNull(callback);
        final List<IndexTreeEntry> entries = mIndexTree.filterForward(mFilter, "", LIST_PAGE_SIZE);
        callback.onResult(entries);
    }

    @Override
    public void loadAfter(@NonNull LoadParams<String> params, @NonNull LoadCallback<IndexTreeEntry> callback) {
        Preconditions.checkNotNull(params);
        Preconditions.checkNotNull(callback);
        final String previousEndKey = params.key;
        final List<IndexTreeEntry> entries = mIndexTree.filterForward(mFilter, previousEndKey, LIST_PAGE_SIZE);

        // End of DataSet, we do not know it in advance so we have to use this trick.
        if (entries != null && !entries.isEmpty()
                && previousEndKey != null
                && previousEndKey.equals(entries.get(entries.size() - 1).getIndexTreeKey())) {
            entries.clear();
        }
        callback.onResult(entries);
    }

    @Override
    public void loadBefore(@NonNull LoadParams<String> params, @NonNull LoadCallback<IndexTreeEntry> callback) {
        Preconditions.checkNotNull(params);
        Preconditions.checkNotNull(callback);
        // Not implemented.
        callback.onResult(new ArrayList<IndexTreeEntry>());
    }
}
