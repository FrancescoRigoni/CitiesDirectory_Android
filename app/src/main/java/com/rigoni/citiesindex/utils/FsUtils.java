package com.rigoni.citiesindex.utils;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class FsUtils {
    /**
     * Deletes a directory and all of its content.
     * @param directory the directory to delete.
     */
    public static void deleteDirectory(@NonNull final File directory) {
        Preconditions.checkNotNull(directory);

        if (directory.exists() && directory.isDirectory()) {
            final File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    }
                    file.delete();
                }
            }
            directory.delete();
        }
    }
}
