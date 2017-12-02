package com.rigoni.citiesindex;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.rigoni.citiesindex.data.City;
import com.rigoni.citiesindex.index.IndexTreeStorageFs;
import com.rigoni.citiesindex.index.IndexTree;
import com.rigoni.citiesindex.index.IndexTreeStorage;
import com.rigoni.citiesindex.utils.FsUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class IndexTreeStorageFsTest {
    /** A reasonably high value to use when we want all entries. */
    private static final int COUNT_ALL = 400000;

    private static final String TEST_JSON_ONE_ENTRY =
            "[{\"country\":\"NL\",\"name\":\"Amsterdam\",\"_id\":6455342,\"coord\":{\"lon\":2.33333,\"lat\":48.900002}}]";

    private static final String TEST_JSON_TWO_ENTRIES_SAME_PREFIX =
            "[{\"country\":\"NL\",\"name\":\"Amstelveen\",\"_id\":6455342,\"coord\":{\"lon\":2.33333,\"lat\":48.900002}},\n" +
            "{\"country\":\"NL\",\"name\":\"Amsterdam\",\"_id\":6455342,\"coord\":{\"lon\":2.33333,\"lat\":48.900002}}]";

    private static final int TEST_JSON_SAINT_ENTRIES = 16;
    private static final String TEST_JSON_SAINT =
            "[{\"country\":\"FR\",\"name\":\"Saint-Ouen\",\"_id\":6455342,\"coord\":{\"lon\":2.33333,\"lat\":48.900002}},\n" +
            "{\"country\":\"FR\",\"name\":\"Saint-Fons\",\"_id\":6441760,\"coord\":{\"lon\":4.86667,\"lat\":45.700001}},\n" +
            "{\"country\":\"FR\",\"name\":\"Saint-Avold\",\"_id\":6454369,\"coord\":{\"lon\":6.7,\"lat\":49.099998}},\n" +
            "{\"country\":\"GP\",\"name\":\"Sainte-Anne\",\"_id\":6690393,\"coord\":{\"lon\":-61.366669,\"lat\":16.23333}},\n" +
            "{\"country\":\"US\",\"name\":\"Saint Charles County\",\"_id\":4406835,\"coord\":{\"lon\":-90.733459,\"lat\":38.76672}},\n" +
            "{\"country\":\"FR\",\"name\":\"Saint-Jean-de-la-Ruelle\",\"_id\":6434715,\"coord\":{\"lon\":1.86667,\"lat\":47.916672}},\n" +
            "{\"country\":\"FR\",\"name\":\"Saint-Herblain\",\"_id\":6434515,\"coord\":{\"lon\":-1.65,\"lat\":47.216671}},\n" +
            "{\"country\":\"FR\",\"name\":\"Saint-Étienne\",\"_id\":6614010,\"coord\":{\"lon\":4.4,\"lat\":45.433331}},\n" +
            "{\"country\":\"FR\",\"name\":\"Saint-Cyr-sur-Loire\",\"_id\":6433106,\"coord\":{\"lon\":0.66667,\"lat\":47.400002}},\n" +
            "{\"country\":\"IN\",\"name\":\"Sainthia\",\"_id\":1257751,\"coord\":{\"lon\":87.666672,\"lat\":23.950001}},\n" +
            "{\"country\":\"RU\",\"name\":\"Saint Petersburg\",\"_id\":498817,\"coord\":{\"lon\":30.264168,\"lat\":59.894444}},\n" +
            "{\"country\":\"CH\",\"name\":\"Saint-Livres\",\"_id\":2658867,\"coord\":{\"lon\":6.38753,\"lat\":46.507938}},\n" +
            "{\"country\":\"FR\",\"name\":\"Saint-Laurent\",\"_id\":2978954,\"coord\":{\"lon\":4.77193,\"lat\":49.764488}},\n" +
            "{\"country\":\"FR\",\"name\":\"Saint-Bris-le-Vineux\",\"_id\":2981274,\"coord\":{\"lon\":3.64922,\"lat\":47.743961}},\n" +
            "{\"country\":\"CA\",\"name\":\"Saint-Bernard-de-Lacolle\",\"_id\":6137509,\"coord\":{\"lon\":-73.415863,\"lat\":45.083382}},\n" +
            "{\"country\":\"CA\",\"name\":\"Sainte-Marguerite\",\"_id\":6944114,\"coord\":{\"lon\":-67.083893,\"lat\":48.29998}}]";

    private Context mContext;
    private File mIndexDirectory;
    // The instance under test
    private IndexTreeStorage mIndexTreeStorage;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();

        mIndexDirectory = new File(mContext.getExternalCacheDir() + File.separator + "test_index");
        mIndexTreeStorage = new IndexTreeStorageFs<City>(City.class, mIndexDirectory.getAbsolutePath(), true);
    }

    @After
    public void tearDown() {
        FsUtils.deleteDirectory(mIndexDirectory);
    }

    @Test
    public void testAddSingleCity() {
        // Given a storage with one single city
        addEntriesFromJson(TEST_JSON_ONE_ENTRY, mIndexTreeStorage);

        // When the storage is queried with the corresponding prefix
        final List<City> cities = new ArrayList<>();
        cities.addAll(mIndexTreeStorage.getEntriesListAtSubPath("a/m/s", 0, COUNT_ALL));

        // Then the correct city should be returned
        assertEquals(1, cities.size());
        assertEquals("Amsterdam", cities.get(0).getName());
    }

    @Test
    public void testAddTwoCitiesSameSubPath() {
        // Given a storage with two cities sharing the same subpath
        addEntriesFromJson(TEST_JSON_TWO_ENTRIES_SAME_PREFIX, mIndexTreeStorage);

        // When the storage is queried with the corresponding prefix
        final List<City> cities = new ArrayList<>();
        cities.addAll(mIndexTreeStorage.getEntriesListAtSubPath("a/m/s", 0, COUNT_ALL));
        assertEquals(2, cities.size());

        // Then the two cities should be returned and should be sorted
        assertEquals("Amstelveen", cities.get(0).getName());
        assertEquals("Amsterdam", cities.get(1).getName());
    }

    @Test
    public void testAddMultipleCitiesSamePath() {
        // Given a storage with a known list of cities sharing the same prefix
        mIndexTreeStorage.initiateBulkInsert();
        addEntriesFromJson(TEST_JSON_SAINT, mIndexTreeStorage);
        mIndexTreeStorage.finalizeBulkInsert();

        // When the storage is queried using the prefix
        final List<City> cities = new ArrayList<>();
        cities.addAll(mIndexTreeStorage.getEntriesListAtSubPath("s/a/i/", 0, COUNT_ALL));

        // Then the size of the returned list should match the size of the known list
        assertEquals(TEST_JSON_SAINT_ENTRIES, cities.size());
    }

    @Test
    public void testGetEntriesCountAtSubpath() {
        // Given a storage with a known list of cities sharing the same prefix
        mIndexTreeStorage.initiateBulkInsert();
        addEntriesFromJson(TEST_JSON_SAINT, mIndexTreeStorage);
        mIndexTreeStorage.finalizeBulkInsert();
        // When the amount of entries sharing the prefix is requested
        final int count = mIndexTreeStorage.getEntriesCountAtSubPath("s/a/i/");
        // Then the amount of entries returned should match the size of the known list
        assertEquals(TEST_JSON_SAINT_ENTRIES, count);
    }

    @Test
    public void testDeleteIndex() {
        // Given storage populated with a known list of cities sharing the same prefix
        mIndexTreeStorage.initiateBulkInsert();
        addEntriesFromJson(TEST_JSON_SAINT, mIndexTreeStorage);
        mIndexTreeStorage.finalizeBulkInsert();
        // When deletion of the storage is requested
        mIndexTreeStorage.deleteIndex();
        // Then a query with the known prefix should return zero items
        final int count = mIndexTreeStorage.getEntriesCountAtSubPath("s/a/i/");
        assertEquals(0, count);
    }

    private void addEntriesFromJson(final String json, final IndexTreeStorage storage) {
        JsonElement element = new JsonParser().parse(json);
        JsonArray array = element.getAsJsonArray();
        final Gson gson = new GsonBuilder().create();

        for (int i = 0; i < array.size(); i++) {
            final City city = gson.fromJson(array.get(i), City.class);
            storage.addEntryAtSubPath(IndexTree.createRelativePathFromFilter(city.getIndexTreeKey()), city);
        }
    }
}
