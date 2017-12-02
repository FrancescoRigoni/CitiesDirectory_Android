package com.rigoni.citiesindex.utils;

import java.util.regex.Pattern;

public class NameNormalizer {
    private static final String EXCLUDED_CHARS_REGEXP = "[\\[|?*.,<>\":+\\]'/’ ]+";
    private static final Pattern EXCLUDED_CHARS_PATTERN = Pattern.compile(EXCLUDED_CHARS_REGEXP);

    /**
     * Normalizes the name to make it suitable for insertion in the index tree.
     */
    public String normalize(final String name) {
        String normalized = name.toLowerCase();
        normalized = EXCLUDED_CHARS_PATTERN.matcher(normalized).replaceAll("_");
        return normalized;
    }
}
