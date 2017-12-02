package com.rigoni.citiesindex.task;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.rigoni.citiesindex.model.CitiesListViewModel;

/**
 * This class is responsible for updating the filter string in the CitiesListViewModel.
 * An AsyncTask is required as setting the filter is a blocking operation.
 */
public class UpdateListFilterTask extends AsyncTask<Void, Void, Void> {
    public interface UpdateListFilterTaskListener {
        void onFilterInProgress();
        void onFilterComplete();
    }
    private CitiesListViewModel mViewModel;
    private String mFilter;
    private UpdateListFilterTaskListener mListener;

    public UpdateListFilterTask(@NonNull final UpdateListFilterTaskListener listener,
                                @NonNull final String filter,
                                @NonNull final CitiesListViewModel viewModel) {

        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(viewModel);
        mListener = listener;
        mFilter = filter;
        mViewModel = viewModel;
    }

    @Override
    protected void onPreExecute() {
        mListener.onFilterInProgress();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        mViewModel.setFilter(mFilter);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        mListener.onFilterComplete();
    }
}
