package com.rigoni.citiesindex.data;

import android.support.annotation.NonNull;

import com.rigoni.citiesindex.index.IndexTreeEntry;
import com.rigoni.citiesindex.utils.NameNormalizer;


public class City implements IndexTreeEntry, Comparable<City> {
    public class Coordinates {
        private float lon;
        private float lat;

        @Override
        public String toString() {
            return "(Lat: " + lat + " Lon: " + lon + ")";
        }
    }

    private String country;
    private String name;
    private long _id;
    private Coordinates coord;

    private String mNormalizedName;

    @Override
    public String toString() {
        return _id + " " + name + " " + country + " " + coord;
    }

    @Override
    public String getIndexTreeKey() {
        if (mNormalizedName == null) {
            mNormalizedName = new NameNormalizer().normalize(name + ", " + country);
        }
        return mNormalizedName;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public float getLat() {
        return (coord != null ? coord.lat : 0);
    }

    public float getLon() {
        return (coord != null ? coord.lon : 0);
    }

    @Override
    public int compareTo(@NonNull final City city) {
        int compareResult = getIndexTreeKey().compareTo(city.getIndexTreeKey());
        if (compareResult == 0) {
            compareResult = (int) (_id - city._id);
        }
        return compareResult;
    }
}