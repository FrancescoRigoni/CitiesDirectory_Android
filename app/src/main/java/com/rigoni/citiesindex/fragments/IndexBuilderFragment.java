package com.rigoni.citiesindex.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
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

import com.rigoni.citiesindex.R;
import com.rigoni.citiesindex.model.IndexBuilderViewModel;

import java.util.Locale;

/**
 * Fragment showing a progress indicator and details while the main index is created.
 */
public class IndexBuilderFragment extends Fragment implements View.OnClickListener {
    public interface IndexBuilderFragmentListener {
        void onIndexBuilt();
        void onIndexBuildError(final String message);
    }

    public static final String ARGUMENT_CITIES_FILE_NAME = "CITIES_FILE_NAME";
    public static final String ARGUMENT_CITIES_COUNT = "CITIES_COUNT";

    /**
     * Safety: press abort this amount of times to actually abort.
     */
    private static final int ABORT_PRESS_COUNT = 3;

    private TextView mTvCurrentCity;
    private TextView mTvDetailedProgress;
    private ProgressBar mProgressBar;

    private int mAbortPressCount;

    // Fragment arguments
    private int mCitiesCount;
    private String mCitiesFileName;

    private IndexBuilderFragmentListener mListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(ARGUMENT_CITIES_FILE_NAME) &&
                !TextUtils.isEmpty(getArguments().getString(ARGUMENT_CITIES_FILE_NAME))) {
                mCitiesFileName = getArguments().getString(ARGUMENT_CITIES_FILE_NAME);
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
    }

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
        mAbortPressCount = ABORT_PRESS_COUNT;
        return view;
    }

    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onStart() {
        super.onStart();
        IndexBuilderViewModel model = ViewModelProviders.of(this).get(IndexBuilderViewModel.class);
        model.getIndexReady().observe(this, ready -> {
            if (ready) {
                mListener.onIndexBuilt();
            }
        });
        model.getIndexInProgress().observe(this, inProgress -> {
            final PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            if (inProgress && mWakeLock == null) {
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
                mWakeLock.acquire(10*60*1000);
            } else {
                mWakeLock.release();
                mWakeLock = null;
            }
        });
        model.getIndexCreationError().observe(this, message -> {
            Toast.makeText(getActivity(), "Error building index: " + message, Toast.LENGTH_LONG).show();
            mListener.onIndexBuildError(message);
        });
        model.getCurrentCityName().observe(this, name -> {
            mTvCurrentCity.setText(name);
        });
        model.getCurrentCityNumber().observe(this, number -> {
            if (number != null) {
                showProgressForNumber(number);
            }
        });

        if (model.getCurrentCityName().getValue() != null) {
            mTvCurrentCity.setText(model.getCurrentCityName().getValue());
        }
        if (model.getCurrentCityNumber().getValue() != null) {
            showProgressForNumber(model.getCurrentCityNumber().getValue());
        }

        if (Boolean.TRUE.equals(model.getIndexReady().getValue())) {
            mListener.onIndexBuilt();
        } else if (Boolean.FALSE.equals(model.getIndexInProgress().getValue())) {
            model.createIndex(mCitiesFileName);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        IndexBuilderViewModel model = ViewModelProviders.of(this).get(IndexBuilderViewModel.class);
        model.getIndexReady().removeObservers(this);
        model.getIndexCreationError().removeObservers(this);
        model.getCurrentCityName().removeObservers(this);
        model.getCurrentCityNumber().removeObservers(this);
    }

    @Override
    public void onClick(final View view) {
        if (view.getId() == R.id.btnAbort) {
            mAbortPressCount--;
            if (mAbortPressCount > 0) {
                Toast.makeText(getActivity(), "Press " + mAbortPressCount + " more times to abort.", Toast.LENGTH_LONG).show();
            } else if (mAbortPressCount == 0) {
                IndexBuilderViewModel model = ViewModelProviders.of(this).get(IndexBuilderViewModel.class);
                model.cancelIndexCreation();
                mListener.onIndexBuildError("Canceled");
            }
        }
    }

    private void showProgressForNumber(int number) {
        float progress = ((float) number / (float) mCitiesCount) * 100;
        mTvDetailedProgress.setText(String.format(Locale.US, "%.2f%%", progress));
        mProgressBar.setProgress((int) progress);
    }
}
