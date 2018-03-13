package com.rigoni.citiesindex.task;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.rigoni.citiesindex.data.City;
import com.rigoni.citiesindex.index.IndexTree;
import com.rigoni.citiesindex.utils.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;


public class CitiesIndexBuilderTask extends AsyncTask<Void, Pair<Integer, String>, Void> {
    public interface IndexBuilderTaskListener {
        void onIndexCreated();
        void onIndexProgress(final int count, final String currentCity);
        void onIndexError(final String message);
        void onIndexBuildCancelled();
    }

    private IndexTree mIndexTree;
    private IndexBuilderTaskListener mListener;
    private InputStream mInputStream;
    private String mTaskErrorMessage;

    public CitiesIndexBuilderTask(@NonNull final InputStream inputStream,
                                  @NonNull final IndexTree indexTree,
                                  @NonNull final IndexBuilderTaskListener listener) {
        Preconditions.checkNotNull(inputStream);
        Preconditions.checkNotNull(indexTree);
        Preconditions.checkNotNull(listener);

        mInputStream = inputStream;
        mIndexTree = indexTree;
        mListener = listener;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            mIndexTree.initiateBulkInsert();

            final JsonReader reader = new JsonReader(new InputStreamReader(mInputStream, "UTF-8"));
            final Gson gson = new GsonBuilder().create();
            reader.beginArray();
            int count = 0;
            while (reader.hasNext() && !isCancelled()) {
                final City city = gson.fromJson(reader, City.class);
                mIndexTree.addEntry(city);
                count++;
                publishProgress(new Pair<>(count, city.getName()));
                if (isCancelled()) {
                    mIndexTree.delete();
                    return null;
                }
            }
            reader.close();
            mIndexTree.finalizeBulkInsert();
        } catch (UnsupportedEncodingException ex) {
            mTaskErrorMessage = "UnsupportedEncodingException: " + ex.getMessage();
            mIndexTree.delete();
        } catch (IOException ex) {
            mTaskErrorMessage = "IOException: " + ex.getMessage();
            mIndexTree.delete();
        } finally {
            StreamUtils.closeInputStreamNoThrow(mInputStream);
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        if (mListener != null) {
            mIndexTree.delete();
            mListener.onIndexBuildCancelled();
        }
    }

    @Override
    protected void onProgressUpdate(final Pair<Integer, String>... values) {
        if (mListener != null) {
            mListener.onIndexProgress(values[0].first, values[0].second);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mListener != null) {
            if (mTaskErrorMessage != null) {
                mListener.onIndexError(mTaskErrorMessage);
            } else {
                mListener.onIndexCreated();
            }
        }
    }
}
