package com.rigoni.citiesindex.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.rigoni.citiesindex.task.CitiesIndexBuilderTask;
import com.rigoni.citiesindex.R;
import com.rigoni.citiesindex.data.City;
import com.rigoni.citiesindex.index.IndexTree;
import com.rigoni.citiesindex.index.IndexTreeStorage;
import com.rigoni.citiesindex.index.IndexTreeStorageFs;
import com.rigoni.citiesindex.utils.IndexStorageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Fragment showing a progress indicator while the main index is created.
 *
 * TODO: This fragment uses an AsyncTask directly, a nice improvement would be to move
 * the AsyncTask inside a ViewModel and use a LiveData instead of a listener.
 */
public class IndexBuilderFragment extends Fragment implements View.OnClickListener {

    public static final String ARGUMENT_CITIES_FILE_NAME = "CITIES_FILE_NAME";
    public static final String ARGUMENT_CITIES_COUNT = "CITIES_COUNT";

    private static final String KEY_LAST_PROGRESS_PERCENT = "lastProgressPercent";
    private static final String KEY_LAST_CITY_NAME = "lastCityName";
    private static final String KEY_ABORT_COUNT = "abortProgressCount";

    /**
     * Safety: press abort this amount of times to actually abort.
     */
    private static final int ABORT_PRESS_COUNT = 3;

    public interface IndexBuilderFragmentListener {
        void onIndexBuilt();
        void onIndexBuildError(final String message);
    }

    @NonNull
    private CitiesIndexBuilderTask.IndexBuilderTaskListener mIndexBuilderListener = new CitiesIndexBuilderTask.IndexBuilderTaskListener() {
        @Override
        public void onIndexCreated() {
            mListener.onIndexBuilt();
        }

        @Override
        public void onIndexProgress(int count, final String cityName) {
            mLastProgressPercent = ((float) count / (float) mCitiesCount) * 100;
            mTvDetailedProgress.setText(String.format(Locale.US, "%.2f%%", mLastProgressPercent));
            mProgressBar.setProgress((int) mLastProgressPercent);
            mTvCurrentCity.setText(cityName);
        }

        @Override
        public void onIndexError(final String message) {
            Toast.makeText(getActivity(), "Error building index: " + message, Toast.LENGTH_LONG).show();
            mListener.onIndexBuildError(message);
        }

        @Override
        public void onIndexBuildCancelled() {
            mListener.onIndexBuildError("Cancelled");
        }
    };

    private TextView mTvCurrentCity;
    private TextView mTvDetailedProgress;
    private ProgressBar mProgressBar;
    private String mLastCity;
    private int mAbortPressCount;
    private int mCitiesCount;
    private float mLastProgressPercent;

    private IndexBuilderFragmentListener mListener;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!(getActivity() instanceof IndexBuilderFragmentListener)) {
            // Programming error, let's make sure we notice.
            throw new RuntimeException("Activity showing this fragment must implement IndexBuilderFragmentListener");
        }
        mListener = (IndexBuilderFragmentListener) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final LinearLayout view = (LinearLayout) inflater.inflate(R.layout.index_builder_fragment, container, false);
        view.findViewById(R.id.btnAbort).setOnClickListener(this);

        mTvCurrentCity = view.findViewById(R.id.tvCurrentCity);
        mTvDetailedProgress = view.findViewById(R.id.tvDetailedProgress);
        mProgressBar = view.findViewById(R.id.progressBar);

        if (savedInstanceState != null) {
            mLastProgressPercent = savedInstanceState.getFloat(KEY_LAST_PROGRESS_PERCENT, 0);
            mLastCity = savedInstanceState.getString(KEY_LAST_CITY_NAME, "");
            mAbortPressCount = savedInstanceState.getInt(KEY_ABORT_COUNT, ABORT_PRESS_COUNT);

            mProgressBar.setProgress((int) mLastProgressPercent);
            mTvDetailedProgress.setText(String.format(Locale.US,"%.2f%%", mLastProgressPercent));
            mTvCurrentCity.setText(mLastCity);
        } else {
            mAbortPressCount = ABORT_PRESS_COUNT;
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        String citiesFileName;
        if (getArguments() != null) {
            if (getArguments().containsKey(ARGUMENT_CITIES_FILE_NAME) &&
                    !TextUtils.isEmpty(getArguments().getString(ARGUMENT_CITIES_FILE_NAME))) {
                citiesFileName = getArguments().getString(ARGUMENT_CITIES_FILE_NAME);
            } else {
                throw new IllegalArgumentException("Please provide the cities file name");
            }
            if (getArguments().containsKey(ARGUMENT_CITIES_COUNT)) {
                mCitiesCount = getArguments().getInt(ARGUMENT_CITIES_COUNT);
            } else {
                throw new IllegalArgumentException("Please provide the cities count");
            }
        } else {
            throw new IllegalArgumentException("Please provide the cities file name and count");
        }

        if (CitiesIndexBuilderTask.isTaskRunning()) {
            // Task is still running
            CitiesIndexBuilderTask.updateListener(mIndexBuilderListener);
        } else if (CitiesIndexBuilderTask.isTaskCompleted()) {
            // Task completed while we were paused
            mListener.onIndexBuilt();
        } else {
            try {
                final File indexTreeDir = IndexStorageUtils.findIndexStorageLocation(getActivity());
                if (indexTreeDir != null) {
                    final String indexPath = indexTreeDir.getAbsolutePath();
                    final IndexTreeStorage storage = new IndexTreeStorageFs<City>(City.class, indexPath, true);
                    final IndexTree indexTree = new IndexTree(storage);
                    final InputStream inputStream = getActivity().getAssets().open(citiesFileName);
                    CitiesIndexBuilderTask.startTask(inputStream, indexTree, mIndexBuilderListener);
                } else {
                    mListener.onIndexBuildError("Not enough space to build the IndexTree");
                }
            } catch (final IOException e) {
                mListener.onIndexBuildError("IOException while opening cities file: " + e.getMessage());
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putFloat(KEY_LAST_PROGRESS_PERCENT, mLastProgressPercent);
        outState.putString(KEY_LAST_CITY_NAME, mLastCity);
        outState.putInt(KEY_ABORT_COUNT, mAbortPressCount);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (CitiesIndexBuilderTask.isTaskRunning()) {
            CitiesIndexBuilderTask.updateListener(null);
        }
    }

    @Override
    public void onClick(final View view) {
        if (view.getId() == R.id.btnAbort) {
            mAbortPressCount--;
            if (mAbortPressCount > 0) {
                Toast.makeText(getActivity(), "Press " + mAbortPressCount + " more times to abort.", Toast.LENGTH_LONG).show();
            } else {
                CitiesIndexBuilderTask.stopTask();
                mListener.onIndexBuildError("Canceled");
            }
        }
    }
}
