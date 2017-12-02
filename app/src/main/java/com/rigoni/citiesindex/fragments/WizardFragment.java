package com.rigoni.citiesindex.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rigoni.citiesindex.R;
import com.rigoni.citiesindex.list.CitiesPagesListAdapter;
import com.rigoni.citiesindex.utils.IndexStorageUtils;


public class WizardFragment extends Fragment implements View.OnClickListener {
    public interface WizardFragmentChoiceListener {
        void onWizardChoiceCreateFastIndex();
        void onWizardChoiceCreateFullIndex();
    }

    private WizardFragmentChoiceListener mListener;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!(getActivity() instanceof WizardFragmentChoiceListener)) {
            // Programming error, let's make sure we notice.
            throw new RuntimeException("Activity showing this fragment must implement WizardFragmentChoiceListener");
        }
        mListener = (WizardFragmentChoiceListener) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (IndexStorageUtils.findIndexStorageLocation(getActivity()) == null) {
            getView().findViewById(R.id.btnGenerateIndexFast).setVisibility(View.GONE);
            getView().findViewById(R.id.btnGenerateIndexFull).setVisibility(View.GONE);
            ((TextView)getView().findViewById(R.id.tvMessage)).setText("You need at least " +
                    IndexStorageUtils.MEGABYTES_NEEDED_FOR_INDEX + " free MB in order to build the index.\n" +
                    "Please free up some space and come back.");
        } else {
            getView().findViewById(R.id.btnGenerateIndexFast).setOnClickListener(this);
            getView().findViewById(R.id.btnGenerateIndexFull).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(final View view) {
        if (view.getId() == R.id.btnGenerateIndexFull) {
            mListener.onWizardChoiceCreateFullIndex();
        } else if (view.getId() == R.id.btnGenerateIndexFast) {
            mListener.onWizardChoiceCreateFastIndex();
        }
    }
}
