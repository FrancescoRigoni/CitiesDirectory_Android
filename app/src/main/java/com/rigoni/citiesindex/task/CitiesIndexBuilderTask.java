package com.rigoni.citiesindex.task;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

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

    private static CitiesIndexBuilderTask sTask;

    private IndexTree mIndexTree;
    private IndexBuilderTaskListener mListener;
    private InputStream mInputStream;
    private boolean mCompleted;
    private String mTaskErrorMessage;

    /**
     * Creates and starts a new {@link CitiesIndexBuilderTask}
     * @param inputStream inputStream from which items are read
     * @param indexTree the indexTree t o be used
     * @param listener mandatory {@link IndexBuilderTaskListener} implementation.
     */
    public static void startTask(@NonNull final InputStream inputStream,
                                 @NonNull final IndexTree indexTree,
                                 @NonNull final IndexBuilderTaskListener listener) {
        sTask = new CitiesIndexBuilderTask();
        sTask.mInputStream = inputStream;
        sTask.mIndexTree = indexTree;
        sTask.mListener = listener;
        sTask.execute();
    }

    /**
     * Stops the currently running {@link CitiesIndexBuilderTask}
     */
    public static void stopTask() {
        if (sTask != null && !sTask.isCancelled()) {
            sTask.cancel(false);
        }
    }

    public static void updateListener(@Nullable final IndexBuilderTaskListener listener) {
        if (sTask != null) {
            sTask.mListener = listener;
        }
    }

    public static boolean isTaskRunning() {
        return (sTask != null && !sTask.isCancelled() && !sTask.mCompleted);
    }

    public static boolean isTaskCompleted() {
        return (sTask != null && !sTask.isCancelled() && sTask.mCompleted);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            mIndexTree.initiateBulkInsert();

            final JsonReader reader = new JsonReader(new InputStreamReader(mInputStream, "UTF-8"));
            final Gson gson = new GsonBuilder().create();
            reader.beginArray();
            int count = 0;
            while (reader.hasNext()) {
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
        mCompleted = true;
    }
}
