package com.rigoni.citiesindex;

import com.rigoni.citiesindex.utils.NameNormalizer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NameNormalizerTest {
    private static final String FORBIDDEN_CHARS = "[|?*.,<>\":+]'/’ ";

    private NameNormalizer mNameNormalizer;

    @Before
    public void createNormalizer() {
        mNameNormalizer = new NameNormalizer();
    }

    @Test
    public void testStringContainingNoForbiddenCharacter() throws Exception {
        final String input = "Amsterdam";
        final String expected = "amsterdam";
        assertEquals(expected, mNameNormalizer.normalize(input));
    }

    @Test
    public void testStringContainingOneForbiddenCharacter() throws Exception {
        for (int i = 0; i < FORBIDDEN_CHARS.length(); i++) {
            final String expected = "amsterdam_";
            final String input = "Amsterdam" + FORBIDDEN_CHARS.charAt(i);
            assertEquals(expected, mNameNormalizer.normalize(input));
        }
        for (int i = 0; i < FORBIDDEN_CHARS.length(); i++) {
            final String expected = "_amsterdam";
            final String input = FORBIDDEN_CHARS.charAt(i) + "Amsterdam";
            assertEquals(expected, mNameNormalizer.normalize(input));
        }
        for (int i = 0; i < FORBIDDEN_CHARS.length(); i++) {
            final String expected = "amster_dam";
            final String input = "Amster" + FORBIDDEN_CHARS.charAt(i) + "Dam";
            assertEquals(expected, mNameNormalizer.normalize(input));
        }
    }

    @Test
    public void testStringContainingManyForbiddenCharacter() throws Exception {
        for (int i = 0; i < FORBIDDEN_CHARS.length(); i++) {
            final String expected = "amsterdam_";
            final String input = "Amsterdam" + FORBIDDEN_CHARS.charAt(i) + FORBIDDEN_CHARS.charAt(i);
            assertEquals(expected, mNameNormalizer.normalize(input));
        }
        for (int i = 0; i < FORBIDDEN_CHARS.length(); i++) {
            final String expected = "_amsterdam";
            final String input = "" + FORBIDDEN_CHARS.charAt(i) + FORBIDDEN_CHARS.charAt(i) + "Amsterdam";
            assertEquals(expected, mNameNormalizer.normalize(input));
        }
        for (int i = 0; i < FORBIDDEN_CHARS.length(); i++) {
            final String expected = "amster_dam";
            final String input = "Amster" + FORBIDDEN_CHARS.charAt(i) + FORBIDDEN_CHARS.charAt(i) + "Dam";
            assertEquals(expected, mNameNormalizer.normalize(input));
        }
    }

    @Test
    public void testStringContainingOneSpace() throws Exception {
        String input = " Amsterdam";
        assertEquals("_amsterdam", mNameNormalizer.normalize(input));
        input = "Amsterdam ";
        assertEquals("amsterdam_", mNameNormalizer.normalize(input));
        input = "Amster Dam";
        assertEquals("amster_dam", mNameNormalizer.normalize(input));
    }

    @Test
    public void testStringContainingManySpaces() throws Exception {
        String input = "  Amsterdam";
        assertEquals("_amsterdam", mNameNormalizer.normalize(input));
        input = "Amsterdam  ";
        assertEquals("amsterdam_", mNameNormalizer.normalize(input));
        input = "Amster  Dam";
        assertEquals("amster_dam", mNameNormalizer.normalize(input));
    }
}