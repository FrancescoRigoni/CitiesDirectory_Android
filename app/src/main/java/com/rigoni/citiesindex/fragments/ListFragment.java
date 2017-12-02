package com.rigoni.citiesindex.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.rigoni.citiesindex.model.CitiesListViewModel;
import com.rigoni.citiesindex.R;
import com.rigoni.citiesindex.list.CitiesPagesListAdapter;
import com.rigoni.citiesindex.task.UpdateListFilterTask;


public class ListFragment extends Fragment implements TextWatcher, UpdateListFilterTask.UpdateListFilterTaskListener {
    private RecyclerView mRecyclerView;
    private EditText mEtFilter;
    private View mFilteringLayout;
    private View mHintLayout;

    private CitiesListViewModel mViewModel;
    private CitiesPagesListAdapter mAdapter;

    private UpdateListFilterTask mUpdateFilterTask;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!(getActivity() instanceof CitiesPagesListAdapter.OnCityClickedListener)) {
            // Programming error, let's make sure we notice.
            throw new RuntimeException("Activity showing this fragment must implement OnCityClickedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, final Bundle savedInstanceState) {
        mViewModel = ViewModelProviders.of(this).get(CitiesListViewModel.class);
        mAdapter = new CitiesPagesListAdapter();
        mAdapter.setOnCityClickedListener((CitiesPagesListAdapter.OnCityClickedListener) getActivity());

        final RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.list_fragment, container, false);
        mFilteringLayout = view.findViewById(R.id.llPleaseWait);
        mHintLayout = view.findViewById(R.id.llInsertFilter);
        mRecyclerView = view.findViewById(R.id.recyclerView);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEtFilter = view.findViewById(R.id.etFilter);
        mEtFilter.addTextChangedListener(this);

        if (savedInstanceState != null) {
            final String filter = savedInstanceState.getString("filter", "");
            updateFilter(filter);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putString("filter", mEtFilter.getText().toString());
    }

    @Override
    public void beforeTextChanged(final CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void afterTextChanged(final Editable editable) {}

    @Override
    public void onTextChanged(final CharSequence charSequence, int i, int i1, int i2) {
        final String filterString = mEtFilter.getText().toString();
        updateFilter(mEtFilter.getText().toString());
        if (TextUtils.isEmpty(filterString)) {
            showHintLayout();
        }
    }

    private void updateFilter(@NonNull final String filter) {
        if (mUpdateFilterTask != null && !mUpdateFilterTask.isCancelled()) {
            mUpdateFilterTask.cancel(true);
        }
        if (!TextUtils.isEmpty(filter)) {
            // We never show the full list of cities, at least one character is required.
            mUpdateFilterTask = new UpdateListFilterTask(this, filter, mViewModel);
            mUpdateFilterTask.execute();
        }
    }

    private void showHintLayout() {
        mFilteringLayout.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.GONE);
        mHintLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFilterInProgress() {
        mFilteringLayout.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mHintLayout.setVisibility(View.GONE);
    }

    @Override
    public void onFilterComplete() {
        mAdapter.setList(mViewModel.getList());
        mRecyclerView.setAdapter(mAdapter);
        mFilteringLayout.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mHintLayout.setVisibility(View.GONE);
    }
}
