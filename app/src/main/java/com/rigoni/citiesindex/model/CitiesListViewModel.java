package com.rigoni.citiesindex.model;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.ViewModel;
import android.arch.paging.PagedList;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.rigoni.citiesindex.data.City;
import com.rigoni.citiesindex.index.IndexTreeStorageFs;
import com.rigoni.citiesindex.index.IndexTree;
import com.rigoni.citiesindex.index.IndexTreeEntry;
import com.rigoni.citiesindex.index.IndexTreeStorage;
import com.rigoni.citiesindex.list.CitiesDataSource;
import com.rigoni.citiesindex.utils.NameNormalizer;
import com.rigoni.citiesindex.utils.IndexStorageUtils;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class CitiesListViewModel extends AndroidViewModel {
    private IndexTree mIndexTree;
    private CitiesDataSource mDataSource;
    private PagedList<IndexTreeEntry> mPagedList;

    private final Executor mMainThreadExecutor = new Executor() {
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        @Override
        public void execute(Runnable command) {
            mHandler.post(command);
        }
    };

    private ThreadPoolExecutor mBackgroundThreadExecutor = new ThreadPoolExecutor(
            3,
            5,
            1,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    public CitiesListViewModel(@NonNull Application application) {
        super(application);

        // This ViewModel is backed by the IndexTree
        if (mIndexTree == null) {
            final File indexDirectory = IndexStorageUtils.findExistingIndex(application);
            if (indexDirectory == null) {
                // This is really bad and unexpected, better throw so we can notice this.
                throw new RuntimeException("Index directory does not exist!");
            }
            final IndexTreeStorage storage = new IndexTreeStorageFs<City>(City.class, indexDirectory.getAbsolutePath(), false);
            mIndexTree = new IndexTree(storage);
        }

        if (mDataSource == null) {
            mDataSource = new CitiesDataSource(mIndexTree);
        }
    }

    /**
     * Creates a new paged list using the provided filter.
     * @param filter the string to be used as filter.
     */
    public void setFilter(@NonNull final String filter) {
        if (mDataSource != null && filter.equals(mDataSource.getFilter())) {
            // Skip
            return;
        }

        mDataSource.setFilter(new NameNormalizer().normalize(filter));
        mPagedList = new PagedList.Builder<>(mDataSource, CitiesDataSource.LIST_PAGE_SIZE)
                .setMainThreadExecutor(mMainThreadExecutor)
                .setBackgroundThreadExecutor(mBackgroundThreadExecutor)
                .build();
    }

    public PagedList<IndexTreeEntry> getList() {
        return mPagedList;
    }
}
