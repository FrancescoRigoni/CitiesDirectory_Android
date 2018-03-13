package com.rigoni.citiesindex.model;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.PagedList;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.rigoni.citiesindex.data.City;
import com.rigoni.citiesindex.index.IndexTree;
import com.rigoni.citiesindex.index.IndexTreeEntry;
import com.rigoni.citiesindex.index.IndexTreeStorage;
import com.rigoni.citiesindex.index.IndexTreeStorageFs;
import com.rigoni.citiesindex.list.CitiesDataSource;
import com.rigoni.citiesindex.task.CitiesIndexBuilderTask;
import com.rigoni.citiesindex.utils.IndexStorageUtils;
import com.rigoni.citiesindex.utils.NameNormalizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class IndexBuilderViewModel extends AndroidViewModel {
    @NonNull
    private CitiesIndexBuilderTask.IndexBuilderTaskListener mIndexBuilderListener
            = new CitiesIndexBuilderTask.IndexBuilderTaskListener() {
        @Override
        public void onIndexCreated() {
            mIndexReady.setValue(true);
            mIndexInProgress.setValue(false);
            mTask = null;
        }

        @Override
        public void onIndexProgress(int count, final String cityName) {
            mCurrentCityNumber.setValue(count);
            mCurrentCityName.setValue(cityName);
        }

        @Override
        public void onIndexError(final String message) {
            mIndexCreationError.setValue(message);
            mIndexInProgress.setValue(false);
            mTask = null;
        }

        @Override
        public void onIndexBuildCancelled() {
            mIndexCreationError.setValue("Cancelled");
            mIndexInProgress.setValue(false);
            mTask = null;
        }
    };

    private final MutableLiveData<Integer> mCurrentCityNumber = new MutableLiveData<>();
    private final MutableLiveData<String> mCurrentCityName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIndexReady = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIndexInProgress = new MutableLiveData<>();
    private final MutableLiveData<String> mIndexCreationError = new MutableLiveData<>();

    private CitiesIndexBuilderTask mTask;

    public IndexBuilderViewModel(@NonNull final Application application) {
        super(application);
        mIndexReady.setValue(IndexStorageUtils.findExistingIndex(this.getApplication()) != null);
        mIndexInProgress.setValue(false);
    }

    public LiveData<Integer> getCurrentCityNumber() {
        return mCurrentCityNumber;
    }
    public LiveData<String> getCurrentCityName() {
        return mCurrentCityName;
    }
    public LiveData<Boolean> getIndexReady() {
        return mIndexReady;
    }
    public LiveData<Boolean> getIndexInProgress() {
        return mIndexInProgress;
    }
    public LiveData<String> getIndexCreationError() {
        return mIndexCreationError;
    }

    public void createIndex(@NonNull final String citiesFileName) {
        Preconditions.checkNotNull(citiesFileName);

        if (mTask != null && !mTask.isCancelled()) {
            throw new IllegalStateException("Index creation in progress");
        }

        if (Boolean.TRUE.equals(mIndexReady.getValue())) {
            throw new IllegalStateException("An index is already present");
        }

        try {
            final File indexTreeDir = IndexStorageUtils.findIndexStorageLocation(getApplication());
            if (indexTreeDir != null) {
                final String indexPath = indexTreeDir.getAbsolutePath();
                final IndexTreeStorage storage = new IndexTreeStorageFs<City>(City.class, indexPath, true);
                final IndexTree indexTree = new IndexTree(storage);
                final InputStream inputStream = getApplication().getAssets().open(citiesFileName);
                mIndexInProgress.setValue(true);
                mTask = new CitiesIndexBuilderTask(inputStream, indexTree, mIndexBuilderListener);
                mTask.execute();
            } else {
                mIndexCreationError.setValue("Not enough space to build the IndexTree");
            }
        } catch (final IOException e) {
            mIndexCreationError.setValue("IOException while opening cities file: " + e.getMessage());
        }
    }

    public void cancelIndexCreation() {
        if (mTask == null || mTask.isCancelled()) {
            throw new IllegalStateException("Index creation not in progress");
        }
        mTask.cancel(false);
    }

    @Override
    protected void onCleared() {
        if (mTask != null && !mTask.isCancelled()) {
            mTask.cancel(false);
        }
    }
}
