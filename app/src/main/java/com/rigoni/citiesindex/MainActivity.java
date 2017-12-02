package com.rigoni.citiesindex;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.rigoni.citiesindex.fragments.CityLocationFragment;
import com.rigoni.citiesindex.fragments.IndexBuilderFragment;
import com.rigoni.citiesindex.fragments.ListFragment;
import com.rigoni.citiesindex.fragments.WizardFragment;
import com.rigoni.citiesindex.list.CitiesPagesListAdapter;
import com.rigoni.citiesindex.utils.IndexStorageUtils;

public class MainActivity extends AppCompatActivity implements WizardFragment.WizardFragmentChoiceListener,
        IndexBuilderFragment.IndexBuilderFragmentListener, CitiesPagesListAdapter.OnCityClickedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            if (findViewById(R.id.fragmentContainer) != null) {
                if (IndexStorageUtils.findExistingIndex(this) != null) {
                    showListFragment();
                } else {
                    final WizardFragment wizardFragment = new WizardFragment();
                    getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer, wizardFragment).commit();
                }
            }
        }
    }

    @Override
    public void onWizardChoiceCreateFastIndex() {
        final IndexBuilderFragment builderFragment = new IndexBuilderFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(IndexBuilderFragment.ARGUMENT_CITIES_FILE_NAME, getString(R.string.cities_small_file_name));
        arguments.putInt(IndexBuilderFragment.ARGUMENT_CITIES_COUNT, 50000);
        builderFragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, builderFragment).commit();
    }

    @Override
    public void onWizardChoiceCreateFullIndex() {
        final IndexBuilderFragment builderFragment = new IndexBuilderFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(IndexBuilderFragment.ARGUMENT_CITIES_FILE_NAME, getString(R.string.cities_original_file_name));
        arguments.putInt(IndexBuilderFragment.ARGUMENT_CITIES_COUNT, 209559);
        builderFragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, builderFragment).commit();
    }

    @Override
    public void onIndexBuilt() {
        showListFragment();
    }

    @Override
    public void onIndexBuildError(String message) {
        Toast.makeText(this, "Index was not built: " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void showListFragment() {
        final ListFragment listFragment = new ListFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, listFragment).commit();
    }

    @Override
    public void onCityClicked(String cityName, float lat, float lon) {
        final Bundle arguments = new Bundle();
        arguments.putString(CityLocationFragment.ARGUMENT_CITY_NAME, cityName);
        arguments.putFloat(CityLocationFragment.ARGUMENT_CITY_LAT, lat);
        arguments.putFloat(CityLocationFragment.ARGUMENT_CITY_LON, lon);

        final CityLocationFragment mapFragment = new CityLocationFragment();
        mapFragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, mapFragment)
                .addToBackStack(null).commit();
    }
}
