package com.rigoni.citiesindex.fragments;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class CityLocationFragment extends SupportMapFragment implements OnMapReadyCallback {
    public static final String ARGUMENT_CITY_LAT = "CITY_LAT";
    public static final String ARGUMENT_CITY_LON = "CITY_LON";
    public static final String ARGUMENT_CITY_NAME = "CITY_NAME";

    private float mLatitude;
    private float mLongitude;
    private String mName;

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null &&
                getArguments().containsKey(ARGUMENT_CITY_LAT) &&
                getArguments().containsKey(ARGUMENT_CITY_LON) &&
                getArguments().containsKey(ARGUMENT_CITY_NAME)) {
            mLatitude = getArguments().getFloat(ARGUMENT_CITY_LAT);
            mLongitude = getArguments().getFloat(ARGUMENT_CITY_LON);
            mName = getArguments().getString(ARGUMENT_CITY_NAME);
        }

        getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        final LatLng cityCoordinates = new LatLng(mLatitude, mLongitude);
        googleMap.addMarker(new MarkerOptions().position(cityCoordinates).title(mName));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(cityCoordinates));
    }
}
