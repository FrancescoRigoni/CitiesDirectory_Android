package com.rigoni.citiesindex.list;

import android.arch.paging.PagedListAdapter;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.DiffCallback;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.rigoni.citiesindex.R;
import com.rigoni.citiesindex.data.City;
import com.rigoni.citiesindex.index.IndexTreeEntry;

public class CitiesPagesListAdapter extends PagedListAdapter<IndexTreeEntry, CitiesPagesListAdapter.CityViewHolder> {
    /**
     * Listener to be invoked when a city entry is clicked.
     */
    public interface OnCityClickedListener {
        void onCityClicked(final String cityName, final float lat, final float lon);
    }

    public CitiesPagesListAdapter() {
        super(new DiffCallback<IndexTreeEntry>() {
            @Override
            public boolean areItemsTheSame(@NonNull IndexTreeEntry oldItem, @NonNull IndexTreeEntry newItem) {
                return oldItem.getIndexTreeKey().equals(newItem.getIndexTreeKey());
            }

            @Override
            public boolean areContentsTheSame(@NonNull IndexTreeEntry oldItem, @NonNull IndexTreeEntry newItem) {
                return oldItem.getIndexTreeKey().equals(newItem.getIndexTreeKey());
            }
        });
    }

    private OnCityClickedListener mOnCityClickedListener;

    /**
     * Sets the listener to be invoked when a city entry is clicked.
     * @param listener the listener, or null to unset it.
     */
    public void setOnCityClickedListener(@NonNull final OnCityClickedListener listener) {
        Preconditions.checkNotNull(listener);
        mOnCityClickedListener = listener;
    }

    @Override
    public CityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.city_list_item, parent, false);
        view.setClickable(true);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        return new CityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CityViewHolder holder, final int position) {
        City city = (City) getItem(position);
        if (city != null) {
            holder.bindTo(city);
        }
    }

    class CityViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private City mCity;
        private TextView mTvCityName;

        CityViewHolder(final View itemView) {
            super(itemView);
            mTvCityName = itemView.findViewById(R.id.tvCityName);
        }

        void bindTo(final IndexTreeEntry entry) {
            mCity = (City) entry;
            mTvCityName.setText(mCity.getName() + ", " + mCity.getCountry());
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(final View view) {
            if (mCity != null) {
                mOnCityClickedListener.onCityClicked(mCity.getName(), mCity.getLat(), mCity.getLon());
            }
        }
    }
}
